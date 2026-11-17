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
import it.neckar.open.collections.fastForEachIndexed
import it.neckar.open.kotlin.lang.toRadians
import org.junit.jupiter.api.Test
import kotlin.math.PI

class RotatedRectangleTest {

  val rectangleToTest = RotatedRectangle(Coordinates(0.0, 0.0), Size(2.0, 1.0), 0.0)

  @Test
  fun rotated15() {
    val rotatedRectangle = rectangleToTest.rotated(15.0.toRadians())
    assertThat(rotatedRectangle.center).isEqualTo(Coordinates(0.0, 0.0))
    assertThat(rotatedRectangle.rotation).isEqualTo(15.0.toRadians())
    assertThat(rotatedRectangle.size).isEqualTo(Size(2.0, 1.0))
  }

  @Test
  fun rotated45() {
    val rotatedRectangle = rectangleToTest.rotated(PI / 4)
    assertThat(rotatedRectangle.center).isEqualTo(Coordinates(0.0, 0.0))
    assertThat(rotatedRectangle.rotation).isEqualTo(PI / 4)
    assertThat(rotatedRectangle.size).isEqualTo(Size(2.0, 1.0))
  }

  @Test
  fun rotated60() {
    val rotatedRectangle = rectangleToTest.rotated(60.0.toRadians())
    assertThat(rotatedRectangle.center).isEqualTo(Coordinates(0.0, 0.0))
    assertThat(rotatedRectangle.rotation).isCloseTo((-30.0).toRadians(), 0.000000001)
    assertThat(rotatedRectangle.size).isEqualTo(Size(1.0, 2.0))
  }

  @Test
  fun rotated90() {
    val rotatedRectangle = rectangleToTest.rotated(PI / 2)
    assertThat(rotatedRectangle.center).isEqualTo(Coordinates(0.0, 0.0))
    assertThat(rotatedRectangle.rotation).isEqualTo(0.0)
    assertThat(rotatedRectangle.size).isEqualTo(Size(1.0, 2.0))
  }

  @Test
  fun rotated180() {
    val rotatedRectangle = rectangleToTest.rotated(PI)
    assertThat(rotatedRectangle.center).isEqualTo(Coordinates(0.0, 0.0))
    assertThat(rotatedRectangle.rotation).isEqualTo(0.0)
    assertThat(rotatedRectangle.size).isEqualTo(Size(2.0, 1.0))
  }

  @Test
  fun vertices() {
    val vertices = rectangleToTest.vertices()
    assertThat(vertices).hasSize(4)
    assertThat(vertices[0].x).isCloseTo(-1.0, 0.000000001)
    assertThat(vertices[0].y).isCloseTo(-0.5, 0.000000001)
    assertThat(vertices[1].x).isCloseTo(1.0, 0.000000001)
    assertThat(vertices[1].y).isCloseTo(-0.5, 0.000000001)
    assertThat(vertices[2].x).isCloseTo(1.0, 0.000000001)
    assertThat(vertices[2].y).isCloseTo(0.5, 0.000000001)
    assertThat(vertices[3].x).isCloseTo(-1.0, 0.000000001)
    assertThat(vertices[3].y).isCloseTo(0.5, 0.000000001)
  }

  @Test
  fun toRectangle() {
    val rectangle = rectangleToTest.toRectangle()
    val expectedDistance = rectangleToTest.center - rectangleToTest.size.toCoordinate() / 2.0
    assertThat(rectangle.location).isEqualTo(Coordinates(expectedDistance.x, expectedDistance.y))
    assertThat(rectangle.size).isEqualTo(Size(2.0, 1.0))
  }

  @Test
  fun toIllegalRectangle() {
    val illegalRectangleToTest = RotatedRectangle(Coordinates(0.0, 0.0), Size(2.0, 1.0), PI / 5)
    assertFailure { illegalRectangleToTest.toRectangle() }
  }

  @Test
  fun toQuadrilateral() {
    val quadrilateral = rectangleToTest.toQuadrilateral()
    assertThat(quadrilateral.vertices()).isEqualTo(rectangleToTest.vertices())
  }

  @Test
  fun createNormalized() {
    val normalizedRectangleToTest = RotatedRectangle.createNormalized(Coordinates(0.0, 0.0), Size(2.0, 1.0), 0.0)
    assertThat(normalizedRectangleToTest.center).isEqualTo(Coordinates(0.0, 0.0))
    assertThat(normalizedRectangleToTest.size).isEqualTo(Size(2.0, 1.0))
    assertThat(normalizedRectangleToTest.rotation).isEqualTo(0.0)
  }

  @Test
  fun createNormalized1() {
    val normalizedRectangleToTest = RotatedRectangle.createNormalized(Coordinates(0.0, 0.0), Size(2.0, 1.0), PI / 4)
    assertThat(normalizedRectangleToTest.center).isEqualTo(Coordinates(0.0, 0.0))
    assertThat(normalizedRectangleToTest.size).isEqualTo(Size(2.0, 1.0))
    assertThat(normalizedRectangleToTest.rotation).isEqualTo(PI / 4)
  }

  @Test
  fun createNormalized2() {
    val normalizedRectangleToTest = RotatedRectangle.createNormalized(Coordinates(0.0, 0.0), Size(2.0, 1.0), PI / 4 + PI / 8)
    assertThat(normalizedRectangleToTest.center).isEqualTo(Coordinates(0.0, 0.0))
    assertThat(normalizedRectangleToTest.size).isEqualTo(Size(1.0, 2.0))
    assertThat(normalizedRectangleToTest.rotation).isEqualTo(-PI / 8)
  }

  @Test
  fun createNormalized3() {
    val normalizedRectangleToTest = RotatedRectangle.createNormalized(Coordinates(0.0, 0.0), Size(2.0, 1.0), PI / 2)
    assertThat(normalizedRectangleToTest.center).isEqualTo(Coordinates(0.0, 0.0))
    assertThat(normalizedRectangleToTest.size).isEqualTo(Size(1.0, 2.0))
    assertThat(normalizedRectangleToTest.rotation).isEqualTo(0.0)
  }

  @Test
  fun createNormalized4() {
    val normalizedRectangleToTest = RotatedRectangle.createNormalized(Coordinates(0.0, 0.0), Size(2.0, 1.0), PI / 2 + PI / 8)
    assertThat(normalizedRectangleToTest.center).isEqualTo(Coordinates(0.0, 0.0))
    assertThat(normalizedRectangleToTest.size).isEqualTo(Size(1.0, 2.0))
    assertThat(normalizedRectangleToTest.rotation).isEqualTo(PI / 8)
  }

  @Test
  fun createNormalized5() {
    val normalizedRectangleToTest = RotatedRectangle.createNormalized(Coordinates(0.0, 0.0), Size(2.0, 1.0), PI / 2 + PI / 4)
    assertThat(normalizedRectangleToTest.center).isEqualTo(Coordinates(0.0, 0.0))
    assertThat(normalizedRectangleToTest.size).isEqualTo(Size(1.0, 2.0))
    assertThat(normalizedRectangleToTest.rotation).isEqualTo(PI / 4)
  }

  @Test
  fun toRectangleTest() {
    val rectangleToTest2 = RotatedRectangle(Coordinates(1800.979736328125, 591.6577758789062), Size(1067.4521484375, 1075.5291748046875), 0.05289179821449198)
    val rotatedRectangle = rectangleToTest2.rotated(-rectangleToTest2.rotation)
    assertThat(rotatedRectangle.center).isEqualTo(Coordinates(1800.979736328125, 591.6577758789062))
    assertThat(rotatedRectangle.rotation).isEqualTo(0.0)
    assertThat(rotatedRectangle.size).isEqualTo(Size(1067.4521484375, 1075.5291748046875))

    val rectangle = rotatedRectangle.toRectangle()
    val expectedDistance = rotatedRectangle.center - rotatedRectangle.size.toCoordinate() / 2.0
    assertThat(rectangle.location).isEqualTo(Coordinates(expectedDistance.x, expectedDistance.y))
    assertThat(rectangle.size).isEqualTo(Size(1067.4521484375, 1075.5291748046875))
    val rotatedVertices = rotatedRectangle.vertices().inCssOrder()
    val vertices = rectangle.vertices().inCssOrder()
    rotatedVertices.fastForEachIndexed { index, coordinate ->
      assertThat(vertices[index].x).isCloseTo(coordinate.x, 0.000000001)
      assertThat(vertices[index].y).isCloseTo(coordinate.y, 0.000000001)
    }
  }
}
