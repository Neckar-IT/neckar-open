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
package it.neckar.geometry

import assertk.*
import assertk.assertions.*
import org.junit.jupiter.api.Test

/**
 * Pins scaling behavior of [Distance].
 *
 * Before the fix, both `times(factor)` and `multiply(factorX, factorY)` added the factor
 * to `y` instead of multiplying — `Distance(2.0, 3.0) * 2.0` returned `(4.0, 5.0)`.
 */
class DistanceScalingTest {
  @Test
  fun `times scales both components by the factor`() {
    val scaled = Distance(2.0, 3.0) * 2.0
    assertThat(scaled).isEqualTo(Distance(4.0, 6.0))
  }

  @Test
  fun `multiply scales each component by its own factor`() {
    val scaled = Distance(2.0, 3.0).multiply(2.0, 3.0)
    assertThat(scaled).isEqualTo(Distance(4.0, 9.0))
  }

  @Test
  fun `times by zero produces a zero distance`() {
    // Positive inputs only: a negative component times 0.0 yields IEEE -0.0, which the
    // data-class equals distinguishes from 0.0. The negative case is covered by the identity test.
    val scaled = Distance(7.0, 4.0) * 0.0
    assertThat(scaled).isEqualTo(Distance(0.0, 0.0))
  }

  @Test
  fun `times by one is identity`() {
    val original = Distance(7.0, -4.0)
    assertThat(original * 1.0).isEqualTo(original)
  }
}
