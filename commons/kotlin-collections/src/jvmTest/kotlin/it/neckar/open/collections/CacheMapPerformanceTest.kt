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
import it.neckar.open.kotlin.lang.fastFor
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.util.Collections
import kotlin.system.measureTimeMillis
import kotlin.time.measureTime

class CacheMapPerformanceTest {
  @Disabled
  @Test
  fun testIt() {
    val cacheSize = 10_000
    val cacheMap = CacheMap<Foo, Bar>(cacheSize)

    val count = 100_000

    measureTimeMillis {
      count.fastFor {
        cacheMap[Foo(it)] = Bar(it.toString())
      }
    }.also {
      println("Add took $it ms")
    }

    assertThat(cacheMap).hasSize(cacheSize)

    measureTimeMillis {
      while (cacheMap.isNotEmpty()) {
        val firstKey = cacheMap.keys.first()
        cacheMap.remove(firstKey)
      }
    }.also {
      println("Removing took $it ms")
    }

    assertThat(cacheMap).isEmpty()
  }

  @Disabled
  @Test
  fun testLinkedHashMap() {
    val cacheMap = LinkedHashMap<Foo, Bar>()

    val count = 2_000_000

    measureTime {
      count.fastFor {
        cacheMap[Foo(it)] = Bar(it.toString())
      }
    }.also {
      println("Add took ${it.inWholeMilliseconds} ms")
    }

    assertThat(cacheMap).hasSize(count)

    measureTime {
      while (cacheMap.isNotEmpty()) {
        val firstKey = cacheMap.keys.first()
        cacheMap.remove(firstKey)
      }
    }.also {
      println("Removing took ${it.inWholeMilliseconds} ms")
    }

    assertThat(cacheMap).isEmpty()
  }

  @Disabled
  @Test
  fun testSynchronized() {
    val cacheMap = Collections.synchronizedMap(LinkedHashMap<Foo, Bar>())

    val count = 2_000_000

    measureTime {
      count.fastFor {
        cacheMap[Foo(it)] = Bar(it.toString())
      }
    }.also {
      println("Add took ${it.inWholeMilliseconds} ms")
    }

    assertThat(cacheMap).hasSize(count)

    measureTime {
      while (cacheMap.isNotEmpty()) {
        val firstKey = cacheMap.keys.first()
        cacheMap.remove(firstKey)
      }
    }.also {
      println("Removing took ${it.inWholeMilliseconds} ms")
    }

    assertThat(cacheMap).isEmpty()
  }

  private data class Foo(val index: Int)
  private data class Bar(val value: String)
}
