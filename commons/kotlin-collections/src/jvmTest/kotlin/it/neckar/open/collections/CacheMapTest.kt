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
import assertk.assertions.support.*
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

/**
 */
@Suppress("DEPRECATION")
class CacheMapTest {
  @Test
  fun test() {
    val freeLog = arrayListOf<String>()
    val cache = Cache<String, Int>(maxSize = 2) { k, v ->
      freeLog += "$k:$v"
    }
    cache["a"] = 1
    cache["b"] = 2
    cache["c"] = 3
    assertEquals("{b=2, c=3}", cache.toString())
    assertEquals("a:1", freeLog.joinToString(", "))

    assertEquals(false, "a" in cache)
    assertEquals(true, "b" in cache)
    assertEquals(true, "c" in cache)

    assertEquals(2, cache.getOrStore("b") { 20 })
    assertEquals(10, cache.getOrStore("a") { 10 })
    assertEquals(3, cache.getOrStore("d") { 3 })

    cache["aa"] = 1
    cache["bb"] = 2
    cache["cc"] = 3

    assertEquals("{bb=2, cc=3}", cache.toString())
  }

  @Test
  fun testRemoveIf() {
    val freeLog = arrayListOf<String>()
    val cache = Cache<String, Int>(maxSize = 4) { k, v ->
      "$k:$v"
    }

    assertThat(freeLog).isEmpty()

    cache["a"] = 1
    cache["b"] = 2
    cache["c"] = 3
    cache["d"] = 4

    assertThat(cache).hasSize(4)
    assertThat(cache["a"]).isEqualTo(1)
    assertThat(cache["b"]).isEqualTo(2)
    assertThat(cache["c"]).isEqualTo(3)

    cache.removeIf {
      it == "a" || it == "c"
    }

    assertThat(cache).hasSize(2)

    assertThat(cache["a"]).isNull()
    assertThat(cache["b"]).isEqualTo(2)
    assertThat(cache["c"]).isNull()
  }

  @Test
  fun `removeIf frees every entry it removes`() {
    val freed = mutableListOf<String>()
    val cache = Cache<String, Int>(maxSize = 4) { k, v -> freed += "$k:$v" }

    cache["a"] = 1
    cache["b"] = 2
    cache["c"] = 3

    cache.removeIf { it == "a" || it == "c" }

    assertThat(freed).containsExactly("a:1", "c:3")
  }

  @Test
  fun `updateMaxSize keeps exactly maxSize entries`() {
    val map = CacheMap<Int, Int>(maxSize = 20)
    repeat(20) { map[it] = it }
    assertThat(map.size).isEqualTo(20)

    map.updateMaxSize(10)
    assertThat(map.size).isEqualTo(10)
  }

  @Test
  fun `maxSize zero is rejected by the constructor`() {
    assertFailure { CacheMap<Int, Int>(maxSize = 0) }
      .isInstanceOf<IllegalArgumentException>()
      .hasMessage("A cache keeps at least one entry, got maxSize 0")
  }

  @Test
  fun `maxSize zero is rejected by updateMaxSize and keeps the entries`() {
    val map = CacheMap<Int, Int>(maxSize = 5)
    repeat(5) { map[it] = it }

    assertFailure { map.updateMaxSize(0) }
      .isInstanceOf<IllegalArgumentException>()
      .hasMessage("A cache keeps at least one entry, got maxSize 0")
    assertThat(map.maxSize).isEqualTo(5)
    assertThat(map.size).isEqualTo(5)
  }
}

private fun Assert<Cache<*, *>>.hasSize(expectedSize: Int) = given { actual ->
  if (actual.size == expectedSize) return@given
  expected("size to be $expectedSize was: ${show(actual.size)} for ${show(actual)}")
}

