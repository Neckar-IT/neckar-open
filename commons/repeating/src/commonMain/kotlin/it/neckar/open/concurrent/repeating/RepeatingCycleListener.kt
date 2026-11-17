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

/**
 * Observes a [Repeating] job: its start, its end, and every cycle — exactly one of
 * [onCycleCompleted], [onCycleFailed] or [onCycleCancelled] follows every [onCycleStarted].
 *
 * Kept free of any health or metrics type so `concurrent` stays independent of them — the health
 * module supplies the implementation that feeds its `BackgroundJobRegistry`.
 *
 * Every callback runs inside the job loop and must not block. A callback that throws is logged and
 * swallowed by [Repeating]: an observer must never take down the job it observes. [onJobStarted] is
 * the exception, see there.
 */
public interface RepeatingCycleListener {
  /**
   * [Repeating.start] is about to launch the job described by [jobDescriptor].
   *
   * Runs on the caller of [Repeating.start], and an exception propagates to it: a listener that
   * cannot observe this configuration rejects it at startup instead of watching it wrongly.
   */
  public fun onJobStarted(jobDescriptor: Repeating.RepeatingJobDescriptor) {
  }

  /**
   * The supervisor of the job ended for [reason]; no worker is revived any more. Called exactly once
   * per [Repeating.start].
   */
  public fun onJobEnded(reason: RepeatingEndReason) {
  }

  /**
   * A cycle is about to run.
   */
  public fun onCycleStarted()

  /**
   * The cycle returned normally.
   */
  public fun onCycleCompleted()

  /**
   * The cycle threw [error], or exceeded [Repeating.RepeatingJobDescriptor.cycleTimeout] (then
   * [error] is the `TimeoutCancellationException`). The job itself continues with the next cycle.
   */
  public fun onCycleFailed(error: Throwable)

  /**
   * The running cycle was cancelled with its job ([Repeating.cancel] or its scope). No further cycle
   * of this start follows.
   */
  public fun onCycleCancelled() {
  }
}
