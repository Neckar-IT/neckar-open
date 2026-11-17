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

import io.opentelemetry.api.GlobalOpenTelemetry
import io.opentelemetry.api.trace.Tracer
import it.neckar.open.dispose.Disposable

private const val TracerPackage: String = "it.neckar.open.health.error"
private const val UncaughtExceptionSpanName: String = "uncaught-exception"

/**
 * Reports every recorded uncaught exception to OpenTelemetry, so exceptions are queryable by error
 * status in Grafana/Tempo and not only present in the logs.
 *
 * One dedicated span per exception, see [recordAsErrorSpan].
 *
 * Registered via [GlobalExceptionCollector.onError], which keeps the collector independent of
 * OpenTelemetry. Call once at startup and keep the returned [Disposable] for the process lifetime.
 *
 * Uses the process-wide [GlobalOpenTelemetry] (installed by the OTel Java agent). The tracer is
 * resolved per exception, not at registration time: calling `GlobalOpenTelemetry.get()` before the
 * SDK is installed would fix the global provider to no-op and break a later SDK setup. Without an
 * agent the tracer is a no-op, so the call is safe in non-instrumented runs.
 */
fun GlobalExceptionCollector.recordExceptionsToOpenTelemetry(): Disposable =
  onError { throwable -> recordAsErrorSpan(GlobalOpenTelemetry.get().getTracer(TracerPackage), UncaughtExceptionSpanName, throwable) }

/**
 * [recordExceptionsToOpenTelemetry] with an explicit [tracer]. Visible for tests so they can assert
 * the emitted span against an in-memory SDK without touching the global OTel instance.
 */
internal fun GlobalExceptionCollector.recordExceptionsToOpenTelemetry(tracer: Tracer): Disposable =
  onError { throwable -> recordAsErrorSpan(tracer, UncaughtExceptionSpanName, throwable) }
