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
import kotlin.time.Duration.Companion.seconds
import kotlin.time.measureTime

/**
 * Spans that a loop over days, months or years cannot finish within the asserted time.
 */
class CalendarComplexityTest {
  private val epoch: LocalDate = LocalDate(1970, 1, 1)
  private val bigBangNewYear: LocalDate = LocalDate(Year(-13_787_000_000L), Month.January, DayOfMonth.FirstDay)

  @Test
  fun plusDaysToTheBigBang() {
    assertFasterThanALoop {
      assertThat(epoch.plusDays(-5_035_599_066_900L)).isEqualTo(LocalDate(Year(-13_787_000_000L), Month(5), DayOfMonth(8)))
    }
  }

  @Test
  fun epochDayRoundTripAtTheBigBang() {
    assertFasterThanALoop {
      assertThat(LocalDate.ofEpochDay(-5_035_599_066_900L).toEpochDay()).isEqualTo(-5_035_599_066_900L)
    }
  }

  @Test
  fun plusMonthsAndPlusYearsToTheBigBang() {
    assertFasterThanALoop {
      assertThat(epoch.plusMonths(-165_444_023_640L)).isEqualTo(bigBangNewYear)
      assertThat(epoch.plusYears(-13_787_001_970L)).isEqualTo(bigBangNewYear)
    }
  }

  @Test
  fun julianDateAtTheBigBang() {
    assertFasterThanALoop {
      // The Julian calendar lies 103,402,502 days behind the Gregorian one in this year.
      assertThat(LocalDate.ofJulian(Year(-13_787_000_000L), Month.March, DayOfMonth.FirstDay).toEpochDay()).isEqualTo(-5_035_702_469_470L)
    }
  }

  @Test
  fun oneMillionEpochDaysAcrossTheWholeSpan() {
    val span = LocalDate.MaxEpochDay - LocalDate.MinEpochDay
    assertFasterThanALoop {
      var differingDays = 0
      for (index in 0 until 1_000_000) {
        val epochDay = LocalDate.MinEpochDay + span / 1_000_000 * index
        if (LocalDate.ofEpochDay(epochDay).toEpochDay() != epochDay) {
          differingDays++
        }
      }
      assertThat(differingDays).isEqualTo(0)
    }
  }

  private fun assertFasterThanALoop(block: () -> Unit) {
    val duration = measureTime(block)
    assertThat(duration).isLessThan(5.seconds)
  }
}
