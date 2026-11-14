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
package it.neckar.rest.health

import it.neckar.open.kotlin.serializers.NativeInstant
import it.neckar.open.tracing.TraceId
import kotlinx.serialization.Serializable

/**
 * Body of `GET /health`.
 *
 * Always returned with `200`: the endpoint is the alerting source for the uptime monitor. [state]
 * and [lastError] describe the infrastructure, [uncaughtExceptions] describes the application. An
 * uncaught exception does not change [state].
 */
@Serializable
data class HealthResponse(
  /**
   * The projected service state, derived from the lifecycle and the dependencies.
   */
  val state: ServiceStateDto,
  /**
   * The most recent dependency error, or `null` when no dependency is down.
   */
  val lastError: HealthError? = null,
  /**
   * Number of dependencies currently down.
   */
  val errorCount: Int,
  /**
   * The uncaught exceptions recorded in this process, or `null` when none were recorded.
   */
  val uncaughtExceptions: UncaughtExceptionsDto? = null,
  /**
   * The essential background jobs of this service and their liveness. Empty when the service has no
   * registered background job.
   */
  val backgroundJobs: List<BackgroundJobDto> = emptyList(),
  /**
   * The trace of the error span recorded when the service turned fatal — where the message and the
   * stack trace this body omits can be read, in Grafana/Tempo.
   *
   * `null` unless [state] is one of the fatal states, and `null` without a configured exporter.
   */
  val traceId: TraceId? = null,
)

/**
 * The liveness of one essential background job, surfaced in [HealthResponse].
 *
 * Reported next to [HealthResponse.state] without entering it: a stale job is an application problem
 * that must be visible and alertable, but it must not make the service not ready — taking the process
 * out of the load balancer does not revive its polling job.
 */
@Serializable
data class BackgroundJobDto(
  /**
   * The job name, as registered by the service (e.g. `target-polling`).
   */
  val name: String,
  /**
   * When the job last started a cycle, or `null` while no cycle started since the process started.
   *
   * Read together with [lastCycleCompletedAt] it tells a hang apart from a death: a start far in the
   * past with no completion after it is a cycle that never returned, while no start at all is a loop
   * that stopped running.
   */
  val lastCycleStartedAt: NativeInstant? = null,
  /**
   * When the job last completed a cycle, or `null` while no cycle completed since the process started.
   */
  val lastCycleCompletedAt: NativeInstant? = null,
  /**
   * Whether the job has been silent longer than its staleness threshold — the job died or hangs.
   */
  val stale: Boolean,
  /**
   * How many cycles completed successfully since the process started.
   */
  val completedCycles: Int,
  /**
   * How many cycles failed (threw or exceeded their timeout) since the process started.
   */
  val failedCycles: Int,
)

/**
 * The most recent dependency error, surfaced in [HealthResponse].
 */
@Serializable
data class HealthError(
  /**
   * The fully qualified exception type (e.g. `java.net.SocketTimeoutException`).
   */
  val type: String,
  /**
   * When the error was recorded.
   */
  val at: NativeInstant,
)

/**
 * The uncaught thread/coroutine exceptions recorded in the process, surfaced in [HealthResponse].
 *
 * An application error: it never makes the service not ready. Carries neither the exception message
 * nor the stack trace — both are reported to Grafana as an OpenTelemetry error span.
 */
@Serializable
data class UncaughtExceptionsDto(
  /**
   * The fully qualified type of the most recent exception (e.g. `java.lang.NullPointerException`).
   */
  val type: String,
  /**
   * When the first exception was recorded.
   */
  val since: NativeInstant,
  /**
   * How many exceptions were recorded since [since].
   */
  val count: Int,
)
