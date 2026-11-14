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
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.util.DefaultTimeZone
import kotlin.time.Instant

@DefaultTimeZone("Europe/Berlin")
class KotlinXDateTimeTest {
  @Test
  fun testIt() {
    val now: Instant = Instant.fromEpochMilliseconds(1693836023242)

    val timeZone = TimeZone.currentSystemDefault()
    assertThat(timeZone.id).isEqualTo("Europe/Berlin")

    val localDateTime = now.toLocalDateTime(timeZone)

    assertThat(localDateTime.toString()).isEqualTo("2023-09-04T16:00:23.242")
    val localDate = localDateTime.date
    assertThat(localDate.toString()).isEqualTo("2023-09-04")
    assertThat(localDateTime.time.toString()).isEqualTo("16:00:23.242")

    assertThat(localDate.dayOfMonth).isEqualTo(4)

    assertThat(localDate.withDayOfMonth(1).toString()).isEqualTo("2023-09-01")


    val datePeriod = DatePeriod(months = 1)
    assertThat(datePeriod.toString()).isEqualTo("P1M")
    assertThat(localDate.plus(datePeriod).toString()).isEqualTo("2023-10-04")
  }

  @Test
  fun test1MonthFebruary() {
    assertThat(LocalDate(2022, 2, 28).toString()).isEqualTo("2022-02-28")
    assertThat(LocalDate(2022, 2, 28).plus(1, DateTimeUnit.DAY).toString()).isEqualTo("2022-03-01")
    assertThat(LocalDate(2022, 2, 28).plus(DatePeriod(months = 1)).toString()).isEqualTo("2022-03-28")

    assertThat(LocalDate(2022, 1, 31).plus(DatePeriod(months = 1)).toString()).isEqualTo("2022-02-28")
    assertThat(LocalDate(2022, 1, 31).plus(DatePeriod(months = 2)).toString()).isEqualTo("2022-03-31")
    assertThat(LocalDate(2022, 1, 31).plus(DatePeriod(months = 3)).toString()).isEqualTo("2022-04-30")
  }
}

private fun LocalDate.withDayOfMonth(dayOfMonth: Int): LocalDate {
  return LocalDate(year, month, dayOfMonth)
}
