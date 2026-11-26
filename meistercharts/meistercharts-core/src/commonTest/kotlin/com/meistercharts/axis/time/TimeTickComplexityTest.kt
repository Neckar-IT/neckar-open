/*
 * Copyright 2023 Neckar IT GmbH, Mössingen, Germany
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.meistercharts.axis.time

import assertk.*
import assertk.assertions.*
import com.meistercharts.time.TimeViewport
import it.neckar.datetime.minimal.DayOfMonth
import it.neckar.datetime.minimal.LocalDate
import it.neckar.datetime.minimal.LocalDateTime
import it.neckar.datetime.minimal.Month
import it.neckar.datetime.minimal.TimeConstants
import it.neckar.datetime.minimal.TimeZone
import it.neckar.datetime.minimal.Year
import it.neckar.open.i18n.I18nConfiguration
import it.neckar.open.unit.si.ms
import kotlin.math.nextDown
import kotlin.test.Test
import kotlin.time.Duration.Companion.seconds
import kotlin.time.measureTime

/**
 * Spans that a loop over days or years cannot finish within the asserted time, and the tick limit.
 */
class TimeTickComplexityTest {
  /** 2026-11-20T00:00:00Z */
  private val today: @ms Double = LocalDateTime.of(2026, 11, 20, 0, 0, 0).toMillis(offset = 0.0)

  /** 1789-07-14T14:00:00Z */
  private val bastilleDayAfternoon: @ms Double = LocalDateTime.of(1789, 7, 14, 14, 0, 0).toMillis(offset = 0.0)

  @Test
  fun monthsAndDaysAtTheEndOfTheSupportedRange() {
    @ms val end = TimeTickDistance.MaxSupportedMillis
    assertThat(DistanceMonths(6).calculateTicks(end - 400 * TimeConstants.MillisPerDay, end, TimeZone.Berlin).size).isGreaterThan(0)
    assertThat(DistanceDays(DistanceDays.TicksPerMonth.EveryDay).calculateTicks(end - 3 * TimeConstants.MillisPerDay, end, TimeZone.Berlin).size).isGreaterThan(0)
  }

  @Test
  fun offsetLabelOfCalendarNumberingIsTheYearOfItsTick() {
    val distance = DistanceYears(10_000, YearNumbering.Calendar)
    // The ticks of 10,000 years lie at 1950, -8050, ...; the year 2026 belongs to the tick 1950.
    assertThat(distance.formatAsOffset(today, I18nConfiguration.GermanyUTC)).isEqualTo("1950")
    assertThat(DistanceYears(10_000).formatAsOffset(today, I18nConfiguration.GermanyUTC)).isEqualTo("0")
    assertThat(DistanceYears(DistanceYears.MaxDistanceInYears).formatAsOffset(TimeTickDistance.MinSupportedMillis, I18nConfiguration.GermanyUTC)).isEqualTo("-15000000000")
  }

  @Test
  fun neighbourIndicesAlternateAcrossYearZeroAnd1970() {
    @ms val startOfYearZero = LocalDateTime.of(0, 1, 1, 0, 0, 0).toMillis(offset = 0.0)
    val lastYearBeforeZero = DistanceYears(10).calculateEstimatedIndex(startOfYearZero - TimeConstants.MillisPerDay, TimeZone.UTC)
    val yearZero = DistanceYears(10).calculateEstimatedIndex(startOfYearZero, TimeZone.UTC)
    assertThat((yearZero.value - lastYearBeforeZero.value).mod(2)).isEqualTo(1)

    val lastHourBefore1970 = DistanceHours(1).calculateEstimatedIndex(-1.0, TimeZone.UTC)
    val firstHourOf1970 = DistanceHours(1).calculateEstimatedIndex(0.0, TimeZone.UTC)
    assertThat((firstHourOf1970.value - lastHourBefore1970.value).mod(2)).isEqualTo(1)
  }

  @Test
  fun startOneStepOfDoubleBeforeMidnight() {
    // 66 million years before 1970 a Double steps by 256 ms; the start belongs to the day before that midnight.
    @ms val start = (-24_106_000_000L * TimeConstants.MillisPerDay).nextDown()
    assertThat(DistanceDays(DistanceDays.TicksPerMonth.EveryDay).calculateTicks(start, start + 3 * TimeConstants.MillisPerDay, TimeZone.UTC).size).isGreaterThan(0)
    assertThat(DistanceHours(1).calculateTicks(start, start + 3 * TimeConstants.MillisPerHour, TimeZone.UTC).size).isGreaterThan(0)
  }

  @Test
  fun neighbourTicksOfCalendarNumberingHaveNeighbourIndices() {
    listOf(DistanceYears(1_000, YearNumbering.Calendar), DistanceYears(1_000_000, YearNumbering.Calendar)).forEach { distance ->
      @ms val start = today - 4.5 * distance.distanceInYears * TimeConstants.MillisPerYear
      val indices = distance.calculateTicks(start, today, TimeZone.UTC).toDoubleArray().map { distance.calculateEstimatedIndex(it, TimeZone.UTC).value }
      assertThat(indices.zipWithNext { index, next -> next - index }.distinct(), distance.toString()).containsExactly(1)
    }
  }

  @Test
  fun billionYearsFromTheBigBang() {
    assertFasterThanALoop {
      val ticks = DistanceYears(1_000_000_000).calculateTicks(TimeViewport.BigBangMillis, today, TimeZone.Berlin)
      assertThat(ticks.size).isLessThanOrEqualTo(16)
    }
  }

  @Test
  fun hundredMillisWithinOneSecond() {
    assertFasterThanALoop {
      val ticks = DistanceMillis(100.0).calculateTicks(bastilleDayAfternoon, bastilleDayAfternoon + 1_000.0, TimeZone.UTC)
      assertThat(ticks.size).isLessThanOrEqualTo(11)
    }
  }

  @Test
  fun millisecondStepsAtTheBigBangEndBelowTheResolutionOfDouble() {
    // Double resolves 65,536 ms at the Big Bang; steps below drop out as equal neighbors.
    assertFasterThanALoop {
      val ticks = DistanceMillis(100.0).calculateTicks(TimeViewport.BigBangMillis, TimeViewport.BigBangMillis + 200_000.0, TimeZone.UTC)
      assertThat(ticks.size).isLessThanOrEqualTo(5)
    }
  }

  @Test
  fun everyDistanceAtTheBigBang() {
    assertFasterThanALoop {
      listOf(DistanceMonths(1), DistanceDays(DistanceDays.TicksPerMonth.EveryDay), DistanceHours(1)).forEach { distance ->
        val ticks = distance.calculateTicks(TimeViewport.BigBangMillis, TimeViewport.BigBangMillis + 5 * 86_400_000.0, TimeZone.Berlin)
        assertThat(ticks.size).isGreaterThan(0)
      }
    }
  }

  @Test
  fun supportedRange() {
    val atStart = DistanceYears(DistanceYears.MaxDistanceInYears).withYearNumbering(YearNumbering.Calendar)
      .calculateTicks(TimeTickDistance.MinSupportedMillis, TimeTickDistance.MinSupportedMillis + 1e19, TimeZone.Berlin)
    assertThat(atStart.size).isGreaterThan(0)
    val atEnd = DistanceYears(DistanceYears.MaxDistanceInYears).calculateTicks(TimeTickDistance.MaxSupportedMillis - 1e19, TimeTickDistance.MaxSupportedMillis, TimeZone.Berlin)
    assertThat(atEnd.size).isGreaterThan(0)
    assertFailure { DistanceYears(1_000).calculateTicks(TimeTickDistance.MinSupportedMillis - 1e15, TimeTickDistance.MinSupportedMillis, TimeZone.UTC) }.isInstanceOf<IllegalArgumentException>()
    assertFailure { DistanceYears(1_000).calculateTicks(TimeTickDistance.MaxSupportedMillis, TimeTickDistance.MaxSupportedMillis + 1e15, TimeZone.UTC) }.isInstanceOf<IllegalArgumentException>()
    assertThat(TimeTickDistance.forTicks(TimeViewport.MaxSpan)).isInstanceOf<DistanceYears>().transform { it.distanceInYears }.isEqualTo(DistanceYears.MaxDistanceInYears)
  }

  @Test
  fun ticksBeforeYearMinStartAtYearMinAndContinueOnTheGrid() {
    val ticks = DistanceYears(DistanceYears.MaxDistanceInYears, YearNumbering.Calendar)
      .calculateTicks(TimeTickDistance.MinSupportedMillis, LocalDate(Year(-9_000_000_000L), Month.January, DayOfMonth.FirstDay).toEpochDay() * 86_400_000.0, TimeZone.UTC)
    val years = ticks.toDoubleArray().map { LocalDate.fromMillis(it, offset = 0.0).year.value }
    assertThat(years).containsExactly(Year.MinValue, 1_950L - 10_000_000_000L)
  }

  @Test
  fun moreThanMaxTicksThrows() {
    assertFailure { DistanceMillis(1.0).calculateTicks(today, today + 86_400_000.0, TimeZone.UTC) }.isInstanceOf<IllegalArgumentException>()
    assertFailure { DistanceYears(1).calculateTicks(TimeViewport.BigBangMillis, today, TimeZone.UTC) }.isInstanceOf<IllegalArgumentException>()
    assertThat(DistanceMillis(1.0).calculateTicks(today, today + TimeTickDistance.MaxTicks - 1.0, TimeZone.UTC).size).isLessThanOrEqualTo(TimeTickDistance.MaxTicks + 1)
  }

  @Test
  fun forTicksNeverExceedsMaxTicks() {
    listOf(TimeViewport.BigBangMillis, -2.0e18, 0.0, today).forEach { start ->
      var span = 1_000.0
      while (span < 4e20) {
        if (start + span == start || start + span > TimeTickDistance.MaxSupportedMillis) {
          span *= 7
          continue
        }
        val distance = TimeTickDistance.forTicks(span / 15)
        assertThat(distance.calculateTicks(start, start + span, TimeZone.UTC).size).isLessThanOrEqualTo(TimeTickDistance.MaxTicks)
        span *= 7
      }
    }
  }

  private fun assertFasterThanALoop(block: () -> Unit) {
    assertThat(measureTime(block)).isLessThan(5.seconds)
  }
}
