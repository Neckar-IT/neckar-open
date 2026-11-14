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

import assertk.*
import assertk.assertions.*
import org.junit.jupiter.api.Test

class FilteredSizedProviderTest {
  @Test
  fun testIt() {
    val unfiltered = SizedProvider.forValues("a", null, "c", "d", "e")
    val filtered = unfiltered.filtered { index: Int, value: String? ->
      value != null
    }

    assertThat(unfiltered.toList()).containsExactly("a", null, "c", "d", "e")

    //Call to size triggers the sorting
    assertThat(filtered.size()).isEqualTo(4)
    assertThat(filtered.toList()).containsExactly("a", "c", "d", "e")
  }

  @Test
  fun testMultiProvider() {
    val unfiltered = SizedProvider.forValues("a", null, "c", "d", "e")
    val filtered: SizedProviderWithIndexMapping<String?> = unfiltered.filtered { index: Int, value: String? ->
      value != null
    }

    assertThat(unfiltered.toList()).containsExactly("a", null, "c", "d", "e")
    assertThat(filtered.toList()).containsExactly("a", "c", "d", "e") //calls size() to recalculate the index

    val multiProvider = MultiProvider.forListModulo<MyIndex1, String>(listOf("A", "null", "B", "C", "D", "E"))
    val multiProviderFiltered = filtered.wrapMultiProvider<MyIndex1, MyIndex2, String?>(multiProvider)

    assertThat(multiProviderFiltered.valueAt(0)).isEqualTo("A")
    assertThat(multiProviderFiltered.valueAt(1)).isEqualTo("B")
    assertThat(multiProviderFiltered.valueAt(2)).isEqualTo("C")
    assertThat(multiProviderFiltered.valueAt(3)).isEqualTo("D")
    assertThat(multiProviderFiltered.valueAt(4)).isEqualTo("A") //modulo
  }

  private annotation class MyIndex1
  private annotation class MyIndex2
}

fun <T> SizedProvider<T>.toList(): List<T> {
  return List(size()) {
    valueAt(it)
  }
}
