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

import assertk.*
import assertk.assertions.*
import it.neckar.open.time.VirtualNowProvider
import it.neckar.open.time.now
import it.neckar.rest.health.DependencyName
import it.neckar.rest.health.ServiceStateDto
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.minutes

class HealthProjectionTest {
  private val mongoUp = DependencyState(DependencyName.MongoDb, DependencyClassification.Required, DependencyStatus.Up())
  private val mongoDown = DependencyState(DependencyName.MongoDb, DependencyClassification.Required, DependencyStatus.Down(RuntimeException("boom"), now()))
  private val s3OptionalDown = DependencyState(DependencyName.S3, DependencyClassification.Optional, DependencyStatus.Down(RuntimeException("boom"), now()))

  @Test
  fun `starting is not live and not ready`() {
    assertThat(HealthProjection.liveStatus(ServiceState.Starting)).isEqualTo(HealthHttpStatus.ServiceUnavailable)
    assertThat(HealthProjection.wireState(ServiceState.Starting, emptyList())).isEqualTo(ServiceStateDto.Starting)
    assertThat(HealthProjection.readinessStatus(ServiceState.Starting, emptyList())).isEqualTo(HealthHttpStatus.ServiceUnavailable)
  }

  @Test
  fun `verifying is live but not ready and projects to starting`() {
    assertThat(HealthProjection.liveStatus(ServiceState.Verifying)).isEqualTo(HealthHttpStatus.Ok)
    assertThat(HealthProjection.wireState(ServiceState.Verifying, emptyList())).isEqualTo(ServiceStateDto.Starting)
    assertThat(HealthProjection.readinessStatus(ServiceState.Verifying, emptyList())).isEqualTo(HealthHttpStatus.ServiceUnavailable)
  }

  @Test
  fun `migrating is live but not ready`() {
    assertThat(HealthProjection.liveStatus(ServiceState.Migrating)).isEqualTo(HealthHttpStatus.Ok)
    assertThat(HealthProjection.wireState(ServiceState.Migrating, emptyList())).isEqualTo(ServiceStateDto.Migrating)
    assertThat(HealthProjection.readinessStatus(ServiceState.Migrating, emptyList())).isEqualTo(HealthHttpStatus.ServiceUnavailable)
  }

  @Test
  fun `ready with all dependencies up is ready`() {
    assertThat(HealthProjection.wireState(ServiceState.Ready, listOf(mongoUp))).isEqualTo(ServiceStateDto.Ready)
    assertThat(HealthProjection.readinessStatus(ServiceState.Ready, listOf(mongoUp))).isEqualTo(HealthHttpStatus.Ok)
    assertThat(HealthProjection.liveStatus(ServiceState.Ready)).isEqualTo(HealthHttpStatus.Ok)
  }

  @Test
  fun `required dependency down projects to dependency-unavailable and is not ready`() {
    assertThat(HealthProjection.wireState(ServiceState.Ready, listOf(mongoDown))).isEqualTo(ServiceStateDto.DependencyUnavailable)
    assertThat(HealthProjection.readinessStatus(ServiceState.Ready, listOf(mongoDown))).isEqualTo(HealthHttpStatus.ServiceUnavailable)
    assertThat(HealthProjection.liveStatus(ServiceState.Ready)).isEqualTo(HealthHttpStatus.Ok)
  }

  @Test
  fun `optional dependency down projects to degraded and stays ready`() {
    assertThat(HealthProjection.wireState(ServiceState.Degraded, listOf(mongoUp, s3OptionalDown))).isEqualTo(ServiceStateDto.Degraded)
    assertThat(HealthProjection.readinessStatus(ServiceState.Degraded, listOf(mongoUp, s3OptionalDown))).isEqualTo(HealthHttpStatus.Ok)
  }

  @Test
  fun `required down takes precedence over optional down`() {
    assertThat(HealthProjection.wireState(ServiceState.Degraded, listOf(mongoDown, s3OptionalDown))).isEqualTo(ServiceStateDto.DependencyUnavailable)
  }

  @Test
  fun `fatal schema-incompatible stays live but is not ready`() {
    val state = ServiceState.Fatal(FatalReason.SchemaIncompatible, "wrong schema")
    assertThat(HealthProjection.liveStatus(state)).isEqualTo(HealthHttpStatus.Ok)
    assertThat(HealthProjection.wireState(state, emptyList())).isEqualTo(ServiceStateDto.SchemaIncompatible)
    assertThat(HealthProjection.readinessStatus(state, emptyList())).isEqualTo(HealthHttpStatus.ServiceUnavailable)
  }

  @Test
  fun `fatal migration-failed stays live but is not ready`() {
    val state = ServiceState.Fatal(FatalReason.MigrationFailed, "migration 1.0.0 → 1.1.0 failed")
    assertThat(HealthProjection.liveStatus(state)).isEqualTo(HealthHttpStatus.Ok)
    assertThat(HealthProjection.wireState(state, emptyList())).isEqualTo(ServiceStateDto.MigrationFailed)
    assertThat(HealthProjection.readinessStatus(state, emptyList())).isEqualTo(HealthHttpStatus.ServiceUnavailable)
  }

  @Test
  fun `fatal unspecified stays live but is not ready`() {
    val state = ServiceState.Fatal(FatalReason.Unspecified, "boom")
    assertThat(HealthProjection.liveStatus(state)).isEqualTo(HealthHttpStatus.Ok)
    assertThat(HealthProjection.wireState(state, emptyList())).isEqualTo(ServiceStateDto.Fatal)
    assertThat(HealthProjection.readinessStatus(state, emptyList())).isEqualTo(HealthHttpStatus.ServiceUnavailable)
  }

  /**
   * The stale job must be reported without touching the state: a service whose polling job wedged is
   * still ready to serve HTTP, and taking it out of the load balancer would not revive the job.
   */
  @Test
  fun `health response reports a stale background job without changing the state`() {
    val registeredAt = now()
    val registeredMark = VirtualNowProvider(registeredAt.toEpochMilliseconds().toDouble()).markNow()
    val staleJob = BackgroundJobState(
      name = BackgroundJobName("target-polling"),
      stalenessThreshold = 5.minutes,
      registeredMark = registeredMark,
      lastCycleStartedAt = registeredAt + 5.minutes,
      lastCycleCompletedAt = registeredAt,
      lastCycleCompletedMark = registeredMark,
      completedCycles = 7,
      failedCycles = 2,
    )

    val response = HealthProjection.healthResponse(
      ServiceState.Ready,
      listOf(mongoUp),
      uncaughtExceptions = null,
      backgroundJobs = listOf(staleJob),
      now = registeredMark + 6.minutes,
    )

    assertThat(response.state).isEqualTo(ServiceStateDto.Ready)
    val reported = response.backgroundJobs.single()
    assertThat(reported.name).isEqualTo("target-polling")
    assertThat(reported.stale).isTrue()
    assertThat(reported.completedCycles).isEqualTo(7)
    assertThat(reported.failedCycles).isEqualTo(2)
    assertThat(reported.lastCycleCompletedAt).isEqualTo(registeredAt)
    //A cycle started after the last completed one and never finished — the hang, not a dead loop
    assertThat(reported.lastCycleStartedAt).isEqualTo(registeredAt + 5.minutes)
  }

  @Test
  fun `health response reports no background jobs when none are registered`() {
    assertThat(HealthProjection.healthResponse(ServiceState.Ready, listOf(mongoUp), uncaughtExceptions = null).backgroundJobs).isEmpty()
  }

  @Test
  fun `health response reports last error and count from down dependencies`() {
    val response = HealthProjection.healthResponse(ServiceState.Ready, listOf(mongoDown, s3OptionalDown), uncaughtExceptions = null)
    assertThat(response.errorCount).isEqualTo(2)
    assertThat(response.lastError?.type).isEqualTo("java.lang.RuntimeException")
    assertThat(response.uncaughtExceptions).isNull()
  }

  @Test
  fun `health response reports uncaught exceptions without changing the state`() {
    val recordedAt = now()
    val uncaughtExceptions = UncaughtExceptions(NullPointerException("null customer"), count = 3, since = recordedAt)

    val response = HealthProjection.healthResponse(ServiceState.Ready, listOf(mongoUp), uncaughtExceptions)

    assertThat(response.state).isEqualTo(ServiceStateDto.Ready)
    assertThat(response.lastError).isNull()
    assertThat(response.errorCount).isEqualTo(0)
    assertThat(response.uncaughtExceptions?.type).isEqualTo("java.lang.NullPointerException")
    assertThat(response.uncaughtExceptions?.count).isEqualTo(3)
    assertThat(response.uncaughtExceptions?.since).isEqualTo(recordedAt)
  }
}
