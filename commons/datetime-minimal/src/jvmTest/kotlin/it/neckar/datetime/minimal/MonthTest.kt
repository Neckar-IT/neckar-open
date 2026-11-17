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
import kotlin.test.assertFails

class MonthTest {


  @Test
  fun `should throw an exception for invalid months`() {
    assertFails { Month(0) }
    assertFails { Month(13) }
    assertFails { Month(-1) }
  }

  @Test
  fun `should return correct days for non-leap year`() {
    assertThat(Month(1).daysInMonth(Year(2021)).value).isEqualTo(31)
    assertThat(Month(2).daysInMonth(Year(2021)).value).isEqualTo(28)
    assertThat(Month(4).daysInMonth(Year(2021)).value).isEqualTo(30)
  }

  @Test
  fun `should return correct days for leap year`() {
    assertThat(Month(2).daysInMonth(Year(2020)).value).isEqualTo(29)
  }

  @Test
  fun `should return correct days for very old non-leap year`() {
    assertThat(Month(2).daysInMonth(Year(1601)).value).isEqualTo(28)
  }

  @Test
  fun `should return correct days for very old leap year`() {
    assertThat(Month(2).daysInMonth(Year(1600)).value).isEqualTo(29)
  }

  @Test
  fun `should return correct days for negative non-leap year`() {
    assertThat(Month(2).daysInMonth(Year(-1)).value).isEqualTo(28)
  }

  @Test
  fun `should return correct days for negative leap year`() {
    assertThat(Month.February.daysInMonth(Year(-4)).value).isEqualTo(29)
  }

  @Test
  fun `should return 0 days for January`() {
    assertThat(Month.January.daysBeforeThisMonth(Year(2023))).isEqualTo(0)
    assertThat(Month.January.daysBeforeThisMonth(Year(2024))).isEqualTo(0)
    assertThat(Month.January.daysBeforeThisMonth(Year(2025))).isEqualTo(0)
  }

  @Test
  fun `should return 31 days for February in non-leap year`() {
    assertThat(Month.February.daysBeforeThisMonth(Year(2023))).isEqualTo(31)
  }

  @Test
  fun `should return 59 days for March in non-leap year`() {
    assertThat(Month.March.daysBeforeThisMonth(Year(2023))).isEqualTo(59)
  }

  @Test
  fun `should return 60 days for March in leap year`() {
    assertThat(Month.March.daysBeforeThisMonth(Year(2024))).isEqualTo(60)
  }
}
