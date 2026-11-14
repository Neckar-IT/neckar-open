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
import java.net.URI
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Asks the server over HTTP whether it still processes requests, [probeInterval] after the previous probe
 * ended, and hands a server that answered none of [silentProbesToStall] probes in a row to [reaction].
 * Every probe is a request with its own server span, so the interval stays long.
 *
 * Any answer counts, a `404` or a `503` included: it proves that the Netty event loops accept the
 * connection and that the call pipeline runs. Only a timeout or a refused connection is silence.
 * Counting probes instead of time keeps a frozen process fair: a freeze costs at most the probe in
 * flight. A server that never answered is still starting and is not judged.
 */
class EventLoopWatchdog(
  /**
   * The server itself, as seen from inside the container (`http://127.0.0.1:8080/`).
   */
  private val probeUrl: URI,
  private val reaction: StallReaction,
  /**
   * The pause between the end of one probe and the start of the next.
   */
  private val probeInterval: Duration = 20.seconds,
  /**
   * How long one probe waits for the connection and the headers; see [HttpServerProbe].
   */
  probeTimeout: Duration = 10.seconds,
  /**
   * How many probes in a row the server may leave unanswered before it counts as stalled: 60 to 120 s
   * after the last answer, by how long each probe waits.
   */
  private val silentProbesToStall: Int = 3,
  private val probe: ServerProbe = HttpServerProbe(probeTimeout),
) {
  init {
    require(silentProbesToStall >= 2) { "a single silent probe is a freeze as much as a stall, but silentProbesToStall was $silentProbesToStall" }
  }

  private val executor: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor { runnable ->
    Thread(runnable, "event-loop-watchdog").apply { isDaemon = true }
  }

  /**
   * Confined to [executor], like [silentProbes].
   */
  private var answeredOnce: Boolean = false

  private var silentProbes: Int = 0

  fun start() {
    executor.scheduleWithFixedDelay({ check() }, 0, probeInterval.inWholeMilliseconds, TimeUnit.MILLISECONDS)
  }

  /**
   * Idempotent.
   */
  fun stop() {
    executor.shutdownNow()
    probe.close()
  }

  /**
   * Internal for tests.
   */
  internal fun check() {
    //A throwing task ends a ScheduledExecutorService's repetition for good
    try {
      if (probe.answers(probeUrl)) {
        answeredOnce = true
        silentProbes = 0
        return
      }
      if (answeredOnce.not()) {
        return
      }

      silentProbes++
      if (silentProbes >= silentProbesToStall) {
        reaction.stalled(stall = "event-loop", cause = "Netty event loop answered none of the last $silentProbes HTTP probes to $probeUrl")
      }
    } catch (_: InterruptedException) {
      //stop() interrupts a probe in flight
      Thread.currentThread().interrupt()
    } catch (e: Throwable) {
      logger.error("Probing the event loop via $probeUrl failed", e)
    }
  }

  companion object {
    private val logger: Logger = LoggerFactory.getLogger("it.neckar.ktor.health.EventLoopWatchdog")
  }
}
