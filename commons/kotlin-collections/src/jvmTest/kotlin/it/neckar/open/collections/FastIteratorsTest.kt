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
import it.neckar.open.kotlin.lang.fastFor
import it.neckar.open.kotlin.lang.join
import org.junit.jupiter.api.Test

/**
 *
 */
class FastIteratorsKtTest {
  @Test
  fun testFastMaxOfArray() {
    val list = Array(10) { "a".repeat(it) }

    assertThat(
      list.maxOf {
        it.length
      }).isEqualTo(9)

    assertThat(
      list.fastMaxBy(-1.0) {
        it.length.toDouble()
      }).isEqualTo(9.0)
  }

  @Test
  fun testFastMaxOfList() {
    val list = List(10) { "a".repeat(it) }

    assertThat(
      list.maxOf {
        it.length
      }).isEqualTo(9)

    assertThat(
      list.fastMaxBy(-1.0) {
        it.length.toDouble()
      }).isEqualTo(9.0)
  }

  @Test
  fun `a NaN propagates and an infinite element beats the seed`() {
    //coerceAtLeast on the element returned a NaN and overwrote the running maximum, so the answer
    //depended on where the NaN sat; -Double.MAX_VALUE as the seed came out as the answer itself.
    assertThat(listOf(3.0, Double.NaN).fastMaxBy { it }.isNaN()).isTrue()
    assertThat(listOf(Double.NaN, 3.0).fastMaxBy { it }.isNaN()).isTrue()
    assertThat(arrayOf(3.0, Double.NaN).fastMaxBy { it }.isNaN()).isTrue()

    assertThat(listOf(Double.NEGATIVE_INFINITY).fastMaxBy { it }).isEqualTo(Double.NEGATIVE_INFINITY)
    assertThat(arrayOf(Double.NEGATIVE_INFINITY).fastMaxBy { it }).isEqualTo(Double.NEGATIVE_INFINITY)

    assertThat(emptyList<Double>().fastMaxBy(fallbackValue = 100.0) { it }).isEqualTo(100.0)
  }

  @Test
  fun testReverse() {
    val list = DoubleArray(10) { it * 10.0 }

    assertThat(list).hasSize(10)

    var callCount = 0

    list.fastForEachIndexedReverse { index, value, _ ->
      callCount++
      assertThat(index, "for index $index").isEqualTo(list.size - callCount)
      assertThat(value, "for index $index").isEqualTo((list.size - callCount) * 10.0)
    }

    assertThat(callCount).isEqualTo(10)
  }

  @Test
  fun testFastAny() {
    val list = DoubleArrayList()

    //No elements
    assertThat(list.fastFindAny { it > 0.0 }).isFalse()

    list.add(17.0)
    assertThat(list.fastFindAny { it > 0.0 }).isTrue()
    assertThat(list.fastFindAny { it > 17.0 }).isFalse()

    list.add(6.0)
    assertThat(list.fastFindAny { it > 0.0 }).isTrue()
    assertThat(list.fastFindAny { it > 6.0 }).isTrue()
    assertThat(list.fastFindAny { it > 17.0 }).isFalse()
    assertThat(list.fastFindAny { it < 17.0 }).isTrue()
    assertThat(list.fastFindAny { it < 6.0 }).isFalse()
  }

  @Test
  fun testJoin1() {
    var callCount = 0

    1.join({ fail("Where is the exception?") }) {
      callCount++
    }
    assertThat(callCount).isEqualTo(1)
  }

  @Test
  fun testJoin2() {
    var callCount = 0
    var between = 0

    2.join({ between++ }) {
      callCount++
    }
    assertThat(callCount).isEqualTo(2)
    assertThat(between).isEqualTo(1)
  }

  @Test
  fun testIntIt() {
    var expected = 0

    7.fastFor {
      assertThat(it).isEqualTo(expected)
      expected++
    }

    assertThat(expected).isEqualTo(7)
  }

  @Test
  fun testReduce() {
    listOf(2, 3, 4).reduce { first, second ->
      first + second
    }.let {
      assertThat(it).isEqualTo(2 + 3 + 4)
    }

    listOf(2, 3, 4).fastReduce { first, second ->
      first + second
    }.let {
      assertThat(it).isEqualTo(2 + 3 + 4)
    }
  }

  @Test
  fun testFastMapNonNull() {
    listOf("1", "2", "3", "asdf", "4").fastMapNotNull {
      it.toIntOrNull()
    }.let {
      assertThat(it).hasSize(4)
      assertThat(it).containsOnly(1, 2, 3, 4)
    }

    listOf<String>().fastMapNotNull {
      throw UnsupportedOperationException()
    }.let {
      assertThat(it).hasSize(0)
    }
  }

  @Test
  fun testFastMap() {
    listOf("1", "2", "3", "4").fastMapNotNull {
      it.toInt()
    }.let {
      assertThat(it).hasSize(4)
      assertThat(it).containsOnly(1, 2, 3, 4)
    }

    listOf<String>().fastMapNotNull {
      throw UnsupportedOperationException()
    }.let {
      assertThat(it).hasSize(0)
    }
  }

  @Test
  fun testFastForeachReversed() {
    val array = Array(7) {
      it
    }

    assertThat(array).hasSize(7)
    assertThat(array.last()).isEqualTo(6)


    var count = 0
    array.fastForEachReversed {
      assertThat(it, "value $it").isEqualTo(6 - count)
      count++
    }

    assertThat(count).isEqualTo(7)
  }

  @Test
  fun testFastForEach() {
    val array = IntArray(7) {
      it
    }

    var counter = 0
    array.forEachIndexed { index, value ->
      assertThat(index)
        .isEqualTo(counter)

      assertThat(index)
        .isEqualTo(value)

      counter++
    }


  }
}
