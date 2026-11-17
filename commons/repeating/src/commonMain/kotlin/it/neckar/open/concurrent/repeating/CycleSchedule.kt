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

import it.neckar.open.time.now
import kotlinx.coroutines.delay
import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Instant

/**
 * Decides *when* the next cycle of a [Repeating] job runs — the one part that differs between a job
 * running every few seconds and one running at 03:00 every night.
 *
 * Everything else a background job needs (the per-cycle timeout, the error handling that keeps the
 * loop alive, the cycle reports that make its liveness observable, the supervisor) lives in
 * [Repeating] and is shared by both.
 */
public interface CycleSchedule {
  /**
   * How this schedule reads in a log line (`every 1m`, `daily at 03:00 Europe/Berlin`).
   */
  public val description: String

  /**
   * The longest this schedule lets pass between the end of one cycle and the start of the next, and
   * between the job's start and its first cycle. A job silent for longer has stopped cycling.
   */
  public val maxGap: Duration

  /**
   * Suspends until the next cycle starts and returns the moment it stands for: the later of slot and
   * current time for a schedule with slots, the current time for every other schedule.
   */
  public suspend fun awaitCycle(
    /**
     * The moment the next cycle follows: the moment the previous cycle stood for, or the end of an error
     * backoff. `null` before the first cycle of a worker, which may run at once.
     */
    after: Instant?,
  ): Instant
}

/**
 * Runs the first cycle immediately and every following one [interval] after the previous cycle
 * *finished*.
 *
 * Deliberately not a fixed rate: a cycle that takes longer than the interval must not queue up
 * follow-up cycles behind it. A job with a 1-minute interval whose cycle takes 20 seconds runs every
 * 80 seconds, and that is the intended reading of "every minute" for polling work.
 */
public class IntervalSchedule(
  /**
   * The pause between the end of one cycle and the start of the next.
   */
  public val interval: Duration,
) : CycleSchedule {
  init {
    require(interval.isPositive()) { "interval must be positive but was $interval" }
  }

  override val description: String
    get() = "every $interval"

  override val maxGap: Duration
    get() = interval

  override suspend fun awaitCycle(after: Instant?): Instant {
    //The first cycle runs right away: a service that starts must not wait one interval before doing
    //its work for the first time.
    if (after != null) {
      delay(interval)
    }
    return now()
  }

  override fun toString(): String = description
}

/**
 * Never waits: the next cycle starts the moment the previous one returned.
 *
 * For work whose own blocking *is* the rhythm — reading the next line from a process's output,
 * taking the next item off a queue, waiting for a child process to exit before restarting it. Such a
 * loop is a repetition like any other and belongs in [Repeating] rather than in a hand-rolled
 * `while (true)`, for the supervisor and the error handling.
 *
 * A cycle that returns immediately would spin the CPU, so a job on this schedule must block, suspend
 * or end itself with [stopRepeating].
 */
public object ImmediateSchedule : CycleSchedule {
  override val description: String
    get() = "continuously"

  override val maxGap: Duration
    get() = Duration.ZERO

  override suspend fun awaitCycle(after: Instant?): Instant {
    //Nothing to wait for - the block itself paces the loop.
    return now()
  }

  override fun toString(): String = description
}

/**
 * Waits a random duration between [minInterval] and [maxInterval] before every cycle, including the
 * first one.
 *
 * For jobs that must not run in lockstep: simulated user activity, or a fleet of pollers that would
 * otherwise hit the same backend at the same instant.
 */
public class RandomIntervalSchedule(
  /**
   * The shortest possible pause before a cycle.
   */
  public val minInterval: Duration,
  /**
   * The longest possible pause before a cycle.
   */
  public val maxInterval: Duration,
  /**
   * Draws the pause. Overridable so a test can make the schedule deterministic.
   */
  private val random: Random = Random.Default,
) : CycleSchedule {
  init {
    require(minInterval.isPositive()) { "minInterval must be positive but was $minInterval" }
    require(maxInterval >= minInterval) { "maxInterval $maxInterval must not be shorter than minInterval $minInterval" }
  }

  override val description: String
    get() = "every $minInterval to $maxInterval"

  override val maxGap: Duration
    get() = maxInterval

  override suspend fun awaitCycle(after: Instant?): Instant {
    //Also before the first cycle: a fleet starting at once must not fire its first cycle in lockstep
    //either, which is the very thing this schedule exists to avoid.
    delay(random.nextLong(minInterval.inWholeMilliseconds, maxInterval.inWholeMilliseconds + 1).milliseconds)
    return now()
  }

  override fun toString(): String = description
}
