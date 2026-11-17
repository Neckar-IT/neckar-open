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

class LocalDateUnboundTest {
  @Test
  fun `should correctly compare LocalDateUnbound instances`() {
    assertThat(LocalDate(2023, 4, 15)).isEqualTo(LocalDate(2023, 4, 15))
    assertThat(LocalDate(2023, 4, 15)).isGreaterThanOrEqualTo(LocalDate(2023, 4, 15))
    assertThat(LocalDate(2023, 4, 15)).isLessThanOrEqualTo(LocalDate(2023, 4, 15))

    assertThat(LocalDate(2023, 4, 15)).isGreaterThan(LocalDate(2023, 4, 14))
    assertThat(LocalDate(2023, 4, 15)).isLessThan(LocalDate(2023, 4, 16))

    assertThat(LocalDate(2024, 4, 15)).isGreaterThan(LocalDate(2023, 4, 15))
    assertThat(LocalDate(2023, 5, 15)).isGreaterThan(LocalDate(2023, 4, 15))
  }

  @Test
  fun `should correctly compare special LocalDateUnbound instances`() {
    assertThat(LocalDate(10000, 1, 1)).isGreaterThan(LocalDate(9999, 12, 31))
    assertThat(LocalDate(-10000, 12, 31)).isLessThan(LocalDate(-9999, 1, 1))

    // Leap year comparisons
    assertThat(LocalDate(2024, 2, 29)).isGreaterThan(LocalDate(2023, 2, 28))
    assertThat(LocalDate(2023, 2, 28)).isLessThan(LocalDate(2024, 2, 29))

    // Comparing negative and positive years
    assertThat(LocalDate(-1, 1, 1)).isLessThan(LocalDate(1, 1, 1))
    assertThat(LocalDate(1, 1, 1)).isGreaterThan(LocalDate(-1, 1, 1))
  }

  @Test
  fun `should correctly add days in leap years`() {
    assertThat(LocalDate(2023, 4, 15).plusDays(2)).isEqualTo(LocalDate(2023, 4, 17))
    assertThat(LocalDate(2023, 4, 30).plusDays(2)).isEqualTo(LocalDate(2023, 5, 2))
    assertThat(LocalDate(2023, 12, 31).plusDays(2)).isEqualTo(LocalDate(2024, 1, 2))
    assertThat(LocalDate(2024, 2, 28).plusDays(2)).isEqualTo(LocalDate(2024, 3, 1))
    assertThat(LocalDate(2023, 1, 1).plusDays(366)).isEqualTo(LocalDate(2024, 1, 2))
    assertThat(LocalDate(2023, 4, 15).plusDays(0)).isEqualTo(LocalDate(2023, 4, 15))
    assertThat(LocalDate(2023, 4, 15).plusDays(-2)).isEqualTo(LocalDate(2023, 4, 13))
  }
}
