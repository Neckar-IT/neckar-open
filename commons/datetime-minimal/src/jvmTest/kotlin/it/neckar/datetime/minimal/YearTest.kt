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

class YearTest {

  @Test
  fun `very old non leap years - before introduction - we ignore the introducation and accept these as leap years!`() {
    assertThat(Year(1580).isLeapYear()).isTrue()
    assertThat(Year(1576).isLeapYear()).isTrue()
    assertThat(Year(1200).isLeapYear()).isTrue()
  }

  @Test
  fun `very old non leap years`() {
    assertThat(Year(1583).isLeapYear()).isFalse()
    assertThat(Year(1587).isLeapYear()).isFalse()
    assertThat(Year(1601).isLeapYear()).isFalse()
  }

  @Test
  fun `should return true for leap years`() {
    assertThat(Year(2000).isLeapYear()).isTrue()
    assertThat(Year(2004).isLeapYear()).isTrue()
    assertThat(Year(2400).isLeapYear()).isTrue()
  }

  @Test
  fun `should return false for non-leap years`() {
    assertThat(Year(1900).isLeapYear()).isFalse()
    assertThat(Year(2001).isLeapYear()).isFalse()
    assertThat(Year(2100).isLeapYear()).isFalse()
  }

  @Test
  fun `should return false for negative non-leap years`() {
    assertThat(Year(-1).isLeapYear()).isFalse()
    assertThat(Year(-100).isLeapYear()).isFalse()
    assertThat(Year(-500).isLeapYear()).isFalse()
  }

  @Test
  fun `should return true for negative leap years`() {
    assertThat(Year(-4).isLeapYear()).isTrue()
    assertThat(Year(-400).isLeapYear()).isTrue()
  }

  @Test
  fun `should handle very large years`() {
    assertThat(Year(Int.MAX_VALUE).isLeapYear()).isFalse()
    assertThat(Year(Int.MAX_VALUE - 1).isLeapYear()).isFalse()
    assertThat(Year(Int.MAX_VALUE - 2).isLeapYear()).isFalse()
    assertThat(Year(Int.MAX_VALUE - 3).isLeapYear()).isTrue()
    assertThat(Year(Int.MAX_VALUE - 4).isLeapYear()).isFalse()
  }

  @Test
  fun `should handle very small years`() {
    assertThat(Year(Int.MIN_VALUE).isLeapYear()).isTrue()
    assertThat(Year(Int.MIN_VALUE + 1).isLeapYear()).isFalse()
    assertThat(Year(Int.MIN_VALUE + 2).isLeapYear()).isFalse()
    assertThat(Year(Int.MIN_VALUE + 3).isLeapYear()).isFalse()
    assertThat(Year(Int.MIN_VALUE + 4).isLeapYear()).isTrue()
  }
}
