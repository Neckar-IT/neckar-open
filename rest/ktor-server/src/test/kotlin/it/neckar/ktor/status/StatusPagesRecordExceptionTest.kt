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
package it.neckar.ktor.status

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import io.mockk.mockk
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.api.trace.StatusCode
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter
import io.opentelemetry.sdk.trace.SdkTracerProvider
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor
import it.neckar.ktor.installJson
import it.neckar.ktor.installOpenTelemetry
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.Test
import it.neckar.open.test.utils.DisableLogging

/**
 * An exception `installStatusPages` catches must still reach the OTel server span: `KtorServerTelemetry`
 * from `installOpenTelemetry` records it, so the full stack trace — the whole `cause` chain — reaches
 * Tempo/Grafana. Only then does the trace the error page links to contain the reason a 5xx body leaves out.
 *
 * Two paths are covered: a ContentNegotiation deserialization failure (surfaces as a
 * [io.ktor.server.plugins.BadRequestException], caught by the specific handler) and an unexpected
 * [RuntimeException] (caught by the `Throwable` fallback). Both must leave an `exception` event on the
 * span carrying the reason in `exception.stacktrace`.
 */
@DisableLogging
class StatusPagesRecordExceptionTest {

  private fun buildTestOpenTelemetry(spanExporter: InMemorySpanExporter): OpenTelemetrySdk {
    val tracerProvider = SdkTracerProvider.builder()
      .addSpanProcessor(SimpleSpanProcessor.create(spanExporter))
      .build()
    return OpenTelemetrySdk.builder()
      .setTracerProvider(tracerProvider)
      .build()
  }

  @Test
  fun `content-negotiation failure records the exception with the offending discriminator on the span`() {
    val spanExporter = InMemorySpanExporter.create()
    val openTelemetry = buildTestOpenTelemetry(spanExporter)
    val logger: org.slf4j.Logger = mockk(relaxed = true)

    testApplication {
      application {
        installOpenTelemetry(openTelemetry)
        installStatusPages(logger)
        installJson { }
        routing {
          post("/enqueue") {
            call.receive<EnqueueRequest>()
            call.respondText("ok")
          }
        }
      }

      // Producer sends the pre-rename discriminator "inline"; this consumer only knows "provided".
      client.post("/enqueue") {
        contentType(ContentType.Application.Json)
        setBody("""{"token":{"type":"inline","secret":"glpat-x"}}""")
      }
    }

    val span = spanExporter.finishedSpanItems.single { it.kind == SpanKind.SERVER }
    assertThat(span.status.statusCode).isEqualTo(StatusCode.ERROR)

    val exceptionEvent = span.events.single { it.name == "exception" }
    val type = exceptionEvent.attributes.get(AttributeKey.stringKey("exception.type"))
    val stacktrace = exceptionEvent.attributes.get(AttributeKey.stringKey("exception.stacktrace"))

    // The outer throwable is Ktor's ContentNegotiation wrapper …
    assertThat(type).isEqualTo("io.ktor.server.plugins.BadRequestException")
    // … and the recorded stack trace carries the full cause chain, down to the kotlinx reason.
    assertThat(stacktrace).isNotNull().contains("inline")
    assertThat(stacktrace).isNotNull().contains("JsonDecodingException")
    assertThat(stacktrace).isNotNull().contains("CredentialLike")
  }

  @Test
  fun `an unexpected throwable is recorded on the span`() {
    val spanExporter = InMemorySpanExporter.create()
    val openTelemetry = buildTestOpenTelemetry(spanExporter)
    val logger: org.slf4j.Logger = mockk(relaxed = true)

    testApplication {
      application {
        installOpenTelemetry(openTelemetry)
        installStatusPages(logger)
        routing {
          get("/boom") {
            throw RuntimeException("boom-marker-42")
          }
        }
      }

      client.get("/boom")
    }

    val span = spanExporter.finishedSpanItems.single { it.kind == SpanKind.SERVER }
    val exceptionEvent = span.events.single { it.name == "exception" }
    val stacktrace = exceptionEvent.attributes.get(AttributeKey.stringKey("exception.stacktrace"))
    assertThat(stacktrace).isNotNull().contains("boom-marker-42")
  }

  @Serializable
  private data class EnqueueRequest(
    val token: CredentialLike,
  )

  @Serializable
  private sealed interface CredentialLike {
    @Serializable
    @SerialName("provided")
    data class Provided(val secret: String) : CredentialLike
  }
}
