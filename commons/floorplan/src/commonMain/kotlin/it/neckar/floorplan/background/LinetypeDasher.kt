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
package it.neckar.floorplan.background

import kotlin.math.sqrt

/**
 * Splits a polyline into the dashes of a linetype; the pattern continues across the vertices of one polyline.
 */
internal class LinetypeDasher(
  /**
   * Alternating dash and gap lengths, starting with a dash; even count.
   */
  private val pattern: DoubleArray,
) {
  init {
    require(pattern.size >= 2 && pattern.size % 2 == 0 && pattern.all { it > 0.0 }) { "pattern needs pairs of positive lengths but was ${pattern.toList()}" }
  }

  /**
   * The dashes of the polyline [coordinates] (x and y of each vertex in turn), each as such a coordinate array.
   */
  fun dashes(coordinates: DoubleArray): List<DoubleArray> {
    val dashes = mutableListOf<DoubleArray>()
    var elementIndex = 0
    var remaining = pattern[0]
    // The dash being drawn; empty during a gap.
    val current = mutableListOf(coordinates[0], coordinates[1])

    for (vertex in 1 until coordinates.size / 2) {
      var x = coordinates[2 * vertex - 2]
      var y = coordinates[2 * vertex - 1]
      val endX = coordinates[2 * vertex]
      val endY = coordinates[2 * vertex + 1]
      var segmentLength = sqrt((endX - x) * (endX - x) + (endY - y) * (endY - y))
      if (segmentLength == 0.0) {
        continue
      }
      val directionX = (endX - x) / segmentLength
      val directionY = (endY - y) / segmentLength

      while (segmentLength >= remaining) {
        x += directionX * remaining
        y += directionY * remaining
        segmentLength -= remaining
        if (elementIndex % 2 == 0) {
          current.add(x)
          current.add(y)
          dashes.add(current.toDoubleArray())
          current.clear()
        } else {
          current.add(x)
          current.add(y)
        }
        elementIndex = (elementIndex + 1) % pattern.size
        remaining = pattern[elementIndex]
      }
      remaining -= segmentLength
      if (current.isNotEmpty()) {
        current.add(endX)
        current.add(endY)
      }
    }
    // A dash that would start exactly at the end of the polyline has no length.
    if (current.size >= 4 && (current[0] != current[current.size - 2] || current[1] != current[current.size - 1])) {
      dashes.add(current.toDoubleArray())
    }
    return dashes
  }
}
