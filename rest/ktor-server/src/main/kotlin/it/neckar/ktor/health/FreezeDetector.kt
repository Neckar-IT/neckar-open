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
package it.neckar.ktor.health

import kotlin.time.ComparableTimeMark
import kotlin.time.Duration

/**
 * Tells a watchdog that its own ticks came late — the process was frozen (a VM pause, a long
 * stop-the-world pause, `SIGSTOP`), and every stall measured across that time is the freeze, not the
 * service.
 */
internal class FreezeDetector(
  private val tickInterval: Duration,
) {
  private var previousTick: ComparableTimeMark? = null

  private var previousTickLate: Boolean = false

  /**
   * The time the process was frozen before the tick at [now], or `null` for a tick that came on time.
   *
   * Only a late tick after one on time is a freeze: a process whose ticks come late one after another
   * (a JVM thrashing in garbage collection) is stalled, and its watchdogs judge it as such.
   */
  fun frozenBefore(now: ComparableTimeMark): Duration? {
    val previous = previousTick
    previousTick = now
    val sincePrevious = previous?.let { now - it } ?: return null
    val late = sincePrevious > tickInterval * LateFactor
    val isolatedFreeze = late && previousTickLate.not()
    previousTickLate = late
    return if (isolatedFreeze) sincePrevious - tickInterval else null
  }

  private companion object {
    /**
     * A scheduler delays a tick by milliseconds; only a frozen process ticks two intervals late. A freeze
     * this misses is shorter than one interval and so within `BackgroundJobRegistry.LoopIdleMargin`.
     */
    const val LateFactor: Int = 2
  }
}
