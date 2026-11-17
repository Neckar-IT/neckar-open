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

import it.neckar.rest.health.DependencyName

/**
 * The last observed state of one dependency, held by the [ServiceStateHolder].
 */
data class DependencyState(
  /**
   * Stable identifier of the dependency.
   */
  val name: DependencyName,
  /**
   * Whether the dependency is hard or soft.
   */
  val classification: DependencyClassification,
  /**
   * The last observed reachability.
   */
  val status: DependencyStatus,
) {
  val isDown: Boolean
    get() = status is DependencyStatus.Down

  /**
   * The dependency serves, but not from what it should serve from. Degradation is not an outage: it
   * keeps the service ready and only shows up as `degraded`.
   */
  val isDegraded: Boolean
    get() = status is DependencyStatus.Degraded

  val isRequired: Boolean
    get() = classification == DependencyClassification.Required

  /**
   * The outage of this dependency, or `null` while it is up.
   */
  val outage: DependencyStatus.Down?
    get() = status as? DependencyStatus.Down
}
