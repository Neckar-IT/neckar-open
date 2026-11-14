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
import kotlin.test.assertFailsWith

/**
 */
class DoubleArrayListTest {
  @Test
  fun testSet() {
    val list = DoubleArrayList(0)
    assertThat(list.size).isEqualTo(0)

    list.set(doubleArrayOf(1.0, 1.1, 1.2))

    assertThat(list.size).isEqualTo(3)
    assertThat(list.toDoubleArray()).containsExactly(1.0, 1.1, 1.2)
  }

  @Test
  fun testAdd() {
    val list = DoubleArrayList(0)
    assertThat(list.size).isEqualTo(0)
    assertFailsWith<IndexOutOfBoundsException> {
      list[0]
    }

    list[0] = 17.0
    assertThat(list[0]).isEqualTo(17.0)
    assertThat(list.size).isEqualTo(1)
  }

  @Test
  fun testSortInline() {
    val list = DoubleArrayList(5)
    list.add(1.0)
    list.add(3.0)
    list.add(2.0)

    assertThat(list.size).isEqualTo(3)
    assertThat(list.contains(1.0)).isTrue()
    assertThat(list.contains(3.0)).isTrue()
    assertThat(list.contains(2.0)).isTrue()

    val sorted = list.sort()

    assertThat(sorted).isSameAs(list)
    assertThat(sorted.size).isEqualTo(3)

    assertThat(sorted.indexOf(1.0)).isEqualTo(0)
    assertThat(sorted.indexOf(2.0)).isEqualTo(1)
    assertThat(sorted.indexOf(3.0)).isEqualTo(2)

  }
}
