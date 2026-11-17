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
import org.junit.jupiter.api.Test

/**
 * Pins negative-value behavior of LocalDate / LocalTime arithmetic.
 *
 * Before the fix:
 * - LocalDate.plusDays(negative) produced a negative DayOfMonth → IllegalArgumentException.
 * - LocalDate.plusYears across a leap-year boundary on Feb 29 → IllegalArgumentException.
 * - LocalTime.plusMinutes/plusSeconds/plusHours with negative arguments → IllegalArgumentException
 *   because Kotlin's `%` returns a negative remainder for negative operands.
 */
class DateTimeArithmeticNegativeTest {
  @Test
  fun `LocalDate plusDays handles negative across month boundary`() {
    val start = LocalDate(Year(2026), Month(2), DayOfMonth(15))
    val result = start.plusDays(-20)
    // 20 days before Feb 15, 2026 is Jan 26, 2026.
    assertThat(result).isEqualTo(LocalDate(Year(2026), Month(1), DayOfMonth(26)))
  }

  @Test
  fun `LocalDate plusDays handles negative across year boundary`() {
    val start = LocalDate(Year(2026), Month(1), DayOfMonth(5))
    val result = start.plusDays(-10)
    // 10 days before Jan 5, 2026 is Dec 26, 2025.
    assertThat(result).isEqualTo(LocalDate(Year(2025), Month(12), DayOfMonth(26)))
  }

  @Test
  fun `LocalDate plusYears coerces Feb 29 to Feb 28 in non-leap year`() {
    val leap = LocalDate(Year(2024), Month(2), DayOfMonth(29))
    val result = leap.plusYears(1)
    assertThat(result).isEqualTo(LocalDate(Year(2025), Month(2), DayOfMonth(28)))
  }

  @Test
  fun `LocalTime plusMinutes handles negative, wrapping around midnight`() {
    val time = LocalTime(0, 30, 0)
    val result = time.plusMinutes(-60)
    // Half past midnight minus 60 minutes = 23:30 of the previous day; LocalTime has no date,
    // so the wrap is purely modulo: 23:30.
    assertThat(result).isEqualTo(LocalTime(23, 30, 0))
  }

  @Test
  fun `LocalTime plusSeconds handles negative across hour boundary`() {
    val time = LocalTime(0, 0, 30)
    val result = time.plusSeconds(-3600)
    // 0:00:30 - 1 hour = 23:00:30.
    assertThat(result).isEqualTo(LocalTime(23, 0, 30))
  }

  @Test
  fun `LocalTime plusHours handles negative wrap`() {
    val time = LocalTime(2, 0, 0)
    val result = time.plusHours(-5)
    // 02:00 - 5 hours = 21:00.
    assertThat(result).isEqualTo(LocalTime(21, 0, 0))
  }
}
