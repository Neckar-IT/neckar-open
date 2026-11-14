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
import org.junit.jupiter.api.Test
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.ZoneId
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class CycleScheduleMaxGapTest {
  private val berlin: ZoneId = ZoneId.of("Europe/Berlin")

  @Test
  fun `the longest pause of each schedule`() {
    assertThat(IntervalSchedule(15.seconds).maxGap).isEqualTo(15.seconds)
    assertThat(RandomIntervalSchedule(1.minutes, 5.minutes).maxGap).isEqualTo(5.minutes)
    assertThat(ImmediateSchedule.maxGap).isEqualTo(Duration.ZERO)
    assertThat(HourlySchedule(15, berlin).maxGap).isEqualTo(1.hours)
    assertThat(DailySchedule(LocalTime.of(3, 0), berlin).maxGap).isEqualTo(25.hours)
    assertThat(WeeklySchedule(DayOfWeek.THURSDAY, LocalTime.of(22, 0), berlin).maxGap).isEqualTo(7.days + 1.hours)
    assertThat(WeekdaySchedule(LocalTime.of(7, 30), berlin).maxGap).isEqualTo(3.days + 1.hours)
  }

  @Test
  fun `the longest pause of a loop adds the longest error backoff`() {
    assertThat(ErrorBackoff.exponential(initial = 30.seconds, maximum = 10.minutes).maximumDelay).isEqualTo(10.minutes)
    assertThat(ErrorBackoff.constant(42.seconds).maximumDelay).isEqualTo(42.seconds)

    val withBackoff = Repeating.every(15.seconds) {}.errorBackoff(ErrorBackoff.exponential(initial = 30.seconds, maximum = 10.minutes)).build()
    val withoutBackoff = Repeating.every(15.seconds) {}.build()

    assertThat(withBackoff.maxGap).isEqualTo(10.minutes + 15.seconds)
    assertThat(withoutBackoff.maxGap).isEqualTo(15.seconds)
  }
}
