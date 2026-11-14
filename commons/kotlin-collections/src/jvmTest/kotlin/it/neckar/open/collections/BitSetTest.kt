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
package it.neckar.open.collections

import assertk.*
import assertk.assertions.*
import org.junit.jupiter.api.Test

/**
 */

class BitSetTest {
  fun BitSet.setCheckSeq(vararg bits: Boolean) {
    for (n in bits.indices) this[n] = bits[n]
    checkSeq(*bits)
  }

  fun BitSet.checkSeq(vararg bits: Boolean) {
    for (n in bits.indices) kotlin.test.assertEquals(bits[n], this[n])
  }

  @Test
  fun justOneBit() {
    val v = BitSet(1)
    v[0] = false
    kotlin.test.assertEquals(false, v[0])
    v[0] = true
    kotlin.test.assertEquals(true, v[0])
  }

  @Test
  fun fewBits() {
    val v = BitSet(3)
    v.setCheckSeq(false, true, false)
    v.setCheckSeq(true, false, true)
    v.setCheckSeq(true, true, true)
    v.clear()
    v.checkSeq(false, false, false)
  }

  @Test
  fun empty() {
    val bs = BitSet(0)
    assertThat(bs.size).isEqualTo(0)
  }

  @Test
  fun bits33() {
    val bs = BitSet(33)
    for (n in 0 until bs.size) {
      bs[n] = false
      kotlin.test.assertEquals(false, bs[n])
      bs[n] = true
      kotlin.test.assertEquals(true, bs[n])
    }
  }

  @Test
  fun unset() {
    val bs = BitSet(8)
    for (n in 0 until bs.size) bs[n] = true
    for (n in 0 until bs.size step 2) bs.unset(n)
    kotlin.test.assertEquals(listOf(false, true, false, true, false, true, false, true), bs.toList())
  }
}
