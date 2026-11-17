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
import assertk.assertions.support.*
import it.neckar.open.kotlin.lang.isCloseTo
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.opentest4j.AssertionFailedError

/**
 *
 */
class CoordinatesPivotTest {
  @Test
  fun testAroundOrigin() {
    assertThat(Coordinates(2.0, 3.0).rotateAround(Coordinates.origin, 0.0)).isEqualTo(Coordinates(2.0, 3.0))
    assertThat(Coordinates(2.0, 3.0).rotateAround(Coordinates.origin, 90.0)).isEqualTo(Coordinates(-3.0, 2.0))
    assertThat(Coordinates(2.0, 3.0).rotateAround(Coordinates.origin, 180.0)).isCloseTo(Coordinates(-2.0, -3.0))
    assertThat(Coordinates(2.0, 3.0).rotateAround(Coordinates.origin, 270.0)).isCloseTo(Coordinates(3.0, -2.0))
  }

  @Test
  fun `rotate around pivot by 0 degrees should return the same coordinates`() {
    assertThat(Coordinates(2.0, 3.0).rotateAround(Coordinates(1.0, 1.0), 0.0)).isCloseTo(Coordinates(2.0, 3.0))
  }

  @Test
  fun `rotate around pivot by 90 degrees should return correct coordinates`() {
    assertThat(Coordinates(2.0, 3.0).rotateAround(Coordinates(1.0, 1.0), 90.0)).isCloseTo(Coordinates(-1.0, 2.0))
  }

  @Test
  fun `rotate around pivot by 180 degrees should return correct coordinates`() {
    assertThat(Coordinates(2.0, 3.0).rotateAround(Coordinates(1.0, 1.0), 180.0)).isCloseTo(Coordinates(-0.0, -1.0))
  }

  @Test
  fun `rotate around pivot by 270 degrees should return correct coordinates`() {
    assertThat(Coordinates(2.0, 3.0).rotateAround(Coordinates(1.0, 1.0), 270.0)).isCloseTo(Coordinates(3.0, 0.0))
  }

  @Test
  fun `rotate around pivot by 360 degrees should return the same coordinates`() {
    assertThat(Coordinates(2.0, 3.0).rotateAround(Coordinates(1.0, 1.0), 360.0)).isCloseTo(Coordinates(2.0, 3.0))
  }

  @Test
  fun testErrorMessage() {
    assertThrows<AssertionFailedError> {
      assertThat(Coordinates(2.0, 3.0)).isCloseTo(Coordinates(77.0, 88.0))
    }.let {
      assertThat(it.message).isEqualTo("expected to be close to <77.0/88.0>, but was <2.0/3.0>")
    }
  }
}

fun Assert<Coordinates>.isCloseTo(coordinates: Coordinates, delta: Double = 0.00000000000001) {
  given { actual ->
    if (actual.x.isCloseTo(coordinates.x, delta) && actual.y.isCloseTo(coordinates.y, delta)) return

    expected(
      "to be close to ${show(coordinates)}, but was ${show(actual)}",
      expected = coordinates,
      actual = actual
    )
  }
}

