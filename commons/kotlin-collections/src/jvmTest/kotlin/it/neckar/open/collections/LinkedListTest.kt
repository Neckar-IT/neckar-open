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
import kotlin.test.assertEquals

internal class LinkedListTest {

  @Test
  fun `an element can be hashed and compared although its links are cyclic`() {
    val list = LinkedList<String>()
    list.addLast("a")
    list.addLast("b")

    val first: Element<String> = list.first!!

    assertThat(first.hashCode()).isEqualTo(first.hashCode())
    assertThat(first == list.first).isTrue()
    assertThat(first == list.last).isFalse()
  }

  @Test
  fun testIt() {

    val list = LinkedList<String>()
    assertEquals(0, list.getLength())

    list.addLast("last")
    assertEquals(1, list.getLength())

    list.clear()
    assertEquals(0, list.getLength())

    list.addFirst("first")
    list.addLast("last")
    assertEquals(2, list.getLength())

    list.clear()

    for (i in 1..100) {
      list.addLast("entry$i")
      assertEquals(i, list.getLength())
    }

    for (i in 1..95) {
      assertEquals("entry$i", list.pollFirst())
    }
    assertEquals(5, list.getLength())

    list.clear()

    for (i in 1..10) {
      list.addLast("entry$i")
    }

    for (i in 10 downTo 3) {
      assertEquals("entry$i", list.pollLast())
    }
    assertEquals(2, list.getLength())
  }


  @Test
  internal fun testIt2() {
    val list = LinkedList<String>()
    assertThat(list.first).isNull()
    assertThat(list.firstValue).isNull()
    assertThat(list.last).isNull()
    assertThat(list.lastValue).isNull()

    list.addFirst("a")
    assertThat(list.first).isNotNull()
    assertThat(list.firstValue).isEqualTo("a")
    assertThat(list.last).isNotNull()
    assertThat(list.lastValue).isEqualTo("a")
  }
}
