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
import kotlin.math.PI

class MathExtensionsKtTest {
  @Test
  fun `log is exact on a power of its base`() {
    //ln(1000)/ln(10) is 2.9999999999999996, so the truncating version answered 2.
    assertThat(log10(1000)).isEqualTo(3)
    assertThat(log10(1_000_000)).isEqualTo(6)
    assertThat(log(1000, 10)).isEqualTo(3)
    assertThat(log2(1024)).isEqualTo(10)

    //One below the power stays one exponent lower.
    assertThat(log10(999)).isEqualTo(2)
    assertThat(log2(1023)).isEqualTo(9)
    assertThat(log10(1)).isEqualTo(0)
  }

  @Test
  fun `aligning a negative value steps away from zero`() {
    //Integer division truncates towards zero, so (-5 / 4 + 1) * 4 stepped up to 0.
    assertThat((-5).nextAlignedTo(4)).isEqualTo(-4)
    assertThat((-5).prevAlignedTo(4)).isEqualTo(-8)
    assertThat((-5L).nextAlignedTo(4L)).isEqualTo(-4L)
    assertThat((-5L).prevAlignedTo(4L)).isEqualTo(-8L)
    assertThat((-5).nextMultipleOf(4)).isEqualTo(-4)
    assertThat((-5).prevMultipleOf(4)).isEqualTo(-8)

    //A positive value and a value already aligned are unchanged.
    assertThat(7.nextAlignedTo(4)).isEqualTo(8)
    assertThat(7.prevAlignedTo(4)).isEqualTo(4)
    assertThat((-8).nextAlignedTo(4)).isEqualTo(-8)
  }

  @Test
  fun `roundHalfUp rounds negative non-tie values to the nearest integer`() {
    //Bug: `number % 1` is negative for negatives, so the old code always truncated toward zero.
    assertThat((-2.6).roundHalfUp()).isEqualTo(-3.0)
    assertThat((-2.4).roundHalfUp()).isEqualTo(-2.0)
    assertThat((-0.6).roundHalfUp()).isEqualTo(-1.0)
    assertThat((-0.4).roundHalfUp()).isEqualTo(0.0)

    //Ties round toward +infinity (unchanged, matches the existing large-negative-value tests).
    assertThat((-2.5).roundHalfUp()).isEqualTo(-2.0)

    //Positive values keep rounding half up.
    assertThat(2.6.roundHalfUp()).isEqualTo(3.0)
    assertThat(2.4.roundHalfUp()).isEqualTo(2.0)
    assertThat(2.5.roundHalfUp()).isEqualTo(3.0)
  }

  @Test
  fun testRoundLargeValues() {
    assertThat(Integer.MAX_VALUE.toDouble().round()).isEqualTo(Integer.MAX_VALUE.toDouble())
    assertThat(Integer.MAX_VALUE.toDouble().roundHalfUp()).isEqualTo(Integer.MAX_VALUE.toDouble())
    assertThat(Integer.MAX_VALUE.toDouble()).isEqualTo(2_147_483_647.0)

    assertThat((Integer.MAX_VALUE.toDouble() + 0.5).roundHalfUp()).isEqualTo(2_147_483_648.0)

    assertThat((Integer.MAX_VALUE.toDouble() + 1.5).roundHalfUp()).isEqualTo(2_147_483_649.0)
    assertThat((Integer.MAX_VALUE.toDouble() + 100.5).roundHalfUp()).isEqualTo(2_147_483_748.0)
  }

  @Test
  fun testRoundLargeNegativeValues() {
    assertThat(Integer.MIN_VALUE.toDouble().round()).isEqualTo(Integer.MIN_VALUE.toDouble())
    assertThat(Integer.MIN_VALUE.toDouble().roundHalfUp()).isEqualTo(Integer.MIN_VALUE.toDouble())
    assertThat(Integer.MIN_VALUE.toDouble()).isEqualTo(-2_147_483_648.0)

    assertThat((Integer.MIN_VALUE.toDouble() - 0.5).roundHalfUp()).isEqualTo(-2_147_483_648.0)

    assertThat((Integer.MIN_VALUE.toDouble() - 1.5).roundHalfUp()).isEqualTo(-2_147_483_649.0)
    assertThat((Integer.MIN_VALUE.toDouble() - 100.5).roundHalfUp()).isEqualTo(-2_147_483_748.0)
  }

  @Test
  fun testFindMagnitude() {
    assertThat(1.0.findMagnitude()).isEqualTo(0)
    assertThat(10.0.findMagnitude()).isEqualTo(1)
    assertThat(100.0.findMagnitude()).isEqualTo(2)
    assertThat(1000.0.findMagnitude()).isEqualTo(3)
    assertThat(0.1.findMagnitude()).isEqualTo(-1)
    assertThat(0.01.findMagnitude()).isEqualTo(-2)
    assertThat(0.001.findMagnitude()).isEqualTo(-3)

    assertThat(2.0.findMagnitude()).isEqualTo(0)
    assertThat(20.0.findMagnitude()).isEqualTo(1)
    assertThat(0.2.findMagnitude()).isEqualTo(-1)
  }

  @Test
  fun findMagnitudeCeil() {
    assertThat(1.0.findMagnitudeCeil()).isEqualTo(0)
    assertThat(10.0.findMagnitudeCeil()).isEqualTo(1)
    assertThat(100.0.findMagnitudeCeil()).isEqualTo(2)
    assertThat(1000.0.findMagnitudeCeil()).isEqualTo(3)
    assertThat(0.1.findMagnitudeCeil()).isEqualTo(-1)
    assertThat(0.01.findMagnitudeCeil()).isEqualTo(-2)
    assertThat(0.001.findMagnitudeCeil()).isEqualTo(-3)

    assertThat(2.0.findMagnitudeCeil()).isEqualTo(1)
    assertThat(20.0.findMagnitudeCeil()).isEqualTo(2)
    assertThat(0.2.findMagnitudeCeil()).isEqualTo(0)
  }

  @Test
  fun testEven() {
    assertThat(1.0.isEven()).isFalse()
    assertThat(2.0.isEven()).isTrue()
    assertThat(3.0.isEven()).isFalse()
    assertThat(4.0.isEven()).isTrue()
    assertThat(4.00000000001.isEven()).isFalse()
    assertThat(0.0.isEven()).isTrue()

    assertThat((-1.0).isEven()).isFalse()
    assertThat((-2.0).isEven()).isTrue()
  }

  @Test
  fun testOdd() {
    assertThat(1.0.isOdd()).isTrue()
    assertThat(2.0.isOdd()).isFalse()
    assertThat(3.0.isOdd()).isTrue()
    assertThat(4.0.isOdd()).isFalse()
    assertThat(4.00000000001.isOdd()).isFalse()
    assertThat(0.0.isOdd()).isFalse()

    assertThat((-1.0).isOdd()).isTrue()
    assertThat((-2.0).isOdd()).isFalse()
  }

  @Test
  fun `Int isOdd and isEven handle negative values`() {
    assertThat(1.isOdd).isTrue()
    assertThat(2.isOdd).isFalse()
    assertThat(0.isOdd).isFalse()

    //Kotlin's % keeps the sign of the dividend: -3 % 2 == -1, so a naive `== 1` check misses negative odds.
    assertThat((-3).isOdd).isTrue()
    assertThat((-1).isOdd).isTrue()
    assertThat((-2).isOdd).isFalse()

    assertThat((-3).isEven).isFalse()
    assertThat((-2).isEven).isTrue()
  }

  @Test
  fun testMaxNullable() {
    assertThat(1.0.coerceAtLeastOrNull(12.0)).isEqualTo(12.0)
    assertThat(1.0.coerceAtLeastOrNull(null)).isEqualTo(1.0)
    assertThat((null as Double?).coerceAtLeastOrNull(12.0)).isEqualTo(12.0)
    assertThat((null as Double?).coerceAtLeastOrNull(1.0)).isEqualTo(1.0)
    assertThat((null as Double?).coerceAtLeastOrNull(null)).isEqualTo(null)
  }

  @Test
  fun ceilTo10() {
    assertThat(12.0.roundDecimalPlaces(-1)).isEqualTo(10.0)
    assertThat(20.0.ceilDecimalPlaces(-1)).isEqualTo(20.0)
    assertThat(99.0.roundDecimalPlaces(-1)).isEqualTo(100.0)
  }

  @Test
  fun testPercent() {
    assertThat(4.0.percent).isEqualTo(0.04)
    assertThat(4.01.percent).isEqualTo(0.0401)
    assertThat(4.percent).isEqualTo(0.04)
  }

  @Test
  fun testBetween() {
    assertThat(7.0.betweenInclusive(1.0, 7.0)).isTrue()
    assertThat(7.0.betweenInclusive(1.0, 7.1)).isTrue()
    assertThat(7.0.betweenInclusive(1.0, 6.999)).isFalse()
  }

  @Test
  fun testBetweenOtherOrder() {
    assertThat(7.0.betweenInclusive(7.0, 1.0)).isTrue()
    assertThat(7.0.betweenInclusive(7.1, 1.0)).isTrue()
    assertThat(7.0.betweenInclusive(6.999, 1.0)).isFalse()
  }

  @Test
  fun testToRadians() {
    assertThat(0.0.toRadians()).isEqualTo(0.0)
    assertThat(45.0.toRadians()).isEqualTo(PI / 4.0 * 1) //bottom-right
    assertThat(90.0.toRadians()).isEqualTo(PI / 4.0 * 2) //bottom
    assertThat(135.0.toRadians()).isEqualTo(PI / 4.0 * 3) //bottom-left
    assertThat(180.0.toRadians()).isEqualTo(PI / 4.0 * 4) //left
    assertThat(225.0.toRadians()).isEqualTo(PI / 4.0 * 5) //top-left
    assertThat(270.0.toRadians()).isEqualTo(PI / 4.0 * 6) //top
    assertThat(315.0.toRadians()).isEqualTo(PI / 4.0 * 7) //top right

    //More than one rotation
    assertThat(360.0.toRadians()).isEqualTo(PI / 4.0 * 8) //top right
    assertThat(405.0.toRadians()).isEqualTo(PI / 4.0 * 9) //top right

    //Negative values
    assertThat(-45.0.toRadians()).isEqualTo(PI / 4.0 * -1) //bottom-right
    assertThat(-90.0.toRadians()).isEqualTo(PI / 4.0 * -2) //bottom-right
  }

  @Test
  internal fun testGeneric() {
    for (i in -50..100) {
      val degrees = 45 * i
      val expectedRad = PI / 4.0 * i

      val toRadians = degrees.toRadians()

      assertThat(toRadians, "$degrees°").isEqualTo(expectedRad)
    }
  }

  @Test
  fun testPointIsLeftOfLine() {
    val line1 = Pair(Pair(0.0, 0.0), Pair(10.0, 10.0))
    val line2 = Pair(Pair(10.0, -10.0), Pair(-10.0, -10.0))
    val line3 = Pair(Pair(5.0, -10.0), Pair(-5.0, 10.0))

    val point1 = Pair(0.0, 5.0)
    val point2 = Pair(5.0, 0.0)
    val point3 = Pair(-50.0, 0.0)
    val point4 = Pair(0.0, 50.0)

    assertThat(pointIsLeftOfLine(line1.first.first, line1.first.second, line1.second.first, line1.second.second, point1.first, point1.second)).isEqualTo(true)
    assertThat(pointIsLeftOfLine(line1.first.first, line1.first.second, line1.second.first, line1.second.second, point2.first, point2.second)).isEqualTo(false)
    assertThat(pointIsLeftOfLine(line1.first.first, line1.first.second, line1.second.first, line1.second.second, point3.first, point3.second)).isEqualTo(true)
    assertThat(pointIsLeftOfLine(line1.first.first, line1.first.second, line1.second.first, line1.second.second, point4.first, point4.second)).isEqualTo(true)

    assertThat(pointIsLeftOfLine(line2.first.first, line2.first.second, line2.second.first, line2.second.second, point1.first, point1.second)).isEqualTo(false)
    assertThat(pointIsLeftOfLine(line2.first.first, line2.first.second, line2.second.first, line2.second.second, point2.first, point2.second)).isEqualTo(false)
    assertThat(pointIsLeftOfLine(line2.first.first, line2.first.second, line2.second.first, line2.second.second, point3.first, point3.second)).isEqualTo(false)
    assertThat(pointIsLeftOfLine(line2.first.first, line2.first.second, line2.second.first, line2.second.second, point4.first, point4.second)).isEqualTo(false)

    assertThat(pointIsLeftOfLine(line3.first.first, line3.first.second, line3.second.first, line3.second.second, point1.first, point1.second)).isEqualTo(false)
    assertThat(pointIsLeftOfLine(line3.first.first, line3.first.second, line3.second.first, line3.second.second, point2.first, point2.second)).isEqualTo(false)
    assertThat(pointIsLeftOfLine(line3.first.first, line3.first.second, line3.second.first, line3.second.second, point3.first, point3.second)).isEqualTo(true)
    assertThat(pointIsLeftOfLine(line3.first.first, line3.first.second, line3.second.first, line3.second.second, point4.first, point4.second)).isEqualTo(false)
  }
}
