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

class LocalDateTimeTest {
  @DefaultTimeZone("Asia/Tokyo")
  @Test
  fun testConversion() {
    val time: @ms Double = 1546875919_123.987_345_678_901

    assertThat(time.formatUtcForDebug()).isEqualTo("2019-01-07T15:45:19.123Z")
    val localDateTime = LocalDateTime.fromMillisCurrentTimeZone(time, TimeZone.Tokyo)
    assertThat(localDateTime.millis).isEqualTo(123)
    assertThat(localDateTime.toMillis(TimeZone.Tokyo).formatUtcForDebug()).isEqualTo("2019-01-07T15:45:19.123Z")

    assertThat(localDateTime.format()).isEqualTo("2019-01-08T00:45:19.123")
    val atStartOfHour = localDateTime.atStartOfHour()
    assertThat(atStartOfHour.format()).isEqualTo("2019-01-08T00:00:00.000")
  }

  @DefaultTimeZone("Europe/Berlin")
  @Test
  fun testNormalConversion() {
    checkConvertAndBack(LocalDateTime(LocalDate(2023, 11, 15), LocalTime(14, 30, 0, 0)), TimeZone("Europe/Berlin"), "2023-11-15T13:30:00.000Z")
    checkConvertAndBack(LocalDateTime(LocalDate(2023, 5, 15), LocalTime(14, 30, 0, 0)), TimeZone("Europe/Berlin"), "2023-05-15T12:30:00.000Z")
  }

  @DefaultTimeZone("Europe/Berlin")
  @Test
  fun `check LocalDateTime during the hour that does not exist due to daylight saving time`() {
    //In the time when changing from daylight saving time to standard time, the hour from 2:00 to 3:00 does not exist
    checkConvertAndBack(LocalDateTime(LocalDate(2023, 3, 26), LocalTime(1, 59, 0, 0)), TimeZone("Europe/Berlin"), "2023-03-26T00:59:00.000Z")

    //Converting back will return 3:00:00
    assertFailure {
      checkConvertAndBack(LocalDateTime(LocalDate(2023, 3, 26), LocalTime(2, 0, 0, 0)), TimeZone("Europe/Berlin"), "2023-03-26T01:00:00.000Z")
    }
    assertFailure {
      checkConvertAndBack(LocalDateTime(LocalDate(2023, 3, 26), LocalTime(2, 30, 0, 0)), TimeZone("Europe/Berlin"), "2023-03-26T01:30:00.000Z")
    }
    assertFailure {
      checkConvertAndBack(LocalDateTime(LocalDate(2023, 3, 26), LocalTime(2, 59, 0, 0)), TimeZone("Europe/Berlin"), "2023-03-26T01:59:00.000Z")

    }
    checkConvertAndBack(LocalDateTime(LocalDate(2023, 3, 26), LocalTime(3, 0, 0, 0)), TimeZone("Europe/Berlin"), "2023-03-26T01:00:00.000Z")
  }

  @DefaultTimeZone("Europe/Berlin")
  @Test
  fun `check LocalDateTime during the hour that is duplicated - winter`() {
    //In the time when changing from daylight saving time to standard time, the hour from 2:00 to 3:00 does not exist
    checkConvertAndBack(LocalDateTime(LocalDate(2023, 10, 29), LocalTime(1, 59, 0, 0)), TimeZone("Europe/Berlin"), "2023-10-28T23:59:00.000Z")

    checkConvertAndBack(LocalDateTime(LocalDate(2023, 10, 29), LocalTime(0, 59, 0, 0)), TimeZone("Europe/Berlin"), "2023-10-28T22:59:00.000Z")
    checkConvertAndBack(LocalDateTime(LocalDate(2023, 10, 29), LocalTime(1, 0, 0, 0)), TimeZone("Europe/Berlin"), "2023-10-28T23:00:00.000Z")
    checkConvertAndBack(LocalDateTime(LocalDate(2023, 10, 29), LocalTime(1, 30, 0, 0)), TimeZone("Europe/Berlin"), "2023-10-28T23:30:00.000Z")
    checkConvertAndBack(LocalDateTime(LocalDate(2023, 10, 29), LocalTime(1, 59, 0, 0)), TimeZone("Europe/Berlin"), "2023-10-28T23:59:00.000Z")

    checkConvertAndBack(LocalDateTime(LocalDate(2023, 10, 29), LocalTime(2, 0, 0, 0)), TimeZone("Europe/Berlin"), "2023-10-29T00:00:00.000Z")
    checkConvertAndBack(LocalDateTime(LocalDate(2023, 10, 29), LocalTime(2, 30, 0, 0)), TimeZone("Europe/Berlin"), "2023-10-29T00:30:00.000Z")
    checkConvertAndBack(LocalDateTime(LocalDate(2023, 10, 29), LocalTime(2, 59, 0, 0)), TimeZone("Europe/Berlin"), "2023-10-29T00:59:00.000Z")

    checkConvertAndBack(LocalDateTime(LocalDate(2023, 10, 29), LocalTime(3, 0, 0, 0)), TimeZone("Europe/Berlin"), "2023-10-29T02:00:00.000Z")
  }

  private fun checkConvertAndBack(original: LocalDateTime, timeZone: TimeZone, expectedUtc: String) {
    @ms val millis = original.toMillis(timeZone)
    assertThat(millis.formatUtcForDebug()).isEqualTo(expectedUtc)

    val convertedBack = LocalDateTime.fromMillisCurrentTimeZone(millis, timeZone)
    assertThat(convertedBack).isEqualTo(original)
  }
}
