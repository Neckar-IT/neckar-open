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
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle

class ShiftedSizedProviderTest {
  @Test
  fun `a negative shift wraps around instead of indexing below zero`() {
    val sizedProvider = SizedProvider.forValues("A", "B", "C")

    // Same convention as a positive shift: shiftedBy(k) starts at the element (size - k) mod size,
    // so -1 starts one further on than 0 does.
    val shifted = sizedProvider.shiftedBy(-1)

    assertThat(shifted.size()).isEqualTo(3)
    assertThat(shifted.valueAt(0)).isEqualTo("B")
    assertThat(shifted.valueAt(1)).isEqualTo("C")
    assertThat(shifted.valueAt(2)).isEqualTo("A")
  }

  @Test
  fun testOffset() {
    val sizedProvider = SizedProvider.mapped(listOf(0, 1, 2, 3, 4)) {
      it.toString()
    }

    val shifted = sizedProvider.shiftedBy(2)
    assertThat(shifted.size()).isEqualTo(sizedProvider.size())
    assertThat(shifted.valueAt(0)).isEqualTo("3")
    assertThat(shifted.valueAt(1)).isEqualTo("4")
    assertThat(shifted.valueAt(2)).isEqualTo("0")
    assertThat(shifted.valueAt(3)).isEqualTo("1")
  }

  @Test
  fun `shifted provider returns correct value`() {
    val listSizedProvider = ListSizedProvider(listOf("A", "B", "C", "D", "E"))

    val shiftedBy0 = listSizedProvider.shiftedBy(0)
    val shiftedBy1 = listSizedProvider.shiftedBy(1)
    val shiftedBy2 = listSizedProvider.shiftedBy(2)
    val shiftedBy3 = listSizedProvider.shiftedBy(3)
    val shiftedBy4 = listSizedProvider.shiftedBy(4)
    val shiftedBy5 = listSizedProvider.shiftedBy(5)

    val shiftedBy77 = listSizedProvider.shiftedBy(77)


    assertThat(shiftedBy0.size()).isEqualTo(5)
    assertThat(shiftedBy1.size()).isEqualTo(5)
    assertThat(shiftedBy2.size()).isEqualTo(5)
    assertThat(shiftedBy3.size()).isEqualTo(5)
    assertThat(shiftedBy4.size()).isEqualTo(5)
    assertThat(shiftedBy5.size()).isEqualTo(5)
    assertThat(shiftedBy77.size()).isEqualTo(5)

    assertThat(shiftedBy0.valueAt(0)).isEqualTo("A")
    assertThat(shiftedBy0.valueAt(1)).isEqualTo("B")
    assertThat(shiftedBy0.valueAt(2)).isEqualTo("C")
    assertThat(shiftedBy0.valueAt(3)).isEqualTo("D")
    assertThat(shiftedBy0.valueAt(4)).isEqualTo("E")

    val visited = buildList {
      shiftedBy1.fastForEachIndexed { index, value ->
        add(index to value)
      }
    }
    assertThat(visited).containsExactly(0 to "E", 1 to "A", 2 to "B", 3 to "C", 4 to "D")


    assertThat(shiftedBy1.mapped2Original(0)).isEqualTo(4) //wraps around
    assertThat(shiftedBy1.mapped2Original(1)).isEqualTo(0)
    assertThat(shiftedBy1.mapped2Original(2)).isEqualTo(1)
    assertThat(shiftedBy1.mapped2Original(3)).isEqualTo(2)
    assertThat(shiftedBy1.mapped2Original(4)).isEqualTo(3)

    assertThat(shiftedBy1.valueAt(0)).isEqualTo("E")
    assertThat(shiftedBy1.valueAt(1)).isEqualTo("A")
    assertThat(shiftedBy1.valueAt(2)).isEqualTo("B")
    assertThat(shiftedBy1.valueAt(3)).isEqualTo("C")
    assertThat(shiftedBy1.valueAt(4)).isEqualTo("D")

    //A shift by the size is a full rotation - the values are unchanged
    assertThat(shiftedBy5.toList()).containsExactly("A", "B", "C", "D", "E")

    assertThat(shiftedBy77.valueAt(0)).isEqualTo("D")
    assertThat(shiftedBy77.valueAt(1)).isEqualTo("E")
    assertThat(shiftedBy77.valueAt(2)).isEqualTo("A")
    assertThat(shiftedBy77.valueAt(3)).isEqualTo("B")
    assertThat(shiftedBy77.valueAt(4)).isEqualTo("C")
  }

  @Test
  fun `empty provider stays empty`() {
    val shifted = SizedProvider.empty<String>().shiftedBy(3)

    assertThat(shifted.size()).isEqualTo(0)
    assertThat(shifted.toList()).isEmpty()
  }

  @Test
  fun `single element provider is unaffected by the shift`() {
    val shifted = SizedProvider.single("A").shiftedBy(7)

    assertThat(shifted.size()).isEqualTo(1)
    assertThat(shifted.mapped2Original(0)).isEqualTo(0)
    assertThat(shifted.valueAt(0)).isEqualTo("A")
  }

  @Test
  fun `the mapping is recalculated when the delegate size changes`() {
    var elements = listOf("A", "B", "C", "D", "E")
    val shifted = SizedProvider.forListProvider { elements }.shiftedBy(2)

    assertThat(shifted.toList()).containsExactly("D", "E", "A", "B", "C")

    elements = listOf("A", "B", "C")
    assertThat(shifted.size()).isEqualTo(3)
    assertThat(shifted.toList()).containsExactly("B", "C", "A")

    elements = listOf("A", "B", "C", "D", "E", "F", "G")
    assertThat(shifted.size()).isEqualTo(7)
    assertThat(shifted.toList()).containsExactly("F", "G", "A", "B", "C", "D", "E")
  }
}
