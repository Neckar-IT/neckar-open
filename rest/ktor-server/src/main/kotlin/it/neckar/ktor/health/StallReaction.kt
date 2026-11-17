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

import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopPreparing
import io.ktor.util.AttributeKey
import it.neckar.logging.Logger
import it.neckar.logging.LoggerFactory
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.Volatile

/**
 * Where the process start leaves the [StallReaction] for the watchdogs `installBackgroundJobs` creates.
 */
val StallReactionAttribute: AttributeKey<StallReaction> = AttributeKey("StallReaction")

/**
 * Hands [stallReaction] to the watchdogs of this application and ends its judging when the
 * application begins to stop — before `ApplicationStopping` cancels the jobs, whose end is then the
 * shutdown and no stall.
 */
fun Application.installStallReaction(stallReaction: StallReaction) {
  attributes.put(StallReactionAttribute, stallReaction)
  monitor.subscribe(ApplicationStopPreparing) { stallReaction.serviceStopping() }
}

/**
 * The one reaction of every watchdog of a service to a stall — logged, and in [WatchdogMode.Exit] the
 * end of the process. Shared by the watchdogs of one server so a stop reaches all of them at once.
 */
class StallReaction(
  val mode: WatchdogMode,
  /**
   * Ends the process in [WatchdogMode.Exit].
   */
  private val processExit: ProcessExit = ProcessExit.Jvm,
) {
  @Volatile
  private var stopping: Boolean = false

  /**
   * The stalls already logged in [WatchdogMode.LogOnly] and [WatchdogMode.DebuggerAttached]: each logs
   * once per process instead of on every check.
   */
  private val reportedStalls: MutableSet<String> = ConcurrentHashMap.newKeySet()

  /**
   * Set by the first stall in [WatchdogMode.Exit]; further stalls while the process ends log no second dump.
   */
  private val exiting: AtomicBoolean = AtomicBoolean(false)

  /**
   * The service stops: from here on a job ending or an event loop going quiet is the shutdown itself.
   */
  fun serviceStopping() {
    stopping = true
  }

  internal val judging: Boolean
    get() = stopping.not() && jvmShuttingDown().not()

  fun stalled(
    /**
     * What stalls, stable while the stall lasts (`auto-merge-cycle`, `event-loop`).
     */
    stall: String,
    /**
     * The log line, with the measurements of this check (`Essential background job [auto-merge-cycle] …`).
     */
    cause: String,
  ) {
    if (judging.not()) {
      logger.info("$cause - the service is stopping, no reaction")
      return
    }

    when (mode) {
      WatchdogMode.LogOnly -> if (reportedStalls.add(stall)) {
        logger.error("$cause - the process keeps running, this server was not started by a service launch")
      }

      WatchdogMode.DebuggerAttached -> if (reportedStalls.add(stall)) {
        logger.warn("$cause - a debugger is attached, the process keeps running")
      }

      WatchdogMode.Exit -> {
        if (exiting.compareAndSet(false, true).not()) {
          logger.error("$cause - the process is already exiting")
          return
        }
        logger.error("$cause - exiting with code $ExitStatus so the container restarts")
        logThreadDump()
        processExit.exit(ExitStatus)
      }
    }
  }

  /**
   * One ERROR line per thread: the stack of the blocked thread is the diagnosis the restart would erase.
   */
  private fun logThreadDump() {
    Thread.getAllStackTraces().forEach { (thread, stack) ->
      logger.error("Thread dump [${thread.name}] (${thread.state}):\n${stack.joinToString("\n") { "\tat $it" }}")
    }
  }

  /**
   * A JVM in its shutdown sequence refuses new shutdown hooks — the one reliable way to ask whether it
   * is shutting down.
   */
  private fun jvmShuttingDown(): Boolean {
    val probe = Thread {}
    return try {
      Runtime.getRuntime().addShutdownHook(probe)
      Runtime.getRuntime().removeShutdownHook(probe)
      false
    } catch (_: IllegalStateException) {
      true
    }
  }

  companion object {
    /**
     * `EX_SOFTWARE` from `sysexits.h`: the process ended because of its own state.
     */
    const val ExitStatus: Int = 70

    private val logger: Logger = LoggerFactory.getLogger("it.neckar.ktor.health.StallReaction")
  }
}
