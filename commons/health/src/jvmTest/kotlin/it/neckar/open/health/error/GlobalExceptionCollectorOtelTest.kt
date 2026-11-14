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

import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.trace.StatusCode
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter
import io.opentelemetry.sdk.trace.SdkTracerProvider
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor
import it.neckar.open.dispose.Disposable
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class GlobalExceptionCollectorOtelTest {
  private lateinit var exporter: InMemorySpanExporter
  private lateinit var tracerProvider: SdkTracerProvider
  private var registration: Disposable? = null

  @BeforeEach
  fun setUp() {
    GlobalExceptionCollector.clearErrors()
    exporter = InMemorySpanExporter.create()
    tracerProvider = SdkTracerProvider.builder()
      .addSpanProcessor(SimpleSpanProcessor.create(exporter))
      .build()
  }

  @AfterEach
  fun tearDown() {
    registration?.dispose()
    registration = null
    tracerProvider.shutdown()
    GlobalExceptionCollector.clearErrors()
  }

  @Test
  fun `records a dedicated error span per uncaught exception`() {
    registration = GlobalExceptionCollector.recordExceptionsToOpenTelemetry(tracerProvider.get("test"))

    GlobalExceptionCollector.addError(RuntimeException("boom"))

    val spans = exporter.finishedSpanItems
    assertThat(spans).hasSize(1)

    val span = spans.single()
    assertThat(span.name).isEqualTo("uncaught-exception")
    assertThat(span.status.statusCode).isEqualTo(StatusCode.ERROR)

    val exceptionEvent = span.events.single { it.name == "exception" }
    assertThat(exceptionEvent.attributes.get(AttributeKey.stringKey("exception.message"))).isEqualTo("boom")
  }

  @Test
  fun `emits nothing once the registration is disposed`() {
    registration = GlobalExceptionCollector.recordExceptionsToOpenTelemetry(tracerProvider.get("test"))
    registration?.dispose()
    registration = null

    GlobalExceptionCollector.addError(RuntimeException("boom"))

    assertThat(exporter.finishedSpanItems).hasSize(0)
  }
}
