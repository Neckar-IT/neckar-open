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
import it.neckar.open.serialization.roundTrip
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

/**
 *
 */
class QuadrilateralTest {
  /**
   * This is a test that can be used to check if the debug output from braunbetonmeister is plausible.
   *
   * This is *not* a unit test
   */
  @Disabled
  @Test
  fun testPastePlausibilityFromMeisterOutput() {
    //Paste the debug output from braunbetonmeister here
    val debugOutputString = "472.0 379.0, 485.0 1138.0, 1866.0 1140.0, 1644.0 97.0"

    val coords = debugOutputString.split(", ").map { it.split(" ") }.map { Coordinates(it[0].toDouble(), it[1].toDouble()) }

    val quad = Quadrilateral(coords[0], coords[1], coords[2], coords[3]).inCssOrder()

    println("top width: ${quad.topWidth()}")
    println("bottom width: ${quad.bottomWidth()}")
    println("left height: ${quad.leftHeight()}")
    println("right height: ${quad.rightHeight()}")

    assertThat(coords.indexOf(quad.topLeft), "Index of top left").isEqualTo(0)
    assertThat(coords.indexOf(quad.topRight), "Index of top right").isEqualTo(1)
    assertThat(coords.indexOf(quad.bottomRight), "Index of bottom right").isEqualTo(2)
    assertThat(coords.indexOf(quad.bottomLeft), "index of bottom left").isEqualTo(3)

    assertThat(coords.first()).isEqualTo(quad.topLeft)
    assertThat(coords[1]).isEqualTo(quad.topRight)
    assertThat(coords[2]).isEqualTo(quad.bottomRight)
    assertThat(coords.last()).isEqualTo(quad.bottomLeft)
  }

  @Test
  fun testSortByCss() {
    val sorted = Quadrilateral(
      Coordinates(2.0, 0.0),
      Coordinates(0.0, 2.0),
      Coordinates(0.0, 0.0),
      Coordinates(2.0, 2.0),
    ).inCssOrder()
    assertThat(
      sorted
    ).isEqualTo(
      CssOrderQuadrilateral(
        Quadrilateral(
          Coordinates(0.0, 0.0),
          Coordinates(2.0, 0.0),
          Coordinates(2.0, 2.0),
          Coordinates(0.0, 2.0)
        )
      )
    )
  }

  @Test
  fun testInner() {
    assertThat(
      Quadrilateral(
        Coordinates(0.0, 0.0),
        Coordinates(2.0, 0.0),
        Coordinates(2.0, 2.0),
        Coordinates(0.0, 2.0)
      ).guesstimateInnerRectangle()
    ).isEqualTo(Rectangle(0.0, 0.0, 2.0, 2.0))

    assertThat(
      Quadrilateral(
        Coordinates(0.1, 0.0),
        Coordinates(2.0, 0.0),
        Coordinates(2.0, 2.0),
        Coordinates(0.0, 2.0)
      ).guesstimateInnerRectangle()
    ).isEqualTo(Rectangle(0.1, 0.0, 1.9, 2.0))

    assertThat(
      Quadrilateral(
        Coordinates(0.1, 0.0),
        Coordinates(2.0, 0.2),
        Coordinates(2.0, 2.0),
        Coordinates(0.0, 2.0)
      ).guesstimateInnerRectangle()
    ).isEqualTo(Rectangle(0.1, 0.2, 1.9, 1.8))


    val quad = Quadrilateral(
      Coordinates(0.0, 0.0),
      Coordinates(4.0, 0.0),
      Coordinates(4.0, 3.0),
      Coordinates(0.0, 3.0)
    )
    assertThat(quad.guesstimateInnerRectangle()).isEqualTo(Rectangle.fromCoords(Coordinates(0.0, 0.0), Coordinates(4.0, 3.0)))

    assertThat(
      Quadrilateral(
        Coordinates(0.1, 0.0),
        Coordinates(2.0, 0.2),
        Coordinates(2.0, 2.0),
        Coordinates(0.0, 2.0)
      ).guesstimateInnerRectangle()
    ).isEqualTo(Rectangle.fromCoords(Coordinates(0.1, 0.2), Coordinates(2.0, 2.0)))


    assertThat(
      Quadrilateral(
        Coordinates(1.0, 1.0),
        Coordinates(4.0, 1.2),
        Coordinates(4.2, 4.0),
        Coordinates(1.2, 3.8)
      ).guesstimateInnerRectangle()
    ).isEqualTo(Rectangle.fromCoords(Coordinates(1.2, 1.2), Coordinates(4.0, 3.8)))
  }

  @Test
  fun testRect() {
    assertThat(
      Quadrilateral(
        Coordinates(0.0, 0.0),
        Coordinates(2.0, 0.0),
        Coordinates(2.0, 2.0),
        Coordinates(0.0, 2.0)
      ).calculateBoundingBox()
    ).isEqualTo(Rectangle(0.0, 0.0, 2.0, 2.0))

    assertThat(
      Quadrilateral(
        Coordinates(1.0, 1.0),
        Coordinates(4.0, 1.0),
        Coordinates(4.0, 3.0),
        Coordinates(1.0, 3.0)
      ).calculateBoundingBox()
    ).isEqualTo(Rectangle.fromCoords(1.0, 1.0, 4.0, 3.0))

    assertThat(
      Quadrilateral(
        Coordinates(3.0, 3.0),
        Coordinates(5.0, 3.0),
        Coordinates(5.0, 5.0),
        Coordinates(3.0, 5.0)
      ).calculateBoundingBox()
    ).isEqualTo(Rectangle.fromCoords(3.0, 3.0, 5.0, 5.0))

    assertThat(
      Quadrilateral(
        Coordinates(0.0, 0.0),
        Coordinates(3.0, 1.0),
        Coordinates(2.0, 4.0),
        Coordinates(1.0, 3.0)
      ).calculateBoundingBox()
    ).isEqualTo(Rectangle.fromCoords(0.0, 0.0, 3.0, 4.0))
  }

  @Test
  fun testJson() {
    roundTrip(
      Quadrilateral(
        Coordinates(0.0, 0.0),
        Coordinates(2.0, 0.0),
        Coordinates(2.0, 2.0),
        Coordinates(0.0, 2.0)
      )
    ) {
      //language=JSON
      """
        {
          "point1" : {
            "x" : 0.0,
            "y" : 0.0
          },
          "point2" : {
            "x" : 2.0,
            "y" : 0.0
          },
          "point3" : {
            "x" : 2.0,
            "y" : 2.0
          },
          "point4" : {
            "x" : 0.0,
            "y" : 2.0
          }
        }
      """.trimIndent()
    }
  }

  @Test
  fun `vertices should return correct coordinates in order`() {
    val quad = Quadrilateral(
      Coordinates(0.0, 0.0),
      Coordinates(2.0, 0.0),
      Coordinates(2.0, 2.0),
      Coordinates(0.0, 2.0)
    )

    val vertices = quad.vertices()
    assertThat(vertices).isEqualTo(
      listOf(
        Coordinates(0.0, 0.0),
        Coordinates(2.0, 0.0),
        Coordinates(2.0, 2.0),
        Coordinates(0.0, 2.0)
      )
    )
  }

  @Test
  fun `move should correctly move all points`() {
    val quad = Quadrilateral(
      Coordinates(0.0, 0.0),
      Coordinates(2.0, 0.0),
      Coordinates(2.0, 2.0),
      Coordinates(0.0, 2.0)
    )
    val movedQuad = quad.move(1.0, 1.0)

    assertThat(movedQuad.vertices()).isEqualTo(
      listOf(
        Coordinates(1.0, 1.0),
        Coordinates(3.0, 1.0),
        Coordinates(3.0, 3.0),
        Coordinates(1.0, 3.0)
      )
    )
  }

  @Test
  fun `calculateCentroid should return correct centroid`() {
    val quad = Quadrilateral(
      Coordinates(0.0, 0.0),
      Coordinates(2.0, 0.0),
      Coordinates(2.0, 2.0),
      Coordinates(0.0, 2.0)
    )
    val centroid = quad.calculateCentroid()

    assertThat(centroid).isEqualTo(Coordinates(1.0, 1.0))
  }

  @Test
  fun `rotate should correctly rotate the quadrilateral around its centroid`() {
    val base = Quadrilateral(
      Coordinates(0.0, 0.0),
      Coordinates(2.0, 0.0),
      Coordinates(2.0, 2.0),
      Coordinates(0.0, 2.0)
    )

    assertThat(base.calculateCentroid()).isEqualTo(Coordinates(1.0, 1.0))

    assertThat(
      base.rotateAroundCentroid(90.0)
    ).isCloseTo(
      Quadrilateral(
        Coordinates(2.0, 0.0),
        Coordinates(2.0, 2.0),
        Coordinates(0.0, 2.0),
        Coordinates(0.0, 0.0),
      )
    )
  }
}


fun Assert<Quadrilateral>.isCloseTo(quadrilateral: Quadrilateral, delta: Double = 0.00000000000001) {
  given { actual ->
    if (actual.vertices().zip(quadrilateral.vertices()).all { (a, e) -> a.isCloseTo(e, delta) }) return

    val stringRepresentation = show(quadrilateral)

    expected(
      "all vertices of quadrilateral to be close to $stringRepresentation, but was ${show(actual)}",
      expected = quadrilateral,
      actual = actual
    )
  }
}

private fun show(quadrilateral: Quadrilateral): String {
  return "Quadrilateral:\n" +
    "  topLeft: ${quadrilateral.point1.format()}\n" +
    "  topRight: ${quadrilateral.point2.format()}\n" +
    "  bottomRight: ${quadrilateral.point3.format()}\n" +
    "  bottomLeft: ${quadrilateral.point4.format()}\n"
}
