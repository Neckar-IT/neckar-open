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
import assertk.assertions.doesNotContain
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import it.neckar.open.health.error.GlobalExceptionCollector
import it.neckar.open.health.error.fromUncaughtExceptions
import it.neckar.open.test.utils.DisableLogging
import it.neckar.open.time.now
import it.neckar.open.tracing.TraceId
import it.neckar.rest.error.GlobalHealth
import it.neckar.rest.health.DependencyName
import it.neckar.rest.health.ServiceStateDto
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test

@DisableLogging
class GlobalHealthProjectionTest {
  @Test
  fun `ready without a down dependency is healthy`() {
    val holder = readyHolder()
    holder.updateDependency(DependencyName.MongoDb, DependencyClassification.Required, DependencyStatus.Up())

    assertThat(GlobalHealth.from(holder)).isEqualTo(GlobalHealth.Healthy)
  }

  @Test
  fun `a down required dependency is unhealthy with dependency-unavailable`() {
    val holder = readyHolder()
    holder.updateDependency(
      DependencyName.MongoDb,
      DependencyClassification.Required,
      DependencyStatus.Down(RuntimeException("timeout after 2s"), now()),
    )

    val unhealthy = GlobalHealth.from(holder) as GlobalHealth.Unhealthy

    assertThat(unhealthy.state).isEqualTo(ServiceStateDto.DependencyUnavailable)
    assertThat(unhealthy.errorType).isEqualTo("java.lang.RuntimeException")
    assertThat(unhealthy.errorCount).isEqualTo(1)
    assertThat(unhealthy.message).isEqualTo("mongodb is down: java.lang.RuntimeException")
  }

  @Test
  fun `a down optional dependency is unhealthy with degraded`() {
    val holder = readyHolder()
    holder.updateDependency(
      DependencyName.S3,
      DependencyClassification.Optional,
      DependencyStatus.Down(RuntimeException("headBucket failed"), now()),
    )

    val unhealthy = GlobalHealth.from(holder) as GlobalHealth.Unhealthy
    assertThat(unhealthy.state).isEqualTo(ServiceStateDto.Degraded)
    assertThat(unhealthy.message).isEqualTo("s3 is down: java.lang.RuntimeException")
  }

  @Test
  fun `starting is unhealthy and carries no error type`() {
    val unhealthy = GlobalHealth.from(ServiceStateHolder()) as GlobalHealth.Unhealthy

    assertThat(unhealthy.state).isEqualTo(ServiceStateDto.Starting)
    assertThat(unhealthy.errorType).isNull()
    assertThat(unhealthy.errorCount).isEqualTo(0)
    assertThat(unhealthy.message).isEqualTo("service state: starting")
  }

  @Test
  fun `fatal reports the reason and the exception type, never the message`() {
    val holder = ServiceStateHolder()
    holder.transitionTo(
      ServiceState.Fatal(
        reason = FatalReason.SchemaIncompatible,
        message = "restore from [backup-2026-09-02] — SECRET-MARKER",
        errorType = "it.neckar.open.mongodb.migration.MigrationBlockedException",
        traceId = TraceId("0af7651916cd43dd8448eb211c80319c"),
      )
    )

    val unhealthy = GlobalHealth.from(holder) as GlobalHealth.Unhealthy
    assertThat(unhealthy.state).isEqualTo(ServiceStateDto.SchemaIncompatible)
    assertThat(unhealthy.message).isEqualTo("service state: schema-incompatible")
    assertThat(unhealthy.errorType).isEqualTo("it.neckar.open.mongodb.migration.MigrationBlockedException")
    assertThat(unhealthy.traceId).isEqualTo(TraceId("0af7651916cd43dd8448eb211c80319c"))
  }

  /**
   * The whole point of the projection: nothing a caller reads may come out of the exception message.
   */
  @Test
  fun `no part of the fatal message reaches the response`() {
    val holder = ServiceStateHolder()
    holder.transitionTo(ServiceState.Fatal(FatalReason.MigrationFailed, "connection to mongodb://user@db-01 failed: SECRET-MARKER"))

    val unhealthy = GlobalHealth.from(holder) as GlobalHealth.Unhealthy
    assertThat(Json.encodeToString(GlobalHealth.serializer(), unhealthy)).doesNotContain("SECRET-MARKER")
  }

  /**
   * A recorded uncaught exception makes the service unhealthy, but leaves the state at
   * [ServiceStateDto.Ready] — it does not gate readiness.
   */
  @Test
  fun `a recorded uncaught exception is unhealthy but stays ready`() {
    val holder = readyHolder()
    holder.recordUncaughtException(NullPointerException("null customer"))

    val unhealthy = GlobalHealth.from(holder) as GlobalHealth.Unhealthy
    assertThat(unhealthy.state).isEqualTo(ServiceStateDto.Ready)
    assertThat(unhealthy.errorType).isEqualTo("java.lang.NullPointerException")
    assertThat(unhealthy.errorCount).isEqualTo(1)
    assertThat(unhealthy.message).isEqualTo("uncaught exception: java.lang.NullPointerException")
  }

  /**
   * A down dependency is the more actionable cause and wins over an uncaught exception.
   */
  @Test
  fun `a down dependency takes precedence over an uncaught exception`() {
    val holder = readyHolder()
    holder.recordUncaughtException(NullPointerException("null customer"))
    holder.updateDependency(
      DependencyName.MongoDb,
      DependencyClassification.Required,
      DependencyStatus.Down(RuntimeException("connection refused"), now()),
    )

    val unhealthy = GlobalHealth.from(holder) as GlobalHealth.Unhealthy
    assertThat(unhealthy.state).isEqualTo(ServiceStateDto.DependencyUnavailable)
    assertThat(unhealthy.message).isEqualTo("mongodb is down: java.lang.RuntimeException")
  }

  /**
   * The deprecated [fromUncaughtExceptions] reports the same verdict for services that do not use
   * the health layer yet: unhealthy, state ready.
   */
  @Test
  fun `the legacy path reports the same verdict as the state holder`() {
    GlobalExceptionCollector.clearErrors()
    GlobalExceptionCollector.addError(RuntimeException("an application error"))

    try {
      @Suppress("DEPRECATION")
      val legacy = GlobalHealth.fromUncaughtExceptions() as GlobalHealth.Unhealthy
      assertThat(legacy.state).isEqualTo(ServiceStateDto.Ready)
      assertThat(legacy.errorType).isEqualTo("java.lang.RuntimeException")
      assertThat(legacy.errorCount).isEqualTo(1)
    } finally {
      GlobalExceptionCollector.clearErrors()
    }
  }

  /**
   * The health endpoints are unauthenticated and exist for orchestration. An exception message may
   * carry connection details or business data and belongs into the OpenTelemetry span, not onto the
   * wire.
   */
  @Test
  fun `no exception message reaches the wire`() {
    val secret = "customer 4711 of tenant acme, mongodb://admin:hunter2@db:27017"
    val holder = readyHolder()
    holder.recordUncaughtException(NullPointerException(secret))
    holder.updateDependency(
      DependencyName.S3,
      DependencyClassification.Optional,
      DependencyStatus.Down(RuntimeException(secret), now()),
    )

    // Every field of the response bodies, via the data classes' toString.
    assertThat(GlobalHealth.from(holder).toString()).doesNotContain(secret)
    assertThat(HealthProjection.healthResponse(holder.state, holder.dependencies, holder.uncaughtExceptions).toString()).doesNotContain(secret)
    assertThat(HealthProjection.readinessResponse(holder.state, holder.dependencies).toString()).doesNotContain(secret)
  }

  private fun readyHolder(): ServiceStateHolder = ServiceStateHolder().apply {
    transitionTo(ServiceState.Verifying)
    transitionTo(ServiceState.Ready)
  }
}
