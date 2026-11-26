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
import it.neckar.open.unit.si.ms
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.util.DefaultTimeZone

class LocalDateTest {
  val time: @ms Double = 1.546855919123123E12

  @Test
  fun testFormatParse() {
    assertThat(LocalDate.parse("2023-07-21")).isEqualTo(LocalDate(2023, 7, 21))
  }

  @Test
  fun testSimple() {
    val localDate = LocalDate(2023, 7, 21)
    assertThat(localDate.toString()).isEqualTo("2023-07-21")
    assertThat(localDate.atStartOfMonth().toString()).isEqualTo("2023-07-01")
  }

  @Test
  fun testAddSubtract() {
    val localDate = LocalDate(2023, 7, 21)
    assertThat(localDate.toString()).isEqualTo("2023-07-21")
    assertThat(localDate.plusMonths(1).toString()).isEqualTo("2023-08-21")
    assertThat(localDate.plusMonths(2).toString()).isEqualTo("2023-09-21")
    assertThat(localDate.plusMonths(5).toString()).isEqualTo("2023-12-21")
    assertThat(localDate.plusMonths(7).toString()).isEqualTo("2024-02-21")
    assertThat(localDate.plusMonths(12).toString()).isEqualTo("2024-07-21")
    assertThat(localDate.plusMonths(36).toString()).isEqualTo("2026-07-21")
  }

  @DefaultTimeZone("Europe/Berlin")
  @Test
  fun testFrom() {
    assertThat(time.formatUtcForDebug()).isEqualTo("2019-01-07T10:11:59.123Z")
    val localDate = LocalDate.fromMillisCurrentTimeZone(time, TimeZone.Berlin)
    assertThat(localDate.format()).isEqualTo("2019-01-07")
  }

  @Test
  fun testInvalidDayOfMonth() {
    assertFailure { LocalDate(2023, 12, 35) }.isInstanceOf<IllegalArgumentException>()
    assertFailure { LocalDate(2023, 12, 0) }.isInstanceOf<IllegalArgumentException>()
  }

  @Test
  fun testAtStartOfDay() {
    assertThat(LocalDate(2024, 1, 4).toMillisAtStartOfDay(TimeZone.Berlin).formatUtcForDebug()).isEqualTo("2024-01-03T23:00:00.000Z")
    assertThat(LocalDate(2024, 1, 4).toMillisAtStartOfDay(TimeZone.NewYork).formatUtcForDebug()).isEqualTo("2024-01-04T05:00:00.000Z")
    assertThat(LocalDate(2024, 1, 4).toMillisAtStartOfDay(TimeZone.Tokyo).formatUtcForDebug()).isEqualTo("2024-01-03T15:00:00.000Z")
  }

  @Test
  fun `plusMonths handles negative crossings across a year boundary`() {
    assertThat(LocalDate(2023, 1, 15).plusMonths(-1)).isEqualTo(LocalDate(2022, 12, 15))
    assertThat(LocalDate(2023, 3, 10).plusMonths(-3)).isEqualTo(LocalDate(2022, 12, 10))
    assertThat(LocalDate(2023, 2, 10).plusMonths(-14)).isEqualTo(LocalDate(2021, 12, 10))
  }

  @Test
  fun `plusMonths still handles positive crossings and no-op`() {
    assertThat(LocalDate(2023, 11, 15).plusMonths(2)).isEqualTo(LocalDate(2024, 1, 15))
    assertThat(LocalDate(2023, 6, 15).plusMonths(0)).isEqualTo(LocalDate(2023, 6, 15))
  }
}
