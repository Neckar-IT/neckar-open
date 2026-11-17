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
 * Pins the equals/hashCode contract for the collection types.
 *
 * Two equal instances must have the same hashCode. Before the fix, several
 * implementations included extra fields in equals (e.g. `free` lambda,
 * `comparator`, `itemsInPool`) that hashCode ignored — equal instances
 * could legitimately have different hashCodes and break HashSet/HashMap
 * lookups.
 */
class CollectionEqualsHashCodeContractTest {
  @Test
  fun `CacheMap equal instances have equal hashCode`() {
    val free: (Int, String) -> Unit = { _, _ -> }
    val a = CacheMap<Int, String>(maxSize = 16, free = free)
    val b = CacheMap<Int, String>(maxSize = 16, free = free)

    assertThat(a).isEqualTo(b)
    assertThat(a.hashCode()).isEqualTo(b.hashCode())
  }

  @Test
  fun `CacheMap with different free lambda is not equal`() {
    val a = CacheMap<Int, String>(maxSize = 16, free = { _, _ -> })
    val b = CacheMap<Int, String>(maxSize = 16, free = { _, _ -> })

    // Different lambda instances are not equal — hashCode may differ; equals must say false.
    assertThat(a == b).isFalse()
  }

  @Test
  fun `TGenPriorityQueue equal instances have equal hashCode`() {
    val cmp = Comparator<Int> { x, y -> x.compareTo(y) }
    val a = PriorityQueue(cmp).also { it.add(3); it.add(1); it.add(2) }
    val b = PriorityQueue(cmp).also { it.add(3); it.add(1); it.add(2) }

    assertThat(a).isEqualTo(b)
    assertThat(a.hashCode()).isEqualTo(b.hashCode())
  }

  @Test
  fun `Pool equal instances have equal hashCode`() {
    // Use a pooled type with value-based equals so two pools with the same
    // pushed elements compare equal.
    val a = Pool<String> { "id-$it" }.also { it.free(it.alloc()) }
    val b = Pool<String> { "id-$it" }.also { it.free(it.alloc()) }

    // Same single recycled element ("id-0") in each pool's stack.
    assertThat(a).isEqualTo(b)
    assertThat(a.hashCode()).isEqualTo(b.hashCode())
  }
}
