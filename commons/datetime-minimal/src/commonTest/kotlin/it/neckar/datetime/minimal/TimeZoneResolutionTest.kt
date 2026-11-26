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
import it.neckar.open.time.formatUtcForDebug
import kotlin.test.Test

class TimeZoneResolutionTest {
  private val provider: TimeZoneOffsetProvider = DefaultTimeZoneOffsetProvider()

  @Test
  fun localMeanTimeBefore1893() {
    // 1789-07-14T12:00:00Z
    assertThat(provider.offset(-5_694_926_400_000.0, TimeZone.Berlin)).isEqualTo(3_208_000.0)
  }

  @Test
  fun offsetsOfTodayIncludingHalfHours() {
    // 2026-01-15T12:00:00Z
    val winter = 1_768_478_400_000.0
    assertThat(provider.offset(winter, TimeZone.Berlin)).isEqualTo(3_600_000.0)
    assertThat(provider.offset(winter, TimeZone.NewYork)).isEqualTo(-18_000_000.0)
    assertThat(provider.offset(winter, TimeZone("Asia/Kolkata"))).isEqualTo(19_800_000.0)
    assertThat(provider.offset(winter, TimeZone("Australia/Lord_Howe"))).isEqualTo(39_600_000.0)
    assertThat(provider.offset(winter, TimeZone.UTC)).isEqualTo(0.0)
  }

  @Test
  fun gapMovesForwardByTheLengthOfTheGap() {
    val inGap = LocalDateTime.of(2026, 3, 29, 2, 30, 0)
    assertThat(inGap.toMillis(TimeZone.Berlin).formatUtcForDebug()).isEqualTo("2026-03-29T01:30:00.000Z")
    assertThat(LocalDateTime.fromMillisCurrentTimeZone(inGap.toMillis(TimeZone.Berlin), TimeZone.Berlin)).isEqualTo(LocalDateTime.of(2026, 3, 29, 3, 30, 0))
  }

  @Test
  fun overlapTakesTheEarlierOffset() {
    val inOverlap = LocalDateTime.of(2026, 10, 25, 2, 30, 0)
    assertThat(inOverlap.toMillis(TimeZone.Berlin).formatUtcForDebug()).isEqualTo("2026-10-25T00:30:00.000Z")
  }

  @Test
  fun newYorkTransitions() {
    assertThat(LocalDateTime.of(2026, 3, 8, 2, 30, 0).toMillis(TimeZone.NewYork).formatUtcForDebug()).isEqualTo("2026-03-08T07:30:00.000Z")
    assertThat(LocalDateTime.of(2026, 11, 1, 1, 30, 0).toMillis(TimeZone.NewYork).formatUtcForDebug()).isEqualTo("2026-11-01T05:30:00.000Z")
  }

  @Test
  fun startOfDayAndDateAtTheSameInstant() {
    val bastilleDay = LocalDate(1789, 7, 14)
    val startOfDay = bastilleDay.toMillisAtStartOfDay(TimeZone.Berlin)
    assertThat(startOfDay).isEqualTo(-5_694_972_808_000.0)
    assertThat(LocalDate.fromMillisCurrentTimeZone(startOfDay, TimeZone.Berlin)).isEqualTo(bastilleDay)
    assertThat(LocalDate.fromMillisCurrentTimeZone(startOfDay - 1, TimeZone.Berlin)).isEqualTo(LocalDate(1789, 7, 13))
  }

  @Test
  fun gmtOffsetNames() {
    assertThat(parseGmtOffset("GMT")).isEqualTo(0.0)
    assertThat(parseGmtOffset("GMT+01:00")).isEqualTo(3_600_000.0)
    assertThat(parseGmtOffset("GMT+00:53:28")).isEqualTo(3_208_000.0)
    assertThat(parseGmtOffset("GMT-03:30")).isEqualTo(-12_600_000.0)
    assertThat(parseGmtOffset("GMT−05:00")).isEqualTo(-18_000_000.0)
    assertFailure { parseGmtOffset("UTC+1") }.isInstanceOf<IllegalArgumentException>()
  }
}
