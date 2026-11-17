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

import assertk.*
import assertk.assertions.*
import it.neckar.open.concurrent.repeating.RepeatingEndReason
import it.neckar.open.time.VirtualNowProvider
import it.neckar.open.time.monotonicTimeSource
import it.neckar.open.time.nowProvider
import it.neckar.open.time.resetMonotonicTimeSource
import it.neckar.open.time.resetNowProvider
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * Which jobs [BackgroundJobRegistry.brokenJobs] reports — the states a restart heals, and none of those
 * it does not.
 */
class BackgroundJobStallTest {
  private val name: BackgroundJobName = BackgroundJobName("auto-merge-cycle")

  private val clock = VirtualNowProvider(1_000_000_000.0)

  @BeforeEach
  fun setUp() {
    nowProvider = clock
    monotonicTimeSource = clock
    BackgroundJobRegistry.clear()
    BackgroundJobRegistry.register(name, stalenessThreshold = 5.minutes)
  }

  @AfterEach
  fun tearDown() {
    BackgroundJobRegistry.clear()
    resetNowProvider()
    resetMonotonicTimeSource()
  }

  private fun start() {
    BackgroundJobRegistry.recordJobStarted(name, cycleTimeout = 5.minutes, maxGap = 15.seconds)
  }

  private fun advance(duration: kotlin.time.Duration) {
    clock.advanceBy(duration.inWholeMilliseconds.toDouble())
  }

  @Test
  fun `a job registered but never started is not judged`() {
    advance(1.days)

    assertThat(BackgroundJobRegistry.brokenJobs()).isEmpty()
  }

  @Test
  fun `a cycle running past twice its timeout is hung`() {
    start()
    BackgroundJobRegistry.recordCycleStarted(name)

    advance(10.minutes)
    assertThat(BackgroundJobRegistry.brokenJobs()).isEmpty()

    advance(1.seconds)
    assertThat(BackgroundJobRegistry.brokenJobs().single()).isInstanceOf(BrokenBackgroundJob.CycleHung::class)
      .transform { it.runningFor }.isEqualTo(10.minutes + 1.seconds)
  }

  @Test
  fun `a short cycle timeout gets at least the minimum grace`() {
    assertThat(BackgroundJobState.hangThreshold(30.seconds)).isEqualTo(30.seconds + 2.minutes)
    assertThat(BackgroundJobState.hangThreshold(12.hours)).isEqualTo(24.hours)
  }

  @Test
  fun `cycles that keep failing are not a stall`() {
    start()
    repeat(100) {
      BackgroundJobRegistry.recordCycleStarted(name)
      advance(10.seconds)
      BackgroundJobRegistry.recordCycleFailed(name, IllegalStateException("401 Unauthorized"))
      advance(15.seconds)
    }

    assertThat(BackgroundJobRegistry.stateOf(name)?.isStale()).isEqualTo(true)
    assertThat(BackgroundJobRegistry.brokenJobs()).isEmpty()
  }

  @Test
  fun `a loop that starts no cycle for longer than the threshold is idle`() {
    start()
    BackgroundJobRegistry.recordCycleStarted(name)
    BackgroundJobRegistry.recordCycleCompleted(name)

    advance(5.minutes + 1.seconds)

    assertThat(BackgroundJobRegistry.brokenJobs().single()).isInstanceOf(BrokenBackgroundJob.LoopIdle::class)
  }

  @Test
  fun `an end nobody asked for is a stall at once`() {
    listOf(RepeatingEndReason.ScopeCancelled, RepeatingEndReason.StoppedItself, RepeatingEndReason.SupervisorFailed(IllegalStateException("boom"))).forEach { reason ->
      start()
      BackgroundJobRegistry.recordJobEnded(name, reason)

      assertThat(BackgroundJobRegistry.brokenJobs()).containsExactly(BrokenBackgroundJob.LoopEnded(name, reason))
    }
  }

  @Test
  fun `a cancel followed by a start is no stall`() {
    start()
    BackgroundJobRegistry.recordCycleStarted(name)
    BackgroundJobRegistry.recordCycleCancelled(name)
    BackgroundJobRegistry.recordJobEnded(name, RepeatingEndReason.Cancelled)
    start()
    BackgroundJobRegistry.recordCycleStarted(name)
    BackgroundJobRegistry.recordCycleCompleted(name)

    advance(1.minutes)

    assertThat(BackgroundJobRegistry.brokenJobs()).isEmpty()
  }

  /**
   * `JwksKeyManager.start` cancels its job and starts a new one at once; the old run reports its end late.
   */
  @Test
  fun `records of a replaced run are dropped`() {
    val replaced = BackgroundJobRegistry.recordJobStarted(name, cycleTimeout = 5.minutes, maxGap = 15.seconds)
    BackgroundJobRegistry.recordCycleStarted(name, replaced)
    val current = BackgroundJobRegistry.recordJobStarted(name, cycleTimeout = 5.minutes, maxGap = 15.seconds)
    BackgroundJobRegistry.recordCycleStarted(name, current)

    BackgroundJobRegistry.recordCycleCancelled(name, replaced)
    BackgroundJobRegistry.recordJobEnded(name, RepeatingEndReason.ScopeCancelled, replaced)
    advance(10.minutes + 1.seconds)

    assertThat(BackgroundJobRegistry.stateOf(name)?.endReason).isNull()
    assertThat(BackgroundJobRegistry.brokenJobs().single()).isInstanceOf(BrokenBackgroundJob.CycleHung::class)
  }

  @Test
  fun `a cancel without a start turns idle after the threshold`() {
    start()
    BackgroundJobRegistry.recordCycleStarted(name)
    BackgroundJobRegistry.recordCycleCancelled(name)
    BackgroundJobRegistry.recordJobEnded(name, RepeatingEndReason.Cancelled)

    advance(5.minutes)
    assertThat(BackgroundJobRegistry.brokenJobs()).isEmpty()

    advance(1.seconds)
    assertThat(BackgroundJobRegistry.brokenJobs().single()).isInstanceOf(BrokenBackgroundJob.LoopIdle::class)
  }

  @Test
  fun `a stall reported by the end reaches the observers at once`() {
    val observed = mutableListOf<BrokenBackgroundJob>()
    val registration = BackgroundJobRegistry.onJobStalled { observed.add(it) }
    start()

    BackgroundJobRegistry.recordJobEnded(name, RepeatingEndReason.Cancelled)
    BackgroundJobRegistry.recordJobEnded(name, RepeatingEndReason.StoppedItself)
    registration.dispose()

    assertThat(observed).containsExactly(BrokenBackgroundJob.LoopEnded(name, RepeatingEndReason.StoppedItself))
  }

  @Test
  fun `a wall clock jump leaves every stall unchanged`() {
    start()
    BackgroundJobRegistry.recordCycleStarted(name)

    nowProvider = VirtualNowProvider(clock.nowMillis() + 1.days.inWholeMilliseconds)

    assertThat(BackgroundJobRegistry.brokenJobs()).isEmpty()
  }

  @Test
  fun `a frozen process is discounted`() {
    start()
    BackgroundJobRegistry.recordCycleStarted(name)
    advance(8.hours)

    BackgroundJobRegistry.discountPause(8.hours)

    assertThat(BackgroundJobRegistry.brokenJobs()).isEmpty()
  }

  @Test
  fun `a cycle recorded after the freeze keeps its mark`() {
    start()
    advance(8.hours)
    BackgroundJobRegistry.recordCycleStarted(name)
    BackgroundJobRegistry.recordCycleCompleted(name)

    BackgroundJobRegistry.discountPause(8.hours)

    assertThat(BackgroundJobRegistry.stateOf(name)?.sinceLastCompletedCycle()).isEqualTo(kotlin.time.Duration.ZERO)
  }

  @Test
  fun `a staleness threshold needs the idle margin beyond the longest pause`() {
    assertFailure { BackgroundJobRegistry.recordJobStarted(name, cycleTimeout = 1.minutes, maxGap = 4.minutes + 30.seconds) }
      .isInstanceOf(IllegalArgumentException::class)
    BackgroundJobRegistry.recordJobStarted(name, cycleTimeout = 1.minutes, maxGap = 3.minutes + 59.seconds)
  }

  @Test
  fun `a derived threshold passes the start check`() {
    listOf(10.seconds, 1.minutes, 15.minutes).forEach { interval ->
      BackgroundJobRegistry.register(name, stalenessThreshold = stalenessThresholdFor(interval))
      BackgroundJobRegistry.recordJobStarted(name, cycleTimeout = interval, maxGap = interval)
    }
  }

  @Test
  fun `a start without a cycle timeout is rejected`() {
    assertFailure { BackgroundJobRegistry.recordJobStarted(name, cycleTimeout = null, maxGap = 15.seconds) }
      .isInstanceOf(IllegalArgumentException::class).messageContains("needs a cycleTimeout")
  }

  @Test
  fun `a staleness threshold not above the longest pause is rejected`() {
    assertFailure { BackgroundJobRegistry.recordJobStarted(name, cycleTimeout = 1.minutes, maxGap = 5.minutes) }
      .isInstanceOf(IllegalArgumentException::class).messageContains("must exceed the longest pause")
  }
}
