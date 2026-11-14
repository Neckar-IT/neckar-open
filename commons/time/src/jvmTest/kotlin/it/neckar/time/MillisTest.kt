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
package it.neckar.time

import assertk.*
import assertk.assertions.*
import org.junit.jupiter.api.Test

/**
 *
 */
class MillisTest {
  @Test
  fun `converts a whole hour into every unit`() {
    val oneHour = Millis(3_600_000.0)

    assertThat(oneHour.value).isEqualTo(3_600_000.0)
    assertThat(oneHour.millis).isEqualTo(3_600_000.0)
    assertThat(oneHour.seconds).isEqualTo(3_600.0)
    assertThat(oneHour.minutes).isEqualTo(60.0)
    assertThat(oneHour.hours).isEqualTo(1.0)
  }

  @Test
  fun `converts a duration that is no whole unit`() {
    val oneAndAHalfMinutes = Millis(90_000.0)

    assertThat(oneAndAHalfMinutes.millis).isEqualTo(90_000.0)
    assertThat(oneAndAHalfMinutes.seconds).isEqualTo(90.0)
    assertThat(oneAndAHalfMinutes.minutes).isEqualTo(1.5)
    assertThat(oneAndAHalfMinutes.hours).isCloseTo(0.025, 1E-12)
  }

  @Test
  fun `converts a fraction of a millisecond`() {
    val halfMillisecond = Millis(0.5)

    assertThat(halfMillisecond.millis).isEqualTo(0.5)
    assertThat(halfMillisecond.seconds).isEqualTo(0.0005)
    assertThat(halfMillisecond.minutes).isCloseTo(8.333333333333333E-6, 1E-12)
    assertThat(halfMillisecond.hours).isCloseTo(1.388888888888889E-7, 1E-13)
  }

  @Test
  fun `converts zero`() {
    val zero = Millis(0.0)

    assertThat(zero.millis).isEqualTo(0.0)
    assertThat(zero.seconds).isEqualTo(0.0)
    assertThat(zero.minutes).isEqualTo(0.0)
    assertThat(zero.hours).isEqualTo(0.0)
  }

  @Test
  fun `converts negative durations`() {
    val minusTwoHours = Millis(-7_200_000.0)

    assertThat(minusTwoHours.millis).isEqualTo(-7_200_000.0)
    assertThat(minusTwoHours.seconds).isEqualTo(-7_200.0)
    assertThat(minusTwoHours.minutes).isEqualTo(-120.0)
    assertThat(minusTwoHours.hours).isEqualTo(-2.0)
  }

  @Test
  fun `formats the raw millis value in toString`() {
    assertThat(Millis(1000.0).toString()).isEqualTo("1000.0 ms")
    assertThat(Millis(0.0).toString()).isEqualTo("0.0 ms")
    assertThat(Millis(0.5).toString()).isEqualTo("0.5 ms")
    assertThat(Millis(-7_200_000.0).toString()).isEqualTo("-7200000.0 ms")
    //Doubles of 1E7 and above are formatted in scientific notation
    assertThat(Millis(90_061_000.0).toString()).isEqualTo("9.0061E7 ms")
  }
}
