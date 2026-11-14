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

import it.neckar.open.concurrent.repeating.RepeatingEndReason
import kotlin.time.Duration

/**
 * An essential background job that stopped cycling — the states a restart of the process heals.
 *
 * A job whose cycles keep failing is none of these: its loop is alive, and what fails it (a dependency
 * down, a revoked token) survives a restart.
 */
sealed interface BrokenBackgroundJob {
  val name: BackgroundJobName

  /**
   * One line for the log that names the job and why it counts as stopped.
   */
  val description: String

  /**
   * The supervisor ended although nobody cancelled the job, so no worker is revived any more.
   */
  data class LoopEnded(
    override val name: BackgroundJobName,
    /**
     * Why the supervisor ended; never [RepeatingEndReason.Cancelled].
     */
    val reason: RepeatingEndReason,
  ) : BrokenBackgroundJob {
    override val description: String
      get() = "[$name] ended: $reason"
  }

  /**
   * A cycle runs longer than [BackgroundJobState.hangThreshold] — blocked in a call the timeout cannot interrupt.
   */
  data class CycleHung(
    override val name: BackgroundJobName,
    val runningFor: Duration,
    val cycleTimeout: Duration,
  ) : BrokenBackgroundJob {
    override val description: String
      get() = "[$name] has a cycle running for $runningFor (cycleTimeout $cycleTimeout)"
  }

  /**
   * No cycle runs, and none started for longer than the staleness threshold — the loop is gone
   * although its supervisor did not report an end.
   */
  data class LoopIdle(
    override val name: BackgroundJobName,
    /**
     * How long since the last cycle ended, or since the job started.
     */
    val idleFor: Duration,
    val stalenessThreshold: Duration,
  ) : BrokenBackgroundJob {
    override val description: String
      get() = "[$name] started no cycle for $idleFor (stalenessThreshold $stalenessThreshold)"
  }
}
