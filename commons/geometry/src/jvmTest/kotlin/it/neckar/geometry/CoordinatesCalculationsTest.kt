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
package it.neckar.geometry

import assertk.*
import assertk.assertions.*
import it.neckar.geometry.Coordinates
import org.junit.jupiter.api.Test
import kotlin.math.sqrt

class CoordinatesCalculationsTest {
  @Test
  fun testIt() {
    assertThat(Coordinates.origin.distanceToLine(Coordinates.origin, Coordinates.origin)).isZero()
  }

  @Test
  fun `distanceToLine before`() {
    val lineStart = Coordinates(0.0, 1.0)
    val lineEnd = Coordinates(4.0, 1.0)

    assertThat(Coordinates(-2.0, 1.0).distanceToLine(lineStart, lineEnd)).isEqualTo(2.0) //left of line
    assertThat(Coordinates(5.0, 1.0).distanceToLine(lineStart, lineEnd)).isEqualTo(1.0) //right of line
    assertThat(Coordinates(6.0, 1.0).distanceToLine(lineStart, lineEnd)).isEqualTo(2.0) //right of line
    assertThat(Coordinates(0.0, 2.0).distanceToLine(lineStart, lineEnd)).isEqualTo(1.0) //above the line

    assertThat(Coordinates(-2.0, 3.0).distanceToLine(lineStart, lineEnd)).isEqualTo(sqrt(2.0 * 2.0 + 2.0 * 2.0)) //above, left
  }

  @Test
  fun `distanceToLine before2`() {
    val lineStart = Coordinates(1.0, 1.0)
    val lineEnd = Coordinates(4.0, 1.0)

    assertThat(Coordinates(-2.0, 1.0).distanceToLine(lineStart, lineEnd)).isEqualTo(3.0) //left of line
    assertThat(Coordinates(5.0, 1.0).distanceToLine(lineStart, lineEnd)).isEqualTo(1.0) //right of line
    assertThat(Coordinates(6.0, 1.0).distanceToLine(lineStart, lineEnd)).isEqualTo(2.0) //right of line
    assertThat(Coordinates(1.0, 2.0).distanceToLine(lineStart, lineEnd)).isEqualTo(1.0) //above the line

    assertThat(Coordinates(-1.0, 3.0).distanceToLine(lineStart, lineEnd)).isEqualTo(sqrt(2.0 * 2.0 + 2.0 * 2.0)) //above, left
  }

  @Test
  fun `distanceToLine should return 0 for point on line`() {
    val lineStart = Coordinates(0.0, 0.0)
    val lineEnd = Coordinates(4.0, 0.0)

    assertThat(Coordinates(2.0, 0.0).distanceToLine(lineStart, lineEnd)).isZero()
  }

  @Test
  fun `distanceToLine should return correct distance for point above line`() {
    val lineStart = Coordinates(0.0, 0.0)
    val lineEnd = Coordinates(4.0, 0.0)

    assertThat(Coordinates(2.0, 2.0).distanceToLine(lineStart, lineEnd)).isEqualTo(2.0)
  }

  @Test
  fun `distanceToLine should return correct distance for point below line`() {
    val lineStart = Coordinates(0.0, 4.0)
    val lineEnd = Coordinates(4.0, 4.0)

    assertThat(Coordinates(2.0, 2.0).distanceToLine(lineStart, lineEnd)).isEqualTo(2.0)
  }

  @Test
  fun `testDiagonal`() {
    val lineStart = Coordinates(0.0, 0.0)
    val lineEnd = Coordinates(4.0, 4.0)

    assertThat(Coordinates(2.0, 2.0).distanceToLine(lineStart, lineEnd)).isZero()
    assertThat(Coordinates(2.0, 1.0).distanceToLine(lineStart, lineEnd)).isCloseTo(sqrt(2.0) / 2.0, 0.00000001)
  }

  @Test
  fun `distanceToLine should return correct distance for point not aligned with line`() {
    val lineStart = Coordinates(0.0, 0.0)
    val lineEnd = Coordinates(4.0, 4.0)

    assertThat(lineStart.distanceTo(lineEnd)).isEqualTo(sqrt(32.0))

    val side = sqrt(32.0) / 2.0
    val hypo = 4.0
    val expectedResult = sqrt(hypo * hypo - side * side)

    assertThat(Coordinates(4.0, 0.0).distanceToLine(lineStart, lineEnd)).isCloseTo(expectedResult, 0.000000001)
  }

  @Test
  fun `distanceToLine should return correct distance for point aligned with line 2`() {
    val lineStart = Coordinates(0.0, 0.0)
    val lineEnd = Coordinates(8.0, 4.0)

    assertThat(Coordinates(4.0, 2.0).distanceToLine(lineStart, lineEnd)).isZero()
  }

  @Test
  fun `distanceToLine should return correct distance for point not aligned with line 2`() {
    val lineStart = Coordinates(0.0, 0.0)
    val lineEnd = Coordinates(8.0, 4.0)

    assertThat(Coordinates(3.0, 4.0).distanceToLine(lineStart, lineEnd)).isCloseTo(sqrt(5.0), 0.000000001)
  }

  @Test
  fun `distanceToLine should return correct distance for point not aligned with line 3`() {
    val lineStart = Coordinates(0.0, 0.0)
    val lineEnd = Coordinates(-8.0, -4.0)

    assertThat(Coordinates(-3.0, -4.0).distanceToLine(lineStart, lineEnd)).isCloseTo(sqrt(5.0), 0.000000001)
  }

  @Test
  fun `distanceToLine should return correct distance for point not aligned with line 4`() {
    val lineStart = Coordinates(0.0, 0.0)
    val lineEnd = Coordinates(-8.0, 4.0)

    assertThat(Coordinates(-4.0, -3.0).distanceToLine(lineStart, lineEnd)).isCloseTo(2.0 * sqrt(5.0), 0.000000001)
  }

  @Test
  fun `distanceToLine should return correct distance for point not aligned with line 5`() {
    val lineStart = Coordinates(0.0, 0.0)
    val lineEnd = Coordinates(8.0, -4.0)

    assertThat(Coordinates(-4.0, -3.0).distanceToLine(lineStart, lineEnd)).isCloseTo(5.0, 0.000000001)
  }
}



