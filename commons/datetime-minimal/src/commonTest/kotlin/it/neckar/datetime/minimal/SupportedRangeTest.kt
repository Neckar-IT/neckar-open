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
import kotlin.test.Test

class SupportedRangeTest {
  @Test
  fun year() {
    assertThat(Year(Year.MinValue).value).isEqualTo(-15_000_000_000L)
    assertThat(Year(Year.MaxValue).value).isEqualTo(15_000_000_000L)
    assertFailure { Year(Year.MinValue - 1) }.isInstanceOf<IllegalArgumentException>()
    assertFailure { Year(20_000_000_000L) }.isInstanceOf<IllegalArgumentException>().hasMessage("Year 20000000000 outside -15000000000..15000000000")
  }

  @Test
  fun epochDay() {
    assertThat(LocalDate.ofEpochDay(LocalDate.MinEpochDay)).isEqualTo(LocalDate(Year.Min, Month.January, DayOfMonth.FirstDay))
    assertThat(LocalDate.ofEpochDay(LocalDate.MaxEpochDay)).isEqualTo(LocalDate(Year.Max, Month.December, DayOfMonth(31)))
    assertFailure { LocalDate.ofEpochDay(LocalDate.MinEpochDay - 1) }.isInstanceOf<IllegalArgumentException>()
    assertFailure { LocalDate.ofEpochDay(LocalDate.MaxEpochDay + 1) }.isInstanceOf<IllegalArgumentException>()
  }

  @Test
  fun plusDays() {
    val epoch = LocalDate(1970, 1, 1)
    assertThat(epoch.plusDays(LocalDate.MinEpochDay).toEpochDay()).isEqualTo(LocalDate.MinEpochDay)
    assertThat(epoch.plusDays(LocalDate.MaxEpochDay).toEpochDay()).isEqualTo(LocalDate.MaxEpochDay)
    assertFailure { epoch.plusDays(LocalDate.MaxEpochDay + 1) }.isInstanceOf<IllegalArgumentException>()
    assertFailure { epoch.plusDays(Long.MIN_VALUE) }.isInstanceOf<IllegalArgumentException>()
  }

  @Test
  fun plusMonths() {
    val start = LocalDate(Year.Max, Month.November, DayOfMonth.FirstDay)
    assertThat(start.plusMonths(1L)).isEqualTo(LocalDate(Year.Max, Month.December, DayOfMonth.FirstDay))
    assertFailure { start.plusMonths(2L) }.isInstanceOf<IllegalArgumentException>()
    assertFailure { start.plusMonths(Long.MIN_VALUE) }.isInstanceOf<IllegalArgumentException>()
    assertThat(LocalDate(Year.Min, Month.February, DayOfMonth.FirstDay).plusMonths(-1L)).isEqualTo(LocalDate(Year.Min, Month.January, DayOfMonth.FirstDay))
    assertFailure { LocalDate(Year.Min, Month.January, DayOfMonth.FirstDay).plusMonths(-1L) }.isInstanceOf<IllegalArgumentException>()
  }

  @Test
  fun plusYears() {
    val start = LocalDate(1970, 1, 1)
    assertThat(start.plusYears(Year.MaxValue - 1970).year).isEqualTo(Year.Max)
    assertThat(start.plusYears(Year.MinValue - 1970).year).isEqualTo(Year.Min)
    assertFailure { start.plusYears(Year.MaxValue - 1969) }.isInstanceOf<IllegalArgumentException>()
    assertFailure { start.plusYears(Long.MAX_VALUE) }.isInstanceOf<IllegalArgumentException>()
  }

  @Test
  fun ofJulian() {
    // The julian calendar drifts by 3 days in 400 years: julian Year.Max is about gregorian Year.Max + 308,000.
    assertThat(LocalDate.ofJulian(Year(Year.MaxValue - 400_000), Month.January, DayOfMonth.FirstDay).year.value).isBetween(Year.MaxValue - 100_000, Year.MaxValue)
    assertFailure { LocalDate.ofJulian(Year.Max, Month.January, DayOfMonth.FirstDay) }.isInstanceOf<IllegalArgumentException>()
    assertThat(LocalDate.ofJulian(Year(Year.MinValue + 400_000), Month.January, DayOfMonth.FirstDay).year.value).isBetween(Year.MinValue, Year.MinValue + 100_000)
    assertFailure { LocalDate.ofJulian(Year.Min, Month.January, DayOfMonth.FirstDay) }.isInstanceOf<IllegalArgumentException>()
  }

  @Test
  fun fromMillisAndToMillis() {
    assertThat(LocalDate.fromMillis(LocalDate.MinMillis, offset = 0.0)).isEqualTo(LocalDate(Year.Min, Month.January, DayOfMonth.FirstDay))
    assertThat(LocalDateTime.fromMillis(LocalDate.MinMillis, offset = 0.0).toMillis(offset = 0.0)).isEqualTo(LocalDate.MinMillis)
    assertThat(LocalDate.fromMillis(LocalDate.MaxMillis - TimeConstants.MillisPerDay, offset = 0.0)).isEqualTo(LocalDate(Year.Max, Month.December, DayOfMonth(31)))
    assertFailure { LocalDate.fromMillis(LocalDate.MaxMillis, offset = 0.0) }.isInstanceOf<IllegalArgumentException>()
    assertFailure { LocalDateTime.fromMillis(LocalDate.MinMillis - 1e9, offset = 0.0) }.isInstanceOf<IllegalArgumentException>()
    assertFailure { LocalDate.fromMillis(Double.NaN, offset = 0.0) }.isInstanceOf<IllegalArgumentException>()
    // The first day of Year.Min starts before LocalDate.MinMillis in a zone east of UTC.
    assertThat(LocalDate.fromMillis(LocalDate.MinMillis - 3_208_000.0, offset = 3_208_000.0)).isEqualTo(LocalDate(Year.Min, Month.January, DayOfMonth.FirstDay))
    assertFailure { LocalDate.fromMillis(LocalDate.MinMillis, offset = -1e6) }.isInstanceOf<IllegalArgumentException>()
  }

  @Test
  fun formatAndParse() {
    val min = LocalDate(Year.Min, Month.January, DayOfMonth.FirstDay)
    val max = LocalDate(Year.Max, Month.December, DayOfMonth(31))
    assertThat(LocalDate.parse(min.format())).isEqualTo(min)
    assertThat(LocalDate.parse(max.format())).isEqualTo(max)
    assertFailure { LocalDate.parse("+15000000001-01-01") }.isInstanceOf<IllegalArgumentException>()
    assertFailure { LocalDate.parse("-15000000001-01-01") }.isInstanceOf<IllegalArgumentException>()
  }

  @Test
  fun timeZoneOffsetOutsideThePlatformRangeIsTheOffsetAtTheBound() {
    val provider = DefaultTimeZoneOffsetProvider()
    val atBound = provider.offset(-DefaultTimeZoneOffsetProvider.MaxPlatformMillis, TimeZone.Berlin)
    assertThat(atBound).isEqualTo(3_208_000.0)
    assertThat(provider.offset(-4.35075697224e20, TimeZone.Berlin)).isEqualTo(atBound)
    assertThat(provider.offset(DefaultTimeZoneOffsetProvider.MaxPlatformMillis * 2, TimeZone.Berlin)).isEqualTo(provider.offset(DefaultTimeZoneOffsetProvider.MaxPlatformMillis, TimeZone.Berlin))
  }
}
