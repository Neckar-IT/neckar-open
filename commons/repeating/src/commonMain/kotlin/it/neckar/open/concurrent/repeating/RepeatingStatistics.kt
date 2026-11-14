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
package it.neckar.open.concurrent.repeating

import kotlin.time.Instant

/**
 * What a [Repeating] job has done so far — a snapshot, taken under the job's lock, so the numbers in
 * it belong to the same moment.
 *
 * Answers the question every long-running job eventually raises: *is it still doing anything, and did
 * the last attempt work?* Available without any observer: a caller that only wants to look at a job
 * reads [Repeating.statistics], while a caller that wants to be told about every cycle registers a
 * [RepeatingCycleListener].
 *
 * In the monorepo, `it.neckar.open.health.BackgroundJobRegistry` counts the same cycles a second time,
 * fed by such a listener. That duplication is the price of the two being independent, and it is worth
 * paying in both directions: this package must work without the health layer (it is a library), and
 * the registry must work without this package (it is the process-wide place where jobs of any kind
 * report in). Do not collapse one into the other.
 */
public data class RepeatingStatistics(
  /**
   * How many cycles were started since the job was first started. Counts the cycle that is running
   * right now.
   */
  val startedCycles: Int = 0,
  /**
   * How many cycles returned normally.
   */
  val completedCycles: Int = 0,
  /**
   * How many cycles threw or exceeded their timeout. A failed cycle does not end the job.
   */
  val failedCycles: Int = 0,
  /**
   * How many cycles failed in a row, reset by the next completed one. What the error backoff scales
   * with (see [Repeating.RepeatingJobDescriptor.errorBackoff]).
   */
  val consecutiveFailures: Int = 0,
  /**
   * When the most recent cycle completed, or `null` while none has.
   */
  val lastCycleCompletedAt: Instant? = null,
  /**
   * What the most recent failed cycle threw, or `null` when no cycle has failed yet. Kept as the
   * latest one only: a job failing every second would otherwise retain every throwable it ever threw,
   * with their stack traces and everything those reference.
   */
  val lastError: Throwable? = null,
) {
  /**
   * `true` when no cycle has finished yet — neither completed nor failed. A job that was just started,
   * or one whose first cycle is still running.
   */
  val hasNeverFinishedACycle: Boolean
    get() = completedCycles == 0 && failedCycles == 0

  override fun toString(): String =
    "RepeatingStatistics(started=$startedCycles, completed=$completedCycles, failed=$failedCycles, consecutiveFailures=$consecutiveFailures)"
}

/**
 * Where a [Repeating] job stands in its lifecycle.
 */
public enum class RepeatingState {
  /**
   * Created but never started, or cancelled since.
   */
  Idle,

  /**
   * Started: the supervisor watches, and the worker runs its cycles.
   */
  Running,

  /**
   * The block ended the job itself through [stopRepeating]. Terminal until [Repeating.start] is called
   * again — the supervisor deliberately does not revive it.
   */
  StoppedItself,
}
