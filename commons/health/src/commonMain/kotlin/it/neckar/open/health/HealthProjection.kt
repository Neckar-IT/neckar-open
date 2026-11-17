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

import it.neckar.open.exceptions.exceptionTypeName
import it.neckar.open.time.monotonicTimeSource
import it.neckar.rest.health.BackgroundJobDto
import it.neckar.rest.health.DependencyName
import it.neckar.rest.health.DependencyReachability
import it.neckar.rest.health.DependencyStatusDto
import it.neckar.rest.health.HealthError
import it.neckar.rest.health.HealthResponse
import it.neckar.rest.health.ReadinessResponse
import it.neckar.rest.health.ServiceStateDto
import it.neckar.rest.health.UncaughtExceptionsDto
import kotlin.time.ComparableTimeMark

/**
 * Pure projection of the [ServiceStateHolder] — and, for `/health`, of the [BackgroundJobRegistry]
 * states the caller reads — onto the probe responses. No I/O, no framework.
 *
 * The probes call these functions and serve the result; they never compute the state themselves.
 */
object HealthProjection {
  /**
   * Liveness: `Ok` as soon as the HTTP server is up. Only [ServiceState.Starting] is not-live;
   * [ServiceState.Fatal] deliberately stays live so no health-check restart loops on a permanent error.
   */
  fun liveStatus(state: ServiceState): HealthHttpStatus = when (state) {
    ServiceState.Starting -> HealthHttpStatus.ServiceUnavailable
    ServiceState.Verifying,
    ServiceState.Migrating,
    ServiceState.Ready,
    ServiceState.Degraded,
    is ServiceState.Fatal,
    -> HealthHttpStatus.Ok
  }

  /**
   * The wire `state` computed from the lifecycle [state] and the [dependencies].
   */
  fun wireState(state: ServiceState, dependencies: List<DependencyState>): ServiceStateDto = when (state) {
    ServiceState.Starting,
    ServiceState.Verifying,
    -> ServiceStateDto.Starting

    ServiceState.Migrating -> ServiceStateDto.Migrating

    is ServiceState.Fatal -> when (state.reason) {
      FatalReason.SchemaIncompatible -> ServiceStateDto.SchemaIncompatible
      FatalReason.MigrationFailed -> ServiceStateDto.MigrationFailed
      FatalReason.Unspecified -> ServiceStateDto.Fatal
    }

    ServiceState.Ready,
    ServiceState.Degraded,
    -> when {
      dependencies.any { it.isRequired && it.isDown } -> ServiceStateDto.DependencyUnavailable
      dependencies.any { it.isDown || it.isDegraded } -> ServiceStateDto.Degraded
      else -> ServiceStateDto.Ready
    }
  }

  /**
   * Readiness: `Ok` only when the wire state is [ServiceStateDto.Ready] or [ServiceStateDto.Degraded].
   */
  fun readinessStatus(state: ServiceState, dependencies: List<DependencyState>): HealthHttpStatus =
    when (wireState(state, dependencies)) {
      ServiceStateDto.Ready,
      ServiceStateDto.Degraded,
      -> HealthHttpStatus.Ok

      ServiceStateDto.Starting,
      ServiceStateDto.Migrating,
      ServiceStateDto.DependencyUnavailable,
      ServiceStateDto.SchemaIncompatible,
      ServiceStateDto.MigrationFailed,
      ServiceStateDto.Fatal,
      -> HealthHttpStatus.ServiceUnavailable
    }

  /**
   * The body of `GET /health/ready`.
   */
  fun readinessResponse(state: ServiceState, dependencies: List<DependencyState>): ReadinessResponse =
    ReadinessResponse(
      state = wireState(state, dependencies),
      dependencies = dependencies.map { it.toDto() },
    )

  /**
   * The body of `GET /health` (always served with `200`).
   *
   * [uncaughtExceptions] and [backgroundJobs] are reported alongside the dependencies but do not enter
   * [wireState]: an application error does not change the service state, and a stale background job
   * must not take the process out of the load balancer — restarting it does not revive the job.
   *
   * @param now the [monotonicTimeSource] mark the background jobs are judged against. Passed in so the
   * projection stays a pure function.
   */
  fun healthResponse(
    state: ServiceState,
    dependencies: List<DependencyState>,
    uncaughtExceptions: UncaughtExceptions?,
    backgroundJobs: List<BackgroundJobState> = emptyList(),
    now: ComparableTimeMark = monotonicTimeSource.markNow(),
  ): HealthResponse {
    val mostRecent = dependencies.mostRecentOutage()?.outage
    return HealthResponse(
      state = wireState(state, dependencies),
      lastError = mostRecent?.let { HealthError(type = it.error.exceptionTypeName(), at = it.since) },
      errorCount = dependencies.count { it.isDown },
      uncaughtExceptions = uncaughtExceptions?.toDto(),
      backgroundJobs = backgroundJobs.map { it.toDto(now) },
      traceId = (state as? ServiceState.Fatal)?.traceId,
    )
  }

  private fun DependencyState.toDto(): DependencyStatusDto = when (val status = status) {
    is DependencyStatus.Up -> DependencyStatusDto(name, isRequired, DependencyReachability.Up, detail = status.detail)
    is DependencyStatus.Degraded -> DependencyStatusDto(name, isRequired, DependencyReachability.Degraded, detail = status.detail)
    is DependencyStatus.Down -> DependencyStatusDto(name, isRequired, DependencyReachability.Down, errorType = status.error.exceptionTypeName())
  }

  private fun UncaughtExceptions.toDto(): UncaughtExceptionsDto =
    UncaughtExceptionsDto(type = latest.exceptionTypeName(), since = since, count = count)

  private fun BackgroundJobState.toDto(now: ComparableTimeMark): BackgroundJobDto = BackgroundJobDto(
    name = name.value,
    lastCycleStartedAt = lastCycleStartedAt,
    lastCycleCompletedAt = lastCycleCompletedAt,
    stale = isStale(now),
    completedCycles = completedCycles,
    failedCycles = failedCycles,
  )
}

/**
 * One dependency that is down, together with the outage that took it down.
 *
 * Unlike [DependencyState.outage] the [outage] is non-null by construction: this is only ever built
 * for a dependency that has already been established to be down, so no caller has to null-check it
 * again.
 */
internal data class DependencyOutage(
  /**
   * Stable identifier of the dependency that is down.
   */
  val name: DependencyName,
  /**
   * The outage of that dependency.
   */
  val outage: DependencyStatus.Down,
)

/**
 * The dependency that went down most recently, or `null` if every dependency is up.
 */
internal fun List<DependencyState>.mostRecentOutage(): DependencyOutage? =
  mapNotNull { state -> state.outage?.let { DependencyOutage(state.name, it) } }
    .maxByOrNull { it.outage.since }

/**
 * One dependency that is degraded, together with the degradation.
 */
internal data class DependencyDegradation(
  /**
   * Stable identifier of the degraded dependency.
   */
  val name: DependencyName,
  /**
   * The degradation of that dependency.
   */
  val degradation: DependencyStatus.Degraded,
)

/**
 * The dependency that turned degraded most recently, or `null` if none is degraded.
 */
internal fun List<DependencyState>.mostRecentDegradation(): DependencyDegradation? =
  mapNotNull { state -> (state.status as? DependencyStatus.Degraded)?.let { DependencyDegradation(state.name, it) } }
    .maxByOrNull { it.degradation.since }
