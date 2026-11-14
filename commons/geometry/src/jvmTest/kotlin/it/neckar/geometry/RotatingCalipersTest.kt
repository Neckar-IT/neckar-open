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
import it.neckar.open.time.nowMillis
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

class RotatingCalipersTest {
  @Test
  fun testRotatingCalipers() {
    val testCoordinates = listOf(
      Coordinates(0.0, 0.0),
      Coordinates(1.0, 0.0),
      Coordinates(1.0, 1.0),
      Coordinates(0.0, 1.0),
      Coordinates(0.5, 0.5)
    )

    val testConvexHull = convexHull(testCoordinates)
    val testRotatingCalipers = rotatingCalipers(testConvexHull)

    assertThat(testRotatingCalipers.vertices()).containsExactly(
      Coordinates(0.0, 0.0),
      Coordinates(1.0, 0.0),
      Coordinates(1.0, 1.0),
      Coordinates(0.0, 1.0),
    )
    //assertThat(testRotatingCalipers.area).isEqualTo(1.0)
    //assertThat(testRotatingCalipers.perimeter).isEqualTo(4.0)
  }

  @Test
  @Disabled
  fun benchmark1KRotatingCalipers() {
    val imageSize = 32
    val testCoordinates = buildList {
      repeat(imageSize) { y ->
        repeat(imageSize) { x ->
          add(Coordinates(x, y))
        }
      }
    }

    val startTime = nowMillis()

    val testConvexHull = convexHull(testCoordinates)
    val convexHullTime = nowMillis() - startTime

    val testRotatingCalipers = rotatingCalipers(testConvexHull)
    val rotatingCalipersTime = nowMillis() - convexHullTime - startTime

    val totalTime = nowMillis() - startTime

    println("convexHullTime 1K: $convexHullTime")
    println("rotatingCalipersTime 1K: $rotatingCalipersTime")
    println("totalTime 1K: $totalTime")

    assertThat(convexHullTime).isCloseTo(10.0, 1.0)
    assertThat(rotatingCalipersTime).isCloseTo(0.5, 0.5)
    assertThat(totalTime).isCloseTo(11.0, 1.0)
  }

  @Test
  @Disabled
  fun benchmark10KRotatingCalipers() {
    val imageSize = 100
    val testCoordinates = buildList {
      repeat(imageSize) { y ->
        repeat(imageSize) { x ->
          add(Coordinates(x, y))
        }
      }
    }

    val startTime = nowMillis()

    val testConvexHull = convexHull(testCoordinates)
    val convexHullTime = nowMillis() - startTime

    val testRotatingCalipers = rotatingCalipers(testConvexHull)
    val rotatingCalipersTime = nowMillis() - convexHullTime - startTime

    val totalTime = nowMillis() - startTime

    println("convexHullTime 10K: $convexHullTime")
    println("rotatingCalipersTime 10K: $rotatingCalipersTime")
    println("totalTime 10K: $totalTime")

    assertThat(convexHullTime).isCloseTo(18.0, 1.0)
    assertThat(rotatingCalipersTime).isCloseTo(0.5, 0.5)
    assertThat(totalTime).isCloseTo(19.0, 1.0)
  }

  @Test
  @Disabled
  fun benchmark1MRotatingCalipers() {
    val imageSize = 1_000
    val testCoordinates = buildList {
      repeat(imageSize) { y ->
        repeat(imageSize) { x ->
          add(Coordinates(x, y))
        }
      }
    }

    val startTime = nowMillis()

    val testConvexHull = convexHull(testCoordinates)
    val convexHullTime = nowMillis() - startTime

    val testRotatingCalipers = rotatingCalipers(testConvexHull)
    val rotatingCalipersTime = nowMillis() - convexHullTime - startTime

    val totalTime = nowMillis() - startTime

    println("convexHullTime 1M: $convexHullTime")
    println("rotatingCalipersTime 1M: $rotatingCalipersTime")
    println("totalTime 1M: $totalTime")

    assertThat(convexHullTime).isCloseTo(160.0, 15.0)
    assertThat(rotatingCalipersTime).isCloseTo(0.5, 0.5)
    assertThat(totalTime).isCloseTo(161.0, 15.0)
  }
}
