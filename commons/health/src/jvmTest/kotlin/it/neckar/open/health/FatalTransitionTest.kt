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
package it.neckar.open.health

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.trace.StatusCode
import io.opentelemetry.api.trace.TracerProvider
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter
import io.opentelemetry.sdk.trace.SdkTracerProvider
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor
import it.neckar.open.test.utils.DisableLogging
import it.neckar.open.time.now
import it.neckar.open.tracing.TraceId
import it.neckar.rest.error.GlobalHealth
import it.neckar.rest.health.DependencyName
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@DisableLogging
class FatalTransitionTest {
  private lateinit var exporter: InMemorySpanExporter
  private lateinit var tracerProvider: SdkTracerProvider

  @BeforeEach
  fun setUp() {
    exporter = InMemorySpanExporter.create()
    tracerProvider = SdkTracerProvider.builder()
      .addSpanProcessor(SimpleSpanProcessor.create(exporter))
      .build()
  }

  @AfterEach
  fun tearDown() {
    tracerProvider.shutdown()
  }

  @Test
  fun `records the exception as an error span and keeps its trace on the state`() {
    val holder = ServiceStateHolder()

    holder.transitionToFatal(tracerProvider.get("test"), FatalReason.MigrationFailed, IllegalStateException("restore from [backup-01]"))

    val span = exporter.finishedSpanItems.single()
    assertThat(span.name).isEqualTo("service-fatal")
    assertThat(span.status.statusCode).isEqualTo(StatusCode.ERROR)
    assertThat(span.events.single { it.name == "exception" }.attributes.get(AttributeKey.stringKey("exception.message")))
      .isEqualTo("restore from [backup-01]")

    val fatal = holder.state as ServiceState.Fatal
    assertThat(fatal.reason).isEqualTo(FatalReason.MigrationFailed)
    assertThat(fatal.message).isEqualTo("restore from [backup-01]")
    assertThat(fatal.errorType).isEqualTo("java.lang.IllegalStateException")
    assertThat(fatal.traceId).isEqualTo(TraceId(span.traceId))
  }

  @Test
  fun `the recorded trace is the one the health projection hands out`() {
    val holder = ServiceStateHolder()

    holder.transitionToFatal(tracerProvider.get("test"), FatalReason.MigrationFailed, IllegalStateException("boom"))

    val recordedTrace = TraceId(exporter.finishedSpanItems.single().traceId)
    assertThat((GlobalHealth.from(holder) as GlobalHealth.Unhealthy).traceId).isEqualTo(recordedTrace)
    assertThat(HealthProjection.healthResponse(holder.state, holder.dependencies, holder.uncaughtExceptions).traceId).isEqualTo(recordedTrace)
  }

  /**
   * Local development without `OTEL_TRACES_EXPORTER`: the span is a no-op, and the state says so
   * rather than carrying the all-zero ID.
   */
  @Test
  fun `without a configured exporter the fatal state carries no trace`() {
    val holder = ServiceStateHolder()

    holder.transitionToFatal(TracerProvider.noop().get("test"), FatalReason.MigrationFailed, IllegalStateException("boom"))

    assertThat((holder.state as ServiceState.Fatal).traceId).isNull()
    assertThat((GlobalHealth.from(holder) as GlobalHealth.Unhealthy).traceId).isNull()
  }

  @Test
  fun `a fatal service that also lost a dependency still hands out its fatal trace`() {
    val holder = ServiceStateHolder()
    holder.transitionToFatal(tracerProvider.get("test"), FatalReason.MigrationFailed, IllegalStateException("boom"))
    holder.updateDependency(DependencyName.MongoDb, DependencyClassification.Required, DependencyStatus.Down(RuntimeException("timeout"), now()))

    val unhealthy = GlobalHealth.from(holder) as GlobalHealth.Unhealthy
    assertThat(unhealthy.message).isEqualTo("mongodb is down: java.lang.RuntimeException")
    assertThat(unhealthy.traceId).isEqualTo(TraceId(exporter.finishedSpanItems.single().traceId))
  }
}
