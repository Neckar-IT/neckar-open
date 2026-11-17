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
import it.neckar.open.concurrent.repeating.Repeating
import it.neckar.open.time.now
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

/**
 * Polls the registered [probes] and feeds the results into the [stateHolder].
 *
 * Runs a periodic poll ([start]) and is also invoked on-demand by `/health/ready` ([checkAll]) so
 * readiness is live. Each probe runs under its own hard timeout ([DependencyProbe.timeout]) — a probe
 * that hangs is counted as down and never blocks the monitor. Recovery is error-scoped: a probe that
 * returns [DependencyStatus.Up] clears only its own dependency from the holder.
 *
 * Every change of a dependency's status kind (`Up`, `Degraded`, `Down`) writes exactly one log line
 * with its cause; an unchanged status writes nothing. A dependency without a previous observation
 * counts as `Up`.
 */
class DependencyMonitor(
  private val stateHolder: ServiceStateHolder,
  /**
   * The dependency probes to run, in declaration order.
   */
  private val probes: List<DependencyProbe>,
) {
  private val checkMutex: Mutex = Mutex()

  /**
   * Starts the periodic poll in [scope]. Returns the [Repeating] handle for cancellation.
   */
  @IgnorableReturnValue
  fun start(
    scope: CoroutineScope,
    /**
     * Interval between polls.
     */
    pollInterval: Duration = DefaultPollInterval,
  ): Repeating {
    return Repeating.every(pollInterval) {
      checkAll()
    }.start(coroutineScope = scope)
  }

  /**
   * Runs every probe once and updates the holder. Invoked by the periodic poll and on-demand by
   * `/health/ready`.
   */
  suspend fun checkAll() {
    //The poll and `/health/ready` run concurrently; each read-probe-write cycle must see the status
    //the previous cycle wrote, or a change is logged twice or not at all.
    checkMutex.withLock {
      probes.forEach { probe ->
        val previous = stateHolder.dependencies.find { it.name == probe.name }?.status
        val current = probeOnce(probe, previous)
        stateHolder.updateDependency(probe.name, probe.classification, current)
        logStatusChange(probe, previous, current)
      }
      reconcileLifecycle()
    }
  }

  private suspend fun probeOnce(probe: DependencyProbe, previous: DependencyStatus?): DependencyStatus {
    val result: DependencyStatus? = try {
      withTimeoutOrNull(probe.timeout) { probe.check() }
    } catch (e: CancellationException) {
      //Rethrows only when the monitor itself is cancelled; a cancellation raised inside the probe
      //(e.g. its own `withTimeout`) marks the dependency down.
      currentCoroutineContext().ensureActive()
      return DependencyStatus.Down(e, outageStart(previous))
    } catch (e: Throwable) {
      return DependencyStatus.Down(e, outageStart(previous))
    }

    return when (result) {
      null -> DependencyStatus.Down(DependencyTimeoutException(probe.name, probe.timeout), outageStart(previous))
      is DependencyStatus.Up -> result
      is DependencyStatus.Degraded -> DependencyStatus.Degraded(result.detail, degradationStart(previous))
      is DependencyStatus.Down -> DependencyStatus.Down(result.error, outageStart(previous))
    }
  }

  private fun logStatusChange(probe: DependencyProbe, previous: DependencyStatus?, current: DependencyStatus) {
    val previousOrUp = previous ?: DependencyStatus.Up()
    if (previousOrUp::class == current::class) {
      return
    }

    val dependency = "Dependency '${probe.name}' (${probe.classification})"
    when (current) {
      is DependencyStatus.Down -> {
        val cause = "${current.error::class.simpleName}: ${current.error.message}"
        val message = "$dependency is down since ${current.since}: $cause"
        when (current.error) {
          //The timeout marker carries no stack trace worth reading.
          is DependencyTimeoutException -> logger.warn(message)
          else -> logger.warn(message, current.error)
        }
      }

      is DependencyStatus.Degraded -> logger.warn("$dependency is degraded: ${current.detail}")
      is DependencyStatus.Up -> logger.info("$dependency is up again after ${(now() - previousOrUp.notUpSince()).inWholeSeconds.seconds}")
    }
  }

  /**
   * When the current [DependencyStatus.Down] or [DependencyStatus.Degraded] began.
   */
  private fun DependencyStatus.notUpSince(): Instant = when (this) {
    is DependencyStatus.Down -> since
    is DependencyStatus.Degraded -> since
    is DependencyStatus.Up -> error("an up dependency has no outage start")
  }

  /**
   * Keeps the outage start across consecutive failing polls; resets it for a fresh outage.
   */
  private fun outageStart(previous: DependencyStatus?): Instant =
    (previous as? DependencyStatus.Down)?.since ?: now()

  /**
   * Keeps the degradation start across consecutive degraded polls; resets it for a fresh degradation.
   */
  private fun degradationStart(previous: DependencyStatus?): Instant =
    (previous as? DependencyStatus.Degraded)?.since ?: now()

  /**
   * Drives `Ready ↔ Degraded` from the optional-dependency state. A down required dependency does
   * not change the lifecycle (it projects to `dependency-unavailable`); the wire state handles it.
   */
  private fun reconcileLifecycle() {
    val current = stateHolder.state
    if (current != ServiceState.Ready && current != ServiceState.Degraded) {
      return
    }

    val optionalDown = stateHolder.dependencies.any {
      it.classification == DependencyClassification.Optional && it.isDown
    }

    //Compare-and-set: this runs on the poll and on every `/health/ready`, and a plain transitionTo
    //would throw out of the request handler when the state turned Fatal in between.
    when {
      optionalDown -> stateHolder.transitionToIfCurrentIs(ServiceState.Ready, ServiceState.Degraded)
      else -> stateHolder.transitionToIfCurrentIs(ServiceState.Degraded, ServiceState.Ready)
    }
  }

  companion object {
    val DefaultPollInterval: Duration = 10.seconds
    private val logger: Logger = LoggerFactory.getLogger("it.neckar.open.health.DependencyMonitor")
  }
}
