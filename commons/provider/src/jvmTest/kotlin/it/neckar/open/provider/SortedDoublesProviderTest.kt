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
@file:Suppress("RETURN_VALUE_NOT_USED")

package it.neckar.open.provider

import assertk.*
import assertk.assertions.*
import it.neckar.open.kotlin.lang.DoublesComparator
import org.junit.jupiter.api.Test

class SortedDoublesProviderTest {
  @Test
  fun testIt() {
    val unsorted = DoublesProvider.forDoubles(10.0, 7.0, 5.0, 11.0, 1.0)
    val sorted = unsorted.sorted()

    assertThat(unsorted.toList()).containsExactly(10.0, 7.0, 5.0, 11.0, 1.0)

    //Call to size triggers the sorting
    assertThat(sorted.size()).isEqualTo(5)
    assertThat(sorted.toList()).containsExactly(1.0, 5.0, 7.0, 10.0, 11.0)

    assertThat(sorted.mapped2Original(0)).isEqualTo(4)
    assertThat(sorted.mapped2Original(1)).isEqualTo(2)
    assertThat(sorted.mapped2Original(2)).isEqualTo(1)
    assertThat(sorted.mapped2Original(3)).isEqualTo(0)
    assertThat(sorted.mapped2Original(4)).isEqualTo(3)
  }

  @Test
  fun testCustomComparatorReversed() {
    val unsorted = DoublesProvider.forDoubles(10.0, 7.0, 5.0, 11.0, 1.0)
    val sorted = unsorted.sorted(object : DoublesComparator {
      override fun compare(valueA: Double, valueB: Double): Int {
        return valueB.compareTo(valueA)
      }
    })

    assertThat(unsorted.toList()).containsExactly(10.0, 7.0, 5.0, 11.0, 1.0)

    //Call to size triggers the sorting
    assertThat(sorted.size()).isEqualTo(5)
    assertThat(sorted.toList()).containsExactly(11.0, 10.0, 7.0, 5.0, 1.0)
  }

  private annotation class MyIndex1
  private annotation class MyIndex2

  @Test
  fun testWithMultiProvider() {
    val unsorted = DoublesProvider.forDoubles(10.0, 7.0, 5.0, 11.0, 1.0)
    val sorted = unsorted.sorted()

    val multiProvider = MultiProvider.forListModulo<MyIndex1, String>(listOf("10.0", "7.0", "5.0", "11.0", "1.0"))
    val sortedMultiProvider = sorted.wrapMultiProvider<MyIndex1, MyIndex2, String>(multiProvider)

    //Indices must match!
    assertThat(unsorted[0]).isEqualTo(10.0)
    assertThat(multiProvider.valueAt(0)).isEqualTo("10.0")

    //Trigger recalculation
    assertThat(sorted.size()).isEqualTo(5)

    assertThat(sorted[0]).isEqualTo(1.0)
    assertThat(sortedMultiProvider.valueAt(0)).isEqualTo("1.0")

    assertThat(sorted[1]).isEqualTo(5.0)
    assertThat(sortedMultiProvider.valueAt(1)).isEqualTo("5.0")

    assertThat(sorted[4]).isEqualTo(11.0)
    assertThat(sortedMultiProvider.valueAt(4)).isEqualTo("11.0")
  }

  @Test
  fun testSimpleMappingSample() {
    val unsorted = DoublesProvider.forDoubles(20.0, 30.0, 40.0, 10.0)
    val sorted = unsorted.sorted()

    //Trigger recalculation and verify size
    assertThat(sorted.size()).isEqualTo(4)
    assertThat(sorted.toList()).isEqualTo(listOf(10.0, 20.0, 30.0, 40.0))

    assertThat(unsorted[0]).isEqualTo(20.0)
    assertThat(sorted[0]).isEqualTo(10.0)

    assertThat(unsorted[1]).isEqualTo(30.0)
    assertThat(sorted[1]).isEqualTo(20.0)

    assertThat(unsorted[2]).isEqualTo(40.0)
    assertThat(sorted[2]).isEqualTo(30.0)

    assertThat(unsorted[3]).isEqualTo(10.0)
    assertThat(sorted[3]).isEqualTo(40.0)

    //Verify index
    assertThat(sorted.mapped2Original(0)).isEqualTo(3)
    assertThat(sorted.mapped2Original(1)).isEqualTo(0)
    assertThat(sorted.mapped2Original(2)).isEqualTo(1)
    assertThat(sorted.mapped2Original(3)).isEqualTo(2)
  }
}

fun DoublesProvider.toList(): List<Double> {
  return List(size()) {
    valueAt(it)
  }
}
