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
package it.neckar.heartbeat

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Checks the heartbeat to a service
 */
class Heartbeat(
  /**
   * The delay between two checks
   */
  val delayBetweenChecks: Duration = 5.seconds,
  /**
   * The timeout for each check. If the check takes longer than this
   * timeout, it is canceled and [HeartbeatState.Dead] emitted.
   */
  val timeout: Duration = 1.seconds,
  /**
   * Will be called regularly.
   * Returns the HeartbeatState
   */
  val checkConnection: suspend () -> HeartbeatState,
) {

  /**
   * Starts the heart beat.
   *
   * Returns a flow with the heartbeat results
   */
  fun createFlow(): Flow<HeartbeatState> {
    return flow {
      while (currentCoroutineContext().isActive) {
        //Run the first check immediately
        val result = withTimeoutOrNull(timeout) {
          try {
            emit(checkConnection())
          } catch (e: CancellationException) {
            throw e
          } catch (e: Throwable) {
            //Some exception has occurred, connection is dead
            emit(HeartbeatState.Dead.ExceptionOccurred(e))
          }
        }

        //Timeout reached
        if (result == null) {
          emit(HeartbeatState.Dead.ConnectionFailure)
        }

        //delay
        delay(delayBetweenChecks)
      }
    }
  }
}


