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

class FastIteratorExtKtTest {
  @Test
  fun testContains() {
    assertThat(doubleArrayOf().fastContains(0.0)).isFalse()
    assertThat(doubleArrayOf().fastContains(1.0)).isFalse()

    assertThat(doubleArrayOf(1.0).fastContains(1.0)).isTrue()
    assertThat(doubleArrayOf(0.0, 1.0).fastContains(1.0)).isTrue()
    assertThat(doubleArrayOf(0.0, 1.0, 7.0).fastContains(1.0)).isTrue()
    assertThat(doubleArrayOf(0.0, 1.0, 7.0).fastContains(99.0)).isFalse()
  }

  @Test
  fun `should remove only even numbers`() {
    val list = mutableListOf(1, 2, 3, 4, 5, 6)
    list.fastForEachDelete { it % 2 == 0 }

    assertThat(list).containsExactly(1, 3, 5)
  }

  @Test
  fun `should remove all items`() {
    val list = mutableListOf(1, 2, 3, 4, 5, 6)
    list.fastForEachDelete { true }

    assertThat(list).isEmpty()
  }

  @Test
  fun `should not remove any items`() {
    val list = mutableListOf(1, 2, 3, 4, 5, 6)
    list.fastForEachDelete { false }

    assertThat(list).containsExactly(1, 2, 3, 4, 5, 6)
  }

  @Test
  fun `should remove only even numbers within size limit`() {
    val list = mutableListOf(1, 2, 3, 4, 5, 6)
    list.fastForEachDelete { it % 2 == 0 }

    assertThat(list).containsExactly(1, 3, 5)
  }

  @Test
  fun `it should pair elements correctly when list has more than one element`() {
    val list = listOf(1, 2, 3, 4, 5)
    val result = mutableListOf<Pair<Int, Int>>()

    list.fastZipWithNext { a, b ->
      result.add(a to b)
    }

    assertThat(result).isEqualTo(listOf(1 to 2, 2 to 3, 3 to 4, 4 to 5))
  }

  @Test
  fun `it should not invoke the action function when list has one element`() {
    val list = listOf(1)
    var invocationCount = 0

    list.fastZipWithNext { _, _ ->
      invocationCount++
    }

    assertThat(invocationCount).isEqualTo(0)
  }

  @Test
  fun `it should not invoke the action function when list is empty`() {
    val list = emptyList<Int>()
    var invocationCount = 0

    list.fastZipWithNext { _, _ ->
      invocationCount++
    }

    assertThat(invocationCount).isEqualTo(0)
  }
}
