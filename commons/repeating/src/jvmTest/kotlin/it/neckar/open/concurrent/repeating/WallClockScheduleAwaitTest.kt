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
import it.neckar.open.test.utils.WithVirtualTime
import it.neckar.open.test.utils.advance
import it.neckar.open.time.VirtualNowProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.ZoneId
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

/**
 * `nextRunAfter` decides *when*, `awaitCycle` turns that into the actual wait — and only the first
 * half was pinned. A sign error or a wrong zone there makes a daily job fire every millisecond or
 * never again, and the second failure is the kind nobody notices for weeks.
 *
 */
@DisableLogging
class WallClockScheduleAwaitTest {
  private val berlin: ZoneId = ZoneId.of("Europe/Berlin")

  @Test
  @WithVirtualTime(value = PinnedClock)
  fun `an hourly schedule waits until the next matching minute`() = runTest {
    HourlySchedule(15, berlin).awaitCycle(after = null)

    //11:00 -> 11:15
    assertThat(currentTime).isEqualTo(15.minutes.inWholeMilliseconds)
  }

  @Test
  @WithVirtualTime(value = PinnedClock)
  fun `a daily schedule waits until the time comes round`() = runTest {
    DailySchedule(LocalTime.of(3, 0), berlin).awaitCycle(after = null)

    //11:00 Monday -> 03:00 Tuesday
    assertThat(currentTime).isEqualTo(16.hours.inWholeMilliseconds)
  }

  @Test
  @WithVirtualTime(value = PinnedClock)
  fun `a weekly schedule waits until its day comes round`() = runTest {
    WeeklySchedule(DayOfWeek.THURSDAY, LocalTime.of(22, 0), berlin).awaitCycle(after = null)

    //11:00 Monday -> 22:00 Thursday
    assertThat(currentTime).isEqualTo((3 * 24 + 11).hours.inWholeMilliseconds)
  }

  /**
   * The wall clock lags the coroutine clock: it reads 11:00 when the wait for 11:15 ends and
   * 11:14:59.995 when the next wait starts; the cycle still counts as the 11:15 slot.
   */
  @Test
  @WithVirtualTime(value = PinnedClock)
  fun `a wake-up before the slot by the wall clock counts as the slot`(nowProvider: VirtualNowProvider) = runTest {
    val schedule = HourlySchedule(15, berlin)

    val firstCycleAt = schedule.awaitCycle(after = null)
    nowProvider.advance(15.minutes - 5.milliseconds)
    val afterFirst = currentTime

    schedule.awaitCycle(firstCycleAt)

    assertThat(currentTime - afterFirst).isEqualTo((1.hours + 5.milliseconds).inWholeMilliseconds)
  }

  /**
   * A wall clock that jumps forward during the sleep, after a suspend of the host, wakes the job late.
   * The cycle counts as the current time, so the slots skipped by the jump make no extra cycle.
   */
  @Test
  @WithVirtualTime(value = PinnedClock)
  fun `a late wake-up stands for the current time`(nowProvider: VirtualNowProvider) = runTest {
    val clockJump = launch {
      delay(1.minutes)
      nowProvider.advance(2.hours)
    }

    val schedule = HourlySchedule(15, berlin)

    val firstCycleAt = schedule.awaitCycle(after = null)
    clockJump.join()
    val afterFirst = currentTime

    //Wakes at 13:00 by the wall clock, past the slots at 11:15 and 12:15; the next cycle waits for 13:15
    assertThat(firstCycleAt).isEqualTo(PinnedInstant + 2.hours)
    schedule.awaitCycle(firstCycleAt)
    assertThat(currentTime - afterFirst).isEqualTo(15.minutes.inWholeMilliseconds)
  }

  @Test
  @WithVirtualTime(value = PinnedClock)
  fun `a slot passed during the previous cycle runs at once`(nowProvider: VirtualNowProvider) = runTest {
    val schedule = HourlySchedule(15, berlin)

    val firstCycleAt = schedule.awaitCycle(after = null)
    nowProvider.advance(15.minutes + 70.minutes)
    val afterFirst = currentTime

    val secondCycleAt = schedule.awaitCycle(firstCycleAt)

    assertThat(currentTime).isEqualTo(afterFirst)
    //12:25, the current time: the cycle stands for the passed slot at 12:15
    assertThat(secondCycleAt).isEqualTo(PinnedInstant + 85.minutes)
  }

  companion object {
    /**
     * 2026-07-27 11:00 Europe/Berlin (09:00 UTC) — a Monday well away from any daylight-saving change.
     *
     * Pinned per test method, not on the class: the extension shares one provider across a class, and
     * a test that advances the clock would then shift the expectations of every other test.
     */
    const val PinnedClock: Double = 1785142800000.0

    private val PinnedInstant: Instant = Instant.fromEpochMilliseconds(PinnedClock.toLong())
  }
}
