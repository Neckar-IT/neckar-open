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
import it.neckar.open.time.VirtualNowProvider
import it.neckar.open.test.utils.WithVirtualTime
import it.neckar.open.test.utils.advance
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@WithVirtualTime
class BackgroundJobRegistryTest {
  private val jobName = BackgroundJobName("target-polling")

  @BeforeEach
  @AfterEach
  fun clearRegistry() {
    BackgroundJobRegistry.clear()
  }

  @Test
  fun `an unregistered job has no state`() {
    assertThat(BackgroundJobRegistry.stateOf(jobName)).isNull()
    assertThat(BackgroundJobRegistry.states).isEmpty()
  }

  @Test
  fun `register creates the state with the registration mark`(nowProvider: VirtualNowProvider) {
    BackgroundJobRegistry.register(jobName, stalenessThreshold = 5.minutes)

    val state = BackgroundJobRegistry.stateOf(jobName)
    assertThat(state?.name).isEqualTo(jobName)
    assertThat(state?.stalenessThreshold).isEqualTo(5.minutes)
    assertThat(state?.registeredMark).isEqualTo(nowProvider.markNow())
    assertThat(state?.completedCycles).isEqualTo(0)
  }

  @Test
  fun `a completed cycle is recorded with its timestamp and counted`(nowProvider: VirtualNowProvider) {
    BackgroundJobRegistry.register(jobName, stalenessThreshold = 5.minutes)

    nowProvider.advance(30.seconds)
    BackgroundJobRegistry.recordCycleStarted(jobName)
    BackgroundJobRegistry.recordCycleCompleted(jobName)

    val state = BackgroundJobRegistry.stateOf(jobName)
    assertThat(state?.completedCycles).isEqualTo(1)
    assertThat(state?.lastCycleCompletedAt?.toEpochMilliseconds()).isEqualTo(nowProvider.nowMillis().toLong())
    assertThat(state?.lastCycleCompletedMark).isEqualTo(nowProvider.markNow())
    assertThat(state?.lastFailure).isNull()
  }

  @Test
  fun `a failed cycle is counted and keeps the last completed cycle`(nowProvider: VirtualNowProvider) {
    BackgroundJobRegistry.register(jobName, stalenessThreshold = 5.minutes)
    BackgroundJobRegistry.recordCycleCompleted(jobName)
    val completedAt = BackgroundJobRegistry.stateOf(jobName)?.lastCycleCompletedAt
    val completedMark = BackgroundJobRegistry.stateOf(jobName)?.lastCycleCompletedMark

    nowProvider.advance(1.minutes)
    BackgroundJobRegistry.recordCycleFailed(jobName, RuntimeException("boom"))

    val state = BackgroundJobRegistry.stateOf(jobName)
    assertThat(state?.failedCycles).isEqualTo(1)
    assertThat(state?.completedCycles).isEqualTo(1)
    assertThat(state?.lastCycleCompletedAt).isEqualTo(completedAt)
    assertThat(state?.lastCycleCompletedMark).isEqualTo(completedMark)
    assertThat(state?.lastFailure?.error?.message).isEqualTo("boom")
  }

  /**
   * The registry is process-wide, so a service that registers the same job twice (a restarted
   * [it.neckar.open.concurrent.Repeating], a second `start()`) must not lose its cycle history —
   * otherwise every restart would reset the staleness clock and mask a job that never recovers.
   */
  @Test
  fun `registering the same job twice keeps the recorded cycles`() {
    BackgroundJobRegistry.register(jobName, stalenessThreshold = 5.minutes)
    BackgroundJobRegistry.recordCycleCompleted(jobName)

    BackgroundJobRegistry.register(jobName, stalenessThreshold = 10.minutes)

    val state = BackgroundJobRegistry.stateOf(jobName)
    assertThat(state?.completedCycles).isEqualTo(1)
    assertThat(state?.stalenessThreshold).isEqualTo(10.minutes)
  }

  /**
   * Recording against a name nobody registered has no staleness threshold to judge it by. Dropping it
   * is deliberate: inventing a threshold would produce an alert nobody can reason about.
   */
  @Test
  fun `recording a cycle for an unregistered job is ignored`() {
    BackgroundJobRegistry.recordCycleCompleted(jobName)

    assertThat(BackgroundJobRegistry.stateOf(jobName)).isNull()
  }

  @Test
  fun `staleJobs lists exactly the jobs past their threshold`(nowProvider: VirtualNowProvider) {
    val fast = BackgroundJobName("fast")
    val slow = BackgroundJobName("slow")
    BackgroundJobRegistry.register(fast, stalenessThreshold = 1.minutes)
    BackgroundJobRegistry.register(slow, stalenessThreshold = 10.minutes)

    nowProvider.advance(2.minutes)

    assertThat(BackgroundJobRegistry.staleJobs().map { it.name }).isEqualTo(listOf(fast))
  }

  @Test
  fun `states are listed in registration order`() {
    val first = BackgroundJobName("first")
    val second = BackgroundJobName("second")
    BackgroundJobRegistry.register(second, stalenessThreshold = 1.minutes)
    BackgroundJobRegistry.register(first, stalenessThreshold = 1.minutes)

    assertThat(BackgroundJobRegistry.states.map { it.name }).isEqualTo(listOf(second, first))
  }
}
