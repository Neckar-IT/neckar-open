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
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes

/**
 * The case a periodic job runs into sooner or later: it polls every few minutes, and then something
 * arrives that should not wait for the next tick — a webhook, an operator, a dependency coming back.
 */
@DisableLogging
class TriggerNowTest {
  @Test
  fun `a trigger cuts the wait short`() = runTest {
    val ranAt = mutableListOf<Long>()
    val repeating = Repeating.every(10.minutes) { ranAt.add(currentTime) }.start(this, this)

    //The first cycle runs immediately, the second would be ten minutes away
    delay(100)
    assertThat(ranAt).hasSize(1)

    repeating.triggerNow()
    delay(100)

    assertThat(ranAt).hasSize(2)
    assertThat(ranAt[1]).isLessThan(10.minutes.inWholeMilliseconds)

    repeating.cancel()
  }

  /**
   * Coalescing: a burst of triggers is one follow-up cycle, because that cycle starts afterwards and
   * therefore already covers what the later triggers were about.
   */
  @Test
  fun `a burst of triggers produces one extra cycle`() = runTest {
    var cycles = 0
    val repeating = Repeating.every(10.minutes) {
      cycles++
      delay(200)
    }.start(this, this)

    delay(50)
    //The first cycle is still running
    assertThat(cycles).isEqualTo(1)

    repeating.triggerNow()
    repeating.triggerNow()
    repeating.triggerNow()

    delay(400)
    repeating.cancel()

    assertThat(cycles).isEqualTo(2)
  }

  @Test
  fun `a triggered cycle counts like any other`() = runTest {
    val repeating = Repeating.every(10.minutes) {}.start(this, this)

    delay(50)
    repeating.triggerNow()
    delay(50)
    repeating.cancel()

    assertThat(repeating.statistics.completedCycles).isEqualTo(2)
  }

  /**
   * A trigger is an extra cycle, not a change of rhythm: after it, the schedule picks up where it
   * would have been.
   */
  @Test
  fun `the schedule survives a trigger`() = runTest {
    val ranAt = mutableListOf<Long>()
    val repeating = Repeating.every(200.milliseconds) { ranAt.add(currentTime) }.start(this, this)

    delay(50)
    repeating.triggerNow()
    delay(700)
    repeating.cancel()

    //Immediate cycle, the trigger, then the plain 200ms rhythm again
    val gapsAfterTrigger = ranAt.drop(2).zipWithNext { earlier, later -> later - earlier }
    assertThat(gapsAfterTrigger.all { it >= 200 }).isTrue()
  }

  /**
   * The very reason to trigger a failing job: the dependency came back. Waiting out a backoff that was
   * calculated while it was down ignores exactly that.
   */
  @Test
  fun `a trigger cuts an error backoff short`() = runTest {
    val ranAt = mutableListOf<Long>()
    val repeating = Repeating.every(10.milliseconds) {
      ranAt.add(currentTime)
      throw IllegalStateException("the dependency is down")
    }
      .errorBackoff(ErrorBackoff.constant(5.minutes))
      .start(this, this)

    delay(50)
    assertThat(ranAt).hasSize(1)

    repeating.triggerNow()
    delay(50)
    repeating.cancel()

    //The trigger at 50 ms starts the attempt the backoff holds back for five minutes
    assertThat(ranAt).containsExactly(0L, 50L)
  }

  /**
   * A trigger describes a moment. One that arrived while the job was stopped must not fire the first
   * cycle of the next start — on a daily schedule that would turn 03:00 into "whenever the service
   * restarted".
   */
  @Test
  fun `a trigger on a stopped job is discarded`() = runTest {
    var cycles = 0
    val repeating = Repeating(Repeating.every(10.minutes) { cycles++ }.build())

    repeating.triggerNow()
    repeating.start(this, this)
    delay(100)
    repeating.cancel()

    //Exactly the one immediate cycle the interval schedule always runs - not a second one
    assertThat(cycles).isEqualTo(1)
  }

  /**
   * A cycle that fails is still a cycle: the failure must not swallow the trigger and leave the caller
   * waiting for a schedule that is minutes away.
   */
  @Test
  fun `a trigger works after a failed cycle`() = runTest {
    var cycles = 0
    val repeating = Repeating.every(10.minutes) {
      cycles++
      throw IllegalStateException("boom")
    }.start(this, this)

    delay(50)
    repeating.triggerNow()
    delay(50)
    repeating.cancel()

    assertThat(cycles).isEqualTo(2)
    assertThat(repeating.statistics.failedCycles).isEqualTo(2)
  }
}
