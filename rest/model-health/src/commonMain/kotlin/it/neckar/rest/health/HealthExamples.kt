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

import kotlin.time.Instant

/**
 * OpenAPI examples for the health-probe response bodies.
 *
 * Fixed instants: `now()` would put the generation time into the spec, so no two runs of one build
 * agree on it.
 */
object HealthExamples {
  val readyResponse: ReadinessResponse = ReadinessResponse(
    state = ServiceStateDto.Ready,
    dependencies = listOf(
      DependencyStatusDto(DependencyName.MongoDb, required = true, status = DependencyReachability.Up),
      DependencyStatusDto(DependencyName.S3, required = true, status = DependencyReachability.Up),
    ),
  )

  val dependencyUnavailableResponse: ReadinessResponse = ReadinessResponse(
    state = ServiceStateDto.DependencyUnavailable,
    dependencies = listOf(
      DependencyStatusDto(
        DependencyName.MongoDb,
        required = true,
        status = DependencyReachability.Down,
        errorType = "java.net.SocketTimeoutException",
      ),
    ),
  )

  val healthResponse: HealthResponse = HealthResponse(
    state = ServiceStateDto.Degraded,
    lastError = HealthError(type = "java.net.SocketTimeoutException", at = Instant.parse("2026-06-29T07:04:31Z")),
    errorCount = 1,
    backgroundJobs = listOf(
      BackgroundJobDto(
        name = "target-polling",
        lastCycleStartedAt = Instant.parse("2026-06-29T07:05:00Z"),
        lastCycleCompletedAt = Instant.parse("2026-06-29T07:05:02Z"),
        stale = false,
        completedCycles = 1440,
        failedCycles = 3,
      ),
    ),
  )
}
