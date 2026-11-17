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

class ExtensionsKtTest {
  @Test
  fun testInvoke() {

    IntArray2.invokeCols(
      arrayOf(
        intArrayOf(1, 2, 3),
        intArrayOf(10, 20, 30),
        intArrayOf(100, 200, 300),
        intArrayOf(1000, 2000, 3000)
      )
    ).let {
      assertThat(it.width).isEqualTo(4)
      assertThat(it.height).isEqualTo(3)

      assertThat(it[0, 0]).isEqualTo(1)
      assertThat(it[0, 1]).isEqualTo(2)
      assertThat(it[0, 2]).isEqualTo(3)

      assertThat(it[1, 0]).isEqualTo(10)
      assertThat(it[1, 1]).isEqualTo(20)
      assertThat(it[1, 2]).isEqualTo(30)
    }
  }
}
