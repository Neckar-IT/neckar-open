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
import assertk.assertions.isEqualTo
import io.opentelemetry.api.trace.Tracer
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter
import io.opentelemetry.sdk.trace.SdkTracerProvider
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Verifies that [withSpan] (without explicit name) auto-detects
 * the caller's class and method name via [StackWalker].
 */
class WithSpanAutoNameTest {

  private val spanExporter = InMemorySpanExporter.create()

  private val otel: OpenTelemetrySdk = OpenTelemetrySdk.builder()
    .setTracerProvider(
      SdkTracerProvider.builder()
        .addSpanProcessor(SimpleSpanProcessor.create(spanExporter))
        .build()
    )
    .build()

  private val tracer = otel.getTracer("test")

  @BeforeEach
  fun setUp() {
    spanExporter.reset()
  }

  @Test
  suspend fun `auto-detected name from suspend function`() {
    myBusinessMethod()

    val spans = spanExporter.finishedSpanItems
    assertThat(spans.size).isEqualTo(1)
    assertThat(spans.single().name).isEqualTo("WithSpanAutoNameTest.myBusinessMethod")
  }

  @Test
  suspend fun `explicit name still works`() {
    tracer.withSpan("CustomName") {
      // simulate work
    }

    val spans = spanExporter.finishedSpanItems
    assertThat(spans.size).isEqualTo(1)
    assertThat(spans.single().name).isEqualTo("CustomName")
  }

  @Test
  suspend fun `auto-detected name from separate business class`() {
    val support = FakeBookSupport(tracer)
    support.find()

    val spans = spanExporter.finishedSpanItems
    assertThat(spans.size).isEqualTo(1)
    assertThat(spans.single().name).isEqualTo("FakeBookSupport.find")
  }

  @Test
  suspend fun `auto-detected name from multiple methods in same class`() {
    val support = FakeBookSupport(tracer)
    support.find()
    support.store()

    val spans = spanExporter.finishedSpanItems
    assertThat(spans.size).isEqualTo(2)
    assertThat(spans[0].name).isEqualTo("FakeBookSupport.find")
    assertThat(spans[1].name).isEqualTo("FakeBookSupport.store")
  }

  @Test
  suspend fun `auto-detected name strips Kotlin name-mangling suffix`() {
    val support = FakeBookSupport(tracer)
    val _ = support.findById(FakeId("abc"))

    val spans = spanExporter.finishedSpanItems
    assertThat(spans.size).isEqualTo(1)

    // The JVM method name is "findById-<hash>" (due to value class parameter).
    // resolveCallerSpanName strips the "-<hash>" suffix.
    assertThat(spans.single().name).isEqualTo("FakeBookSupport.findById")
  }

  private suspend fun myBusinessMethod() {
    tracer.withSpan {
      // simulate work
    }
  }
}

/**
 * Simulates a business-logic Support class calling [withSpan] without explicit name.
 *
 * A separate class, not a method of the test itself: `resolveCallerSpanName` skips frames of the
 * tracing package and takes the first one after them, so a caller in its own class is the shape
 * production has and the one that would break if the frame skipping were off by one.
 */
private class FakeBookSupport(
  private val tracer: Tracer,
) {
  suspend fun find() = tracer.withSpan {
    // simulate work
  }

  suspend fun store() = tracer.withSpan {
    // simulate work
  }

  /**
   * Method with value class parameter — Kotlin mangles the JVM name
   * (e.g., `findById-<hash>`) to avoid signature clashes after erasure.
   */
  suspend fun findById(id: FakeId) = tracer.withSpan {
    id // use parameter to prevent removal
  }
}

/**
 * Value class that triggers Kotlin name mangling on methods using it as a parameter.
 */
@JvmInline
private value class FakeId(val value: String)
