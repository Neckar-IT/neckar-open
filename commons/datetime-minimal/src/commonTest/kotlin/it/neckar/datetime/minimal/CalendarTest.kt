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
package it.neckar.datetime.minimal

import assertk.*
import assertk.assertions.*
import kotlin.math.nextDown
import kotlin.test.Test

class CalendarTest {
  @Test
  fun startOfDayFarFrom1970IsThatDay() {
    // 3,832,363,394,770 days times 86,400,000 has no exact Double; the nearest one lies 31 s before midnight.
    val farDays = listOf(3_832_363_394_770L, 4_533_507_637_485L, -5_035_599_066_900L, -24_106_000_000L, 0L, 20_412L)
    farDays.forEach { epochDay ->
      val date = LocalDate.ofEpochDay(epochDay)
      val startOfDay = date.toMillisAtStartOfDay(TimeZone.UTC)
      assertThat(LocalDate.fromMillis(startOfDay, offset = 0.0), "epoch day $epochDay").isEqualTo(date)
      assertThat(LocalDateTime.fromMillis(startOfDay, offset = 0.0), "epoch day $epochDay").isEqualTo(LocalDateTime(date, LocalTime.Midnight))
    }
  }

  @Test
  fun lastInstantBeforeTheEndOfTheRangeIsTheLastDay() {
    val lastInstant = LocalDate.MaxMillis.nextDown()
    assertThat(LocalDate.fromMillis(lastInstant, offset = 0.0)).isEqualTo(LocalDate.ofEpochDay(LocalDate.MaxEpochDay))
    assertThat(LocalDateTime.fromMillis(lastInstant, offset = 0.0).date).isEqualTo(LocalDate.ofEpochDay(LocalDate.MaxEpochDay))
  }

  @Test
  fun lastMillisecondOfADayStaysOnThatDay() {
    // In the year -150000 a Double steps by 1 ms: the last millisecond lies one step before midnight.
    listOf(LocalDate(1789, 7, 14), LocalDate(-150_000, 7, 13)).forEach { date ->
      val lastMillisecond = date.plusDays(1).toMillisAtStartOfDay(TimeZone.UTC) - 1.0
      assertThat(LocalDate.fromMillis(lastMillisecond, offset = 0.0)).isEqualTo(date)
      assertThat(LocalDateTime.fromMillis(lastMillisecond, offset = 0.0)).isEqualTo(LocalDateTime(date, LocalTime(23, 59, 59, 999)))
    }
  }

  @Test
  fun bastilleDayIsEpochDayMinus65914() {
    val bastilleDay = LocalDate(Year(1789), Month(7), DayOfMonth(14))
    assertThat(bastilleDay.toEpochDay()).isEqualTo(-65_914L)
    assertThat(LocalDate.ofEpochDay(-65_914L)).isEqualTo(bastilleDay)
  }

  @Test
  fun epochDayZeroIs1970() {
    assertThat(LocalDate(1970, 1, 1).toEpochDay()).isEqualTo(0L)
    assertThat(LocalDate.ofEpochDay(0L)).isEqualTo(LocalDate(1970, 1, 1))
    assertThat(LocalDate.ofEpochDay(-1L)).isEqualTo(LocalDate(1969, 12, 31))
  }

  @Test
  fun idesOfMarchAreYearMinus43() {
    val idesOfMarch = LocalDate(Year(-43), Month.March, DayOfMonth(15))
    assertThat(idesOfMarch.format()).isEqualTo("-0043-03-15")
    assertThat(LocalDate.parse("-0043-03-15")).isEqualTo(idesOfMarch)
    assertThat(LocalDate.ofEpochDay(idesOfMarch.toEpochDay())).isEqualTo(idesOfMarch)
  }

  @Test
  fun julianDatesConvertToGregorian() {
    assertThat(LocalDate.ofJulian(Year(1582), Month.October, DayOfMonth(4))).isEqualTo(LocalDate(1582, 10, 14))
    assertThat(LocalDate.ofJulian(Year(1752), Month.September, DayOfMonth(2))).isEqualTo(LocalDate(1752, 9, 13))
    assertThat(LocalDate.ofJulian(Year(1700), Month.February, DayOfMonth(29))).isEqualTo(LocalDate(1700, 3, 11))
    assertFailure { LocalDate.ofJulian(Year(1701), Month.February, DayOfMonth(29)) }.isInstanceOf<IllegalArgumentException>()
  }

  @Test
  fun gregorianDatesBeforeTheReformAreValid() {
    assertThat(LocalDate(1582, 10, 10).plusDays(1)).isEqualTo(LocalDate(1582, 10, 11))
  }

  @Test
  fun leapDays() {
    assertThat(LocalDate(2000, 2, 29).plusDays(1)).isEqualTo(LocalDate(2000, 3, 1))
    listOf(1700, 1800, 1900, 2100).forEach { year ->
      assertFailure { LocalDate(year, 2, 29) }.isInstanceOf<IllegalArgumentException>()
    }
  }

  @Test
  fun bigBangYear() {
    val bigBang = LocalDate(Year(-13_787_000_000L), Month.January, DayOfMonth.FirstDay)
    assertThat(LocalDate.ofEpochDay(bigBang.toEpochDay())).isEqualTo(bigBang)
    assertThat(bigBang.format()).isEqualTo("-13787000000-01-01")
    assertThat(LocalDate.parse("-13787000000-01-01")).isEqualTo(bigBang)
  }

  @Test
  fun plusDaysOneMillion() {
    assertThat(LocalDate(2000, 1, 1).plusDays(1_000_000L)).isEqualTo(LocalDate(4737, 11, 28))
    assertThat(LocalDate(4737, 11, 28).plusDays(-1_000_000L)).isEqualTo(LocalDate(2000, 1, 1))
  }

  @Test
  fun plusMonthsAndYearsCoerceTheDayOfMonth() {
    assertThat(LocalDate(2023, 1, 31).plusMonths(1L)).isEqualTo(LocalDate(2023, 2, 28))
    assertThat(LocalDate(2024, 2, 29).plusYears(-400L)).isEqualTo(LocalDate(1624, 2, 29))
  }

  @Test
  fun formatAndParseExpandedYears() {
    listOf(
      LocalDate(1789, 7, 14) to "1789-07-14",
      LocalDate(999, 1, 1) to "0999-01-01",
      LocalDate(0, 1, 1) to "0000-01-01",
      LocalDate(-1, 12, 31) to "-0001-12-31",
      LocalDate(9999, 12, 31) to "9999-12-31",
      LocalDate(10_000, 1, 1) to "+10000-01-01",
    ).forEach { (date, iso) ->
      assertThat(date.format()).isEqualTo(iso)
      assertThat(LocalDate.parse(iso)).isEqualTo(date)
    }
  }

  @Test
  fun parseRejectsInvalidInput() {
    assertFailure { LocalDate.parse("2023-7-1") }.isInstanceOf<IllegalArgumentException>()
    assertFailure { LocalDate.parse("2023-02-30") }.isInstanceOf<IllegalArgumentException>()
    assertFailure { LocalDate.parse("99999999999999999999-01-01") }.isInstanceOf<IllegalArgumentException>()
  }

  @Test
  fun fromMillisAppliesTheOffset() {
    // 1789-07-14T12:00:00Z
    val noon = -5_694_926_400_000.0
    assertThat(LocalDate.fromMillis(noon, offset = 0.0)).isEqualTo(LocalDate(1789, 7, 14))
    assertThat(LocalDate.fromMillis(noon, offset = 13 * TimeConstants.MillisPerHour)).isEqualTo(LocalDate(1789, 7, 15))
    assertThat(LocalDateTime.fromMillis(noon, offset = 3_208_000.0)).isEqualTo(LocalDateTime.of(1789, 7, 14, 12, 53, 28))
    assertThat(LocalDateTime.of(1789, 7, 14, 12, 53, 28).toMillis(offset = 3_208_000.0)).isEqualTo(noon)
  }

  @Test
  fun fromMillisAtTheBigBang() {
    val bigBangMillis = -4.35075697224e20
    val dateTime = LocalDateTime.fromMillis(bigBangMillis, offset = 0.0)
    assertThat(dateTime.date).isEqualTo(LocalDate(Year(-13_786_998_030L), Month.January, DayOfMonth.FirstDay))
    assertThat(dateTime.toMillis(offset = 0.0)).isEqualTo(bigBangMillis)
  }
}
