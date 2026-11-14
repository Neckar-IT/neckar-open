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

class _ExtensionsKtTest {
  @Test
  fun `binarySearch should find element in sorted array`() {
    val array = arrayOf(1, 2, 3, 4, 5)
    val result = array.binarySearch(3, 0, array.size, Comparator { a, b -> a.compareTo(b) })
    assertThat(result).isEqualTo(BSearchResult.found(2))
  }

  @Test
  fun `binarySearch should return insert position in sorted array`() {
    val array = arrayOf(1, 2, 4, 5)
    val result = array.binarySearch(3, 0, array.size, Comparator { a, b -> a.compareTo(b) })
    assertThat(result).isEqualTo(BSearchResult.insertAt(2))
  }

  @Test
  fun `binarySearch should return insert position at beginning of sorted array`() {
    val array = arrayOf(1, 2, 4, 5)
    val result = array.binarySearch(0, 0, array.size, Comparator { a, b -> a.compareTo(b) })
    assertThat(result).isEqualTo(BSearchResult.insertAt(0))
  }

  @Test
  fun `binarySearch should return insert position at end of sorted array`() {
    val array = arrayOf(1, 2, 4, 5)
    val result = array.binarySearch(6, 0, array.size, Comparator { a, b -> a.compareTo(b) })
    assertThat(result).isEqualTo(BSearchResult.insertAt(4))
  }

  @Test
  fun testRight() {
    //Never returns 1 (is exclusive)
    assertThat(genericBinarySearchRight(0, 1) { -1 }).isEqualTo(0)
    assertThat(genericBinarySearchRight(0, 1) { 0 }).isEqualTo(0)
    assertThat(genericBinarySearchRight(0, 1) { 1 }).isEqualTo(0)

    //Return at most 1
    assertThat(genericBinarySearchRight(0, 2) { -1 }).isEqualTo(1)
    assertThat(genericBinarySearchRight(0, 2) { 0 }).isEqualTo(0)
    assertThat(genericBinarySearchRight(0, 2) { 1 }).isEqualTo(0)

    //Return at most 7
    assertThat(genericBinarySearchRight(0, 8) { -1 }).isEqualTo(7)
    assertThat(genericBinarySearchRight(0, 8) { 0 }).isEqualTo(3) //the first index that is questioned
    assertThat(genericBinarySearchRight(0, 8) { 1 }).isEqualTo(0)

    assertThat(genericBinarySearchRight(0, 10) { -1 }).isEqualTo(9)
  }

  @Test
  fun testLeft2() {
    //Never returns 1 (is exclusive)
    assertThat(genericBinarySearchLeft(0, 1) { -1 }).isEqualTo(0)
    assertThat(genericBinarySearchLeft(0, 1) { 0 }).isEqualTo(0)
    assertThat(genericBinarySearchLeft(0, 1) { 1 }).isEqualTo(0)

    //Return at most 1
    assertThat(genericBinarySearchLeft(0, 2) { -1 }).isEqualTo(1)
    assertThat(genericBinarySearchLeft(0, 2) { 0 }).isEqualTo(0)
    assertThat(genericBinarySearchLeft(0, 2) { 1 }).isEqualTo(0)

    //Return at most 7
    assertThat(genericBinarySearchLeft(0, 8) { -1 }).isEqualTo(7)
    assertThat(genericBinarySearchLeft(0, 8) { 0 }).isEqualTo(3) //the first index that is questioned
    assertThat(genericBinarySearchLeft(0, 8) { 1 }).isEqualTo(0)
  }

  @Test
  fun testLeftRight() {
    //Exact hit
    assertThat(genericBinarySearchLeft(0, 3) { it.compareTo(1.0) }).isEqualTo(1)
    assertThat(genericBinarySearchRight(0, 3) { it.compareTo(1.0) }).isEqualTo(1)

    //between two values - returning different values for left/right
    assertThat(genericBinarySearchLeft(0, 3) { it.compareTo(1.5) }).isEqualTo(1)
    assertThat(genericBinarySearchRight(0, 3) { it.compareTo(1.5) }).isEqualTo(2)

    //very small - left most index
    assertThat(genericBinarySearchLeft(0, 3) { 1 }).isEqualTo(0)
    assertThat(genericBinarySearchRight(0, 3) { 1 }).isEqualTo(0)

    //very large - right most index
    assertThat(genericBinarySearchLeft(0, 3) { -1 }).isEqualTo(2)
    assertThat(genericBinarySearchRight(0, 3) { -1 }).isEqualTo(2)
  }

  @Test
  fun testLeft() {
    assertThat(genericBinarySearchRight(0, 10) { it.compareTo(10.5) }).isEqualTo(9)
  }

  @Test
  fun testBounds() {
    genericBinarySearchRight(1, 20) {
      it.compareTo(0)
    }.let {
      assertThat(it).isEqualTo(1)
    }

    genericBinarySearchLeft(1, 20) {
      it.compareTo(0)
    }.let {
      assertThat(it).isEqualTo(1)
    }

    genericBinarySearchLeft(1, 2) {
      it.compareTo(0)
    }.let {
      assertThat(it).isEqualTo(1)
    }
    genericBinarySearchRight(1, 2) {
      it.compareTo(0)
    }.let {
      assertThat(it).isEqualTo(1)
    }
  }

  @Test
  fun testBiSect() {
    genericBinarySearchRight(0, 20) {
      it.compareTo(4)
    }.let {
      assertThat(it).isEqualTo(4)
    }

    genericBinarySearchRight(0, 20) {
      it.compareTo(17)
    }.let {
      assertThat(it).isEqualTo(17)
    }

    genericBinarySearchRight(0, 20) {
      it.compareTo(4.5)
    }.let {
      assertThat(it).isEqualTo(5)
    }

    genericBinarySearchLeft(0, 20) {
      it.compareTo(4.5)
    }.let {
      assertThat(it).isEqualTo(4)
    }
  }
}
