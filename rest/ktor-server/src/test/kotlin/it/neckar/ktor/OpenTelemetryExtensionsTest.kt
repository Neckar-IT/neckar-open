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
package it.neckar.ktor

import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.api.trace.StatusCode
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter
import io.opentelemetry.sdk.trace.SdkTracerProvider
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor
import it.neckar.rest.test.isOk
import org.junit.jupiter.api.Test

class OpenTelemetryExtensionsTest {

  private fun buildTestOpenTelemetry(spanExporter: InMemorySpanExporter): OpenTelemetrySdk {
    val tracerProvider = SdkTracerProvider.builder()
      .addSpanProcessor(SimpleSpanProcessor.create(spanExporter))
      .build()
    return OpenTelemetrySdk.builder()
      .setTracerProvider(tracerProvider)
      .build()
  }

  @Test
  fun `2xx response yields span without ERROR status`() {
    val spanExporter = InMemorySpanExporter.create()
    val openTelemetry = buildTestOpenTelemetry(spanExporter)

    testApplication {
      application {
        installOpenTelemetry(openTelemetry)
        routing {
          get("/ok") {
            call.respondText("ok")
          }
        }
      }

      val response = client.get("/ok")
      assertThat(response).isOk()
    }

    val span = spanExporter.finishedSpanItems.single { it.kind == SpanKind.SERVER }
    assertThat(span.status.statusCode).isEqualTo(StatusCode.UNSET)
  }

  @Test
  fun `the span keeps the query of a request with the values of the OAuth parameters replaced`() {
    val spanExporter = InMemorySpanExporter.create()
    val openTelemetry = buildTestOpenTelemetry(spanExporter)

    testApplication {
      application {
        installOpenTelemetry(openTelemetry)
        routing {
          get("/auth/callback") {
            call.respondText("ok")
          }
        }
      }

      assertThat(client.get("/auth/callback?code=4f1c2e&state=a9b8&session_state=77&lang=de")).isOk()
    }

    val span = spanExporter.finishedSpanItems.single { it.kind == SpanKind.SERVER }
    assertThat(span.attributes.get(AttributeKey.stringKey("url.query"))).isEqualTo("code=REDACTED&state=REDACTED&session_state=REDACTED&lang=de")
  }

  @Test
  fun `a name that only starts with a redacted one, a name without value and every repetition are told apart`() {
    assertThat(redactQueryParameters("codex=1&code&code=a&code=b&state=", OAuthCallbackQueryParameters)).isEqualTo("codex=1&code&code=REDACTED&code=REDACTED&state=REDACTED")
  }

  @Test
  fun `4xx response marks span as ERROR`() {
    val spanExporter = InMemorySpanExporter.create()
    val openTelemetry = buildTestOpenTelemetry(spanExporter)

    testApplication {
      application {
        installOpenTelemetry(openTelemetry)
        routing {
          get("/missing") {
            call.respondText("not found", status = HttpStatusCode.NotFound)
          }
        }
      }

      val response = client.get("/missing")
      assertThat(response.status).isEqualTo(HttpStatusCode.NotFound)
    }

    val span = spanExporter.finishedSpanItems.single { it.kind == SpanKind.SERVER }
    assertThat(span.status.statusCode).isEqualTo(StatusCode.ERROR)
  }

  @Test
  fun `5xx response marks span as ERROR`() {
    val spanExporter = InMemorySpanExporter.create()
    val openTelemetry = buildTestOpenTelemetry(spanExporter)

    testApplication {
      application {
        installOpenTelemetry(openTelemetry)
        routing {
          get("/boom") {
            call.respondText("server error", status = HttpStatusCode.InternalServerError)
          }
        }
      }

      val response = client.get("/boom")
      assertThat(response.status).isEqualTo(HttpStatusCode.InternalServerError)
    }

    val span = spanExporter.finishedSpanItems.single { it.kind == SpanKind.SERVER }
    assertThat(span.status.statusCode).isEqualTo(StatusCode.ERROR)
  }

  @Test
  fun `CallId MDC entry is copied to span as mdc CallId attribute`() {
    val spanExporter = InMemorySpanExporter.create()
    val openTelemetry = buildTestOpenTelemetry(spanExporter)

    testApplication {
      application {
        installOpenTelemetry(openTelemetry)
        installCallId()
        installCallLogging()
        routing {
          get("/ok") {
            call.respondText("ok")
          }
        }
      }

      val response = client.get("/ok")
      assertThat(response).isOk()
    }

    val span = spanExporter.finishedSpanItems.single { it.kind == SpanKind.SERVER }
    val callIdAttribute = span.attributes.get(AttributeKey.stringKey("mdc.CallId"))
    assertThat(callIdAttribute).isNotNull()
  }

  @Test
  fun `unknown MDC entries are not copied to the span`() {
    val spanExporter = InMemorySpanExporter.create()
    val openTelemetry = buildTestOpenTelemetry(spanExporter)

    testApplication {
      application {
        installOpenTelemetry(openTelemetry)
        // No CallId / CallLogging installed → no allow-listed MDC entry is populated.
        routing {
          get("/ok") {
            call.respondText("ok")
          }
        }
      }

      val response = client.get("/ok")
      assertThat(response).isOk()
    }

    val span = spanExporter.finishedSpanItems.single { it.kind == SpanKind.SERVER }
    val mdcAttributes = span.attributes.asMap().keys.filter { it.key.startsWith("mdc.") }
    assertThat(mdcAttributes).isEqualTo(emptyList())
    assertThat(span.attributes.get(AttributeKey.stringKey("mdc.CallId"))).isNull()
  }
}
