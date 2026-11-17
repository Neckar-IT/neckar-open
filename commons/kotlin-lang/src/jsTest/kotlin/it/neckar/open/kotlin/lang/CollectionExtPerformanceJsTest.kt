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
package it.neckar.open.kotlin.lang

import assertk.*
import assertk.assertions.*
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.time.measureTime

class CollectionExtPerformanceJsTest {
  @Test
  fun testLinkedHashSet() {
    val set = LinkedHashSet<Int>()
    assertThat(set).isEmpty()

    set.add(7)
    set.add(8)
    set.add(9)
    assertThat(set).containsExactlyInAnyOrder(7, 8, 9)
    set.add(8)
    assertThat(set).containsExactlyInAnyOrder(7, 8, 9)

    assertThat(set).hasSize(3)
    assertThat(set.first()).isEqualTo(7)
    assertThat(set).containsExactlyInAnyOrder(7, 8, 9)

    set.removeFirst()

    assertThat(set).hasSize(2)
    assertThat(set).containsExactlyInAnyOrder(8, 9)
    assertThat(set.first()).isEqualTo(8)

    set.add(7)
    assertThat(set).containsExactlyInAnyOrder(8, 9, 7)
  }


  @Ignore
  @Test()
  fun testPerformanceSet() {
    val count = 1_000_000

    val set = mutableSetOf<Int>()

    measureTime {
      count.fastFor {
        set.add(it)
      }
    }.also {
      println("Took $it") //258
    }

    assertThat(set).hasSize(count)
  }

  @Ignore
  @Test
  fun testPerformanceLinkedSet() {
    val count = 1_000_000

    val set = LinkedHashSet<Int>()

    measureTime {
      count.fastFor {
        set.add(it)
      }
    }.also {
      println("Took $it") //174
    }

    assertThat(set).hasSize(count)
  }
}
