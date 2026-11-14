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
package it.neckar.financial.currency

import assertk.*
import assertk.assertions.*
import org.junit.jupiter.api.Test

class RoundingStrategyTest {
  @Test
  fun `commercial rounding moves a half cent away from zero`() {
    val rounding = RoundingStrategy.HalfAwayFromZero
    assertThat(rounding.roundToLong(0.5)).isEqualTo(1L)
    assertThat(rounding.roundToLong(-0.5)).isEqualTo(-1L)
    assertThat(rounding.roundToLong(199.5)).isEqualTo(200L)
    assertThat(rounding.roundToLong(-199.5)).isEqualTo(-200L)
    assertThat(rounding.roundToLong(2.4)).isEqualTo(2L)
    assertThat(rounding.roundToLong(-2.6)).isEqualTo(-3L)
    assertThat(rounding.roundToLong(0.49999999999999994)).isEqualTo(0L)
    assertThat(rounding.roundToLong(0.0)).isEqualTo(0L)
  }

  @Test
  fun `half to even sends a half cent to the even neighbour`() {
    val rounding = RoundingStrategy.HalfToEven
    assertThat(rounding.roundToLong(0.5)).isEqualTo(0L)
    assertThat(rounding.roundToLong(1.5)).isEqualTo(2L)
    assertThat(rounding.roundToLong(2.5)).isEqualTo(2L)
    assertThat(rounding.roundToLong(-2.5)).isEqualTo(-2L)
    assertThat(rounding.roundToLong(2.4)).isEqualTo(2L)
  }

  @Test
  fun `a quotient tie follows the strategy on either sign`() {
    assertThat(RoundingStrategy.HalfAwayFromZero.roundQuotient(1050, 100)).isEqualTo(11L)
    assertThat(RoundingStrategy.HalfAwayFromZero.roundQuotient(-1050, 100)).isEqualTo(-11L)
    assertThat(RoundingStrategy.HalfAwayFromZero.roundQuotient(-50, 100)).isEqualTo(-1L)
    assertThat(RoundingStrategy.HalfToEven.roundQuotient(1050, 100)).isEqualTo(10L)
    assertThat(RoundingStrategy.HalfToEven.roundQuotient(-1050, 100)).isEqualTo(-10L)
    assertThat(RoundingStrategy.HalfToEven.roundQuotient(1150, 100)).isEqualTo(12L)
    assertThat(RoundingStrategy.HalfToEven.roundQuotient(-1150, 100)).isEqualTo(-12L)
    assertThat(RoundingStrategy.HalfToEven.roundQuotient(-50, 100)).isEqualTo(0L)
    assertThat(RoundingStrategy.HalfToEven.roundQuotient(-150, 100)).isEqualTo(-2L)
    assertThat(RoundingStrategy.HalfAwayFromZero.roundQuotient(1049, 100)).isEqualTo(10L)
    assertThat(RoundingStrategy.HalfAwayFromZero.roundQuotient(-1051, 100)).isEqualTo(-11L)
    assertThat(RoundingStrategy.HalfAwayFromZero.roundQuotient(Long.MAX_VALUE - 1, Long.MAX_VALUE)).isEqualTo(1L)
  }

  @Test
  fun `a whole value beyond 2 to the 52 passes unchanged`() {
    assertThat(RoundingStrategy.HalfToEven.roundToLong(4_503_599_627_370_497.0)).isEqualTo(4_503_599_627_370_497L)
  }

  @Test
  fun `a double and a quotient tie agree`() {
    RoundingStrategy.entries.forEach { rounding ->
      (-30L..30L).forEach { numerator ->
        assertThat(rounding.roundToLong(numerator / 4.0), "$rounding $numerator / 4").isEqualTo(rounding.roundQuotient(numerator, 4))
      }
    }
  }

  @Test
  fun `NaN, an infinity, a value beyond Long and a zero denominator are refused`() {
    assertFailure { RoundingStrategy.HalfAwayFromZero.roundToLong(Double.NaN) }.isInstanceOf(IllegalArgumentException::class)
    assertFailure { RoundingStrategy.HalfToEven.roundToLong(Double.POSITIVE_INFINITY) }.isInstanceOf(IllegalArgumentException::class)
    assertFailure { RoundingStrategy.HalfToEven.roundToLong(Long.MAX_VALUE.toDouble()) }.isInstanceOf(IllegalArgumentException::class)
    assertFailure { RoundingStrategy.HalfToEven.roundQuotient(1, 0) }.isInstanceOf(IllegalArgumentException::class)
  }
}
