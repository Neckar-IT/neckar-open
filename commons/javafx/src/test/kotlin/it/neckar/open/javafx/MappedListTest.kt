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

package it.neckar.open.javafx

import javafx.collections.FXCollections
import javafx.collections.ListChangeListener
import javafx.collections.ObservableList
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.ArrayDeque
import java.util.Queue

/**
 */
class MappedListTest {
  private lateinit var inputs: ObservableList<String>
  private lateinit var outputs: ObservableList<String>
  private lateinit var changes: Queue<ListChangeListener.Change<out String>>

  @BeforeEach
  fun setup() {
    inputs = FXCollections.observableArrayList()
    outputs = MappedList(inputs) { str: String? -> "Hello $str" }
    changes = ArrayDeque()
    outputs.addListener(ListChangeListener<String> { changes.add(it) })
  }

  @Test
  fun add() {
    assertEquals(0, outputs.size)
    inputs.add("Mike")

    val change = change
    assertTrue(change.wasAdded())
    assertEquals("Hello Mike", change.addedSubList[0])
    assertEquals(1, outputs.size)
    assertEquals("Hello Mike", outputs[0])
    inputs.removeAt(0)
    assertEquals(0, outputs.size)
  }

  private val change: ListChangeListener.Change<out String?>
    get() {
      val change = changes.poll()
      change.next()
      return change
    }

  @Test
  fun remove() {
    inputs.add("Mike")
    inputs.add("Dave")
    inputs.add("Katniss")
    change
    change
    change
    assertEquals("Hello Mike", outputs[0])
    assertEquals("Hello Dave", outputs[1])
    assertEquals("Hello Katniss", outputs[2])
    inputs.removeAt(0)
    val change = change
    assertTrue(change.wasRemoved())
    assertEquals(2, outputs.size)
    assertEquals(1, change.removedSize)
    assertEquals("Hello Mike", change.removed[0])
    assertEquals("Hello Dave", outputs[0])
    inputs.removeAt(1)
    assertEquals(1, outputs.size)
    assertEquals("Hello Dave", outputs[0])
  }

  @Test
  @Throws(Exception::class)
  fun replace() {
    inputs.add("Mike")
    inputs.add("Dave")
    change
    change
    inputs[0] = "Bob"
    assertEquals("Hello Bob", outputs[0])
    val change = change
    assertTrue(change.wasReplaced())
    assertEquals("Hello Mike", change.removed[0])
    assertEquals("Hello Bob", change.addedSubList[0])
  } // Could also test permutation here if I could figure out how to actually apply one!
}
