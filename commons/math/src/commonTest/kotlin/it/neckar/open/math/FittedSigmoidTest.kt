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
package it.neckar.open.math

import assertk.assertThat
import assertk.assertions.isBetween
import assertk.assertions.isCloseTo
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isGreaterThan
import assertk.assertions.isTrue
import kotlin.test.Test
import kotlin.test.assertFailsWith

/**
 * Covers [FittedSigmoid] — the logistic curve itself, the fit and the goodness-of-fit measure.
 *
 * Part of it reproduces the division-by-zero defects from issue #2103. Before that fix both
 * degenerate paths silently propagated `Infinity`/`NaN`:
 * - [FittedSigmoid.fitLogisticModel] computed `bInit = 1.0 / (x.max() - x.min())`,
 *   which is `1.0 / 0.0 == Infinity` for constant x, so gradient descent started from garbage.
 * - [FittedSigmoid.calculateRSquared] computed `1 - ssResidual / ssTotal` with `ssTotal == 0.0`
 *   for constant actual values, yielding `NaN` or `±Infinity`.
 */
class FittedSigmoidTest {

  @Test
  fun `fitLogisticModel rejects empty x data`() {
    assertFailsWith<IllegalArgumentException> {
      FittedSigmoid.fitLogisticModel(emptyList(), emptyList())
    }
  }

  @Test
  fun `fitLogisticModel rejects constant x instead of starting from an Infinity slope`() {
    // Constant x: x.max() - x.min() == 0.0, so the old bInit = 1.0 / 0.0 was Infinity.
    val constantX = listOf(2.0, 2.0, 2.0, 2.0)
    val p = listOf(0.1, 0.4, 0.6, 0.9)

    assertFailsWith<IllegalArgumentException> {
      FittedSigmoid.fitLogisticModel(constantX, p)
    }
  }

  @Test
  fun `fitLogisticModel fits non-degenerate input to a curve that explains the data`() {
    val (x, p) = FittedSigmoid.generateSyntheticData(amount = 50)

    val (a, b, c) = FittedSigmoid.fitLogisticModel(x, p)

    listOf(a, b, c).forEach { parameter ->
      assertThat(parameter.isFinite()).isTrue()
    }

    // The synthetic p values rise monotonically over x, so the fitted growth rate has to be
    // positive and the inflection point has to sit inside the sampled x range.
    assertThat(b).isGreaterThan(0.0)
    assertThat(c).isBetween(x.min(), x.max())

    // The fitted curve has to explain the data, not just be finite: predicting the mean
    // everywhere scores exactly 0.0, any worse constant scores below that.
    val predicted = x.map { FittedSigmoid.logistic(it, a, b, c) }
    assertThat(FittedSigmoid.calculateRSquared(p, predicted)).isGreaterThan(0.75)
  }

  @Test
  fun `logistic runs from the lower asymptote a up to 1 with the inflection point at c`() {
    val a = 0.2
    val b = 1.5
    val c = 3.0

    assertThat(FittedSigmoid.logistic(-1000.0, a, b, c)).isCloseTo(a, 1e-12)
    assertThat(FittedSigmoid.logistic(c, a, b, c)).isCloseTo(0.6, 1e-12) // halfway between a and 1
    assertThat(FittedSigmoid.logistic(1000.0, a, b, c)).isCloseTo(1.0, 1e-12)
  }

  @Test
  fun `calculateRSquared returns 1 for a perfectly matched constant series instead of NaN`() {
    // Constant actual: ssTotal == 0.0, so the old 1 - ssResidual / ssTotal was NaN (0.0 / 0.0).
    val actual = listOf(3.0, 3.0, 3.0)
    val predicted = listOf(3.0, 3.0, 3.0)

    val rSquared = FittedSigmoid.calculateRSquared(actual, predicted)

    assertThat(rSquared.isNaN()).isFalse()
    assertThat(rSquared).isEqualTo(1.0)
  }

  @Test
  fun `calculateRSquared returns 0 for a mismatched constant series instead of Infinity`() {
    // Constant actual with non-zero residual: ssTotal == 0.0 but ssResidual > 0.0,
    // so the old 1 - ssResidual / ssTotal was -Infinity (positive / 0.0).
    val actual = listOf(3.0, 3.0, 3.0)
    val predicted = listOf(1.0, 2.0, 5.0)

    val rSquared = FittedSigmoid.calculateRSquared(actual, predicted)

    assertThat(rSquared.isInfinite()).isFalse()
    assertThat(rSquared).isEqualTo(0.0)
  }

  @Test
  fun `calculateRSquared returns 1 for a perfect prediction of varying input`() {
    val actual = listOf(1.0, 2.0, 3.0, 4.0)
    val predicted = listOf(1.0, 2.0, 3.0, 4.0)

    val rSquared = FittedSigmoid.calculateRSquared(actual, predicted)

    assertThat(rSquared).isEqualTo(1.0)
  }

  @Test
  fun `calculateRSquared returns the classical coefficient for an imperfect prediction`() {
    // mean 2.5, ssTotal = 1.5² + 0.5² + 0.5² + 1.5² = 5.0
    val actual = listOf(1.0, 2.0, 3.0, 4.0)
    // ssResidual = 0.5² + 0.0 + 0.5² + 0.0 = 0.5, so 1 - 0.5 / 5.0 = 0.9
    val predicted = listOf(1.5, 2.0, 2.5, 4.0)

    val rSquared = FittedSigmoid.calculateRSquared(actual, predicted)

    assertThat(rSquared).isCloseTo(0.9, 1e-12)
  }

  @Test
  fun `calculateRSquared is negative for a prediction worse than the mean`() {
    // mean 2.5, ssTotal = 5.0
    val actual = listOf(1.0, 2.0, 3.0, 4.0)
    // The inverted series: ssResidual = 3² + 1² + 1² + 3² = 20.0, so 1 - 20.0 / 5.0 = -3.0
    val predicted = listOf(4.0, 3.0, 2.0, 1.0)

    val rSquared = FittedSigmoid.calculateRSquared(actual, predicted)

    assertThat(rSquared).isCloseTo(-3.0, 1e-12)
  }
}
