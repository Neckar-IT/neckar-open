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
package it.neckar.rest.error

import it.neckar.open.kotlin.serializers.NativeInstant
import it.neckar.open.tracing.TraceId
import it.neckar.rest.health.ServiceStateDto
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Body of the legacy `global-health` endpoint: the binary verdict over the service state.
 *
 * [Healthy] when the service state projects to [ServiceStateDto.Ready] and no uncaught exception is
 * recorded, otherwise [Unhealthy]. The richer `/health` endpoint carries the same information plus
 * the per-dependency breakdown.
 */
@Serializable
sealed interface GlobalHealth {
  /**
   * No dependency is down, no uncaught exception is recorded, and the service is past its startup
   * sequence.
   */
  @Serializable
  @SerialName("Healthy")
  data object Healthy : GlobalHealth

  @Serializable
  @SerialName("Error")
  data class Unhealthy(
    /**
     * Human readable summary, e.g. "mongodb is down: java.net.SocketTimeoutException".
     *
     * Built from the state and the exception type alone. Never contains the exception message or
     * stack trace: this endpoint is unauthenticated, and an exception message may carry connection
     * details or business data. Both go to the log and to an OpenTelemetry error span, which
     * [traceId] points at.
     */
    val message: String,
    /**
     * The projected service state. Stays [ServiceStateDto.Ready] when the only recorded problem is
     * an uncaught exception: an application error does not change the service state.
     */
    val state: ServiceStateDto,
    /**
     * The fully qualified type of the most recent error (e.g. "java.io.IOException"). `null` when
     * no throwable was recorded, e.g. in [ServiceStateDto.Starting].
     */
    val errorType: String? = null,
    /**
     * Number of dependencies currently down, or the number of recorded uncaught exceptions when no
     * dependency is down.
     */
    val errorCount: Int,
    /**
     * When the reported error was first recorded, or when this snapshot was taken if there is none.
     */
    val timestamp: NativeInstant,
    /**
     * The trace of the error span recorded when the service turned fatal — the way to the message
     * and the stack trace this body deliberately omits.
     *
     * Set whenever the service is fatal, whichever condition [message] describes: a fatal service
     * whose dependency is also down reports the outage and still points at the reason it turned
     * fatal. `null` for a service that is not fatal, and whenever no exporter is configured.
     */
    val traceId: TraceId? = null,
  ) : GlobalHealth

  companion object
}
