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

import io.opentelemetry.api.GlobalOpenTelemetry
import io.opentelemetry.api.trace.Tracer
import it.neckar.open.exceptions.exceptionTypeName
import it.neckar.open.health.error.recordAsErrorSpan

private const val TracerPackage: String = "it.neckar.open.health"
private const val FatalSpanName: String = "service-fatal"

/**
 * Turns the service fatal because of [cause]: records the exception as an OpenTelemetry error span
 * and enters [ServiceState.Fatal] carrying that span's trace.
 *
 * The one way to reach [ServiceState.Fatal] with a cause. It keeps the two apart that must stay
 * apart: the message and the stack trace reach Grafana through the span, the health endpoints
 * answer the reason, the exception type and the trace ID. Building the state by hand splits them
 * again and produces a fatal state nobody can look up.
 */
fun ServiceStateHolder.transitionToFatal(
  reason: FatalReason,
  cause: Throwable,
) {
  transitionToFatal(GlobalOpenTelemetry.get().getTracer(TracerPackage), reason, cause)
}

/**
 * Visible for tests, which assert the recorded span against an in-memory exporter.
 */
internal fun ServiceStateHolder.transitionToFatal(
  tracer: Tracer,
  reason: FatalReason,
  cause: Throwable,
) {
  transitionTo(
    ServiceState.Fatal(
      reason = reason,
      message = cause.message.orEmpty(),
      errorType = cause.exceptionTypeName(),
      traceId = recordAsErrorSpan(tracer, FatalSpanName, cause),
    )
  )
}
