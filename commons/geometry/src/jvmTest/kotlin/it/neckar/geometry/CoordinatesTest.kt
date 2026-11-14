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
import it.neckar.open.kotlin.lang.sqrt
import it.neckar.open.test.utils.isNaN
import org.junit.jupiter.api.Test
import kotlin.math.PI
import kotlin.math.sqrt

/**
 */
internal class CoordinatesTest {
  @Test
  fun testSortByCssOrder() {
    listOf(Coordinates(1.0, 2.0), Coordinates(7.0, 2.0), Coordinates(4.0, 2.0), Coordinates(2.0, 4.5)).let {
      assertThat(Coordinates.sortCssOrder(it).quadrilateral).isEqualTo(
        Quadrilateral(
          point1 = Coordinates(1.0, 2.0), point2 = Coordinates(4.0, 2.0), point3 = Coordinates(7.0, 2.0), point4 = Coordinates(2.0, 4.5)
        )
      )
    }
  }

  @Test
  fun testComparator() {
    val list = listOf(Coordinates(1.0, 2.0), Coordinates(7.0, 2.0), Coordinates(4.0, 2.0))
    list.sortedWith(Coordinates.CompareByYThenX).let {
      assertThat(it).isEqualTo(listOf(Coordinates(1.0, 2.0), Coordinates(4.0, 2.0), Coordinates(7.0, 2.0)))
    }
  }

  @Test
  fun `CompareByYThenX should correctly compare coordinates`() {
    assertThat(Coordinates.CompareByYThenX.compare(Coordinates(1.0, 2.0), Coordinates(2.0, 1.0))).isGreaterThan(0)
    assertThat(Coordinates.CompareByYThenX.compare(Coordinates(2.0, 1.0), Coordinates(1.0, 2.0))).isLessThan(0)
    assertThat(Coordinates.CompareByYThenX.compare(Coordinates(1.0, 1.0), Coordinates(2.0, 1.0))).isLessThan(0)
    assertThat(Coordinates.CompareByYThenX.compare(Coordinates(1.0, 1.0), Coordinates(1.0, 1.0))).isEqualTo(0)
  }

  @Test
  fun `CompareByXThenY should correctly compare coordinates`() {
    assertThat(Coordinates.CompareByXThenY.compare(Coordinates(1.0, 2.0), Coordinates(2.0, 1.0))).isLessThan(0)
    assertThat(Coordinates.CompareByXThenY.compare(Coordinates(2.0, 1.0), Coordinates(1.0, 2.0))).isGreaterThan(0)
    assertThat(Coordinates.CompareByXThenY.compare(Coordinates(1.0, 1.0), Coordinates(2.0, 1.0))).isLessThan(0)
    assertThat(Coordinates.CompareByXThenY.compare(Coordinates(1.0, 1.0), Coordinates(1.0, 1.0))).isEqualTo(0)
  }

  @Test
  fun testRotate() {
    assertThat(Coordinates(2.0, 2.0).rotateAround(Coordinates(1.0, 1.0), 90.0)).isCloseTo(Coordinates(0.0, 2.0))
  }

  @Test
  fun testMod() {
    assertThat(Coordinates(0.0, 0.0).mod(1.0, 1.0)).isEqualTo(Coordinates.origin)
    assertThat(Coordinates(10.0, 10.0).mod(2.0, 3.0)).isEqualTo(Coordinates(0.0, 1.0))
    assertThat(Coordinates(-11.5, -10.0).mod(2.0, 3.0)).isEqualTo(Coordinates(-1.5, -1.0))
  }

  @Test
  fun testDistanceX() {
    assertThat(Coordinates(0.0, 0.0).distanceTo(0.0, 0.0)).isEqualTo(0.0)
    assertThat(Coordinates(0.0, 0.0).distanceTo(1.0, 0.0)).isEqualTo(1.0)
    assertThat(Coordinates(0.0, 0.0).distanceTo(2.0, 0.0)).isEqualTo(2.0)
    assertThat(Coordinates(2.0, 0.0).distanceTo(2.0, 0.0)).isEqualTo(0.0)
    assertThat(Coordinates(2.0, 0.0).distanceTo(4.0, 0.0)).isEqualTo(2.0)
    assertThat(Coordinates(2.0, 0.0).distanceTo(0.0, 0.0)).isEqualTo(2.0)
  }

  @Test
  fun testDistanceY() {
    assertThat(Coordinates(0.0, 0.0).distanceTo(0.0, 0.0)).isEqualTo(0.0)
    assertThat(Coordinates(0.0, 0.0).distanceTo(0.0, 1.0)).isEqualTo(1.0)
    assertThat(Coordinates(0.0, 0.0).distanceTo(0.0, 2.0)).isEqualTo(2.0)
    assertThat(Coordinates(0.0, 2.0).distanceTo(0.0, 2.0)).isEqualTo(0.0)
    assertThat(Coordinates(0.0, 2.0).distanceTo(0.0, 4.0)).isEqualTo(2.0)
    assertThat(Coordinates(0.0, 2.0).distanceTo(0.0, 0.0)).isEqualTo(2.0)
  }

  @Test
  fun testDistanceBoth() {
    assertThat(Coordinates(0.0, 0.0).distanceTo(1.0, 1.0)).isEqualTo(2.0.sqrt())
    assertThat(Coordinates(0.0, 0.0).distanceTo(2.0, 2.0)).isEqualTo(8.0.sqrt())
    assertThat(Coordinates(7.0, 5.0).distanceTo(9.0, 3.0)).isEqualTo(8.0.sqrt())
  }

  @Test
  fun plusDelta() {
    assertThat(Coordinates(2.0, 1.0).plus(0.0, 0.0)).isEqualTo(Coordinates(2.0, 1.0))
    assertThat(Coordinates(2.0, 1.0).plus(1.0, 0.0)).isEqualTo(Coordinates(3.0, 1.0))
    assertThat(Coordinates(2.0, 1.0).plus(0.0, 1.0)).isEqualTo(Coordinates(2.0, 2.0))
    assertThat(Coordinates(2.0, 1.0).plus(1.0, 1.0)).isEqualTo(Coordinates(3.0, 2.0))
    assertThat(Coordinates(2.0, 1.0).plus(-1.3, -5.6)).isEqualTo(Coordinates(0.7, -4.6))
  }

  @Test
  fun plusDistance() {
    assertThat(Coordinates(2.0, 1.0).plus(Distance(0.0, 0.0))).isEqualTo(Coordinates(2.0, 1.0))
    assertThat(Coordinates(2.0, 1.0).plus(Distance(1.0, 0.0))).isEqualTo(Coordinates(3.0, 1.0))
    assertThat(Coordinates(2.0, 1.0).plus(Distance(0.0, 1.0))).isEqualTo(Coordinates(2.0, 2.0))
    assertThat(Coordinates(2.0, 1.0).plus(Distance(1.0, 1.0))).isEqualTo(Coordinates(3.0, 2.0))
    assertThat(Coordinates(2.0, 1.0).plus(Distance(-1.3, -5.6))).isEqualTo(Coordinates(0.7, -4.6))
  }

  @Test
  fun plusSize() {
    assertThat(Coordinates(2.0, 1.0).plus(Size(0.0, 0.0))).isEqualTo(Coordinates(2.0, 1.0))
    assertThat(Coordinates(2.0, 1.0).plus(Size(1.0, 0.0))).isEqualTo(Coordinates(3.0, 1.0))
    assertThat(Coordinates(2.0, 1.0).plus(Size(0.0, 1.0))).isEqualTo(Coordinates(2.0, 2.0))
    assertThat(Coordinates(2.0, 1.0).plus(Size(1.0, 1.0))).isEqualTo(Coordinates(3.0, 2.0))
    assertThat(Coordinates(2.0, 1.0).plus(Size(-1.3, -5.6))).isEqualTo(Coordinates(0.7, -4.6))
  }

  @Test
  fun withMax() {
    assertThat(Coordinates(3.3, 4.5).withMax(4.0, 5.0)).isEqualTo(Coordinates(3.3, 4.5))
    assertThat(Coordinates(3.3, 4.5).withMax(4.0, 4.0)).isEqualTo(Coordinates(3.3, 4.0))
    assertThat(Coordinates(3.3, 4.5).withMax(3.0, 5.0)).isEqualTo(Coordinates(3.0, 4.5))
    assertThat(Coordinates(3.3, 4.5).withMax(3.0, 4.0)).isEqualTo(Coordinates(3.0, 4.0))
  }

  @Test
  fun withMin() {
    assertThat(Coordinates(3.3, 4.5).withMin(4.0, 5.0)).isEqualTo(Coordinates(4.0, 5.0))
    assertThat(Coordinates(3.3, 4.5).withMin(4.0, 4.0)).isEqualTo(Coordinates(4.0, 4.5))
    assertThat(Coordinates(3.3, 4.5).withMin(3.0, 5.0)).isEqualTo(Coordinates(3.3, 5.0))
    assertThat(Coordinates(3.3, 4.5).withMin(3.0, 4.0)).isEqualTo(Coordinates(3.3, 4.5))
  }

  @Test
  fun withX() {
    assertThat(Coordinates(3.5, -2.7).withX(1.0)).isEqualTo(Coordinates(1.0, -2.7))
    assertThat(Coordinates(3.5, -2.7).withX(-1.0)).isEqualTo(Coordinates(-1.0, -2.7))
    assertThat(Coordinates(3.5, -2.7).withX(Double.POSITIVE_INFINITY)).isEqualTo(Coordinates(Double.POSITIVE_INFINITY, -2.7))
    assertThat(Coordinates(3.5, -2.7).withX(Double.NEGATIVE_INFINITY)).isEqualTo(Coordinates(Double.NEGATIVE_INFINITY, -2.7))
    assertThat(Coordinates(3.5, -2.7).withX(Double.MAX_VALUE)).isEqualTo(Coordinates(Double.MAX_VALUE, -2.7))
    assertThat(Coordinates(3.5, -2.7).withX(Double.MIN_VALUE)).isEqualTo(Coordinates(Double.MIN_VALUE, -2.7))
  }

  @Test
  fun withY() {
    assertThat(Coordinates(3.5, -2.7).withY(1.0)).isEqualTo(Coordinates(3.5, 1.0))
    assertThat(Coordinates(3.5, -2.7).withY(-1.0)).isEqualTo(Coordinates(3.5, -1.0))
    assertThat(Coordinates(3.5, -2.7).withY(Double.POSITIVE_INFINITY)).isEqualTo(Coordinates(3.5, Double.POSITIVE_INFINITY))
    assertThat(Coordinates(3.5, -2.7).withY(Double.NEGATIVE_INFINITY)).isEqualTo(Coordinates(3.5, Double.NEGATIVE_INFINITY))
    assertThat(Coordinates(3.5, -2.7).withY(Double.MAX_VALUE)).isEqualTo(Coordinates(3.5, Double.MAX_VALUE))
    assertThat(Coordinates(3.5, -2.7).withY(Double.MIN_VALUE)).isEqualTo(Coordinates(3.5, Double.MIN_VALUE))
  }

  @Test
  fun delta() {
    assertThat(Coordinates(0.0, 0.0).delta(Coordinates(0.0, 0.0))).isEqualTo(Distance(0.0, 0.0))
    assertThat(Coordinates(1.0, 1.0).delta(Coordinates(0.0, 0.0))).isEqualTo(Distance(1.0, 1.0))
    assertThat(Coordinates(-1.0, 1.0).delta(Coordinates(1.0, -1.0))).isEqualTo(Distance(-2.0, 2.0))
    assertThat(Coordinates(1.0, -1.0).delta(Coordinates(-1.0, 1.0))).isEqualTo(Distance(2.0, -2.0))
  }

  @Test
  fun testDeltaAbsolute() {
    assertThat(Coordinates(0.0, 0.0).deltaAbsolute(Coordinates(0.0, 0.0))).isEqualTo(Distance(0.0, 0.0))
    assertThat(Coordinates(10.0, 20.0).deltaAbsolute(Coordinates(0.0, 0.0))).isEqualTo(Distance(10.0, 20.0))
    assertThat(Coordinates(0.0, 0.0).deltaAbsolute(Coordinates(10.0, 20.0))).isEqualTo(Distance(10.0, 20.0))
    assertThat(Coordinates(-10.0, 20.0).deltaAbsolute(Coordinates(10.0, -20.0))).isEqualTo(Distance(20.0, 40.0))
    assertThat(Coordinates(10.0, -20.0).deltaAbsolute(Coordinates(-10.0, 20.0))).isEqualTo(Distance(20.0, 40.0))
  }

  @Test
  fun testNaN() {
    val base = Coordinates.origin

    val added = base.plus(Double.NaN, Double.NaN)
    assertThat(added.x).isNaN()
    assertThat(added.y).isNaN()
  }

  @Test
  fun testCenter() {
    assertThat(Coordinates(0.0, 0.0).center(Coordinates(0.0, 0.0))).isEqualTo(Coordinates(0.0, 0.0))
    assertThat(Coordinates(10.0, 20.0).center(Coordinates(20.0, 40.0))).isEqualTo(Coordinates(15.0, 30.0))
    assertThat(Coordinates(-10.0, -20.0).center(Coordinates(20.0, 40.0))).isEqualTo(Coordinates(5.0, 10.0))
    assertThat(Coordinates(10.0, 20.0).center(Coordinates(-20.0, -40.0))).isEqualTo(Coordinates(-5.0, -10.0))
    assertThat(Coordinates(-10.0, -20.0).center(Coordinates(-20.0, -40.0))).isEqualTo(Coordinates(-15.0, -30.0))
  }

  @Test
  fun toPolar() {

    // check with: http://www.calc3d.com/gjavascriptcoordcalc.html
    // x, y = 0.0, 0.0
    assertThat(Coordinates(0.0, 0.0).toPolar().r).isCloseTo(0.0, 0.00001)
    assertThat(Coordinates(0.0, 0.0).toPolar().theta).isCloseTo(0.0, 0.00001)

    // x, y = 1.0, 0.0
    assertThat(Coordinates(1.0, 0.0).toPolar().r).isCloseTo(1.0, 0.00001)
    assertThat(Coordinates(1.0, 0.0).toPolar().theta).isCloseTo(0.0, 0.00001)

    // x, y = -1.0, 0.0
    assertThat(Coordinates(-1.0, 0.0).toPolar().r).isCloseTo(1.0, 0.00001)
    assertThat(Coordinates(-1.0, 0.0).toPolar().theta).isCloseTo(PI, 0.00001)

    // x, y = 0.0, 1.0
    assertThat(Coordinates(0.0, 1.0).toPolar().r).isCloseTo(1.0, 0.00001)
    assertThat(Coordinates(0.0, 1.0).toPolar().theta).isCloseTo(PI / 2.0, 0.00001)

    // x, y = 0.0, -1.0
    assertThat(Coordinates(0.0, -1.0).toPolar().r).isCloseTo(1.0, 0.00001)
    assertThat(Coordinates(0.0, -1.0).toPolar().theta).isCloseTo(-PI / 2.0, 0.00001)

    // x, y = 1.0, -1.0
    assertThat(Coordinates(1.0, -1.0).toPolar().r).isCloseTo(sqrt(2.0), 0.00001)
    assertThat(Coordinates(1.0, -1.0).toPolar().theta).isCloseTo(-PI / 4.0, 0.00001)
  }

  @Test
  fun roundTrip2() {
    val source = Coordinates(1.0, -1.0)
    val polar = source.toPolar()
    val roundTrip = polar.toCartesian()

    assertThat(source.x).isCloseTo(roundTrip.x, 0.00001)
    assertThat(source.y).isCloseTo(roundTrip.y, 0.00001)
  }
}
