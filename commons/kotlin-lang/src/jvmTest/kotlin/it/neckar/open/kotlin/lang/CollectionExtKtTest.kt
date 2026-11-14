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
package it.neckar.open.kotlin.lang

import assertk.*
import assertk.assertions.*
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import kotlin.system.measureTimeMillis

class CollectionExtKtTest {
  @Test
  fun `abbreviated joins everything while it fits`() {
    assertThat(listOf("a", "b", "c").abbreviated(maxReported = 5, noun = "value")).isEqualTo("a, b, c")
  }

  @Test
  fun `abbreviated counts what it leaves out`() {
    assertThat(listOf("a", "b", "c").abbreviated(maxReported = 2, noun = "value")).isEqualTo("a, b, and 1 more value")
  }

  @Test
  fun `abbreviated pluralises the remainder`() {
    assertThat(listOf("a", "b", "c", "d").abbreviated(maxReported = 2, noun = "value")).isEqualTo("a, b, and 2 more values")
  }

  @Test
  fun `abbreviated joins a collection of exactly the limit without counting`() {
    assertThat(listOf("a", "b").abbreviated(maxReported = 2, noun = "value")).isEqualTo("a, b")
  }

  @Test
  fun `abbreviated rejects a limit that would report nothing`() {
    assertFailure { listOf("a").abbreviated(maxReported = 0, noun = "value") }
      .isInstanceOf(IllegalArgumentException::class)
      .messageContains("maxReported")
  }

  @Test
  fun `toRelativeValues normalizes so the sum is 1_0`() {
    val relatives = listOf(1.0, 1.0, 2.0).toRelativeValues()
    assertThat(relatives).containsExactly(0.25, 0.25, 0.5)
    assertThat(relatives.sum()).isEqualTo(1.0)
  }

  @Test
  fun `toRelativeValues fails when sum is zero`() {
    assertFailure {
      listOf(0.0, 0.0, 0.0).toRelativeValues()
    }.messageContains("sum 0.0")
  }

  @Test
  fun `toRelativeValues fails on empty list`() {
    assertFailure {
      emptyList<Double>().toRelativeValues()
    }.messageContains("sum 0.0")
  }

  @Test
  fun testCovariance() {
    val listOf = listOf("a", "b", "c")

    val predicateCharSequence: (CharSequence) -> Boolean = {
      it.length > 3
    }
    val predicateString: (String) -> Boolean = {
      it.length > 3
    }

    assertThat(listOf.filter(predicateCharSequence)).isEmpty()
    assertThat(listOf.filter(predicateString)).isEmpty()
  }

  @Test
  fun testLinkedHashSet() {
    val set = LinkedHashSet<Int>()
    assertThat(set).isEmpty()

    set.add(7)
    set.add(8)
    set.add(9)
    assertThat(set).containsExactlyInAnyOrder(7, 8, 9)
    set.add(8)
    assertThat(set).containsExactlyInAnyOrder(7, 8, 9)

    assertThat(set).hasSize(3)
    assertThat(set.first()).isEqualTo(7)
    assertThat(set).containsExactlyInAnyOrder(7, 8, 9)

    set.removeFirst()

    assertThat(set).hasSize(2)
    assertThat(set).containsExactlyInAnyOrder(8, 9)
    assertThat(set.first()).isEqualTo(8)

    set.add(7)
    assertThat(set).containsExactlyInAnyOrder(8, 9, 7)
  }

  @Disabled
  @Test
  fun testPerformanceSet() {
    val count = 1_000_000

    val set = mutableSetOf<Int>()

    measureTimeMillis {
      count.fastFor {
        set.add(it)
      }
    }.also {
      println("Took $it ms") //67
    }

    assertThat(set).hasSize(count)
  }

  @Disabled
  @Test
  fun testPerformanceLinkedSet() {
    val count = 1_000_000

    val set = LinkedHashSet<Int>()

    measureTimeMillis {
      count.fastFor {
        set.add(it)
      }
    }.also {
      println("Took $it ms") //59
    }

    assertThat(set).hasSize(count)
  }
}
