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
import java.time.ZonedDateTime

class WallClockScheduleTest {
  private val berlin: ZoneId = ZoneId.of("Europe/Berlin")

  private val quarterPast = HourlySchedule(15, berlin)
  private val threeInTheMorning = DailySchedule(LocalTime.of(3, 0), berlin)
  private val thursdayEvening = WeeklySchedule(DayOfWeek.THURSDAY, LocalTime.of(22, 0), berlin)
  private val weekdayMorning = WeekdaySchedule(LocalTime.of(7, 30), berlin)

  @Test
  fun `an hourly schedule fires later the same hour when the minute is still ahead`() {
    val fivePastNine = ZonedDateTime.of(2026, 7, 27, 9, 5, 0, 0, berlin)

    assertThat(quarterPast.nextRunAfter(fivePastNine))
      .isEqualTo(ZonedDateTime.of(2026, 7, 27, 9, 15, 0, 0, berlin))
  }

  @Test
  fun `an hourly schedule fires the next hour once the minute has passed`() {
    val halfPastNine = ZonedDateTime.of(2026, 7, 27, 9, 30, 0, 0, berlin)

    assertThat(quarterPast.nextRunAfter(halfPastNine))
      .isEqualTo(ZonedDateTime.of(2026, 7, 27, 10, 15, 0, 0, berlin))
  }

  @Test
  fun `an hourly schedule at exactly its own minute fires the next hour`() {
    val quarterPastNine = ZonedDateTime.of(2026, 7, 27, 9, 15, 0, 0, berlin)

    assertThat(quarterPast.nextRunAfter(quarterPastNine))
      .isEqualTo(ZonedDateTime.of(2026, 7, 27, 10, 15, 0, 0, berlin))
  }

  @Test
  fun `an hourly schedule crosses midnight into the next day`() {
    val lateEvening = ZonedDateTime.of(2026, 7, 27, 23, 45, 0, 0, berlin)

    assertThat(quarterPast.nextRunAfter(lateEvening))
      .isEqualTo(ZonedDateTime.of(2026, 7, 28, 0, 15, 0, 0, berlin))
  }

  /**
   * The hour that does not exist locally: at 02:00 the clock jumps to 03:00 CEST, so the run after
   * 01:15 CET is 03:15 CEST — one elapsed hour later, as an hourly job should be.
   */
  @Test
  fun `an hourly schedule skips the hour the daylight-saving change removes`() {
    val beforeTheJump = ZonedDateTime.of(2026, 3, 29, 1, 15, 0, 0, berlin)

    val nextRun = quarterPast.nextRunAfter(beforeTheJump)

    assertThat(nextRun).isEqualTo(ZonedDateTime.of(2026, 3, 29, 3, 15, 0, 0, berlin))
    assertThat(java.time.Duration.between(beforeTheJump, nextRun).toHours()).isEqualTo(1)
  }

  /**
   * The hour that exists twice locally: 02:00 CEST falls back to 02:00 CET, so an hourly job fires in
   * each pass through the repeated hour, an hour of elapsed time apart.
   */
  @Test
  fun `an hourly schedule runs in both passes through the repeated daylight-saving hour`() {
    val firstPass = ZonedDateTime.parse("2026-10-25T01:20:00+02:00").withZoneSameInstant(berlin)

    val insideRepeatedHour = quarterPast.nextRunAfter(firstPass)
    val secondPass = quarterPast.nextRunAfter(insideRepeatedHour)

    assertThat(insideRepeatedHour).isEqualTo(ZonedDateTime.parse("2026-10-25T02:15:00+02:00").withZoneSameInstant(berlin))
    assertThat(secondPass).isEqualTo(ZonedDateTime.parse("2026-10-25T02:15:00+01:00").withZoneSameInstant(berlin))
    assertThat(java.time.Duration.between(insideRepeatedHour, secondPass).toHours()).isEqualTo(1)
  }

  /**
   * A daily time that the spring change removes: 02:30 does not exist on 2026-03-29, so `java.time`
   * moves the run to 03:30 rather than skipping the day.
   */
  @Test
  fun `a daily schedule at a time the daylight-saving change removes moves forward`() {
    val halfPastTwo = DailySchedule(LocalTime.of(2, 30), berlin)
    val theEveningBefore = ZonedDateTime.of(2026, 3, 28, 20, 0, 0, 0, berlin)

    assertThat(halfPastTwo.nextRunAfter(theEveningBefore))
      .isEqualTo(ZonedDateTime.of(2026, 3, 29, 3, 30, 0, 0, berlin))
  }

  @Test
  fun `a minute outside an hour is rejected`() {
    assertFailure { HourlySchedule(60, berlin) }.isInstanceOf(IllegalArgumentException::class)
  }

  @Test
  fun `a daily schedule fires later the same day when the time is still ahead`() {
    val monday1am = ZonedDateTime.of(2026, 7, 27, 1, 0, 0, 0, berlin)

    assertThat(threeInTheMorning.nextRunAfter(monday1am))
      .isEqualTo(ZonedDateTime.of(2026, 7, 27, 3, 0, 0, 0, berlin))
  }

  @Test
  fun `a daily schedule fires the next day once the time has passed`() {
    val monday4am = ZonedDateTime.of(2026, 7, 27, 4, 0, 0, 0, berlin)

    assertThat(threeInTheMorning.nextRunAfter(monday4am))
      .isEqualTo(ZonedDateTime.of(2026, 7, 28, 3, 0, 0, 0, berlin))
  }

  /**
   * The moment a run finishes, "next" must mean tomorrow. Matching the same instant again would run
   * the job twice, or spin.
   */
  @Test
  fun `a daily schedule at exactly its own time fires the next day`() {
    val monday3am = ZonedDateTime.of(2026, 7, 27, 3, 0, 0, 0, berlin)

    assertThat(threeInTheMorning.nextRunAfter(monday3am))
      .isEqualTo(ZonedDateTime.of(2026, 7, 28, 3, 0, 0, 0, berlin))
  }

  /**
   * Wall-clock, not elapsed time: across the March daylight-saving change the job stays at 03:00
   * local, so the elapsed gap is 22 hours where the calendar says one day.
   */
  @Test
  fun `a daily schedule keeps its local time across the daylight-saving change`() {
    //2026-03-29 is the night Europe/Berlin skips from 02:00 to 03:00 CEST
    val beforeTheChange = ZonedDateTime.of(2026, 3, 28, 4, 0, 0, 0, berlin)

    val nextRun = threeInTheMorning.nextRunAfter(beforeTheChange)

    assertThat(nextRun.toLocalTime()).isEqualTo(LocalTime.of(3, 0))
    assertThat(nextRun.toLocalDate().toString()).isEqualTo("2026-03-29")
    //22 hours of elapsed time, one calendar day of wall-clock time
    assertThat(java.time.Duration.between(beforeTheChange, nextRun).toHours()).isEqualTo(22)
  }

  @Test
  fun `a weekly schedule finds its day later in the same week`() {
    val monday = ZonedDateTime.of(2026, 7, 27, 9, 0, 0, 0, berlin)

    assertThat(thursdayEvening.nextRunAfter(monday))
      .isEqualTo(ZonedDateTime.of(2026, 7, 30, 22, 0, 0, 0, berlin))
  }

  @Test
  fun `a weekly schedule wraps into the next week once its day has passed`() {
    val thursdayLate = ZonedDateTime.of(2026, 7, 30, 23, 0, 0, 0, berlin)

    assertThat(thursdayEvening.nextRunAfter(thursdayLate))
      .isEqualTo(ZonedDateTime.of(2026, 8, 6, 22, 0, 0, 0, berlin))
  }

  @Test
  fun `a weekly schedule at exactly its own time fires a week later`() {
    val thursday10pm = ZonedDateTime.of(2026, 7, 30, 22, 0, 0, 0, berlin)

    assertThat(thursdayEvening.nextRunAfter(thursday10pm))
      .isEqualTo(ZonedDateTime.of(2026, 8, 6, 22, 0, 0, 0, berlin))
  }

  @Test
  fun `a weekday schedule fires later the same weekday when the time is still ahead`() {
    val wednesday6am = ZonedDateTime.of(2026, 10, 14, 6, 0, 0, 0, berlin)

    assertThat(weekdayMorning.nextRunAfter(wednesday6am))
      .isEqualTo(ZonedDateTime.of(2026, 10, 14, 7, 30, 0, 0, berlin))
  }

  @Test
  fun `a weekday schedule steps from Friday over the weekend to Monday`() {
    val friday = ZonedDateTime.of(2026, 10, 16, 7, 31, 0, 0, berlin)

    assertThat(weekdayMorning.nextRunAfter(friday))
      .isEqualTo(ZonedDateTime.of(2026, 10, 19, 7, 30, 0, 0, berlin))
  }

  @Test
  fun `a weekday schedule fires on Monday when the reference is on Saturday`() {
    val saturday = ZonedDateTime.of(2026, 10, 17, 7, 30, 0, 0, berlin)

    assertThat(weekdayMorning.nextRunAfter(saturday))
      .isEqualTo(ZonedDateTime.of(2026, 10, 19, 7, 30, 0, 0, berlin))
  }

  @Test
  fun `a weekday schedule looks back from Sunday to Friday`() {
    val sunday = ZonedDateTime.of(2026, 10, 18, 12, 0, 0, 0, berlin)

    assertThat(weekdayMorning.lastRunAtOrBefore(sunday))
      .isEqualTo(ZonedDateTime.of(2026, 10, 16, 7, 30, 0, 0, berlin))
  }

  @Test
  fun `a weekday schedule looks back from Monday before its time to Friday`() {
    val mondayEarly = ZonedDateTime.of(2026, 10, 19, 7, 0, 0, 0, berlin)

    assertThat(weekdayMorning.lastRunAtOrBefore(mondayEarly))
      .isEqualTo(ZonedDateTime.of(2026, 10, 16, 7, 30, 0, 0, berlin))
  }

  @Test
  fun `a weekday schedule at exactly its own time looks back to that time`() {
    val tuesday = ZonedDateTime.of(2026, 10, 20, 7, 30, 0, 0, berlin)

    assertThat(weekdayMorning.lastRunAtOrBefore(tuesday)).isEqualTo(tuesday)
  }

  /**
   * 2027-03-28 is the Sunday Europe/Berlin springs forward to CEST, so the weekend gap is 71 hours of elapsed time.
   */
  @Test
  fun `a weekday schedule keeps its local time across the spring daylight-saving weekend`() {
    val friday = ZonedDateTime.of(2027, 3, 26, 7, 30, 0, 0, berlin)

    val nextRun = weekdayMorning.nextRunAfter(friday)

    assertThat(nextRun).isEqualTo(ZonedDateTime.of(2027, 3, 29, 7, 30, 0, 0, berlin))
    assertThat(java.time.Duration.between(friday, nextRun).toHours()).isEqualTo(71)
  }

  /**
   * 2026-10-25 is the Sunday Europe/Berlin falls back to CET, so the weekend gap is 73 hours of elapsed time.
   */
  @Test
  fun `a weekday schedule keeps its local time across the daylight-saving weekend`() {
    val friday = ZonedDateTime.of(2026, 10, 23, 7, 30, 0, 0, berlin)

    val nextRun = weekdayMorning.nextRunAfter(friday)

    assertThat(nextRun).isEqualTo(ZonedDateTime.of(2026, 10, 26, 7, 30, 0, 0, berlin))
    assertThat(java.time.Duration.between(friday, nextRun).toHours()).isEqualTo(73)
  }

  @Test
  fun `an hourly schedule looks back to the current hour once its minute has passed`() {
    val halfPastNine = ZonedDateTime.of(2026, 7, 27, 9, 30, 0, 0, berlin)

    assertThat(quarterPast.lastRunAtOrBefore(halfPastNine))
      .isEqualTo(ZonedDateTime.of(2026, 7, 27, 9, 15, 0, 0, berlin))
  }

  @Test
  fun `an hourly schedule looks back to the previous hour while its minute is still ahead`() {
    val fivePastNine = ZonedDateTime.of(2026, 7, 27, 9, 5, 0, 0, berlin)

    assertThat(quarterPast.lastRunAtOrBefore(fivePastNine))
      .isEqualTo(ZonedDateTime.of(2026, 7, 27, 8, 15, 0, 0, berlin))
  }

  @Test
  fun `an hourly schedule at exactly its own minute looks back to that minute`() {
    val quarterPastNine = ZonedDateTime.of(2026, 7, 27, 9, 15, 0, 0, berlin)

    assertThat(quarterPast.lastRunAtOrBefore(quarterPastNine)).isEqualTo(quarterPastNine)
  }

  @Test
  fun `a daily schedule looks back to today once its time has passed`() {
    val monday4am = ZonedDateTime.of(2026, 7, 27, 4, 0, 0, 0, berlin)

    assertThat(threeInTheMorning.lastRunAtOrBefore(monday4am))
      .isEqualTo(ZonedDateTime.of(2026, 7, 27, 3, 0, 0, 0, berlin))
  }

  @Test
  fun `a daily schedule looks back to yesterday while its time is still ahead`() {
    val monday1am = ZonedDateTime.of(2026, 7, 27, 1, 0, 0, 0, berlin)

    assertThat(threeInTheMorning.lastRunAtOrBefore(monday1am))
      .isEqualTo(ZonedDateTime.of(2026, 7, 26, 3, 0, 0, 0, berlin))
  }

  @Test
  fun `a daily schedule at exactly its own time looks back to that time`() {
    val monday3am = ZonedDateTime.of(2026, 7, 27, 3, 0, 0, 0, berlin)

    assertThat(threeInTheMorning.lastRunAtOrBefore(monday3am)).isEqualTo(monday3am)
  }

  @Test
  fun `a weekly schedule looks back to its day earlier in the same week`() {
    val friday = ZonedDateTime.of(2026, 7, 31, 9, 0, 0, 0, berlin)

    assertThat(thursdayEvening.lastRunAtOrBefore(friday))
      .isEqualTo(ZonedDateTime.of(2026, 7, 30, 22, 0, 0, 0, berlin))
  }

  @Test
  fun `a weekly schedule wraps into the previous week when its day is still ahead`() {
    val monday = ZonedDateTime.of(2026, 7, 27, 9, 0, 0, 0, berlin)

    assertThat(thursdayEvening.lastRunAtOrBefore(monday))
      .isEqualTo(ZonedDateTime.of(2026, 7, 23, 22, 0, 0, 0, berlin))
  }

  /**
   * The day matches while the slot is still ahead, so the search steps past it instead of answering
   * with a future moment.
   */
  @Test
  fun `a weekly schedule on its own day before its time wraps into the previous week`() {
    val thursdayMorning = ZonedDateTime.of(2026, 7, 30, 9, 0, 0, 0, berlin)

    assertThat(thursdayEvening.lastRunAtOrBefore(thursdayMorning))
      .isEqualTo(ZonedDateTime.of(2026, 7, 23, 22, 0, 0, 0, berlin))
  }

  @Test
  fun `a weekly schedule at exactly its own time looks back to that time`() {
    val thursday10pm = ZonedDateTime.of(2026, 7, 30, 22, 0, 0, 0, berlin)

    assertThat(thursdayEvening.lastRunAtOrBefore(thursday10pm)).isEqualTo(thursday10pm)
  }

  @Test
  fun `an hourly schedule looks back across midnight into the previous day`() {
    val justAfterMidnight = ZonedDateTime.of(2026, 7, 28, 0, 5, 0, 0, berlin)

    assertThat(quarterPast.lastRunAtOrBefore(justAfterMidnight))
      .isEqualTo(ZonedDateTime.of(2026, 7, 27, 23, 15, 0, 0, berlin))
  }

  /**
   * 02:15 does not exist locally, so the run before 03:00 CEST is 01:15 CET.
   */
  @Test
  fun `an hourly schedule looks back across the hour the daylight-saving change removes`() {
    val afterTheJump = ZonedDateTime.of(2026, 3, 29, 3, 0, 0, 0, berlin)

    assertThat(quarterPast.lastRunAtOrBefore(afterTheJump))
      .isEqualTo(ZonedDateTime.of(2026, 3, 29, 1, 15, 0, 0, berlin))
  }

  /**
   * The offset the reference carries decides which of the two 02:15 is meant.
   */
  @Test
  fun `an hourly schedule looks back into the same pass through the repeated daylight-saving hour`() {
    val secondPass = ZonedDateTime.parse("2026-10-25T02:20:00+01:00").withZoneSameInstant(berlin)

    assertThat(quarterPast.lastRunAtOrBefore(secondPass))
      .isEqualTo(ZonedDateTime.parse("2026-10-25T02:15:00+01:00").withZoneSameInstant(berlin))
  }

  @Test
  fun `an hourly schedule looks back into the first pass from inside it`() {
    val firstPass = ZonedDateTime.parse("2026-10-25T02:20:00+02:00").withZoneSameInstant(berlin)

    assertThat(quarterPast.lastRunAtOrBefore(firstPass))
      .isEqualTo(ZonedDateTime.parse("2026-10-25T02:15:00+02:00").withZoneSameInstant(berlin))
  }

  @Test
  fun `a daily schedule at a time the daylight-saving change removes looks back to the moved slot`() {
    val halfPastTwo = DailySchedule(LocalTime.of(2, 30), berlin)
    val thatMorning = ZonedDateTime.of(2026, 3, 29, 9, 0, 0, 0, berlin)

    assertThat(halfPastTwo.lastRunAtOrBefore(thatMorning))
      .isEqualTo(ZonedDateTime.of(2026, 3, 29, 3, 30, 0, 0, berlin))
  }

  /**
   * The contract the catch-up in `AgentScheduleCron` rests on: a service starting exactly on a slot
   * neither runs it twice nor misses it.
   */
  @Test
  fun `the last run and the next one enclose the reference with nothing in between`() {
    val references = listOf(
      ZonedDateTime.of(2026, 7, 27, 9, 15, 0, 0, berlin),
      ZonedDateTime.of(2026, 7, 30, 22, 0, 0, 0, berlin),
      ZonedDateTime.of(2026, 3, 29, 3, 0, 0, 0, berlin),
      ZonedDateTime.parse("2026-10-25T02:20:00+01:00").withZoneSameInstant(berlin),
      ZonedDateTime.of(2026, 12, 31, 23, 59, 0, 0, berlin),
      ZonedDateTime.of(2026, 10, 18, 7, 30, 0, 0, berlin),
    )

    listOf(quarterPast, threeInTheMorning, thursdayEvening, weekdayMorning).forEach { schedule ->
      references.forEach { reference ->
        val lastRun = schedule.lastRunAtOrBefore(reference)
        val nextRun = schedule.nextRunAfter(reference)

        assertThat(lastRun, "last run of $schedule at $reference").isLessThanOrEqualTo(reference)
        assertThat(nextRun, "next run of $schedule at $reference").isGreaterThan(reference)
        // No slot between the two.
        assertThat(schedule.nextRunAfter(lastRun), "slot after the last run of $schedule at $reference").isEqualTo(nextRun)
      }
    }
  }

  @Test
  fun `the description names the schedule for a log line`() {
    assertThat(quarterPast.description).isEqualTo("hourly at :15 Europe/Berlin")
    assertThat(threeInTheMorning.description).isEqualTo("daily at 03:00 Europe/Berlin")
    assertThat(thursdayEvening.description).isEqualTo("weekly on THURSDAY at 22:00 Europe/Berlin")
    assertThat(weekdayMorning.description).isEqualTo("weekdays at 07:30 Europe/Berlin")
  }
}
