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
import assertk.assertions.isEqualTo
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
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
import it.neckar.open.test.utils.DisableLogging
import it.neckar.open.tracing.TraceId
import it.neckar.problem.ApplicationError
import it.neckar.problem.UnexpectedApplicationError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.junit.jupiter.api.Test

/**
 * Whether the trace survives the unwinding: the exception is thrown deep in a suspending call chain,
 * caught by the status pages far above it, and the trace ID is read there — after suspension points
 * and after the coroutine has changed threads.
 */
@DisableLogging
class StatusPagesTraceIdUnwindTest {
  @Test
  @Suppress("RealTimeDelayInTest") // the handler suspends for real before it throws, which is the case under test
  fun `a throw behind a suspension point still answers the server span`() {
    val spanExporter = InMemorySpanExporter.create()

    testApplication {
      application {
        installTracedStatusPages(spanExporter)

        routing {
          get("/api/suspended") {
            delay(5)
            throw IllegalStateException("thrown after suspending")
          }
        }
      }

      assertThat(traceIdOfErrorBody("/api/suspended")).isEqualTo(serverTraceId(spanExporter))
    }
  }

  @Test
  fun `a throw from another dispatcher still answers the server span`() {
    val spanExporter = InMemorySpanExporter.create()

    testApplication {
      application {
        installTracedStatusPages(spanExporter)

        routing {
          get("/api/dispatched") {
            loadFromStorage()
          }
        }
      }

      assertThat(traceIdOfErrorBody("/api/dispatched")).isEqualTo(serverTraceId(spanExporter))
    }
  }

  /**
   * The shape a service actually has: routing calls a support layer, which calls a data-access layer,
   * which fails on a worker thread.
   */
  private suspend fun loadFromStorage(): Nothing = queryDatabase()

  private suspend fun queryDatabase(): Nothing = withContext(Dispatchers.IO) {
    delay(5)
    throw IllegalStateException("thrown on a worker thread")
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

  private suspend fun ApplicationTestBuilder.traceIdOfErrorBody(path: String): TraceId? {
    val client = createClient {
      install(ContentNegotiation) {
        json()
      }
    }

    return (client.get(path).body<ApplicationError>() as UnexpectedApplicationError).traceId
  }

  private fun serverTraceId(spanExporter: InMemorySpanExporter): TraceId =
    TraceId(spanExporter.finishedSpanItems.single { it.kind == SpanKind.SERVER }.spanContext.traceId)
}
