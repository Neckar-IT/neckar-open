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
import it.neckar.open.time.now
import it.neckar.open.unit.other.GuardedBy
import it.neckar.rest.health.DependencyName

/**
 * In-process single source of truth for the service state, its dependencies and the recorded
 * uncaught exceptions.
 *
 * The health probes (`/health/live`, `/health/ready`, `/health`) only project this holder onto
 * HTTP; they never compute the state themselves. The startup sequence, the [DependencyMonitor] and
 * the `GlobalExceptionCollector` mutate it. Thread-safe.
 *
 * [dependencies] and [uncaughtExceptions] are evaluated separately: a down dependency can make the
 * service not ready, an uncaught exception never does.
 */
class ServiceStateHolder(
  /**
   * Invoked whenever the lifecycle state changes (not on every dependency update).
   */
  private val onStateChanged: (ServiceState) -> Unit = {},
) {
  private val lock = PlatformReadWriteLock()

  @GuardedBy("lock")
  private var _state: ServiceState = ServiceState.Starting

  @GuardedBy("lock")
  private val dependencyStates: MutableMap<DependencyName, DependencyState> = mutableMapOf()

  @GuardedBy("lock")
  private var _uncaughtExceptions: UncaughtExceptions? = null

  /**
   * The current lifecycle state.
   */
  val state: ServiceState
    get() = lock.read { _state }

  /**
   * The last observed state of every declared dependency.
   */
  val dependencies: List<DependencyState>
    get() = lock.read { dependencyStates.values.toList() }

  /**
   * The uncaught exceptions recorded since the last [clearUncaughtExceptions], or `null` if none
   * were recorded.
   */
  val uncaughtExceptions: UncaughtExceptions?
    get() = lock.read { _uncaughtExceptions }

  /**
   * Transitions to [target]. Throws on an illegal transition (see [ServiceState.canTransitionTo]).
   * A transition to the current state is a no-op and does not fire [onStateChanged].
   */
  fun transitionTo(target: ServiceState) {
    val changed = lock.write {
      val current = _state
      if (current == target) {
        return@write false
      }
      require(current.canTransitionTo(target)) { "illegal transition: $current -> $target" }
      logger.info("Service state transition: $current -> $target")
      _state = target
      true
    }

    if (changed) {
      onStateChanged(target)
    }
  }

  /**
   * Transitions to [target], but only while the state is still [expected]. Returns whether it moved.
   *
   * The compare and the write happen under one lock, which is the whole point: a caller that reads
   * the state, decides, and then calls [transitionTo] can find a state in between that the transition
   * is illegal from, and [transitionTo] answers that with an exception. Where the transition is a
   * reconciliation rather than a command — the dependency monitor driving `Ready ↔ Degraded` from a
   * `/health/ready` request — a state that moved on means the reconciliation is simply obsolete.
   *
   * [expected] equal to [target] answers `false` without firing [onStateChanged]: a reconciliation
   * that finds what it wanted has nothing to announce.
   */
  @IgnorableReturnValue
  fun transitionToIfCurrentIs(expected: ServiceState, target: ServiceState): Boolean {
    val changed = lock.write {
      if (_state != expected || expected == target) {
        return@write false
      }
      require(expected.canTransitionTo(target)) { "illegal transition: $expected -> $target" }
      logger.info("Service state transition: $expected -> $target")
      _state = target
      true
    }

    if (changed) {
      onStateChanged(target)
    }
    return changed
  }

  /**
   * Records the latest [status] for the dependency [name] classified as [classification].
   * Replaces only this dependency's entry — other dependencies' errors are untouched (error-scoped).
   */
  fun updateDependency(name: DependencyName, classification: DependencyClassification, status: DependencyStatus) {
    lock.write {
      dependencyStates[name] = DependencyState(name, classification, status)
    }
  }

  /**
   * Records an uncaught [error]. Increments the count and keeps the timestamp of the first exception.
   * Does not change the lifecycle state and does not gate readiness — an application error must not
   * take the process out of the load balancer.
   */
  fun recordUncaughtException(error: Throwable) {
    lock.write {
      val previous = _uncaughtExceptions
      _uncaughtExceptions = UncaughtExceptions(
        latest = error,
        count = (previous?.count ?: 0) + 1,
        since = previous?.since ?: now(),
      )
    }
  }

  /**
   * Adopts the exceptions [recorded] before this holder was connected to the collector — the startup
   * sequence runs before the health layer is installed. Adds to whatever this holder recorded itself.
   */
  fun recordUncaughtExceptions(recorded: UncaughtExceptions) {
    lock.write {
      val previous = _uncaughtExceptions
      _uncaughtExceptions = when (previous) {
        null -> recorded
        else -> UncaughtExceptions(
          latest = previous.latest,
          count = previous.count + recorded.count,
          since = minOf(previous.since, recorded.since),
        )
      }
    }
  }

  /**
   * Clears all recorded uncaught exceptions.
   *
   * Nothing calls this in production: a recorded exception is retained until the process restarts, so
   * an application error is never silently discarded. Exists for tests and for an operator-driven
   * reset.
   */
  fun clearUncaughtExceptions() {
    lock.write {
      _uncaughtExceptions = null
    }
  }

  companion object {
    private val logger: Logger = LoggerFactory.getLogger("it.neckar.open.health.ServiceStateHolder")
  }
}
