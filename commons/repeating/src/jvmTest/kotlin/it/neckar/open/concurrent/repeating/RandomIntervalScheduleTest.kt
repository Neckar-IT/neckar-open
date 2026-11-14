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
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import kotlin.random.Random
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

class RandomIntervalScheduleTest {
  private val schedule = RandomIntervalSchedule(10.seconds, 20.seconds, Random(seed = 42))

  @Test
  fun `every pause stays within the configured bounds`() = runTest {
    var cycleAt: Instant? = null
    repeat(20) {
      val before = currentTime
      cycleAt = schedule.awaitCycle(cycleAt)
      val waited = currentTime - before

      assertThat(waited).isBetween(10_000, 20_000)
    }
  }

  /**
   * Unlike [IntervalSchedule], the first cycle waits too: a fleet started at once must not fire its
   * first cycle in lockstep either.
   */
  @Test
  fun `the first cycle waits as well`() = runTest {
    schedule.awaitCycle(after = null)

    assertThat(currentTime).isGreaterThanOrEqualTo(10_000)
  }

  @Test
  fun `an inverted range is rejected`() {
    assertFailure { RandomIntervalSchedule(20.seconds, 10.seconds) }.isInstanceOf(IllegalArgumentException::class)
  }
}
