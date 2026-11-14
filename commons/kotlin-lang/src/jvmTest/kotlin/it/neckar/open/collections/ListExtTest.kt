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
import assertk.assertions.isEqualTo
import org.junit.jupiter.api.Test

class ListExtTest {
  private val list = listOf("A", "B", "C")

  @Test
  fun `shifted rotates left for a positive shift`() {
    assertThat(list.shifted(1)).isEqualTo(listOf("B", "C", "A"))
    assertThat(list.shifted(2)).isEqualTo(listOf("C", "A", "B"))
    assertThat(list.shifted(3)).isEqualTo(list)
    assertThat(list.shifted(4)).isEqualTo(listOf("B", "C", "A"))
  }

  @Test
  fun `shifted rotates the other way for a negative shift`() {
    assertThat(list.shifted(-1)).isEqualTo(listOf("C", "A", "B"))
    assertThat(list.shifted(-2)).isEqualTo(listOf("B", "C", "A"))
    assertThat(list.shifted(-3)).isEqualTo(list)
    assertThat(list.shifted(-4)).isEqualTo(listOf("C", "A", "B"))
  }

  @Test
  fun `shifted returns the same list when empty`() {
    assertThat(emptyList<String>().shifted(3)).isEqualTo(emptyList())
  }
}
