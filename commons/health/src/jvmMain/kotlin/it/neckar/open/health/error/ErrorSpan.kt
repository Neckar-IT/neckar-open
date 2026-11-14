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

import io.opentelemetry.api.trace.StatusCode
import io.opentelemetry.api.trace.Tracer
import it.neckar.open.exceptions.exceptionTypeName
import it.neckar.open.tracing.TraceId

/**
 * Emits one dedicated span named [spanName] carrying [throwable] as a recorded exception with
 * [StatusCode.ERROR], and answers its trace.
 *
 * A dedicated span instead of recording onto the current one: an exception reported here has no span
 * of its own to attach to — an uncaught one has already left its originating span, a fatal startup
 * failure never ran inside a request span.
 *
 * @param spanName what happened, as the span name — `uncaught-exception`, `service-fatal`.
 * @return the trace to hand a caller, or `null` when no exporter is configured and the span is a
 * no-op.
 */
internal fun recordAsErrorSpan(tracer: Tracer, spanName: String, throwable: Throwable): TraceId? {
  val span = tracer.spanBuilder(spanName).startSpan()
  return try {
    span.recordException(throwable)
    span.setStatus(StatusCode.ERROR, throwable.message ?: throwable.exceptionTypeName())
    TraceId.orNull(span.spanContext.traceId)
  } finally {
    span.end()
  }
}
