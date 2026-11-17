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

import assertk.*
import assertk.assertions.*
import it.neckar.open.test.utils.DisableLogging
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.milliseconds

/**
 * A caller that only wants to look at a job — a health endpoint, an admin page, a log line — should
 * not have to register an observer to find out whether it still works.
 */
@DisableLogging
class RepeatingStatisticsTest {
  @Test
  fun `a fresh job has done nothing and is idle`() {
    val repeating = Repeating(Repeating.every(50.milliseconds) {}.build())

    assertThat(repeating.state).isEqualTo(RepeatingState.Idle)
    assertThat(repeating.statistics.startedCycles).isEqualTo(0)
    assertThat(repeating.statistics.hasNeverFinishedACycle).isTrue()
  }

  @Test
  fun `completed cycles are counted and timestamped`() = runTest {
    val repeating = Repeating.every(50.milliseconds) {}.start(this, this)

    delay(120)
    repeating.cancel()

    val statistics = repeating.statistics
    assertThat(statistics.completedCycles).isEqualTo(3)
    assertThat(statistics.startedCycles).isEqualTo(3)
    assertThat(statistics.failedCycles).isEqualTo(0)
    assertThat(statistics.lastCycleCompletedAt).isNotNull()
    assertThat(statistics.hasNeverFinishedACycle).isFalse()
  }

  @Test
  fun `a failing cycle is counted with its error and does not stop the job`() = runTest {
    val repeating = Repeating.every(50.milliseconds) { throw IllegalStateException("boom") }.start(this, this)

    delay(120)
    repeating.cancel()

    val statistics = repeating.statistics
    assertThat(statistics.failedCycles).isEqualTo(3)
    assertThat(statistics.completedCycles).isEqualTo(0)
    assertThat(statistics.consecutiveFailures).isEqualTo(3)
    assertThat(statistics.lastError?.message).isEqualTo("boom")
  }

  /**
   * The counter the backoff scales with: a job that recovers must not keep backing off as if it were
   * still failing.
   */
  @Test
  fun `a completed cycle resets the consecutive failures`() = runTest {
    var cycles = 0
    val repeating = Repeating.every(50.milliseconds) {
      cycles++
      if (cycles <= 2) throw IllegalStateException("boom")
    }.start(this, this)

    delay(170)
    repeating.cancel()

    val statistics = repeating.statistics
    assertThat(statistics.failedCycles).isEqualTo(2)
    assertThat(statistics.consecutiveFailures).isEqualTo(0)
    assertThat(statistics.completedCycles).isGreaterThan(0)
  }

  @Test
  suspend fun `the state follows the lifecycle`(): Unit = coroutineScope {
    val repeating = Repeating.every(50.milliseconds) {}.start(this, this)
    assertThat(repeating.state).isEqualTo(RepeatingState.Running)

    repeating.cancel()
    assertThat(repeating.state).isEqualTo(RepeatingState.Idle)
  }

  @Test
  fun `a job that stopped itself says so`() = runTest {
    val repeating = Repeating.every(20.milliseconds) { stopRepeating() }.start(this, this)

    delay(60)

    assertThat(repeating.state).isEqualTo(RepeatingState.StoppedItself)
    //Stopping itself is a completed cycle, not a failure
    assertThat(repeating.statistics.completedCycles).isEqualTo(1)
    assertThat(repeating.statistics.failedCycles).isEqualTo(0)

    repeating.cancel()
  }

  /**
   * Starting an already running job is what two callers racing on startup do. Throwing would make the
   * loser fail for something that is already true.
   */
  @Test
  fun `starting twice is a no-op`() = runTest {
    var cycles = 0
    val repeating = Repeating.every(50.milliseconds) { cycles++ }.start(this, this)

    repeating.start(this, this)
    delay(120)
    repeating.cancel()

    //Two workers would double the count
    assertThat(cycles).isEqualTo(3)
  }

  @Test
  fun `the statistics survive a cancel, so a stopped job can still be inspected`() = runTest {
    val repeating = Repeating.every(50.milliseconds) {}.start(this, this)
    delay(120)

    repeating.cancel()

    assertThat(repeating.statistics.completedCycles).isEqualTo(3)
  }
}
