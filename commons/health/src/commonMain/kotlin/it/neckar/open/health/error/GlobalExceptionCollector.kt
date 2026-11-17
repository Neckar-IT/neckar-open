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
package it.neckar.open.health.error

import it.neckar.logging.Logger
import it.neckar.logging.LoggerFactory
import it.neckar.open.async.PlatformReadWriteLock
import it.neckar.open.async.read
import it.neckar.open.async.write
import it.neckar.open.dispose.Disposable
import it.neckar.open.health.UncaughtExceptions
import it.neckar.open.health.error.GlobalExceptionCollector.addError
import it.neckar.open.health.error.GlobalExceptionCollector.clearErrors
import it.neckar.open.health.error.GlobalExceptionCollector.onError
import it.neckar.open.health.error.GlobalExceptionCollector.registerGlobalCoroutineExceptionHandler
import it.neckar.open.time.now
import it.neckar.open.unit.other.GuardedBy
import kotlinx.coroutines.*

/**
 * Process-wide collector of uncaught thread/coroutine exceptions. Records each exception and passes
 * it to every callback registered via [onError]. Thread-safe.
 *
 * Keeps the latest exception, the count and the timestamp of the first one — not every throwable.
 * A process whose scheduled job fails with an uncaught exception every ten seconds would otherwise
 * retain thousands of throwables with their stack traces and everything those reference. The full
 * history is in Grafana: every recorded exception is reported as an OpenTelemetry error span.
 */
object GlobalExceptionCollector {
  private val lock = PlatformReadWriteLock()

  @GuardedBy("lock")
  private var recorded: UncaughtExceptions? = null

  /**
   * The uncaught exceptions recorded since the last [clearErrors], or `null` if none were recorded.
   */
  val uncaughtExceptions: UncaughtExceptions?
    get() = lock.read { recorded }

  /**
   * Records an uncaught [error] and notifies the [onError] callbacks. Increments the count and keeps
   * the timestamp of the first exception.
   *
   * Does **not** log: the uncaught exception handlers ([registerGlobalCoroutineExceptionHandler] and
   * the JVM thread handler `registerGlobalExceptionHandler`) already log every exception at ERROR
   * with the throwable and their thread/coroutine context.
   */
  fun addError(error: Throwable) {
    // A snapshot: the callbacks must be invoked without holding the lock, because a callback may
    // read the collector (deadlock) and because it may take arbitrarily long. Subscriptions are
    // added and removed at runtime by `installHealthLayer`, so iterating the live list would race
    // with them.
    val callbacks = lock.write {
      val previous = recorded
      recorded = UncaughtExceptions(
        latest = error,
        count = (previous?.count ?: 0) + 1,
        since = previous?.since ?: now(),
      )
      onErrorCallbacks.toList()
    }

    // Runs in the uncaught-exception path. A failing callback must neither break the exception
    // handling nor prevent the remaining callbacks from running.
    callbacks.forEach { callback ->
      try {
        callback(error)
      } catch (callbackFailure: Throwable) {
        logger.error("onError callback failed while recording an exception", callbackFailure)
      }
    }
  }

  /**
   * Clears all recorded exceptions.
   */
  fun clearErrors() {
    lock.write {
      recorded = null
    }
  }

  @GuardedBy("lock")
  private val onErrorCallbacks: MutableList<(Throwable) -> Unit> = mutableListOf()

  /**
   * Registers a callback invoked whenever an exception is recorded via [addError]. A callback that
   * throws is logged, not propagated — see [addError].
   *
   * May be called at any time, also while another thread records an exception: `installHealthLayer`
   * subscribes when the application starts and disposes when it stops.
   */
  fun onError(callback: (Throwable) -> Unit): Disposable {
    lock.write {
      onErrorCallbacks.add(callback)
    }

    return Disposable {
      lock.write {
        onErrorCallbacks.remove(callback)
      }
    }
  }

  /**
   * Creates a global exception handler for Coroutines that catches all uncaught exceptions.
   */
  fun registerGlobalCoroutineExceptionHandler(): CoroutineExceptionHandler {
    return CoroutineExceptionHandler { context, throwable ->
      val coroutineName = context[CoroutineName]?.name ?: "Unknown"
      logger.error("Uncaught exception in coroutine $coroutineName", throwable)
      addError(throwable)
    }
  }

  /**
   * Can be used to handle uncaught exceptions in Coroutines.
   */
  val coroutineContext: CoroutineExceptionHandler = registerGlobalCoroutineExceptionHandler()

  private val logger: Logger = LoggerFactory.getLogger("it.neckar.open.health.error.GlobalExceptionCollector")
}
