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
import kotlin.js.Date
import kotlin.test.Test

class LocalTimeJsTest {
  val time: @ms Double = 1546875919_123.987_345_678_901

  @Test
  fun testIt() {
    if (TimeZoneJsTestUtils.isWrongTimezone()) return

    assertThat(time.formatUtcForDebug()).isEqualTo("2019-01-07T15:45:19.123Z")

    val date = Date(time)
    assertThat(date.getMilliseconds()).isEqualTo(123)

    assertThat(date.toISOString()).isEqualTo("2019-01-07T15:45:19.123Z")
    assertThat(date.toUTCString()).isEqualTo("Mon, 07 Jan 2019 15:45:19 GMT")

    val localTime: LocalTime = LocalTime.fromMillisCurrentTimeZone(time, TimeZone.Berlin)
    assertThat(localTime.hour).isEqualTo(16)
    assertThat(localTime.minute).isEqualTo(45)
    assertThat(localTime.millis).isEqualTo(123) //not exact, double is not good enough
    assertThat(localTime.format()).isEqualTo("16:45:19.123")
  }
}
