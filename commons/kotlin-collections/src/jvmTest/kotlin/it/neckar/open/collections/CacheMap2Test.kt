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
 *
 */
@Suppress("DEPRECATION")
class CacheMap2Test {
  @Test
  internal fun testRemove() {
    val cache = Cache<Int, String>(1)

    cache[1] = "1"
    assertThat(cache[1]).isEqualTo("1")

    assertThat(cache.remove(1)).isEqualTo("1")
    assertThat(cache[1]).isNull()

    assertThat(cache.size).isEqualTo(0)

    cache[2] = "1"
    cache[3] = "1"
    cache[4] = "1"
  }

  @Test
  internal fun testCache() {
    val cache = Cache<Int, String>(4)
    assertThat(cache.maxSize).isEqualTo(4)

    assertThat(cache[7]).isNull()

    cache.apply {
      set(7, "asdf")
      assertThat(get(7)).isEqualTo("asdf")
    }
  }

  @Test
  internal fun testEviction() {
    val cache = Cache<Int, String>(4)
    cache[0] = "0"
    assertThat(cache.size).isEqualTo(1)

    cache[1] = "1"
    assertThat(cache.size).isEqualTo(2)

    cache[2] = "2"
    assertThat(cache.size).isEqualTo(3)

    cache[3] = "3"
    assertThat(cache.size).isEqualTo(4)

    cache[4] = "4"
    assertThat(cache.size).isEqualTo(4)
    assertThat(cache[0]).isNull()
    assertThat(cache[1]).isEqualTo("1")
    assertThat(cache[2]).isEqualTo("2")
    assertThat(cache[3]).isEqualTo("3")
    assertThat(cache[4]).isEqualTo("4")
  }

  @Test
  internal fun testGetOrput() {
    val cache = Cache<Int, String>(4)

    assertThat(cache[0]).isNull()
    assertThat(cache.getOrStore(0) { "A" }).isEqualTo("A")
    assertThat(cache[0]).isEqualTo("A")
    assertThat(cache.getOrStore(0) { "B" }).isEqualTo("A")
  }

}
