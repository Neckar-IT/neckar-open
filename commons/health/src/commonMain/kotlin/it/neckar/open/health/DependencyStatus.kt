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

import kotlin.time.Instant

/**
 * Result of a single [DependencyProbe.check].
 */
sealed interface DependencyStatus {
  /**
   * The dependency is reachable.
   */
  data class Up(
    /**
     * What the probe observed, for an operator reading `/health/ready` — e.g. the origin of a held
     * key set. `null` when reachability is the whole story.
     */
    val detail: String? = null,
  ) : DependencyStatus

  /**
   * The dependency serves, but not from what it should serve from — a held key set that no fetch
   * ever confirmed, a replica running behind its primary. The service stays ready and reports
   * `degraded`, because taking it out of the load balancer would replace a partial answer with none.
   */
  data class Degraded(
    /**
     * Why the dependency is degraded, in the words an operator reads on `/health/ready`.
     */
    val detail: String,
    /**
     * When the dependency was first observed as degraded in the current degradation.
     */
    val since: Instant,
  ) : DependencyStatus

  /**
   * The dependency is unreachable. A probe timeout is reported as [Down] (the probe never blocks).
   */
  data class Down(
    /**
     * The error that made the dependency unreachable (or a timeout marker).
     */
    val error: Throwable,
    /**
     * When the dependency was first observed as down in the current outage.
     */
    val since: Instant,
  ) : DependencyStatus
}
