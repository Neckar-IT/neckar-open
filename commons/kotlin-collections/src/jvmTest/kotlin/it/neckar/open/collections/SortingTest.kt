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

class SortingTest {
  @Test
  fun testSortAlso() {
    val array = IntArrayList(1, 2, 3, 4)

    val zIndices = DoubleArrayList(7.0, 2.0, 3.0, 0.5)
    assertThat(zIndices.toDoubleArray()).containsExactly(7.0, 2.0, 3.0, 0.5)

    zIndices.sortAlso { indexL: Int, indexR: Int ->
      array.swap(indexL, indexR)
    }

    assertThat(zIndices.toDoubleArray()).containsExactly(0.5, 2.0, 3.0, 7.0)
    assertThat(array.toIntArray()).containsExactly(4, 2, 3, 1)
  }

  @Test
  fun testSortListWithComparator() {
    val list = mutableListOf(MyEntry(5), MyEntry(6), MyEntry(3))
    //list.sortWith(MyEntry.byZIndex)
    list.genericSort()

    assertThat(list[0].index).isEqualTo(3)
    assertThat(list[1].index).isEqualTo(5)
    assertThat(list[2].index).isEqualTo(6)
  }

  private data class MyEntry(val index: Int) : Comparable<MyEntry> {
    override fun compareTo(other: MyEntry): Int {
      return this.index.compareTo(other.index)
    }

    companion object {
      val byZIndex: Comparator<MyEntry> = Comparator { o1, o2 -> o1.index.compareTo(o2.index) }
    }
  }
}

private fun DoubleArrayList.sortAlso(start: Int = 0, end: Int = size, reversed: Boolean = false, swap: (indexL: Int, indexR: Int) -> Unit) {
  val sortOps = DoubleArrayListSortOpsAlso(swap)
  genericSort(this, start, end - 1, sortOps, reversed)
}

private class DoubleArrayListSortOpsAlso(
  val swap: (indexL: Int, indexR: Int) -> Unit,
) : SortOps<DoubleArrayList>() {
  override fun compare(subject: DoubleArrayList, l: Int, r: Int): Int = subject.getAt(l).compareTo(subject.getAt(r))

  override fun swap(subject: DoubleArrayList, indexL: Int, indexR: Int) {
    val l = subject.getAt(indexL)
    val r = subject.getAt(indexR)
    subject[indexR] = l
    subject[indexL] = r

    swap(indexL, indexR)
  }
}
