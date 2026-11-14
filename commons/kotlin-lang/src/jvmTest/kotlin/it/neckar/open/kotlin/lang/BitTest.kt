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

/**
 *
 */
class BitTest {
  @Test
  fun `a mask over the full width sets every bit`() {
    //shl uses the low five bits of the shift, so 1 shl 32 is 1 and the subtraction answered 0.
    assertThat(32.mask()).isEqualTo(-1)
    assertThat((-1).extract(0, 32)).isEqualTo(-1)
    assertThat(64L.mask()).isEqualTo(-1L)

    //One bit below the width is unchanged.
    assertThat(31.mask()).isEqualTo(Int.MAX_VALUE)
    assertThat(8.mask()).isEqualTo(0xFF)
  }

  @Test
  fun testBitset() {
    assertThat(0.isBitSet(0)).isFalse()
    assertThat(0.isBitSet(1)).isFalse()
    assertThat(0.isBitSet(2)).isFalse()
    assertThat(0.isBitSet(3)).isFalse()
    assertThat(0.isBitSet(4)).isFalse()

    assertThat(1.isBitSet(0)).isTrue()
    assertThat(1.isBitSet(1)).isFalse()
    assertThat(1.isBitSet(2)).isFalse()
    assertThat(1.isBitSet(3)).isFalse()
    assertThat(1.isBitSet(4)).isFalse()

    assertThat(2.isBitSet(0)).isFalse()
    assertThat(2.isBitSet(1)).isTrue()
    assertThat(2.isBitSet(2)).isFalse()
    assertThat(2.isBitSet(3)).isFalse()
    assertThat(2.isBitSet(4)).isFalse()
  }

  @Test
  fun testOther() {
    assertThat(0.takeLowestOneBit()).isEqualTo(0)
    assertThat(1.takeLowestOneBit()).isEqualTo(1)
    assertThat(2.takeLowestOneBit()).isEqualTo(2)
    assertThat(3.takeLowestOneBit()).isEqualTo(1)
    assertThat(4.takeLowestOneBit()).isEqualTo(4)
    assertThat(5.takeLowestOneBit()).isEqualTo(1)
    assertThat(8.takeLowestOneBit()).isEqualTo(8)
  }

  @Test
  fun testFindFirstBit() {
    assertThat(0.findLowestOneBit()).isEqualTo(-1)
    assertThat(1.findLowestOneBit()).isEqualTo(0)
    assertThat(2.findLowestOneBit()).isEqualTo(1)
    assertThat(3.findLowestOneBit()).isEqualTo(0)
    assertThat(4.findLowestOneBit()).isEqualTo(2)
    assertThat(5.findLowestOneBit()).isEqualTo(0)
    assertThat(8.findLowestOneBit()).isEqualTo(3)

    assertThat(0b0_1000.findLowestOneBit()).isEqualTo(3)
    assertThat(0b1_0000.findLowestOneBit()).isEqualTo(4)
    assertThat(0b10_0000.findLowestOneBit()).isEqualTo(5)
    assertThat(0b100_0000.findLowestOneBit()).isEqualTo(6)
    assertThat(0b0_1000_0000.findLowestOneBit()).isEqualTo(7)
    assertThat(0b01_0000_0000.findLowestOneBit()).isEqualTo(8)
    assertThat(0b10_0000_0000.findLowestOneBit()).isEqualTo(9)

    assertThat(Int.MAX_VALUE.findLowestOneBit()).isEqualTo(0)
    assertThat((Int.MAX_VALUE - 1).findLowestOneBit()).isEqualTo(1)

    assertThat(Int.MIN_VALUE.findLowestOneBit()).isEqualTo(31)
    assertThat((Int.MIN_VALUE + 1).findLowestOneBit()).isEqualTo(0)
    assertThat((Int.MIN_VALUE + 2).findLowestOneBit()).isEqualTo(1)

    assertThat(0b1_0000_0000_0000_0000_0000_0000.findLowestOneBit()).isEqualTo(24)
    assertThat(0b1000_0000_0000_0000_0000_0000_0000.findLowestOneBit()).isEqualTo(27)
    assertThat(0b100_0000_0000_0000_0000_0000_0000_0000.findLowestOneBit()).isEqualTo(30)
  }
}
