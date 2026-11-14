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
 * The `state` value carried in the body of the health probes (`/health`, `/health/ready`).
 *
 * This is the wire projection of the in-process service state. The frontend reads it from `GET /health`
 * (5s poll, `200` in every state), the uptime monitor from `/health` as well.
 *
 * The wire enum has more values than the internal service-state lifecycle: a down required
 * dependency projects to [DependencyUnavailable], a schema mismatch or a rejected migration dry run
 * to [SchemaIncompatible] and a failed migration to [MigrationFailed] — all derived, not lifecycle
 * states.
 */
@Serializable
enum class ServiceStateDto(
  /**
   * The value this state is serialized as. Repeats the `@SerialName`, which cannot be read back at
   * runtime, so a message text uses the same spelling as the response body.
   */
  val wireValue: String,
) {
  @SerialName("starting")
  Starting("starting"),

  @SerialName("migrating")
  Migrating("migrating"),

  @SerialName("ready")
  Ready("ready"),

  @SerialName("degraded")
  Degraded("degraded"),

  @SerialName("dependency-unavailable")
  DependencyUnavailable("dependency-unavailable"),

  @SerialName("schema-incompatible")
  SchemaIncompatible("schema-incompatible"),

  @SerialName("migration-failed")
  MigrationFailed("migration-failed"),

  @SerialName("fatal")
  Fatal("fatal"),
}
