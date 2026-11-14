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
package it.neckar.open.health

import it.neckar.logging.Logger
import it.neckar.logging.LoggerFactory
import it.neckar.open.async.PlatformReadWriteLock
import it.neckar.open.async.read
import it.neckar.open.async.write
import it.neckar.open.concurrent.repeating.RepeatingEndReason
import it.neckar.open.dispose.Disposable
import it.neckar.open.kotlin.lang.requireNotNull
import it.neckar.open.time.monotonicTimeSource
import it.neckar.open.time.now
import it.neckar.open.unit.other.GuardedBy
import kotlin.time.ComparableTimeMark
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

/**
 * Process-wide registry of the essential background jobs of a service and their liveness.
 *
 * Answers the one question `restart: unless-stopped` and the HTTP probes cannot: *is the job the
 * service exists for still doing its work?* A dead or wedged polling job leaves the process alive and
 * the HTTP server answering `200`, so nothing restarts and nothing alerts.
 *
 * Process-wide on purpose, like the `GlobalExceptionCollector`: a job records its cycles where it
 * runs, without a holder threaded through every constructor from the service to the job. The recorded
 * states reach an operator through `GET /health` and through the OpenTelemetry gauge
 * (`recordBackgroundJobsToOpenTelemetry`), which is what the Prometheus alert fires on.
 *
 * Recording is decoupled from restarting: this registry only answers [brokenJobs]; the service's
 * watchdog decides what a stalled job means for the process. Thread-safe.
 *
 * Not to be confused with `it.neckar.heartbeat.Heartbeat`, which points the other way: that one polls
 * a *remote* service and reports whether it answers. This registry observes the jobs of the process it
 * runs in — the very thing an HTTP probe from outside cannot see.
 */
object BackgroundJobRegistry {
  private val lock = PlatformReadWriteLock()

  @GuardedBy("lock")
  private val jobStates: MutableMap<BackgroundJobName, BackgroundJobState> = linkedMapOf()

  @GuardedBy("lock")
  private val jobStalledObservers: MutableList<(BrokenBackgroundJob) -> Unit> = mutableListOf()

  /**
   * The state of every registered job, in registration order.
   */
  val states: List<BackgroundJobState>
    get() = lock.read { jobStates.values.toList() }

  /**
   * The state of [name], or `null` when no job registered under that name.
   */
  fun stateOf(name: BackgroundJobName): BackgroundJobState? = lock.read { jobStates[name] }

  /**
   * Declares the job [name] with its [stalenessThreshold] and starts its staleness clock. Called once
   * when the service starts its job — a job records nothing before it is registered.
   *
   * Re-registering an already known job keeps its recorded cycles and only adopts the new threshold:
   * a job whose loop restarts must not reset the staleness clock, or a job that never recovers would
   * look healthy after every restart.
   */
  fun register(name: BackgroundJobName, stalenessThreshold: Duration) {
    lock.write {
      val existing = jobStates[name]
      jobStates[name] = existing?.copy(stalenessThreshold = stalenessThreshold)
        ?: BackgroundJobState(name = name, stalenessThreshold = stalenessThreshold, registeredMark = monotonicTimeSource.markNow())
    }
  }

  /**
   * Records that a cycle of [name] started. Diagnostic only — it does not affect staleness, because a
   * cycle that starts and then hangs is exactly the failure this registry exposes.
   */
  fun recordCycleStarted(name: BackgroundJobName, run: BackgroundJobRun? = null) {
    update(name, run) { it.copy(lastCycleStartedAt = now(), runningCycleStartedMark = monotonicTimeSource.markNow()) }
  }

  /**
   * Records that a cycle of [name] completed successfully. This is the liveness signal: it resets the
   * staleness clock.
   */
  fun recordCycleCompleted(name: BackgroundJobName, run: BackgroundJobRun? = null) {
    val mark = monotonicTimeSource.markNow()
    update(name, run) { it.copy(lastCycleCompletedAt = now(), lastCycleCompletedMark = mark, lastCycleEndedMark = mark, runningCycleStartedMark = null, completedCycles = it.completedCycles + 1) }
  }

  /**
   * Records that a cycle of [name] failed with [error] (threw, or exceeded its cycle timeout). Counts
   * the failure and keeps the last completed cycle, so the staleness clock keeps running.
   */
  fun recordCycleFailed(name: BackgroundJobName, error: Throwable, run: BackgroundJobRun? = null) {
    update(name, run) { it.copy(lastFailure = BackgroundJobFailure(error, now()), failedCycles = it.failedCycles + 1, lastCycleEndedMark = monotonicTimeSource.markNow(), runningCycleStartedMark = null) }
  }

  /**
   * Records that the running cycle of [name] was cancelled with its job. Counts as neither completed
   * nor failed; it only ends the running cycle.
   */
  fun recordCycleCancelled(name: BackgroundJobName, run: BackgroundJobRun? = null) {
    update(name, run) { it.copy(lastCycleEndedMark = monotonicTimeSource.markNow(), runningCycleStartedMark = null) }
  }

  /**
   * Records that the job [name] started with [cycleTimeout] and a loop whose longest pause is
   * [maxGap], and forgets the end of any previous start. Returns the run the job's later records carry.
   *
   * Throws when the job cannot be judged: without a cycle timeout a hung cycle is indistinguishable
   * from a long one, and a staleness threshold not above [maxGap] reports a healthy job as stalled.
   */
  @IgnorableReturnValue
  fun recordJobStarted(name: BackgroundJobName, cycleTimeout: Duration?, maxGap: Duration): BackgroundJobRun {
    return lock.write {
      val existing = jobStates[name].requireNotNull { "background job [$name] started without being registered" }
      require(cycleTimeout != null) { "essential background job [$name] needs a cycleTimeout" }
      require(existing.stalenessThreshold > maxGap + LoopIdleMargin) {
        "essential background job [$name] has a stalenessThreshold of ${existing.stalenessThreshold}, which must exceed the longest pause of its loop ($maxGap) by more than $LoopIdleMargin"
      }
      val run = existing.run?.next() ?: BackgroundJobRun.First
      jobStates[name] = existing.copy(run = run, cycleTimeout = cycleTimeout, jobStartedMark = monotonicTimeSource.markNow(), runningCycleStartedMark = null, endReason = null)
      run
    }
  }

  /**
   * Records that the supervisor of [name] ended for [reason] and tells the observers of
   * [onJobStalled] when that leaves the job stalled.
   */
  fun recordJobEnded(name: BackgroundJobName, reason: RepeatingEndReason, run: BackgroundJobRun? = null) {
    if (update(name, run) { it.copy(endReason = reason) }.not()) {
      return
    }

    val stalled = stateOf(name)?.stallAt() ?: return
    lock.read { jobStalledObservers.toList() }.forEach { observer ->
      //One failing observer must not keep the stall from the others
      try {
        observer(stalled)
      } catch (e: Throwable) {
        logger.error("Observer of the stalled background job [$name] failed", e)
      }
    }
  }

  /**
   * Calls [observer] the moment a job ends in a way [brokenJobs] reports — without waiting for the
   * next poll of a watchdog.
   */
  fun onJobStalled(observer: (BrokenBackgroundJob) -> Unit): Disposable {
    lock.write { jobStalledObservers.add(observer) }
    return Disposable { lock.write { jobStalledObservers.remove(observer) } }
  }

  /**
   * The started jobs that stopped cycling at [now]. See [BackgroundJobState.stallAt].
   */
  fun brokenJobs(now: ComparableTimeMark = monotonicTimeSource.markNow()): List<BrokenBackgroundJob> = states.mapNotNull { it.stallAt(now) }

  /**
   * Moves the marks of every job forward by [pause] — the time the process was frozen (a VM pause, a
   * `SIGSTOP`), during which no job could cycle. See [BackgroundJobState.discountPause].
   */
  fun discountPause(pause: Duration, now: ComparableTimeMark = monotonicTimeSource.markNow()) {
    lock.write {
      jobStates.replaceAll { _, state -> state.discountPause(pause, now) }
    }
  }

  /**
   * The registered jobs that have been silent longer than their threshold at [now].
   */
  fun staleJobs(now: ComparableTimeMark = monotonicTimeSource.markNow()): List<BackgroundJobState> = states.filter { it.isStale(now) }

  /**
   * Forgets every registered job. For tests — a production process keeps its jobs for its lifetime.
   */
  fun clear() {
    lock.write {
      jobStates.clear()
    }
  }

  /**
   * Applies [transform] to the state of [name] and returns whether it did.
   *
   * Drops a record of an earlier [run]: a cancelled job reports its end after its successor started
   * under the same name. Logs and drops a record for an unregistered name: without a registration
   * there is no staleness threshold to judge the job by, and inventing one would produce an alert
   * nobody can reason about.
   */
  @IgnorableReturnValue
  private fun update(name: BackgroundJobName, run: BackgroundJobRun?, transform: (BackgroundJobState) -> BackgroundJobState): Boolean {
    val outcome = lock.write {
      val existing = jobStates[name] ?: return@write UpdateOutcome.Unregistered
      if (run != null && run != existing.run) {
        return@write UpdateOutcome.EarlierRun
      }
      jobStates[name] = transform(existing)
      UpdateOutcome.Applied
    }

    return when (outcome) {
      UpdateOutcome.Applied -> true
      UpdateOutcome.EarlierRun -> false
      UpdateOutcome.Unregistered -> {
        logger.warn("Ignoring cycle record for unregistered background job [$name]")
        false
      }
    }
  }

  private enum class UpdateOutcome {
    Applied,
    EarlierRun,
    Unregistered,
  }

  /**
   * What a job needs beyond its longest pause before it counts as idle: the supervisor revives a dead
   * worker every 10 s, and a watchdog discounts only a freeze longer than its 30 s check interval.
   */
  val LoopIdleMargin: Duration = 1.minutes

  private val logger: Logger = LoggerFactory.getLogger("it.neckar.open.health.BackgroundJobRegistry")
}
