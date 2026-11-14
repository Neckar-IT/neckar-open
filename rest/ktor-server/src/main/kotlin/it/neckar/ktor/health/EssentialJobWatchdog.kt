/*
 * Copyright (C) 2013-2026 Neckar IT GmbH, Mössingen, Germany
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Linking this library statically or dynamically with other modules is
 * making a combined work based on this library. Thus, the terms and
 * conditions of the GNU General Public License cover the whole combination.
 *
 * As a special exception, the copyright holders of this library give you
 * permission to link this library with independent modules, regardless of
 * the license terms of these independent modules, and to copy and distribute
 * the resulting combined work under terms of your choice, provided that every
 * copy of the combined work is accompanied by a complete copy of the source
 * code of this library.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package it.neckar.ktor.health

import it.neckar.logging.Logger
import it.neckar.logging.LoggerFactory
import it.neckar.open.dispose.Disposable
import it.neckar.open.health.BackgroundJobRegistry
import it.neckar.open.health.BrokenBackgroundJob
import it.neckar.open.time.monotonicTimeSource
import java.util.concurrent.Executors
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import kotlin.time.ComparableTimeMark
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Hands every essential background job that stopped cycling ([BackgroundJobRegistry.brokenJobs]) to
 * [reaction] — every [checkInterval], and at once when a job's supervisor ends.
 *
 * Runs on a thread of its own, outside the coroutine dispatchers: the jobs it judges may have blocked
 * exactly those.
 */
class EssentialJobWatchdog(
  private val reaction: StallReaction,
  private val checkInterval: Duration = 30.seconds,
) {
  init {
    require(checkInterval < BackgroundJobRegistry.LoopIdleMargin) { "checkInterval $checkInterval must stay below the idle margin ${BackgroundJobRegistry.LoopIdleMargin}, or a freeze it misses exceeds it" }
  }

  private val executor: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor { runnable ->
    Thread(runnable, "essential-job-watchdog").apply { isDaemon = true }
  }

  /**
   * Confined to [executor].
   */
  private val freezeDetector: FreezeDetector = FreezeDetector(checkInterval)

  @Volatile
  private var jobStalledRegistration: Disposable? = null

  fun start() {
    jobStalledRegistration = BackgroundJobRegistry.onJobStalled { job ->
      try {
        executor.execute { report(listOf(job)) }
      } catch (_: RejectedExecutionException) {
        //The job ended in the moment stop() shut the executor down
      }
    }
    executor.execute { markStarted() }
    executor.scheduleWithFixedDelay({ check() }, checkInterval.inWholeMilliseconds, checkInterval.inWholeMilliseconds, TimeUnit.MILLISECONDS)
  }

  /**
   * The first tick: a freeze before the first check is a freeze too. Internal for tests.
   */
  internal fun markStarted(now: ComparableTimeMark = monotonicTimeSource.markNow()) {
    check(freezeDetector.frozenBefore(now) == null) { "the watchdog started twice" }
  }

  /**
   * Idempotent.
   */
  fun stop() {
    jobStalledRegistration?.dispose()
    jobStalledRegistration = null
    executor.shutdownNow()
  }

  /**
   * One check at [now]. Internal for tests, which drive it with a virtual clock.
   */
  internal fun check(now: ComparableTimeMark = monotonicTimeSource.markNow()) {
    //A throwing task ends a ScheduledExecutorService's repetition for good
    try {
      val frozenFor = freezeDetector.frozenBefore(now)
      if (frozenFor != null) {
        logger.warn("The process was frozen for $frozenFor - the background jobs are judged from now on")
        BackgroundJobRegistry.discountPause(frozenFor, now)
        return
      }
      report(BackgroundJobRegistry.brokenJobs(now))
    } catch (e: Throwable) {
      logger.error("Checking the essential background jobs failed", e)
    }
  }

  private fun report(brokenJobs: List<BrokenBackgroundJob>) {
    brokenJobs.forEach { reaction.stalled(stall = it.name.value, cause = "Essential background job ${it.description}") }
  }

  private companion object {
    val logger: Logger = LoggerFactory.getLogger("it.neckar.ktor.health.EssentialJobWatchdog")
  }
}
