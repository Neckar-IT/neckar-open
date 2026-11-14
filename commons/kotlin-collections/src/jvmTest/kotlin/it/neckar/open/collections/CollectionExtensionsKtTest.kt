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
import it.neckar.open.kotlin.lang.consumeUntil
import it.neckar.open.kotlin.lang.consumeWhile
import it.neckar.open.kotlin.lang.getModulo
import it.neckar.open.kotlin.lang.withNullAtFirst
import org.junit.jupiter.api.Test

class CollectionExtensionsKtTest {
  @Test
  fun name() {
    assertThat(listOf(1, 2, 3).withNullAtFirst()).containsExactly(null, 1, 2, 3)
  }

  @Test
  fun testConsumeUntil2() {
    val list = listOf(1, 2, 3, 4, 5)

    val consumeUntil = list.consumeUntil("3") {
      it.toString()
    } ?: "null"


  }

  @Test
  fun testModulo() {
    val elements = listOf("0", "1", "2", "3")

    assertThat(elements.getModulo(0)).isEqualTo("0")
    assertThat(elements.getModulo(1)).isEqualTo("1")
    assertThat(elements.getModulo(2)).isEqualTo("2")
    assertThat(elements.getModulo(3)).isEqualTo("3")
    assertThat(elements.getModulo(4)).isEqualTo("0")
    assertThat(elements.getModulo(400)).isEqualTo("0")
    assertThat(elements.getModulo(401)).isEqualTo("1")

    assertThat(elements.getModulo(-1)).isEqualTo("3")
  }

  @Test
  fun testConsumeWhile() {
    val list = listOf(1, 2, 3, 4, 5)
    var count = 0
    list.consumeWhile {
      if (it < 4) {
        ++count
        true
      } else {
        false
      }
    }
    assertThat(count).isEqualTo(3)
  }

  @Test
  fun testConsumeUntil() {
    val list = listOf(1, 2, 3, 4, 5)
    var count = 0
    val cancelled = list.consumeUntil {
      if (it < 4) {
        ++count
        false
      } else {
        true
      }
    }
    assertThat(cancelled).isTrue()
    assertThat(count).isEqualTo(3)
  }

}
