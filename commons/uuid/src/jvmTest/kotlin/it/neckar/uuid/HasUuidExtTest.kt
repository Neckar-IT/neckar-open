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
package it.neckar.uuid

import assertk.*
import assertk.assertions.*
import it.neckar.open.http.UrlParameterName
import it.neckar.open.http.UrlPattern
import org.junit.jupiter.api.Test
import kotlin.uuid.Uuid

class HasUuidExtTest {
  private data class Item(override val uuid: Uuid, val payload: String = "") : HasUuid {
    // Important: explicit non-UUID toString — emulates real domain types whose
    // toString does not equal the UUID. Earlier code accidentally used this
    // representation in URL resolution.
    override fun toString(): String = "Item($payload)"
  }

  @Test
  fun `Relative3 resolve uses the uuid of the third parameter, not its toString`() {
    val pattern = UrlPattern.relative(
      "api/foo/{a}/bar/{b}/baz/{c}",
      UrlParameterName("a"),
      UrlParameterName("b"),
      UrlParameterName("c"),
    )

    val a = Item(randomUuid4(), "a")
    val b = Item(randomUuid4(), "b")
    val c = Item(randomUuid4(), "c")

    val resolved = pattern.resolve(a, b, c).value

    assertThat(resolved).isEqualTo("api/foo/${a.uuid}/bar/${b.uuid}/baz/${c.uuid}")
    // None of the toString outputs ("Item(...)") may leak into the URL.
    assertThat(resolved).doesNotContain("Item(")
  }

  @Test
  fun `withReplaced(list) replaces existing elements and appends unknown ones`() {
    val keep = Item(randomUuid4(), "keep")
    val replaceMe = Item(randomUuid4(), "old")
    val original = listOf(keep, replaceMe)

    val updatedReplaceMe = Item(replaceMe.uuid, "new")
    val newItem = Item(randomUuid4(), "added")

    // Order matters: the first update is for an existing element, the second
    // is unknown. Before the fix, the unknown element triggered a non-local
    // return that discarded the already-applied replacement.
    val result = original.withReplaced(listOf(updatedReplaceMe, newItem))

    assertThat(result).containsExactly(keep, updatedReplaceMe, newItem)
  }

  @Test
  fun `withReplaced(list) processes all updates when none are unknown`() {
    val a = Item(randomUuid4(), "a")
    val b = Item(randomUuid4(), "b")
    val original = listOf(a, b)

    val newA = Item(a.uuid, "a-new")
    val newB = Item(b.uuid, "b-new")

    val result = original.withReplaced(listOf(newA, newB))

    assertThat(result).containsExactly(newA, newB)
  }

  @Test
  fun `withReplaced(uuid, element) appends when uuid is unknown`() {
    val keep = Item(randomUuid4(), "keep")
    val newItem = Item(randomUuid4(), "added")

    val result = listOf(keep).withReplaced(newItem.uuid, newItem)

    assertThat(result).containsExactly(keep, newItem)
  }
}
