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

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import org.junit.jupiter.api.Test

class SelectionTest {

  @Test
  fun `empty selection is empty`() {
    val selection = TestSelection(entries = emptyList())

    assertThat(selection.isEmpty).isTrue()
    assertThat(selection.isNotEmpty).isFalse()
    assertThat(selection.totalAmount).isEqualTo(0)
    assertThat(selection.items).isEmpty()
    assertThat(selection.entriesNonEmpty).isEmpty()
  }

  @Test
  fun `selection where every entry has amount 0 is empty`() {
    val selection = TestSelection(
      entries = listOf(
        TestEntry(item = "apple", amount = 0),
        TestEntry(item = "banana", amount = 0),
      ),
    )

    assertThat(selection.isEmpty).isTrue()
    assertThat(selection.isNotEmpty).isFalse()
    assertThat(selection.totalAmount).isEqualTo(0)
    assertThat(selection.entriesNonEmpty).isEmpty()
    // items still includes both — items reflects entries, not entriesNonEmpty
    assertThat(selection.items).containsExactly("apple", "banana")
  }

  @Test
  fun `entriesNonEmpty filters out amount 0 entries`() {
    val apple = TestEntry(item = "apple", amount = 3)
    val banana = TestEntry(item = "banana", amount = 0)
    val cherry = TestEntry(item = "cherry", amount = 5)
    val selection = TestSelection(entries = listOf(apple, banana, cherry))

    assertThat(selection.entriesNonEmpty).containsExactly(apple, cherry)
  }

  @Test
  fun `totalAmount sums amounts and ignores amount 0 entries`() {
    val selection = TestSelection(
      entries = listOf(
        TestEntry(item = "apple", amount = 3),
        TestEntry(item = "banana", amount = 0),
        TestEntry(item = "cherry", amount = 5),
      ),
    )

    assertThat(selection.totalAmount).isEqualTo(8)
  }

  @Test
  fun `items returns all entry items in entry order`() {
    val selection = TestSelection(
      entries = listOf(
        TestEntry(item = "apple", amount = 3),
        TestEntry(item = "banana", amount = 0),
        TestEntry(item = "cherry", amount = 5),
      ),
    )

    assertThat(selection.items).containsExactly("apple", "banana", "cherry")
  }

  @Test
  fun `get returns the amount for an existing item`() {
    val selection = TestSelection(
      entries = listOf(
        TestEntry(item = "apple", amount = 3),
        TestEntry(item = "banana", amount = 0),
        TestEntry(item = "cherry", amount = 5),
      ),
    )

    assertThat(selection["apple"]).isEqualTo(3)
    assertThat(selection["banana"]).isEqualTo(0)
    assertThat(selection["cherry"]).isEqualTo(5)
  }

  @Test
  fun `get returns 0 for an item that is not in the selection`() {
    val selection = TestSelection(entries = listOf(TestEntry(item = "apple", amount = 3)))

    assertThat(selection["missing"]).isEqualTo(0)
  }

  @Test
  fun `selection with at least one positive amount entry is not empty`() {
    val selection = TestSelection(
      entries = listOf(
        TestEntry(item = "apple", amount = 0),
        TestEntry(item = "cherry", amount = 1),
      ),
    )

    assertThat(selection.isEmpty).isFalse()
    assertThat(selection.isNotEmpty).isTrue()
  }

  @Test
  fun `entry isEmpty is true for amount 0`() {
    val entry = TestEntry(item = "apple", amount = 0)

    assertThat(entry.isEmpty).isTrue()
    assertThat(entry.isNotEmpty).isFalse()
  }

  @Test
  fun `entry isNotEmpty is true for any positive amount`() {
    val entry = TestEntry(item = "apple", amount = 1)

    assertThat(entry.isEmpty).isFalse()
    assertThat(entry.isNotEmpty).isTrue()
  }

  /**
   * Minimal in-test implementation of [Selection] used to verify the default-method behaviour.
   */
  private data class TestSelection(
    override val entries: List<TestEntry>,
  ) : Selection<String, TestEntry>

  private data class TestEntry(
    override val item: String,
    override val amount: Int,
  ) : Selection.Entry<String>
}
