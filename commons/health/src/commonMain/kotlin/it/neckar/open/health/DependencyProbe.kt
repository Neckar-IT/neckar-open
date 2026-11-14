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
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * A health check for one external dependency (e.g. MongoDB, S3).
 *
 * Implementations live in the dependency's own module (`mongodb`, `s3`) and perform a lightweight
 * connectivity test. The [DependencyMonitor] runs every probe periodically and on-demand under a
 * hard [timeout] so that a hung dependency never blocks the monitor.
 */
interface DependencyProbe {
  /**
   * Stable identifier of the dependency.
   */
  val name: DependencyName

  /**
   * Whether the dependency is hard ([DependencyClassification.Required]) or soft
   * ([DependencyClassification.Optional]).
   */
  val classification: DependencyClassification

  /**
   * Hard upper bound for one [check]. A check exceeding this is counted as
   * [DependencyStatus.Down]; the probe itself never blocks past this.
   */
  val timeout: Duration
    get() = DefaultTimeout

  /**
   * Performs a single connectivity check. Implementations must not apply their own timeout —
   * the [DependencyMonitor] enforces [timeout].
   */
  suspend fun check(): DependencyStatus

  companion object {
    val DefaultTimeout: Duration = 2.seconds
  }
}
