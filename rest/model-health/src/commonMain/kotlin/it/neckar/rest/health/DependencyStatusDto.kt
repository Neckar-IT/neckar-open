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

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Per-dependency status in a readiness-probe body (`dependencies[]`).
 *
 * Carries no exception message and no stack trace: the health endpoints are unauthenticated and
 * exist for orchestration. Message and stack trace are reported to Grafana as an OpenTelemetry
 * error span.
 */
@Serializable
data class DependencyStatusDto(
  /**
   * The dependency this entry describes.
   */
  val name: DependencyName,
  /**
   * Whether the dependency is required (a `down` required dependency blocks readiness)
   * or optional (a `down` optional dependency only degrades).
   */
  val required: Boolean,
  /**
   * Reachability of the dependency.
   */
  val status: DependencyReachability,
  /**
   * The fully qualified type of the error when [status] is [DependencyReachability.Down] (e.g.
   * `java.net.SocketTimeoutException`); `null` otherwise.
   */
  val errorType: String? = null,
  /**
   * What the probe observed beyond reachability — the origin of a held JWKS key set, why a
   * dependency is [DependencyReachability.Degraded]. Carries no exception message, for the same
   * reason [errorType] carries only the type.
   */
  val detail: String? = null,
)

/**
 * Reachability of a single dependency as projected onto the wire.
 */
@Serializable
enum class DependencyReachability {
  @SerialName("up")
  Up,

  /**
   * The dependency serves, but not from what it should serve from. Keeps the service ready.
   */
  @SerialName("degraded")
  Degraded,

  @SerialName("down")
  Down,
}
