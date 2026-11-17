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
 */
class Array2Test {
  @Test
  fun testSimpleOrientation() {
    val array2 = IntArray2.withGen(3, 4) { x, y ->
      x * 100 + y
    }

    assertThat(array2.width).isEqualTo(3)
    assertThat(array2.height).isEqualTo(4)

    assertThat(array2.data[0]).isEqualTo(0) //x == 0, y == 0
    assertThat(array2.data[1]).isEqualTo(100) //x == 1, y == 0
    assertThat(array2.data[2]).isEqualTo(200) //x == 2, y == 0

    assertThat(array2.data[3]).isEqualTo(1) //x == 0, y == 1
    assertThat(array2.data[4]).isEqualTo(101) //x == 1, y == 1
    assertThat(array2.data[5]).isEqualTo(201) //x == 2, y == 1

    assertThat(array2.data[5]).isEqualTo(array2[2, 1])
  }

  @Test
  fun testIt() {
    val array2 = IntArray2.withGen(100, 50) { x, y ->
      x * 100 + y
    }

    assertThat(array2.width).isEqualTo(100)
    assertThat(array2.height).isEqualTo(50)

    assertThat(array2[3, 4]).isEqualTo(3 * 100 + 4)
  }

  @Test
  fun `a smaller height keeps the rows from the top`() {
    val array = IntArray2.withGen(2, 3) { x, y ->
      x * 10 + y
    }

    val shrunk = array.withHeight(1)

    assertThat(shrunk.width).isEqualTo(2)
    assertThat(shrunk.height).isEqualTo(1)
    assertThat(shrunk[0, 0]).isEqualTo(0)
    assertThat(shrunk[1, 0]).isEqualTo(10)
  }

  @Test
  fun testResize() {
    val array1 = IntArray2.withGen(100, 50) { x, y ->
      x * 100 + y
    }

    assertThat(array1.width).isEqualTo(100)
    assertThat(array1.height).isEqualTo(50)

    assertThat(array1[99, 49]).isEqualTo(99 * 100 + 49)

    val array2 = array1.withHeight(51)
    assertThat(array2.width).isEqualTo(100)
    assertThat(array2.height).isEqualTo(51)

    //Check the old array has not been changed
    assertThat(array1[0, 0]).isEqualTo(0)
    assertThat(array1[1, 0]).isEqualTo(100)
    assertThat(array1[0, 1]).isEqualTo(1)
    assertThat(array1[1, 1]).isEqualTo(101)
    assertThat(array1[99, 49]).isEqualTo(99 * 100 + 49)

    //Array 2
    assertThat(array2[0, 0]).isEqualTo(0)
    assertThat(array2[1, 0]).isEqualTo(100)
    assertThat(array2[0, 1]).isEqualTo(1)
    assertThat(array2[99, 49]).isEqualTo(99 * 100 + 49)

    //New value - uninitialized
    assertThat(array2[99, 50]).isEqualTo(0)
  }

  @Test
  fun `invoke with marginChar strips the leading margin character`() {
    // Each line starts with the margin char '|', which must be removed before the grid is built.
    val array = Array2("|ab\n|cd", marginChar = '|') { c, _, _ -> c }

    assertThat(array.width).isEqualTo(2)
    assertThat(array.height).isEqualTo(2)
    assertThat(array[0, 0]).isEqualTo('a')
    assertThat(array[1, 0]).isEqualTo('b')
    assertThat(array[0, 1]).isEqualTo('c')
    assertThat(array[1, 1]).isEqualTo('d')
  }
}
