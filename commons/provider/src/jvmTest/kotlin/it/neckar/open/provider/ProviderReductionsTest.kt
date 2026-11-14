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
package it.neckar.open.provider

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isTrue
import org.junit.jupiter.api.Test

class ProviderReductionsTest {
  @Test
  fun `a NaN propagates whatever its position`() {
    assertThat(SizedProvider.forValues(3.0, Double.NaN).fastMaxBy { it }.isNaN()).isTrue()
    assertThat(SizedProvider.forValues(Double.NaN, 3.0).fastMaxBy { it }.isNaN()).isTrue()
    assertThat(SizedProvider.forValues(3.0, Double.NaN).fastMinBy { it }.isNaN()).isTrue()
    assertThat(SizedProvider.forValues(Double.NaN, 3.0).fastMinBy { it }.isNaN()).isTrue()
    assertThat(DoublesProvider.forDoubles(3.0, Double.NaN).max().isNaN()).isTrue()
  }

  @Test
  fun `an infinite element beats the seed`() {
    assertThat(DoublesProvider.forDoubles(Double.NEGATIVE_INFINITY).max()).isEqualTo(Double.NEGATIVE_INFINITY)
    assertThat(SizedProvider.forValues(1).fastMaxBy { Double.NEGATIVE_INFINITY }).isEqualTo(Double.NEGATIVE_INFINITY)
    assertThat(SizedProvider.forValues(1).fastMinBy { Double.POSITIVE_INFINITY }).isEqualTo(Double.POSITIVE_INFINITY)
  }

  @Test
  fun `an empty provider answers the fallback`() {
    assertThat(SizedProvider.empty<Double>().fastMaxBy(fallbackValue = 100.0) { it }).isEqualTo(100.0)
    assertThat(SizedProvider.empty<Double>().fastMinBy(fallbackValue = 100.0) { it }).isEqualTo(100.0)
  }
}
