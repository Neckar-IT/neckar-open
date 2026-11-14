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

import it.neckar.logging.Logger
import it.neckar.logging.LoggerFactory
import it.neckar.logging.warn
import it.neckar.open.unit.si.ms
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Support heartbeat to a service
 */
class ServiceHeartbeatSupport(
  /**
   * Will be called regularly.
   */
  val heartbeatCheck: HeartbeatCheck,

  /**
   * The delay between two heartbeat checks
   */
  delayBetweenChecks: Duration = 30.seconds,

  /**
   * The timeout for each check. If the check takes longer than this, it is canceled and [HeartbeatState.Dead] emitted.
   */
  timeout: @ms Duration = 5.seconds,

  /**
   * Is used to with [flowOn] when creating the [heartbeatFlow]
   */
  private val heartbeatFlowDispatcher: CoroutineDispatcher = Dispatchers.Default,

  /**
   * Is used with [shareIn] when creating the [heartbeatFlow]
   */
  private val heartbeatFlowShareScope: CoroutineScope = CoroutineScope(Dispatchers.Default),

  ) {

  /**
   * The logger for this specific heartbeat instance!
   */
  val logger: Logger = LoggerFactory.getLogger("heartbeat-${heartbeatCheck.description}")

  init {
    logger.info("Creating ServiceHeartbeatSupport for [${heartbeatCheck.description}]")
  }

  /**
   * The heartbeat
   */
  internal val heartbeat: Heartbeat = Heartbeat(
    delayBetweenChecks = delayBetweenChecks,
    timeout = timeout,
  ) {
    logger.debug("Checking heartbeat for [${heartbeatCheck.description}]")

    try {
      return@Heartbeat heartbeatCheck.check()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Throwable) {
      logger.warn { "Exception $e\n - ${e.message}" }
      logger.warn { e.stackTraceToString() }

      return@Heartbeat HeartbeatState.Dead.ExceptionOccurred(e)
    }
  }

  /**
   * Contains a flow of the heartbeat state
   */
  val heartbeatFlow: SharedFlow<HeartbeatState> = heartbeat
    .createFlow()
    .flowOn(heartbeatFlowDispatcher)
    .shareIn(heartbeatFlowShareScope, SharingStarted.Lazily)
}
