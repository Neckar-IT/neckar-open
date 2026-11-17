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
package it.neckar.floorplan

import it.neckar.geometry.Coordinates
import kotlin.math.max
import kotlin.math.min
import kotlin.math.round

/**
 * The image of one floor: the model window it shows in meters, its scale and its size in pixels.
 *
 * A model point maps to `px_x = (x - minX) * pixelsPerMeter`, `px_y = (maxY - y) * pixelsPerMeter`; the room outlines
 * of `coordinates.json` and the background drawing of the floor share this transform.
 */
data class FloorFrame(
  val windowMeters: ModelBounds,
  val pixelsPerMeter: Double,
  val widthPx: Int,
  val heightPx: Int,
) {
  /**
   * Maps a model point in meters to the pixels of the image, y down.
   */
  fun toPixel(xMeters: Double, yMeters: Double): Coordinates {
    return Coordinates.of(
      x = (xMeters - windowMeters.minX) * pixelsPerMeter,
      y = (windowMeters.maxY - yMeters) * pixelsPerMeter,
    )
  }

  companion object {
    /**
     * The longest side an image of a floor may have; a larger window lowers the pixels per meter.
     */
    const val MaxLongSidePx: Int = 6000

    /**
     * The frame of a floor whose room outlines span [boundsMeters]: the window adds [marginMeters] on every side.
     *
     * Rounds the size half to even, as the Python pipeline does, so both compute the same image size.
     */
    fun around(boundsMeters: ModelBounds, marginMeters: Double, pixelsPerMeter: Double): FloorFrame {
      val window = boundsMeters.expandedBy(marginMeters)
      val scale = min(pixelsPerMeter, MaxLongSidePx / max(max(window.width, window.height), 1e-9))
      return FloorFrame(
        windowMeters = window,
        pixelsPerMeter = scale,
        widthPx = max(round(window.width * scale).toInt(), 1),
        heightPx = max(round(window.height * scale).toInt(), 1),
      )
    }
  }
}
