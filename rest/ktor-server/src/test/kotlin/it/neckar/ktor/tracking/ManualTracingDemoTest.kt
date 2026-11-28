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
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter
import io.opentelemetry.sdk.trace.SdkTracerProvider
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor
import io.opentelemetry.semconv.DbAttributes
import io.opentelemetry.semconv.HttpAttributes
import io.opentelemetry.semconv.UrlAttributes
import it.neckar.ktor.installOpenTelemetry
import it.neckar.rest.test.*
import org.junit.jupiter.api.Test
import org.slf4j.MDC

/**
 * Demonstrates OpenTelemetry tracing in a Ktor service — without the Java Agent.
 *
 * - A hand-written interceptor puts the trace ID of its span into the Mapped Diagnostic Context.
 * - [installOpenTelemetry] records one server span per request with the HTTP method, URL path and response status code.
 * - A route handler creates a child span with `db.*` attributes inside the server span.
 */
class ManualTracingDemoTest {

  /**
   * Creates an in-memory OpenTelemetry instance; [InMemorySpanExporter.getFinishedSpanItems] returns the recorded spans.
   */
  private fun createTestOpenTelemetry(spanExporter: InMemorySpanExporter): OpenTelemetrySdk {
    val tracerProvider: SdkTracerProvider = SdkTracerProvider.builder()
      .addSpanProcessor(SimpleSpanProcessor.create(spanExporter))
      .build()

    return OpenTelemetrySdk.builder()
      .setTracerProvider(tracerProvider)
      .build()
  }

  /**
   * A hand-written interceptor puts the trace ID of its span into the Mapped Diagnostic Context, where the route handler reads it.
   */
  @Test
  fun `manual span creation with MDC injection`() {
    val spanExporter = InMemorySpanExporter.create()
    val openTelemetry: OpenTelemetrySdk = createTestOpenTelemetry(spanExporter)

    val tracer = openTelemetry.getTracer("it.neckar.ktor")
    var traceIdInHandler: String? = null

    testApplication {
      application {
        // Intercept early in the pipeline (Monitoring phase) to wrap every request in a span.
        // In production, installOpenTelemetry does this via the KtorServerTelemetry plugin.
        intercept(ApplicationCallPipeline.Monitoring) {
          val span: Span = tracer.spanBuilder("${call.request.local.method.value} ${call.request.path()}")
            .setSpanKind(SpanKind.SERVER)
            .startSpan()

          // Log statements on this thread carry the trace ID from the MDC.
          // In production, the Java agent's logback-mdc-1.0 instrumentation does this.
          MDC.put(MDCKeys.TraceId.value, span.spanContext.traceId)
          MDC.put(MDCKeys.SpanId.value, span.spanContext.spanId)

          try {
            proceed()
          } finally {
            span.end()
            // MDC is thread-local; a leftover trace ID would leak into the next request on this thread.
            MDC.remove(MDCKeys.TraceId.value)
            MDC.remove(MDCKeys.SpanId.value)
          }
        }

        routing {
          get("/api/demo") {
            traceIdInHandler = MDCKeys.TraceId.get()
            call.respondText("trace=$traceIdInHandler", ContentType.Text.Plain)
          }
        }
      }

      val response = client.get("/api/demo")
      assertThat(response).isOk()
    }

    assertThat(traceIdInHandler).isEqualTo(spanExporter.finishedSpanItems.single().traceId)
  }

  /**
   * The `KtorServerTelemetry` plugin that [installOpenTelemetry] installs records one [SpanKind.SERVER] span per request
   * with the attributes `http.request.method`, `url.path` and `http.response.status_code` of the
   * [OpenTelemetry HTTP semantic conventions](https://opentelemetry.io/docs/specs/semconv/http/).
   */
  @Test
  fun `the plugin records one server span per request`() {
    val spanExporter = InMemorySpanExporter.create()
    val openTelemetry: OpenTelemetrySdk = createTestOpenTelemetry(spanExporter)

    testApplication {
      application {
        installOpenTelemetry(openTelemetry)
        routing {
          get("/api/books") {
            call.respondText("[]", ContentType.Application.Json)
          }
        }
      }

      val response = client.get("/api/books")
      assertThat(response).isOk()
    }

    val spans = spanExporter.finishedSpanItems
    assertThat(spans).hasSize(1)

    val span = spans.single()
    assertThat(span.kind).isEqualTo(SpanKind.SERVER)
    assertThat(span.attributes.get(HttpAttributes.HTTP_REQUEST_METHOD)).isEqualTo("GET")
    assertThat(span.attributes.get(UrlAttributes.URL_PATH)).isEqualTo("/api/books")
    assertThat(span.attributes.get(HttpAttributes.HTTP_RESPONSE_STATUS_CODE)).isEqualTo(200L)
  }

  /**
   * A span that a route handler starts becomes a child of the server span: both share the `traceId`, and the
   * `parentSpanId` of the child is the `spanId` of the server span. The child keeps its attributes
   * `db.system.name`, `db.operation.name` and `db.collection.name`.
   */
  @Test
  fun `child spans track individual operations within a request`() {
    val spanExporter = InMemorySpanExporter.create()
    val openTelemetry: OpenTelemetrySdk = createTestOpenTelemetry(spanExporter)
    val tracer = openTelemetry.getTracer("it.neckar.ktor")

    testApplication {
      application {
        installOpenTelemetry(openTelemetry)
        routing {
          get("/api/books/{id}") {
            // KtorServerTelemetry makes the server span Context.current() in the handler; the span builder takes it as parent.
            val dbSpan: Span = tracer.spanBuilder("MongoDB findBookById")
              .setAttribute(DbAttributes.DB_SYSTEM_NAME, "mongodb")
              .setAttribute(DbAttributes.DB_OPERATION_NAME, "find")
              .setAttribute(DbAttributes.DB_COLLECTION_NAME, "books")
              .startSpan()

            dbSpan.end()

            call.respondText("""{"id": "123", "title": "Demo"}""", ContentType.Application.Json)
          }
        }
      }

      val response = client.get("/api/books/123")
      assertThat(response).isOk()
    }

    val spans = spanExporter.finishedSpanItems
    assertThat(spans).hasSize(2)

    val dbSpan = spans.single { it.name == "MongoDB findBookById" }
    val serverSpan = spans.single { it.kind == SpanKind.SERVER }

    assertThat(dbSpan.traceId).isEqualTo(serverSpan.traceId)
    assertThat(dbSpan.parentSpanId).isEqualTo(serverSpan.spanId)
    assertThat(dbSpan.attributes.get(DbAttributes.DB_SYSTEM_NAME)).isEqualTo("mongodb")
    assertThat(dbSpan.attributes.get(DbAttributes.DB_OPERATION_NAME)).isEqualTo("find")
    assertThat(dbSpan.attributes.get(DbAttributes.DB_COLLECTION_NAME)).isEqualTo("books")
  }
}
