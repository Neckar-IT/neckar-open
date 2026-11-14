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
package it.neckar.open.kotlin.lang

import assertk.*
import assertk.assertions.*
import org.junit.jupiter.api.Test
import kotlin.math.roundToInt

class MathExtKtTest {
  @Test
  fun testRoundingDecimals() {
    assertThat((119.5 * 1.19).roundDecimalPlacesHalfUp(2)).isEqualTo(142.21)
  }

  @Test
  fun testRounding() {
    assertThat((1195 * 11.9).roundHalfUp()).isEqualTo(14221.0)
    assertThat((119.5 * 1.19 * 100).roundHalfUp()).isEqualTo(14221.0)
    assertThat((119.5 * 1.19 * 100).roundHalfUp() / 100.0).isEqualTo(142.21)
  }

  @Test
  fun testRoundToHalf() {
    assertThat(119.50.roundHalfUp()).isEqualTo(120.0)
    assertThat(119.50.round()).isEqualTo(120.0)

    assertThat(2270.5.round()).isEqualTo(2270.0) //rounds to even integer
    assertThat(2270.5.roundHalfUp()).isEqualTo(2271.0)

    assertThat(2271.49.roundHalfUp()).isEqualTo(2271.0)
    assertThat(2271.5.roundHalfUp()).isEqualTo(2272.0)
    assertThat(2272.5.roundHalfUp()).isEqualTo(2273.0)

    assertThat(2270.5.roundHalfUp()).isEqualTo(2271.0)
    assertThat(2271.5.roundHalfUp()).isEqualTo(2272.0)
  }

  @Test
  fun testRoundDecimals() {
    assertThat(119.50.roundDecimalPlaces(2)).isEqualTo(119.50)
    assertThat(119.5000000000002.roundDecimalPlaces(2)).isEqualTo(119.50)

    assertThat((2270.5).round()).isEqualTo(2270.0) //round to nearest even integer
    assertThat((2270.5).roundHalfUp()).isEqualTo(2271.0)

    assertThat((22.705).roundDecimalPlaces(2)).isEqualTo(22.70)
    assertThat((22.705000000000002).roundDecimalPlaces(2)).isEqualTo(22.70)

    assertThat((22.705).roundDecimalPlacesHalfUp(2)).isEqualTo(22.71)
    assertThat((22.705000000000002).roundDecimalPlacesHalfUp(2)).isEqualTo(22.71)
  }

  @Test
  fun testIt() {
    assertThat((2270.5).roundToInt()).isEqualTo(2271)
  }

  @Test
  fun `should round down to base correctly when value is already a multiple of base`() {
    assertThat(10.0.roundDownToBase(5.0)).isEqualTo(10.0)
  }

  @Test
  fun `should round down to base correctly when value is not a multiple of base`() {
    assertThat(12.3.roundDownToBase(5.0)).isEqualTo(10.0)
  }

  @Test
  fun `should round down to zero when base is greater than value`() {
    assertThat(4.9.roundDownToBase(5.0)).isEqualTo(0.0)
  }

  @Test
  fun `should handle negative values correctly`() {
    assertThat((-12.3).roundDownToBase(5.0)).isEqualTo(-15.0)
  }

  @Test
  fun `should return zero when value is zero`() {
    assertThat(0.0.roundDownToBase(5.0)).isEqualTo(0.0)
  }

  @Test
  fun `negative values`() {
    assertThat((-100.0).roundDownToBase(5.0)).isEqualTo(-100.0)
    assertThat((-99.0).roundDownToBase(5.0)).isEqualTo(-100.0)
    assertThat((-94.0).roundDownToBase(5.0)).isEqualTo(-95.0)
    assertThat((-12.3).roundDownToBase(5.0)).isEqualTo(-15.0)
  }
}
