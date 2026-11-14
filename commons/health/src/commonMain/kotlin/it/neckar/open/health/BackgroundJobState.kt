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
import it.neckar.open.kotlin.lang.requireNotNull
import it.neckar.open.time.monotonicTimeSource
import kotlin.jvm.JvmInline
import kotlin.time.ComparableTimeMark
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

/**
 * Identifies one essential background job of a service (`target-polling`, `auto-merge-cycle`).
 *
 * The name reaches Grafana as the `job.name` attribute of the liveness metric and appears in
 * `/health`, so it is chosen for an operator reading an alert, not for the class that implements it.
 */
@JvmInline
value class BackgroundJobName(val value: String) {
  init {
    require(value.matches(NamePattern)) {
      "background job name must be lower-case kebab-case matching ${NamePattern.pattern} but was '$value' — use BackgroundJobName.of(…) to derive one"
    }
  }

  override fun toString(): String = value

  companion object {
    /**
     * Lower-case kebab-case. Enforced because the name becomes the `job.name` label of a metric: a
     * space or a capital in one service's name and the same alert reads differently per service.
     */
    val NamePattern: Regex = Regex("[a-z0-9]+(-[a-z0-9]+)*")

    /**
     * Derives a name from [parts] — for a job named after something that is not kebab-case already:
     * a task class (`IssueSnapshotFullReconcile`) or a schedule label (`E1 Bug-Sweep`).
     *
     * ```
     * BackgroundJobName.of("gitlab-companion", "IssueSnapshotFullReconcile")  // gitlab-companion-issue-snapshot-full-reconcile
     * BackgroundJobName.of("gitlab-companion", "E1 Bug-Sweep")                // gitlab-companion-e1-bug-sweep
     * ```
     */
    fun of(vararg parts: String): BackgroundJobName = BackgroundJobName(parts.joinToString("-") { it.toKebabCase() })

    /**
     * `IssueSnapshotFullReconcile` → `issue-snapshot-full-reconcile`, `E1 Bug-Sweep` → `e1-bug-sweep`.
     */
    private fun String.toKebabCase(): String = this
      //Split camel humps before lower-casing, otherwise the word boundaries are gone
      .replace(Regex("(?<=[a-z0-9])(?=[A-Z])"), "-")
      .lowercase()
      .replace(Regex("[^a-z0-9]+"), "-")
      .trim('-')
  }
}

/**
 * The liveness record of one background job: when it last completed a cycle, how often it completed
 * and failed, and how long it may stay silent before it counts as stale.
 *
 * Exists because `restart: unless-stopped` and the HTTP liveness probe only observe the process, not
 * its purpose: a service whose polling job died or wedged keeps answering `200` forever.
 * The record makes "the job is still doing its work" an observable fact.
 *
 * A `Repeating` job counts its own cycles too (`Repeating.statistics`), so these numbers exist twice.
 * Deliberately: that package is a library that must not depend on this one, and this registry is the
 * process-wide place where a job of any kind reports in — including one that is not a `Repeating`.
 */
data class BackgroundJobState(
  /**
   * The job this record belongs to.
   */
  val name: BackgroundJobName,
  /**
   * How long the job may go without completing a cycle before [isStale] turns `true`. Chosen per job
   * from its interval and its worst realistic cycle duration; a start below the bound of
   * [BackgroundJobRegistry.recordJobStarted] throws.
   */
  val stalenessThreshold: Duration,
  /**
   * The [monotonicTimeSource] mark taken at registration. The staleness clock starts here until the
   * first cycle completes.
   */
  val registeredMark: ComparableTimeMark,
  /**
   * When the most recent cycle started, or `null` while no cycle started yet.
   */
  val lastCycleStartedAt: Instant? = null,
  /**
   * When the most recent cycle completed successfully, or `null` while no cycle completed yet.
   */
  val lastCycleCompletedAt: Instant? = null,
  /**
   * The [monotonicTimeSource] mark taken when the most recent cycle completed successfully, or `null`
   * while no cycle completed yet. The staleness clock restarts here.
   */
  val lastCycleCompletedMark: ComparableTimeMark? = null,
  /**
   * The most recent failed cycle, or `null` when no cycle failed since the process started.
   */
  val lastFailure: BackgroundJobFailure? = null,
  /**
   * How many cycles completed successfully since the process started.
   */
  val completedCycles: Int = 0,
  /**
   * How many cycles failed (threw or exceeded their timeout) since the process started.
   */
  val failedCycles: Int = 0,
  /**
   * The latest start of the job, or `null` while it never started. Records of an earlier run are dropped.
   */
  val run: BackgroundJobRun? = null,
  /**
   * The upper bound of one cycle as its latest start declared it, or `null` while the job never
   * started. Decides when a running cycle counts as hung.
   */
  val cycleTimeout: Duration? = null,
  /**
   * The [monotonicTimeSource] mark of the latest start of the job, or `null` while it never started.
   * A job registered but never started is judged by [isStale] alone.
   */
  val jobStartedMark: ComparableTimeMark? = null,
  /**
   * The [monotonicTimeSource] mark taken when the running cycle started, or `null` while no cycle runs.
   */
  val runningCycleStartedMark: ComparableTimeMark? = null,
  /**
   * The [monotonicTimeSource] mark taken when the most recent cycle ended — completed, failed or
   * cancelled — or `null` while no cycle ended yet.
   */
  val lastCycleEndedMark: ComparableTimeMark? = null,
  /**
   * Why the job's supervisor ended, or `null` while it supervises. Reset by the next start.
   */
  val endReason: RepeatingEndReason? = null,
) {
  /**
   * How long ago the job completed a cycle — measured from [registeredMark] while no cycle completed
   * yet, so a job whose very first cycle wedges does not look eternally fresh.
   *
   * Measured between marks of [monotonicTimeSource]: a wall clock adjustment leaves the result unchanged.
   */
  fun sinceLastCompletedCycle(now: ComparableTimeMark = monotonicTimeSource.markNow()): Duration = now - (lastCycleCompletedMark ?: registeredMark)

  /**
   * Whether the job has been silent longer than [stalenessThreshold] — the signal that the job died
   * or hangs, regardless of what the HTTP probes report.
   *
   * Only a *completed* cycle counts: a cycle that keeps failing proves the loop is alive but not that
   * the job does its work, and it is already loud in the logs and in [failedCycles].
   */
  fun isStale(now: ComparableTimeMark = monotonicTimeSource.markNow()): Boolean = sinceLastCompletedCycle(now) > stalenessThreshold

  /**
   * What stalls this job at [now], or `null` while it cycles — absence, not failure: a job whose cycles
   * keep failing still cycles, and a restart heals neither a dependency outage nor a revoked token.
   */
  fun stallAt(now: ComparableTimeMark = monotonicTimeSource.markNow()): BrokenBackgroundJob? {
    val startedMark = jobStartedMark ?: return null

    when (val reason = endReason) {
      RepeatingEndReason.ScopeCancelled,
      RepeatingEndReason.StoppedItself,
      is RepeatingEndReason.SupervisorFailed,
        -> return BrokenBackgroundJob.LoopEnded(name, reason)

      //The owner's decision - the job only counts as stalled if nothing started it again
      RepeatingEndReason.Cancelled,
      null,
        -> Unit
    }

    val cycleStartedMark = runningCycleStartedMark
    if (cycleStartedMark != null) {
      val timeout = cycleTimeout.requireNotNull { "job [$name] started without a cycle timeout" }
      val runningFor = now - cycleStartedMark
      return if (runningFor > hangThreshold(timeout)) BrokenBackgroundJob.CycleHung(name, runningFor = runningFor, cycleTimeout = timeout) else null
    }

    val idleSince = lastCycleEndedMark?.let { maxOf(it, startedMark) } ?: startedMark
    val idleFor = now - idleSince
    return if (idleFor > stalenessThreshold) BrokenBackgroundJob.LoopIdle(name, idleFor = idleFor, stalenessThreshold = stalenessThreshold) else null
  }

  /**
   * Moves every mark forward by [pause], at most to [now]: time the process spent frozen counts for no
   * job, and a mark a job took after the freeze moves no further than [now].
   */
  fun discountPause(pause: Duration, now: ComparableTimeMark): BackgroundJobState {
    fun ComparableTimeMark.discounted(): ComparableTimeMark = minOf(this + pause, now)
    return copy(
      registeredMark = registeredMark.discounted(),
      lastCycleCompletedMark = lastCycleCompletedMark?.discounted(),
      jobStartedMark = jobStartedMark?.discounted(),
      runningCycleStartedMark = runningCycleStartedMark?.discounted(),
      lastCycleEndedMark = lastCycleEndedMark?.discounted(),
    )
  }

  companion object {
    /**
     * The least a cycle may outlive its [cycleTimeout]. `withTimeout` ends a cycle only at a suspension
     * point, so a blocking call still in flight has to return first.
     */
    val MinimumHangGrace: Duration = 2.minutes

    /**
     * How long a cycle with [cycleTimeout] may run before it counts as hung: twice its timeout, at least
     * [MinimumHangGrace] beyond it. A body of many blocking calls without a suspension point outlives its
     * timeout by all of them, and a slow dependency must not read as a hang.
     */
    fun hangThreshold(cycleTimeout: Duration): Duration = cycleTimeout + maxOf(cycleTimeout, MinimumHangGrace)
  }
}

/**
 * A failed cycle of a background job, kept for `/health` and the logs.
 */
data class BackgroundJobFailure(
  /**
   * What the cycle threw — an exception of the job itself, or the `TimeoutCancellationException` of
   * its cycle timeout.
   */
  val error: Throwable,
  /**
   * When the failure was recorded.
   */
  val at: Instant,
)
