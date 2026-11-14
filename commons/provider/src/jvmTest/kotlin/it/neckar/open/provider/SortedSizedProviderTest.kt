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
package it.neckar.open.provider.it.neckar.open.provider

import assertk.*
import assertk.assertions.*
import it.neckar.open.kotlin.lang.parseInt
import it.neckar.open.provider.MultiProvider
import it.neckar.open.provider.SizedProvider
import it.neckar.open.provider.SortedSizedProvider
import it.neckar.open.provider.sorted
import it.neckar.open.provider.wrapMultiProvider
import org.junit.jupiter.api.Test

class SortedSizedProviderTest {
  @Test
  fun testSimple() {
    val unsorted = SizedProvider.forValues("17", "22", "5", "9", "1")
    assertThat(unsorted.size()).isEqualTo(5)

    assertThat(unsorted.toList()).containsExactly("17", "22", "5", "9", "1")

    val sorted = unsorted.sorted(compareBy { it.parseInt() })

    assertThat(sorted.size()).isEqualTo(5)

    assertThat(unsorted.toList()).containsExactly("17", "22", "5", "9", "1")
    assertThat(sorted.toList()).containsExactly("1", "5", "9", "17", "22")

    //Mapped indices have now been updated
    assertThat(sorted.mapped2Original(0)).isEqualTo(4) //--> 1
    assertThat(sorted.mapped2Original(4)).isEqualTo(1) //--> 22
  }

  @Test
  fun testUseForMultiProvider() {
    val unsorted = SizedProvider.forValues("17", "22", "5", "9", "1")
    val sorted = SortedSizedProvider(unsorted, compareBy { it.parseInt() })

    assertThat(unsorted.toList()).containsExactly("17", "22", "5", "9", "1")
    assertThat(sorted.toList()).containsExactly("1", "5", "9", "17", "22")

    val multiProvider = MultiProvider<Int, String> { "value_$it" }
    assertThat(multiProvider.valueAt(0)).isEqualTo("value_0")
    assertThat(multiProvider.valueAt(7)).isEqualTo("value_7")

    val sortedMultiProvider = sorted.wrapMultiProvider<Int, Int, String>(multiProvider)

    assertThat(sortedMultiProvider.toList(5)).containsExactly("value_4", "value_2", "value_3", "value_0", "value_1")
  }
}

fun <T> MultiProvider<*, T>.toList(size: Int): List<T> {
  return List(size) {
    valueAt(it)
  }
}

/**
 * Collects the values into a list.
 * ATTENTION: Only use for tests!
 */
fun <T> SizedProvider<T>.toList(): List<T> {
  return List(this.size()) {
    valueAt(it)
  }
}
