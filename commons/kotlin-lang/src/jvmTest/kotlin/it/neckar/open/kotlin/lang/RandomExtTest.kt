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
import kotlin.math.sqrt
import kotlin.random.Random

class RandomExtTest {
  /**
   * Box-Muller produces samples whose empirical standard deviation matches the configured sigma.
   *
   * Before the fix `randomNormal` used `log(x, 10.0)` instead of `ln(x)`,
   * yielding sigma_actual ≈ sigma * sqrt(1/ln(10)) ≈ sigma * 0.659 — distribution too narrow.
   */
  @Test
  fun `randomNormal produces samples with the configured standard deviation`() {
    // Seed the global random source for reproducibility (the function reads `random` directly).
    val previous = random
    try {
      random = Random(42L)

      val n = 100_000
      val center = 10.0
      val sigma = 3.0

      var sum = 0.0
      val samples = DoubleArray(n) {
        val s = randomNormal(center, sigma)
        sum += s
        s
      }
      val mean = sum / n
      val variance = samples.sumOf { (it - mean) * (it - mean) } / n
      val stddev = sqrt(variance)

      // 100k samples — empirical sigma is well within 5% of true sigma.
      assertThat(mean).isCloseTo(center, 0.05)
      assertThat(stddev).isCloseTo(sigma, 0.05)
    } finally {
      random = previous
    }
  }
}
