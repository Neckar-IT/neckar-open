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
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@DisableLogging
class ErrorBackoffTest {
  @Test
  fun `exponential doubles with every consecutive failure`() {
    val backoff = ErrorBackoff.exponential(initial = 30.seconds, maximum = 10.minutes)

    assertThat(backoff.delayAfter(1)).isEqualTo(30.seconds)
    assertThat(backoff.delayAfter(2)).isEqualTo(1.minutes)
    assertThat(backoff.delayAfter(3)).isEqualTo(2.minutes)
    assertThat(backoff.delayAfter(4)).isEqualTo(4.minutes)
    assertThat(backoff.delayAfter(5)).isEqualTo(8.minutes)
  }

  @Test
  fun `exponential stops at the maximum`() {
    val backoff = ErrorBackoff.exponential(initial = 30.seconds, maximum = 10.minutes)

    assertThat(backoff.delayAfter(6)).isEqualTo(10.minutes)
    assertThat(backoff.delayAfter(100)).isEqualTo(10.minutes)
  }

  /**
   * An outage lasting days produces a failure count that would overflow the doubling into a
   * meaningless Duration — the cap has to hold before the multiplication, not after.
   */
  @Test
  fun `exponential survives an absurd failure count`() {
    val backoff = ErrorBackoff.exponential(initial = 1.seconds, maximum = 1.hours)

    assertThat(backoff.delayAfter(Int.MAX_VALUE)).isEqualTo(1.hours)
  }

  @Test
  fun `constant waits the same however often it failed`() {
    val backoff = ErrorBackoff.constant(42.seconds)

    assertThat(backoff.delayAfter(1)).isEqualTo(42.seconds)
    assertThat(backoff.delayAfter(99)).isEqualTo(42.seconds)
  }

  @Test
  fun `an inverted range is rejected`() {
    assertFailure { ErrorBackoff.exponential(initial = 10.minutes, maximum = 1.minutes) }
      .isInstanceOf(IllegalArgumentException::class)
  }

  /**
   * The point of the whole thing: while the failures last, the retries thin out instead of hammering
   * a dependency that is already down.
   */
  @Test
  fun `a failing job retries less and less often`() = runTest {
    val attemptedAt = mutableListOf<Long>()

    val repeating = Repeating.every(10.milliseconds) {
      attemptedAt.add(currentTime)
      throw IllegalStateException("the dependency is down")
    }
      .errorBackoff(ErrorBackoff.exponential(initial = 100.milliseconds, maximum = 1.seconds))
      .start(this, this)

    delay(1_000)
    repeating.cancel()

    //100ms, 200ms, 400ms … each gap at least as long as the one before it
    val gaps = attemptedAt.zipWithNext { earlier, later -> later - earlier }
    assertThat(gaps.size).isGreaterThan(2)
    assertThat(gaps).isEqualTo(gaps.sorted())
  }

  @Test
  fun `a job that recovers goes back to its normal rhythm`() = runTest {
    var cycles = 0
    val attemptedAt = mutableListOf<Long>()

    val repeating = Repeating.every(10.milliseconds) {
      attemptedAt.add(currentTime)
      cycles++
      if (cycles <= 3) throw IllegalStateException("still down")
    }
      .errorBackoff(ErrorBackoff.exponential(initial = 100.milliseconds, maximum = 1.seconds))
      .start(this, this)

    delay(1_500)
    repeating.cancel()

    //Once it recovers the gaps are the plain interval again, far below the backoff it had reached
    val lastGaps = attemptedAt.takeLast(3).zipWithNext { earlier, later -> later - earlier }
    assertThat(lastGaps.all { it < 100 }).isTrue()
  }
}
