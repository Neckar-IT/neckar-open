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

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test

class StableRandomTest {
  /**
   * Known-answer test: These values are computed from the SplitMix64 reference implementation
   * (https://prng.di.unimi.it/splitmix64.c), taking the upper 32 bits of each 64 bit output.
   *
   * This test runs on all targets (JVM, JS, Wasm) - it therefore also verifies that the
   * sequence is identical across platforms. If this test ever fails, the E2E baseline
   * screenshots of the MeisterCharts demos become invalid.
   */
  @Test
  fun `nextInt matches the SplitMix64 reference sequence`() {
    assertThat(StableRandom(0).run { List(5) { nextInt() } }).isEqualTo(
      listOf(-501176263, 1853398634, 113532184, -125060952, 456755562),
    )
    assertThat(StableRandom(42).run { List(5) { nextInt() } }).isEqualTo(
      listOf(-1109970394, 686809907, 1196582743, 1478287871, 163338330),
    )
    assertThat(StableRandom(-1).run { List(5) { nextInt() } }).isEqualTo(
      listOf(-455511689, -375392153, 942667852, 1830663020, -1264564434),
    )
  }

  @Test
  fun `nextDouble matches the reference derivation`() {
    assertThat(StableRandom(42).nextDouble()).isEqualTo(0.7415648722635267)
  }

  @Test
  fun `same seed produces the same sequence`() {
    val first = StableRandom(123456789).run { List(100) { nextDouble() } }
    val second = StableRandom(123456789).run { List(100) { nextDouble() } }
    assertThat(first).isEqualTo(second)
  }

  @Test
  fun `nextInt with bound stays within the bound`() {
    val random = StableRandom(99)
    repeat(1_000) {
      val value = random.nextInt(0, 10)
      check(value in 0..9) { "Value out of bounds: $value" }
    }
  }
}
