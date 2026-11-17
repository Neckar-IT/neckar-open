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

import it.neckar.open.exceptions.exceptionTypeName
import it.neckar.open.time.now
import it.neckar.rest.error.GlobalHealth
import it.neckar.rest.health.ServiceStateDto

/**
 * Projects the [stateHolder] onto the legacy `global-health` body.
 *
 * Reads the same state as the `/health/live`, `/health/ready` and `/health` probes, reduced to
 * [GlobalHealth.Healthy] / [GlobalHealth.Unhealthy]. A recorded uncaught exception makes the result
 * [GlobalHealth.Unhealthy] without changing [GlobalHealth.Unhealthy.state]: the service still serves
 * traffic. A down dependency takes precedence over an uncaught exception, because it is the more
 * actionable cause.
 *
 * Reports the exception type and, for a fatal state, the trace of its error span — never a message
 * or a stack trace, see [GlobalHealth.Unhealthy.message].
 */
fun GlobalHealth.Companion.from(stateHolder: ServiceStateHolder): GlobalHealth {
  val dependencies = stateHolder.dependencies
  val uncaughtExceptions = stateHolder.uncaughtExceptions
  // Read once: a second read could see a state the wire `state` was not computed from, and answer a
  // trace ID that belongs to no reported condition.
  val state = stateHolder.state
  val wireState = HealthProjection.wireState(state, dependencies)

  if (wireState == ServiceStateDto.Ready && uncaughtExceptions == null) {
    return GlobalHealth.Healthy
  }

  val mostRecentOutage = dependencies.mostRecentOutage()
  val mostRecentDegradation = dependencies.mostRecentDegradation()
  // Reported in every branch: a fatal service with a down dependency answers the outage, and its
  // trace is still the only way to the reason it turned fatal.
  val fatal = state as? ServiceState.Fatal

  return when {
    mostRecentOutage != null -> {
      val errorType = mostRecentOutage.outage.error.exceptionTypeName()
      GlobalHealth.Unhealthy(
        message = "${mostRecentOutage.name} is down: $errorType",
        state = wireState,
        errorType = errorType,
        errorCount = dependencies.count { it.isDown },
        timestamp = mostRecentOutage.outage.since,
        traceId = fatal?.traceId,
      )
    }

    mostRecentDegradation != null -> GlobalHealth.Unhealthy(
      message = "${mostRecentDegradation.name} is degraded: ${mostRecentDegradation.degradation.detail}",
      state = wireState,
      errorCount = dependencies.count { it.isDegraded },
      timestamp = mostRecentDegradation.degradation.since,
      traceId = fatal?.traceId,
    )

    uncaughtExceptions != null -> GlobalHealth.Unhealthy(
      message = "uncaught exception: ${uncaughtExceptions.latest.exceptionTypeName()}",
      state = wireState,
      errorType = uncaughtExceptions.latest.exceptionTypeName(),
      errorCount = uncaughtExceptions.count,
      timestamp = uncaughtExceptions.since,
      traceId = fatal?.traceId,
    )

    else -> GlobalHealth.Unhealthy(
      message = "service state: ${wireState.wireValue}",
      state = wireState,
      errorType = fatal?.errorType,
      errorCount = 0,
      timestamp = now(),
      traceId = fatal?.traceId,
    )
  }
}
