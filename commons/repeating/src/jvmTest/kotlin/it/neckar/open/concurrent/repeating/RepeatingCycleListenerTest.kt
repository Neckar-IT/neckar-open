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
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.milliseconds

@DisableLogging
class RepeatingCycleListenerTest {
  /**
   * Records what the listener was told, in order, so the tests can assert the exact call sequence.
   */
  private class RecordingCycleListener(
    /**
     * Thrown by every callback when set — a listener that breaks must not break the job.
     */
    private val failWith: Throwable? = null,
  ) : RepeatingCycleListener {
    val events: MutableList<String> = mutableListOf()
    val failures: MutableList<Throwable> = mutableListOf()

    override fun onCycleStarted() {
      events.add("started")
      failWith?.let { throw it }
    }

    override fun onCycleCompleted() {
      events.add("completed")
      failWith?.let { throw it }
    }

    override fun onCycleFailed(error: Throwable) {
      events.add("failed")
      failures.add(error)
      failWith?.let { throw it }
    }
  }

  @Test
  fun `a successful cycle reports started and completed`() = runTest {
    val listener = RecordingCycleListener()

    val repeating = Repeating.every(100.milliseconds) {
      //the cycle itself does nothing - only the reported events matter
    }.cycleListener(listener).start(this, this)

    delay(150)
    repeating.cancel()

    assertThat(listener.events).isEqualTo(listOf("started", "completed", "started", "completed"))
  }

  @Test
  fun `a throwing cycle reports the failure instead of a completion`() = runTest {
    val listener = RecordingCycleListener()

    val repeating = Repeating.every(100.milliseconds) {
      throw IllegalStateException("boom")
    }.cycleListener(listener).start(this, this)

    delay(50)
    repeating.cancel()

    assertThat(listener.events).isEqualTo(listOf("started", "failed"))
    assertThat(listener.failures.single()).isInstanceOf(IllegalStateException::class)
  }

  /**
   * The hang case (#2002): the cycle never returns, the cycle timeout cancels it. Without the reported
   * failure the job would look alive to everything except the logs.
   */
  @Test
  fun `a cycle that exceeds its timeout reports a failure`() = runTest {
    val listener = RecordingCycleListener()

    val repeating = Repeating.every(100.milliseconds) {
      delay(10_000)
    }.cycleTimeout(50.milliseconds).cycleListener(listener).start(this, this)

    delay(60)
    repeating.cancel()

    assertThat(listener.events).isEqualTo(listOf("started", "failed"))
    assertThat(listener.failures.single()).isInstanceOf(kotlinx.coroutines.TimeoutCancellationException::class)
  }

  /**
   * The listener runs in the job loop. A listener that throws — a metric backend that is down, a
   * registry that rejects — must not take down the very job it observes.
   */
  @Test
  fun `a throwing listener does not stop the job`() = runTest {
    var cycles = 0
    val listener = RecordingCycleListener(failWith = IllegalStateException("listener is broken"))

    val repeating = Repeating.every(100.milliseconds) {
      cycles++
    }.cycleListener(listener).start(this, this)

    delay(250)
    repeating.cancel()

    assertThat(cycles).isEqualTo(3)
  }
}
