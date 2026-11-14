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
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith

/**
 *
 */
class PolygonTest {
  @Test
  fun testBasics() {
    val polygon = Polygon(
      listOf(
        Coordinates(0.0, 0.0),
        Coordinates(1.0, 0.0),
        Coordinates(1.0, 1.0),
        Coordinates(0.0, 1.0),
      )
    )

    assertThat(polygon.location).isEqualTo(Coordinates(0.0, 0.0))
    assertThat(polygon.verticesCount).isEqualTo(4)
    assertThat(polygon.size.width).isEqualTo(1.0)
    assertThat(polygon.size.height).isEqualTo(1.0)
    assertThat(polygon.perimeter).isEqualTo(4.0)
    assertThat(polygon.area).isEqualTo(1.0)
    assertThat(polygon.isConvex).isTrue()

    assertThat(polygon.contains(Coordinates(0.5, 0.5))).isTrue()
    assertThat(polygon.contains(Coordinates(1.5, 0.5))).isFalse()
    assertThat(polygon.contains(Coordinates(0.0, 0.0))).isTrue()
    assertThat(polygon.contains(Coordinates(1.0, 1.0))).isTrue()

    assertThat(polygon.containsRayCast(Coordinates(0.5, 0.5))).isTrue()
    assertThat(polygon.containsRayCast(Coordinates(1.5, 0.5))).isFalse()
    assertThat(polygon.containsRayCast(Coordinates(0.0, 0.0))).isTrue()
    // Due to floating point precision issues, this returns false as the point is on the corner
    assertThat(polygon.containsRayCast(Coordinates(1.0, 1.0))).isFalse()
  }

  @Test
  fun testComplex() {
    val polygon = Polygon(
      listOf(
        Coordinates(0.0, 0.0),
        Coordinates(2.0, 2.0),
        Coordinates(3.0, 4.0),
        Coordinates(6.0, -3.0),
      )
    )

    assertThat(polygon.location).isEqualTo(Coordinates(0.0, 0.0))
    assertThat(polygon.verticesCount).isEqualTo(4)
    assertThat(polygon.size.width).isEqualTo(6.0)
    assertThat(polygon.size.height).isEqualTo(7.0)
    assertThat(polygon.perimeter).isEqualTo(19.38847214060926)
    assertThat(polygon.area).isEqualTo(15.5)
    assertThat(polygon.isConcave).isTrue()

    assertThat(polygon.contains(Coordinates(3.0, 1.0))).isTrue()
    assertThat(polygon.contains(Coordinates(7.0, 1.0))).isFalse()
    assertThat(polygon.contains(Coordinates(0.0, 0.0))).isTrue()
    assertThat(polygon.contains(Coordinates(6.0, -3.0))).isTrue()

    assertThat(polygon.containsRayCast(Coordinates(3.0, 1.0))).isTrue()
    assertThat(polygon.containsRayCast(Coordinates(7.0, 1.0))).isFalse()
    assertThat(polygon.containsRayCast(Coordinates(0.0, 0.0))).isTrue()
    // Due to floating point precision issues, this returns false as the point is on the corner
    assertThat(polygon.containsRayCast(Coordinates(6.0, -3.0))).isFalse()
  }

  @Test
  fun testVeryComplex() {
    val polygon = Polygon(
      listOf(
        Coordinates(0.0, 0.0),
        Coordinates(2.0, 2.0),
        Coordinates(3.0, 4.0),
        Coordinates(6.0, -3.0),
        Coordinates(-10.0, -5.0),
        Coordinates(-2.0, 10.0),
      )
    )

    assertThat(polygon.location).isEqualTo(Coordinates(0.0, 0.0))
    assertThat(polygon.verticesCount).isEqualTo(6)
    assertThat(polygon.size.width).isEqualTo(16.0)
    assertThat(polygon.size.height).isEqualTo(15.0)
    assertThat(polygon.perimeter).isEqualTo(56.00282273189255)
    assertThat(polygon.area).isEqualTo(100.5)
    assertThat(polygon.isConcave).isTrue()

    assertThat(polygon.contains(Coordinates(3.0, 1.0))).isTrue()
    assertThat(polygon.contains(Coordinates(7.0, 1.0))).isFalse()
    assertThat(polygon.contains(Coordinates(0.0, 0.0))).isTrue()
    assertThat(polygon.contains(Coordinates(6.0, -3.0))).isTrue()

    assertThat(polygon.containsRayCast(Coordinates(3.0, 1.0))).isTrue()
    assertThat(polygon.containsRayCast(Coordinates(7.0, 1.0))).isFalse()
    assertThat(polygon.containsRayCast(Coordinates(0.0, 0.0))).isTrue()
    // Due to floating point precision issues, this returns false as the point is on the corner
    assertThat(polygon.containsRayCast(Coordinates(6.0, -3.0))).isFalse()
  }

  @Test
  fun `error message shows actual vertex count for invalid polygon`() {
    val vertices = listOf(
      Coordinates(0.0, 0.0),
      Coordinates(1.0, 1.0),
    )

    val exception = assertFailsWith<IllegalArgumentException> {
      Polygon(vertices)
    }

    assertThat(exception.message).isNotNull()
    assertThat(exception.message!!).contains("but was 2")
    assertThat(exception.message!!).contains("0.0/0.0")
    assertThat(exception.message!!).contains("1.0/1.0")
  }

  @Test
  fun `error message shows vertices for empty polygon`() {
    val exception = assertFailsWith<IllegalArgumentException> {
      Polygon(emptyList())
    }

    assertThat(exception.message).isNotNull()
    assertThat(exception.message!!).contains("but was 0")
  }
}
