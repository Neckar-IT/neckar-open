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
import it.neckar.open.test.utils.advance
import it.neckar.open.time.VirtualNowProvider
import it.neckar.open.time.monotonicTimeSource
import it.neckar.open.time.nowProvider
import it.neckar.open.time.resetMonotonicTimeSource
import it.neckar.open.time.resetNowProvider
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import kotlin.time.ComparableTimeMark
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

class BackgroundJobStateTest {
  private val registeredAt: Instant = Instant.fromEpochSeconds(1_000_000)

  private val timeSource = VirtualNowProvider(registeredAt.toEpochMilliseconds().toDouble())

  private val registeredMark: ComparableTimeMark = timeSource.markNow()

  private val state = BackgroundJobState(
    name = BackgroundJobName("target-polling"),
    stalenessThreshold = 5.minutes,
    registeredMark = registeredMark,
  )

  @AfterEach
  fun reset() {
    resetNowProvider()
    resetMonotonicTimeSource()
  }

  @Test
  fun `a job that never completed a cycle is measured from its registration`() {
    assertThat(state.sinceLastCompletedCycle(registeredMark + 30.seconds)).isEqualTo(30.seconds)
  }

  /**
   * The startup case that matters: a job whose very first cycle wedges never completes a cycle, so
   * without the registration fallback it would look eternally fresh.
   */
  @Test
  fun `a job that never completed a cycle turns stale after the threshold`() {
    assertThat(state.isStale(registeredMark + 5.minutes)).isFalse()
    assertThat(state.isStale(registeredMark + 5.minutes + 1.seconds)).isTrue()
  }

  @Test
  fun `a completed cycle resets the staleness clock`() {
    val completed = state.copy(
      lastCycleCompletedAt = registeredAt + 4.minutes,
      lastCycleCompletedMark = registeredMark + 4.minutes,
      completedCycles = 1,
    )

    assertThat(completed.sinceLastCompletedCycle(registeredMark + 6.minutes)).isEqualTo(2.minutes)
    assertThat(completed.isStale(registeredMark + 6.minutes)).isFalse()
    assertThat(completed.isStale(registeredMark + 10.minutes)).isTrue()
  }

  /**
   * A failing cycle is loud in the logs but does not count as liveness: only a completed cycle proves
   * the job is still doing its work.
   */
  @Test
  fun `a failed cycle does not reset the staleness clock`() {
    val failed = state.copy(
      lastCycleStartedAt = registeredAt + 6.minutes,
      lastFailure = BackgroundJobFailure(RuntimeException("boom"), registeredAt + 6.minutes),
      failedCycles = 1,
    )

    assertThat(failed.isStale(registeredMark + 6.minutes)).isTrue()
  }

  @Test
  fun `a wall clock jump leaves the staleness unchanged`() {
    val wallClock = VirtualNowProvider(registeredAt.toEpochMilliseconds().toDouble())
    nowProvider = wallClock
    monotonicTimeSource = timeSource
    timeSource.advance(10.seconds)

    wallClock.advance(1.days)

    assertThat(state.sinceLastCompletedCycle()).isEqualTo(10.seconds)
    assertThat(state.isStale()).isFalse()
  }

  @Test
  fun `advancing the virtual time past the threshold turns the job stale`() {
    nowProvider = timeSource
    monotonicTimeSource = timeSource

    timeSource.advance(6.minutes)

    assertThat(state.isStale()).isTrue()
  }
}
