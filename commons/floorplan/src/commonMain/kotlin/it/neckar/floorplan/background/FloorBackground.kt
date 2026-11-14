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

import it.neckar.floorplan.FloorFrame

/**
 * The drawing of one floor in the pixels of its [frame]: every stroke and fill in black on white.
 */
class FloorBackground(
  val frame: FloorFrame,
  val strokes: List<Stroke>,
  val fills: List<Fill>,
) {
  /**
   * An open polyline; a single point draws a dot of [widthPx].
   */
  class Stroke(
    /**
     * x and y of each vertex in turn, in pixels, y down.
     */
    val coordinates: DoubleArray,
    val widthPx: Double,
  ) {
    val pointCount: Int
      get() = coordinates.size / 2
  }

  /**
   * An area bounded by [rings]; a point lies inside when a ray from it crosses the rings an odd number of times.
   */
  class Fill(
    /**
     * Each ring as x and y of each vertex in turn, in pixels, y down; the last vertex connects to the first.
     */
    val rings: List<DoubleArray>,
  )
}
