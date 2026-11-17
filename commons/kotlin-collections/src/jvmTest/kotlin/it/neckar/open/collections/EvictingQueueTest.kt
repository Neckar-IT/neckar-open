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

/**
 */
class EvictingQueueTest {
  @Test
  fun testIt() {
    val queue = EvictingQueue<Int>(2)

    assertThat(queue).hasSize(0)
    queue.add(77)
    assertThat(queue).hasSize(1)
    assertThat(queue).containsOnly(77)

    queue.add(78)
    assertThat(queue).hasSize(2)
    assertThat(queue).containsOnly(77, 78)

    queue.add(79)
    assertThat(queue).hasSize(2)
    assertThat(queue).containsOnly(78, 79)
  }

  @Test
  internal fun testAddAll() {
    val queue = EvictingQueue<Int>(3)

    assertThat(queue).hasSize(0)
    queue.add(5)
    queue.add(6)
    queue.add(7)
    assertThat(queue).containsOnly(5, 6, 7)

    //Empty
    queue.addAll(listOf())
    assertThat(queue).containsOnly(5, 6, 7)

    //1 element
    queue.addAll(listOf(11))
    assertThat(queue).containsOnly(6, 7, 11)

    //2 elements
    queue.addAll(listOf(21, 22))
    assertThat(queue).containsOnly(11, 21, 22)

    //3 elements
    queue.addAll(listOf(31, 32, 33))
    assertThat(queue).containsOnly(31, 32, 33)

    //4 elements
    queue.addAll(listOf(41, 42, 43, 44))
    assertThat(queue).containsOnly(42, 43, 44)
  }
}
