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

import it.neckar.open.concurrent.repeating.Repeating
import it.neckar.open.concurrent.repeating.RepeatingCycleListener
import it.neckar.open.concurrent.repeating.RepeatingEndReason
import kotlin.concurrent.Volatile
import kotlin.time.Duration

/**
 * Feeds the cycles of one [Repeating] job into the [BackgroundJobRegistry] under [name].
 *
 * The bridge between `concurrent` (which knows how to repeat) and `health` (which knows what liveness
 * means): `Repeating` restarts a dead job by itself, but nothing outside the process could tell
 * whether it still completes its work.
 */
class BackgroundJobCycleListener(
  /**
   * The job whose state is updated.
   */
  val name: BackgroundJobName,
) : RepeatingCycleListener {
  /**
   * The latest start this listener reported; the registry drops what a cancelled start reports late.
   */
  @Volatile
  private var run: BackgroundJobRun? = null

  override fun onCycleStarted() {
    BackgroundJobRegistry.recordCycleStarted(name, run)
  }

  override fun onCycleCompleted() {
    BackgroundJobRegistry.recordCycleCompleted(name, run)
  }

  override fun onCycleFailed(error: Throwable) {
    BackgroundJobRegistry.recordCycleFailed(name, error, run)
  }

  override fun onCycleCancelled() {
    BackgroundJobRegistry.recordCycleCancelled(name, run)
  }

  override fun onJobStarted(jobDescriptor: Repeating.RepeatingJobDescriptor) {
    run = BackgroundJobRegistry.recordJobStarted(name, cycleTimeout = jobDescriptor.cycleTimeout, maxGap = jobDescriptor.maxGap)
  }

  override fun onJobEnded(reason: RepeatingEndReason) {
    BackgroundJobRegistry.recordJobEnded(name, reason, run)
  }
}

/**
 * Registers this job as the essential background job [name] and reports every cycle to the
 * [BackgroundJobRegistry] — one call turns a repeating job into an observable one:
 *
 * ```
 * Repeating.every(60.seconds) { pollAllTargets() }
 *   .cycleTimeout(2.minutes)
 *   .trackedAs(BackgroundJobName("target-polling"), stalenessThreshold = 5.minutes)
 *   .start(coroutineScope = scope)
 * ```
 *
 * Registration happens here, not on the first cycle, so a job whose very first cycle wedges turns
 * stale instead of staying unknown.
 *
 * An essential job never ends by itself: [it.neckar.open.concurrent.repeating.stopRepeating] in its
 * block ends an armed process like any other end nobody cancelled.
 *
 * Combine it with [Repeating.RepeatingJobDescriptor.Builder.cycleTimeout]: the timeout ends a wedged
 * cycle so the next one can succeed, the tracking makes the wedge visible while it lasts.
 *
 * @param stalenessThreshold how long the job may go without a completed cycle before it counts as
 * stale. See [BackgroundJobState.stalenessThreshold].
 */
fun Repeating.RepeatingJobDescriptor.Builder.trackedAs(
  name: BackgroundJobName,
  stalenessThreshold: Duration,
): Repeating.RepeatingJobDescriptor.Builder {
  BackgroundJobRegistry.register(name, stalenessThreshold)
  return cycleListener(BackgroundJobCycleListener(name))
}

/**
 * The staleness threshold of a job without error backoff repeating every [interval]: three intervals,
 * and at least what [BackgroundJobRegistry.recordJobStarted] demands for a short one.
 */
fun stalenessThresholdFor(interval: Duration): Duration = maxOf(interval * 3, interval + BackgroundJobRegistry.LoopIdleMargin * 2)
