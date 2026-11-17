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
import assertk.assertions.hasLength
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.instrumentation.ktor.v3_0.KtorServerTelemetry
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter
import io.opentelemetry.sdk.trace.SdkTracerProvider
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor
import it.neckar.rest.tracking.XTraceId
import org.junit.jupiter.api.Test

class XTraceIdHeaderPluginTest {

  @Test
  fun `writes NeckarIT-Trace-Id header on success response that matches the server span`() {
    val spanExporter = InMemorySpanExporter.create()
    val openTelemetry = openTelemetryFor(spanExporter)

    testApplication {
      application {
        install(KtorServerTelemetry) {
          setOpenTelemetry(openTelemetry)
        }
        install(XTraceIdHeaderPlugin)

        routing {
          get("/api/ok") {
            call.respondText("ok")
          }
        }
      }

      val response = client.get("/api/ok")
      val headerValue = response.headers[HttpHeaders.XTraceId]
      assertThat(headerValue).isNotNull()

      val span = spanExporter.finishedSpanItems.single { it.kind == SpanKind.SERVER }
      assertThat(headerValue).isEqualTo(span.spanContext.traceId)
    }
  }

  @Test
  fun `writes NeckarIT-Trace-Id header on error response`() {
    val spanExporter = InMemorySpanExporter.create()
    val openTelemetry = openTelemetryFor(spanExporter)

    testApplication {
      application {
        install(KtorServerTelemetry) {
          setOpenTelemetry(openTelemetry)
        }
        install(XTraceIdHeaderPlugin)

        routing {
          get("/api/boom") {
            call.respond(HttpStatusCode.InternalServerError, "boom")
          }
        }
      }

      val response = client.get("/api/boom")
      assertThat(response.status).isEqualTo(HttpStatusCode.InternalServerError)
      val headerValue = response.headers[HttpHeaders.XTraceId]
      assertThat(headerValue).isNotNull()

      val span = spanExporter.finishedSpanItems.single { it.kind == SpanKind.SERVER }
      assertThat(headerValue).isEqualTo(span.spanContext.traceId)
    }
  }

  @Test
  fun `does not write NeckarIT-Trace-Id header when no OpenTelemetry is installed`() {
    testApplication {
      application {
        install(XTraceIdHeaderPlugin)

        routing {
          get("/api/no-otel") {
            call.respondText("ok")
          }
        }
      }

      val response = client.get("/api/no-otel")
      assertThat(response.headers[HttpHeaders.XTraceId]).isNull()
    }
  }

  @Test
  fun `header value is the 32-character lowercase hex W3C trace ID`() {
    val spanExporter = InMemorySpanExporter.create()
    val openTelemetry = openTelemetryFor(spanExporter)

    testApplication {
      application {
        install(KtorServerTelemetry) {
          setOpenTelemetry(openTelemetry)
        }
        install(XTraceIdHeaderPlugin)

        routing {
          get("/api/format") {
            call.respondText("ok")
          }
        }
      }

      val headerValue = client.get("/api/format").headers[HttpHeaders.XTraceId]
      assertThat(headerValue).isNotNull()
      assertThat(headerValue!!).hasLength(32)
      check(headerValue.all { it.isDigit() || it in 'a'..'f' }) {
        "NeckarIT-Trace-Id is not lowercase hex: $headerValue"
      }
    }
  }

  private fun openTelemetryFor(exporter: InMemorySpanExporter): OpenTelemetrySdk {
    val tracerProvider = SdkTracerProvider.builder()
      .addSpanProcessor(SimpleSpanProcessor.create(exporter))
      .build()
    return OpenTelemetrySdk.builder()
      .setTracerProvider(tracerProvider)
      .build()
  }
}
