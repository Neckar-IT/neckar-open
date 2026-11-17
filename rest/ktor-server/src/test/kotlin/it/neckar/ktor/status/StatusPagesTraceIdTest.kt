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
import assertk.assertions.doesNotContain
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import io.mockk.mockk
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.instrumentation.ktor.v3_0.KtorServerTelemetry
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter
import io.opentelemetry.sdk.trace.SdkTracerProvider
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor
import it.neckar.ktor.installJson
import it.neckar.ktor.tracking.currentTraceId
import it.neckar.open.authorization.CapabilityKey
import it.neckar.open.authorization.MissingCapabilityException
import it.neckar.open.exceptions.ApplicationException
import it.neckar.open.exceptions.ErrorCode
import it.neckar.open.test.utils.DisableLogging
import it.neckar.open.tracing.TraceId
import it.neckar.problem.ApplicationError
import it.neckar.problem.ExpectedApplicationError
import it.neckar.problem.UnexpectedApplicationError
import it.neckar.problem.toUnexpectedProblem
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test

/**
 * The error body points at the span that holds the detail — the rule every error surface follows.
 */
@DisableLogging
class StatusPagesTraceIdTest {
  @Test
  fun `an unexpected exception answers with the trace of the server span`() {
    val spanExporter = InMemorySpanExporter.create()

    testApplication {
      application {
        installTracedStatusPages(spanExporter)

        routing {
          get("/api/boom") {
            throw RuntimeException("This is a test exception")
          }
        }
      }

      val error = tracedClient().get("/api/boom").body<ApplicationError>()
      assertThat((error as UnexpectedApplicationError).traceId).isEqualTo(serverTraceId(spanExporter))
    }
  }

  @Test
  fun `a missing capability answers with the trace of the server span`() {
    val spanExporter = InMemorySpanExporter.create()

    testApplication {
      application {
        installTracedStatusPages(spanExporter)

        routing {
          get("/api/forbidden") {
            throw MissingCapabilityException(CapabilityKey("core:operate-product"))
          }
        }
      }

      val error = tracedClient().get("/api/forbidden").body<ApplicationError>()
      assertThat((error as ExpectedApplicationError).traceId).isEqualTo(serverTraceId(spanExporter))
      assertThat(error.attributes?.get(MissingCapabilityException.MissingCapabilityAttribute)).isEqualTo("core:operate-product")
    }
  }

  @Test
  fun `an application exception answers with the trace of the server span`() {
    val spanExporter = InMemorySpanExporter.create()

    testApplication {
      application {
        installTracedStatusPages(spanExporter)

        routing {
          get("/api/expected") {
            throw TestApplicationException()
          }
        }
      }

      val error = tracedClient().get("/api/expected").body<ApplicationError>()
      assertThat((error as ExpectedApplicationError).traceId).isEqualTo(serverTraceId(spanExporter))
    }
  }

  /**
   * A service registers its own handlers through the `config()` block. They see the same span as the
   * built-in ones — the trace ID is read at request time, inside the call's coroutine context.
   */
  @Test
  fun `a service-supplied handler sees the same span`() {
    val spanExporter = InMemorySpanExporter.create()

    testApplication {
      application {
        install(KtorServerTelemetry) {
          setOpenTelemetry(
            OpenTelemetrySdk.builder()
              .setTracerProvider(
                SdkTracerProvider.builder()
                  .addSpanProcessor(SimpleSpanProcessor.create(spanExporter))
                  .build()
              )
              .build()
          )
        }
        installStatusPages(logger = mockk(relaxed = true)) {
          exception<IllegalStateException> { call, cause ->
            call.respond<ApplicationError>(HttpStatusCode.Conflict, cause.toUnexpectedProblem(currentTraceId()))
          }
        }
        installJson { }

        routing {
          get("/api/conflict") {
            throw IllegalStateException("a service-specific failure")
          }
        }
      }

      val response = tracedClient().get("/api/conflict")
      assertThat(response.status).isEqualTo(HttpStatusCode.Conflict)
      assertThat((response.body<ApplicationError>() as UnexpectedApplicationError).traceId).isEqualTo(serverTraceId(spanExporter))
    }
  }

  /**
   * Local development without `OTEL_TRACES_EXPORTER`: there is no span to point at, and the field
   * stays out of the body rather than carrying an ID that resolves to nothing.
   */
  @Test
  fun `without OpenTelemetry the error body carries no trace`() {
    testApplication {
      application {
        installStatusPages(logger = mockk(relaxed = true), config = {})
        installJson { }

        routing {
          get("/api/boom") {
            throw RuntimeException("This is a test exception")
          }
        }
      }

      val error = tracedClient().get("/api/boom").body<ApplicationError>()
      assertThat((error as UnexpectedApplicationError).traceId).isNull()
    }
  }

  /**
   * Both serialization handlers answered a bare `text/plain` string before; the structured body is
   * what carries the trace at all.
   */
  @Test
  fun `a json decoding failure answers a structured body with the trace and without the raw input`() {
    val spanExporter = InMemorySpanExporter.create()

    testApplication {
      application {
        installTracedStatusPages(spanExporter)

        routing {
          get("/api/decode") {
            Json.decodeFromString(MapSerializer(String.serializer(), String.serializer()), """{"secret": "SECRET-MARKER" """)
          }
        }
      }

      val response = tracedClient().get("/api/decode")
      val error = response.body<ApplicationError>() as UnexpectedApplicationError
      assertThat(error.traceId).isEqualTo(serverTraceId(spanExporter))
      assertThat(error.exceptionType).contains("JsonDecodingException")
      assertThat(response.bodyAsText()).doesNotContain("SECRET-MARKER")
    }
  }

  @Test
  fun `a serialization failure answers a structured body with the trace`() {
    val spanExporter = InMemorySpanExporter.create()

    testApplication {
      application {
        installTracedStatusPages(spanExporter)

        routing {
          get("/api/serialize") {
            throw SerializationException("cannot serialize")
          }
        }
      }

      val error = tracedClient().get("/api/serialize").body<ApplicationError>() as UnexpectedApplicationError
      assertThat(error.traceId).isEqualTo(serverTraceId(spanExporter))
      assertThat(error.fallbackMessage).isEqualTo("cannot serialize")
    }
  }

  private class TestApplicationException : ApplicationException(cause = null) {
    override val errorCode: ErrorCode = ErrorCode(ErrorCode.Prefix("Test"), 42)
    override val parameters: Map<String, Any>? = null
    override val fallbackMessage: String = "a test application exception"
  }

  private fun Application.installTracedStatusPages(spanExporter: InMemorySpanExporter) {
    install(KtorServerTelemetry) {
      setOpenTelemetry(
        OpenTelemetrySdk.builder()
          .setTracerProvider(
            SdkTracerProvider.builder()
              .addSpanProcessor(SimpleSpanProcessor.create(spanExporter))
              .build()
          )
          .build()
      )
    }
    installStatusPages(logger = mockk(relaxed = true), config = {})
    installJson { }
  }

  private fun serverTraceId(spanExporter: InMemorySpanExporter): TraceId =
    TraceId(spanExporter.finishedSpanItems.single { it.kind == SpanKind.SERVER }.spanContext.traceId)

  private fun ApplicationTestBuilder.tracedClient() = createClient {
    install(ContentNegotiation) {
      json()
    }
  }
}
