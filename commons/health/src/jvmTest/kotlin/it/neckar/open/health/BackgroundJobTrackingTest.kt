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
import it.neckar.open.concurrent.repeating.Repeating
import it.neckar.open.test.utils.DisableLogging
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes

@DisableLogging
class BackgroundJobTrackingTest {
  private val jobName = BackgroundJobName("target-polling")

  @BeforeEach
  @AfterEach
  fun clearRegistry() {
    BackgroundJobRegistry.clear()
  }

  /**
   * The job must be known before its first cycle: a job whose very first cycle wedges never reports
   * anything, and an unregistered job cannot turn stale.
   */
  @Test
  fun `trackedAs registers the job before the first cycle runs`() {
    val builder = Repeating.every(100.milliseconds) {}.trackedAs(jobName, stalenessThreshold = 5.minutes)
    assertThat(builder.cycleListener).isNotNull()

    val state = BackgroundJobRegistry.stateOf(jobName)
    assertThat(state?.stalenessThreshold).isEqualTo(5.minutes)
    assertThat(state?.completedCycles).isEqualTo(0)
  }

  @Test
  fun `every completed cycle is recorded in the registry`() = runTest {
    val repeating = Repeating.every(100.milliseconds) {}
      .cycleTimeout(1.minutes)
      .trackedAs(jobName, stalenessThreshold = 5.minutes)
      .start(this, this)

    delay(150)
    repeating.cancel()

    assertThat(BackgroundJobRegistry.stateOf(jobName)?.completedCycles).isEqualTo(2)
    assertThat(BackgroundJobRegistry.stateOf(jobName)?.lastCycleCompletedAt).isNotNull()
  }

  /**
   * The wedge of #2002: cycles keep starting and keep timing out. Nothing completes, so the job must
   * count as stale even though its loop is demonstrably alive.
   */
  @Test
  fun `a job whose cycles keep timing out records failures and no completion`() = runTest {
    val repeating = Repeating.every(10.milliseconds) { delay(10_000) }
      .cycleTimeout(20.milliseconds)
      .trackedAs(jobName, stalenessThreshold = 5.minutes)
      .start(this, this)

    delay(100)
    repeating.cancel()

    val state = BackgroundJobRegistry.stateOf(jobName)
    assertThat(state?.completedCycles).isEqualTo(0)
    assertThat(state?.failedCycles ?: 0).isGreaterThan(0)
    assertThat(state?.lastCycleCompletedAt).isNull()
  }
}
