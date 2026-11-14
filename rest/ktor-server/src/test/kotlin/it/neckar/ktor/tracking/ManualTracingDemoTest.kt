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
package it.neckar.ktor.tracking

import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.context.Context
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter
import io.opentelemetry.sdk.trace.SdkTracerProvider
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor
import it.neckar.rest.test.*
import org.junit.jupiter.api.Test
import org.slf4j.MDC

/**
 * Demonstrates manual OpenTelemetry tracing in a Ktor service — without the Java Agent.
 *
 * ## Glossary
 *
 * - **Trace:** The complete journey of a request through the system. A trace has a unique
 *   `traceId` (32 hex characters) that connects all related operations across services.
 * - **Span:** A single named operation within a trace — e.g., "handle HTTP request",
 *   "query database", "call external API". Each span records start time, duration, status,
 *   and key-value attributes. Spans form a tree: one root span with child spans nested below.
 * - **Tracer:** Factory for creating spans. Scoped to an instrumentation name (e.g., module name)
 *   so that spans can be grouped by origin in the tracing backend.
 * - **Exporter:** Sends finished spans to a tracing backend. [InMemorySpanExporter] keeps them
 *   in memory for testing; in production, the OpenTelemetry Protocol (OTLP) exports them
 *   to Grafana Tempo.
 * - **SpanProcessor:** Sits between the tracer and the exporter. [SimpleSpanProcessor] forwards
 *   spans immediately (good for tests); in production, `BatchSpanProcessor` batches them.
 * - **Mapped Diagnostic Context (MDC):** A feature of the SLF4J (Simple Logging Facade for Java)
 *   / Logback logging framework that binds key-value pairs to the current thread. By putting
 *   `trace_id` into the Mapped Diagnostic Context, every log line within a request automatically
 *   includes the trace ID — enabling log↔trace correlation in Grafana (Loki logs link to
 *   Tempo traces).
 *
 * ## What this demo shows
 *
 * This test demonstrates explicit span creation and Mapped Diagnostic Context (MDC) injection
 * using the OpenTelemetry library instrumentation. In production, `opentelemetry-ktor-3.0`
 * handles HTTP span creation, and the `logback-mdc-1.0` instrumentation of the OpenTelemetry
 * Java agent handles MDC injection.
 */
class ManualTracingDemoTest {

  @Test
  fun `manual span creation with MDC injection`() {
    // Set up an in-memory OpenTelemetry instance.
    // InMemorySpanExporter collects all finished spans so we can assert on them after the test.
    val spanExporter = InMemorySpanExporter.create()
    val tracerProvider: SdkTracerProvider = SdkTracerProvider.builder()
      .addSpanProcessor(SimpleSpanProcessor.create(spanExporter))
      .build()
    val openTelemetry: OpenTelemetrySdk = OpenTelemetrySdk.builder()
      .setTracerProvider(tracerProvider)
      .build()

    // A Tracer is scoped to an instrumentation name (typically the module or library name).
    // All spans created by this tracer carry this name as their "instrumentation scope".
    val tracer = openTelemetry.getTracer("it.neckar.ktor")

    testApplication {
      application {
        // Intercept early in the pipeline (Monitoring phase) to wrap every request in a span.
        // In production, opentelemetry-ktor-3.0 does this via KtorServerTelemetry plugin.
        intercept(ApplicationCallPipeline.Monitoring) {
          val span: Span = tracer.spanBuilder("${call.request.local.method.value} ${call.request.path()}")
            .setSpanKind(SpanKind.SERVER)
            .setParent(Context.current())
            .startSpan()

          // Inject trace context into MDC so that log statements within this request
          // automatically include the trace ID. Without this, logs and traces are disconnected.
          // In production, the Java agent's logback-mdc-1.0 instrumentation does this.
          MDC.put(MDCKeys.TraceId.value, span.spanContext.traceId)
          MDC.put(MDCKeys.SpanId.value, span.spanContext.spanId)

          try {
            proceed()
          } finally {
            span.end()
            // Clean up MDC to prevent trace IDs from leaking into subsequent requests
            // on the same thread (MDC is thread-local).
            MDC.remove(MDCKeys.TraceId.value)
            MDC.remove(MDCKeys.SpanId.value)
          }
        }

        routing {
          get("/api/demo") {
            // Inside the route handler, MDC keys are available for the duration of the request.
            // Any logger call here will automatically include trace_id in the log output.
            val traceId: String? = MDCKeys.TraceId.get()
            assertThat(traceId).isNotNull()

            call.respondText("trace=$traceId", ContentType.Text.Plain)
          }
        }
      }

      val response = client.get("/api/demo")
      assertThat(response).isOk()
    }

    // After the request, the InMemorySpanExporter contains all finished spans.
    val spans = spanExporter.finishedSpanItems
    assertThat(spans).hasSize(1)

    val span = spans.single()
    assertThat(span.name).isEqualTo("GET /api/demo")
    assertThat(span.kind).isEqualTo(SpanKind.SERVER)
  }
}
