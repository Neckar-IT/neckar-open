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

class LocalDateJSTest {
  @Test
  fun testConversion() {
    if (TimeZoneJsTestUtils.isWrongTimezone()) return

    val localDate = LocalDate(2023, 7, 24)
    assertThat(localDate.format()).isEqualTo("2023-07-24")

    val jsDate = localDate.toJsDateCurrentTimeZone()
    assertThat(jsDate.toLocaleDateString("en-US")).isEqualTo("7/24/2023")
    assertThat(jsDate.toISOString()).isEqualTo("2023-07-23T22:00:00.000Z")
    assertThat(jsDate.toUTCString()).isEqualTo("Sun, 23 Jul 2023 22:00:00 GMT")

    val back = jsDate.toLocalDate()
    assertThat(back.format()).isEqualTo("2023-07-24")
  }

  @Test
  fun testPlus1() {
    if (TimeZoneJsTestUtils.isWrongTimezone()) return

    val localDate = LocalDate(2023, 4, 30)
    assertThat(localDate.toJsDateCurrentTimeZone().toISOString()).isEqualTo("2023-04-29T22:00:00.000Z")
    assertThat(localDate.toJsDateCurrentTimeZone().getTime().formatUtcForDebug()).isEqualTo("2023-04-29T22:00:00.000Z")

    assertThat(localDate.plusDays(1).format()).isEqualTo("2023-05-01")
    assertThat(localDate.plusDays(1).toJsDateCurrentTimeZone().toISOString()).isEqualTo("2023-04-30T22:00:00.000Z")

    assertThat(localDate.plusDays(2).format()).isEqualTo("2023-05-02")
    assertThat(localDate.plusDays(2).toJsDateCurrentTimeZone().toISOString()).isEqualTo("2023-05-01T22:00:00.000Z")
  }

  @Test
  fun testUnbound() {
    if (TimeZoneJsTestUtils.isWrongTimezone()) return


    assertThat(LocalDate(2023, 12, 35).toJsDateCurrentTimeZone().getTimezoneOffset()).isEqualTo(-60)

    assertThat(LocalDate(2023, 12, 35).toJsDateCurrentTimeZone().toISOString()).isEqualTo("2024-01-03T23:00:00.000Z")
    assertThat(LocalDate(2023, 12, -2).toJsDateCurrentTimeZone().toISOString()).isEqualTo("2023-11-27T23:00:00.000Z")

    assertThat(LocalDate(2023, 12, 35).toJsDateCurrentTimeZone().toUTCString()).isEqualTo("Wed, 03 Jan 2024 23:00:00 GMT")
  }

  @Test
  fun testAtStartOfDay() {
    if (TimeZoneJsTestUtils.isWrongTimezone()) return

    assertThat(LocalDate(2023,12,35).toMillisAtStartOfDay(TimeZone.Berlin).formatUtcForDebug()).isEqualTo("2024-01-03T23:00:00.000Z")

    if (false) {
      //TODO not supported at the moment for JS
      assertThat(LocalDate(2023,12,35).toMillisAtStartOfDay(TimeZone.NewYork).formatUtcForDebug()).isEqualTo("2024-01-04T05:00:00.000Z")
      assertThat(LocalDate(2023,12,35).toMillisAtStartOfDay(TimeZone.Tokyo).formatUtcForDebug()).isEqualTo("2024-01-03T15:00:00.000Z")
    }
  }
}
