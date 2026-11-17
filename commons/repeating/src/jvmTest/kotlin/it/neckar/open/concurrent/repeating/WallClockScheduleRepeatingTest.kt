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
import it.neckar.open.time.now
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaInstant

/**
 * Which slot of a [WallClockSchedule] follows a cycle past a slot, a triggered cycle and a failed
 * cycle with an error backoff; each test moves the repo clock itself or with [launchClockTicker].
 */
@DisableLogging
class WallClockScheduleRepeatingTest {
  private val berlin: ZoneId = ZoneId.of("Europe/Berlin")

  @Test
  @WithVirtualTime(value = WallClockScheduleAwaitTest.PinnedClock)
  fun `a triggered cycle past the daily slot runs the scheduled cycle right after it`(nowProvider: VirtualNowProvider) = runTest {
    val cycleStarts = mutableListOf<String>()
    val repeating = Repeating.following(DailySchedule(LocalTime.of(11, 30), berlin)) {
      cycleStarts.add(wallClockNow())
      //The triggered cycle runs from 11:00 to 11:50, past the slot at 11:30
      val duration = if (cycleStarts.size == 1) 50.minutes else Duration.ZERO
      nowProvider.advance(duration)
      delay(duration)
    }.start(this, this)

    delay(1)
    repeating.triggerNow()
    delay(50.minutes + 30.minutes)
    repeating.cancel()

    assertThat(cycleStarts).containsExactly("11:00", "11:50")
  }

  @Test
  @WithVirtualTime(value = WallClockScheduleAwaitTest.PinnedClock)
  fun `slots passed during one cycle make one cycle after it`(nowProvider: VirtualNowProvider) = runTest {
    val cycleStarts = mutableListOf<String>()
    val repeating = Repeating.following(HourlySchedule(15, berlin)) {
      cycleStarts.add(wallClockNow())
      //The triggered cycle runs from 11:00 to 13:30, past the slots at 11:15, 12:15 and 13:15
      val duration = if (cycleStarts.size == 1) 150.minutes else Duration.ZERO
      nowProvider.advance(duration)
      delay(duration)
    }.start(this, this)

    delay(1)
    repeating.triggerNow()
    //Ends before the slot at 14:15
    delay(150.minutes + 30.minutes)
    repeating.cancel()

    assertThat(cycleStarts).containsExactly("11:00", "13:30")
  }

  @Test
  @WithVirtualTime(value = WallClockScheduleAwaitTest.PinnedClock)
  fun `an error backoff past the next slot waits for the slot after it`(nowProvider: VirtualNowProvider) = runTest {
    val clockTicker = launchClockTicker(nowProvider)
    val cycleStartMinutes = mutableListOf<Long>()
    val repeating = Repeating.following(HourlySchedule(15, berlin)) {
      cycleStartMinutes.add(currentTime.milliseconds.inWholeMinutes)
      throw IllegalStateException("the dependency is down")
    }
      .errorBackoff(ErrorBackoff.constant(90.minutes))
      .start(this, this)

    //11:15 fails, the backoff ends at 12:45, the next slot is 13:15; ends before 14:15
    delay(3.hours)
    repeating.cancel()
    clockTicker.cancel()

    assertThat(cycleStartMinutes).containsExactly(15L, 135L)
  }

  /**
   * A trigger that cuts the backoff short starts a cycle that stands for its own start: the slot
   * passed during the backoff makes no extra cycle.
   */
  @Test
  @WithVirtualTime(value = WallClockScheduleAwaitTest.PinnedClock)
  fun `a trigger during an error backoff starts a cycle that stands for its own start`(nowProvider: VirtualNowProvider) = runTest {
    val clockTicker = launchClockTicker(nowProvider)
    val cycleStartMinutes = mutableListOf<Long>()
    val repeating = Repeating.following(HourlySchedule(15, berlin)) {
      cycleStartMinutes.add(currentTime.milliseconds.inWholeMinutes)
      if (cycleStartMinutes.size == 1) {
        throw IllegalStateException("the dependency is down")
      }
    }
      .errorBackoff(ErrorBackoff.constant(90.minutes))
      .start(this, this)

    //11:15 fails; the trigger at 12:30 starts a cycle, the next slot is 13:15; ends before 14:15
    delay(90.minutes)
    repeating.triggerNow()
    delay(90.minutes)
    repeating.cancel()
    clockTicker.cancel()

    assertThat(cycleStartMinutes).containsExactly(15L, 90L, 135L)
  }

  /**
   * A trigger during a cycle that runs past a slot makes one follow-up cycle, which stands for its own
   * start and so for the passed slot too.
   */
  @Test
  @WithVirtualTime(value = WallClockScheduleAwaitTest.PinnedClock)
  fun `a trigger during a cycle past a slot makes one follow-up cycle`(nowProvider: VirtualNowProvider) = runTest {
    val clockTicker = launchClockTicker(nowProvider)
    val cycleStartMinutes = mutableListOf<Long>()
    val repeating = Repeating.following(HourlySchedule(15, berlin)) {
      cycleStartMinutes.add(currentTime.milliseconds.inWholeMinutes)
      //The triggered cycle runs from 11:00 to 11:20, past the slot at 11:15
      if (cycleStartMinutes.size == 1) {
        delay(20.minutes)
      }
    }.start(this, this)

    delay(1)
    repeating.triggerNow()
    delay(10.minutes)
    repeating.triggerNow()
    //Ends before the slot at 13:15
    delay(2.hours)
    repeating.cancel()
    clockTicker.cancel()

    assertThat(cycleStartMinutes).containsExactly(0L, 20L, 75L)
  }

  /**
   * Moves the repo clock along with the coroutine clock, one second at a time.
   */
  private fun CoroutineScope.launchClockTicker(nowProvider: VirtualNowProvider): Job = launch {
    while (isActive) {
      delay(1.seconds)
      nowProvider.advance(1.seconds)
    }
  }

  private fun wallClockNow(): String = ZonedDateTime.ofInstant(now().toJavaInstant(), berlin).toLocalTime().toString()
}
