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
package it.neckar.open.time

import assertk.*
import assertk.assertions.*
import it.neckar.datetime.minimal.TimeConstants
import it.neckar.open.formatting.formatUtc
import org.junit.jupiter.api.Test
import java.time.ZoneOffset
import java.time.ZonedDateTime

class TimeConstantsTest {
  @Test
  fun testMinMaxDouble() {
    assertThat(Double.MIN_VALUE.formatUtc()).isEqualTo("1970-01-01T00:00:00.000Z")
    assertThat(Double.MAX_VALUE.formatUtc()).isEqualTo("+292278994-08-17T07:12:55.807Z")
    assertThat((-Double.MAX_VALUE).formatUtc()).isEqualTo("-292275055-05-16T16:47:04.192Z")
  }

  @Test
  fun testReasonableMinMax() {
    assertThat(Double.MIN_VALUE.formatUtc()).isEqualTo("1970-01-01T00:00:00.000Z")
    assertThat((Double.MAX_VALUE / 2.0).formatUtc()).isEqualTo("+292278994-08-17T07:12:55.807Z")
    assertThat(((-Double.MAX_VALUE / 2.0)).formatUtc()).isEqualTo("-292275055-05-16T16:47:04.192Z")

    val delta = (Double.MAX_VALUE / 2.0) - (-Double.MAX_VALUE / 2.0)
    assertThat(delta).isEqualTo(1.7976931348623157E308)

  }

  @Test
  fun testRefTime() {
    assertThat(TimeConstants.ReferenceTimestamp.formatUtc()).isEqualTo("2024-01-01T00:00:00.000Z")

    ZonedDateTime.of(2024, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC).let {
      val millis = it.toDoubleMillis()
      assertThat(millis.formatUtc()).isEqualTo("2024-01-01T00:00:00.000Z")
      assertThat(millis).isEqualTo(1.7040672E12)
    }
  }
}
