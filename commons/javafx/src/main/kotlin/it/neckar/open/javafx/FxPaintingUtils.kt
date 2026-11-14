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
package it.neckar.open.javafx

import it.neckar.open.unit.other.px
import javafx.scene.SnapshotParameters
import javafx.scene.canvas.Canvas
import javafx.scene.canvas.GraphicsContext
import javafx.scene.image.Image
import javafx.scene.paint.Color
import javafx.scene.paint.ImagePattern
import kotlin.math.ceil

/**
 * Helper methods for fx painting
 *
 */
object FxPaintingUtils {

  /**
   * Returns the *lower* value if snapToPixel is set to true
   */
  @JvmOverloads
  fun snapSize(value: @px Double, snapToPixel: Boolean = true): @px Double {
    return if (!snapToPixel) {
      value
    } else ceil(value)
  }

  /**
   * Rounds the position
   */
  @JvmOverloads
  fun snapPosition(value: @px Double, snapToPixel: Boolean = true): @px Double {
    return if (!snapToPixel) {
      value
    } else Math.round(value).toDouble()
  }

  fun createHatch(stroke: Color, lineWidth: Double, width: @px Double, height: @px Double): ImagePattern {
    val canvas = Canvas(width, height)
    val gc = canvas.graphicsContext2D
    gc.lineWidth = lineWidth
    gc.stroke = stroke
    hatch1(gc, width, height)
    val patternImage: Image = canvas.snapshot(SnapshotParameters(), null)
    return ImagePattern(patternImage, 0.0, 0.0, width, height, false)
  }

  fun createCrossed(stroke: Color, lineWidth: Double, width: @px Double, height: @px Double): ImagePattern {
    val canvas = Canvas(width, height)
    val gc = canvas.graphicsContext2D
    gc.lineWidth = lineWidth
    gc.stroke = stroke
    hatch1(gc, width, height)
    hatch2(gc, width, height)
    val patternImage: Image = canvas.snapshot(SnapshotParameters(), null)
    return ImagePattern(patternImage, 0.0, 0.0, width, height, false)
  }

  private fun hatch1(gc: GraphicsContext, width: @px Double, height: @px Double) {
    gc.beginPath()
    gc.moveTo(0.0, 0.0)
    gc.lineTo(width, height)
    gc.stroke()
    gc.beginPath()
    gc.moveTo(width / 2.0, -height / 2.0)
    gc.lineTo(width * 1.5, height / 2.0)
    gc.stroke()
    gc.beginPath()
    gc.moveTo(-width / 2.0, height / 2.0)
    gc.lineTo(width / 2.0, height * 1.5)
    gc.stroke()
  }

  private fun hatch2(gc: GraphicsContext, width: @px Double, height: @px Double) {
    gc.beginPath()
    gc.moveTo(width, 0.0)
    gc.lineTo(0.0, height)
    gc.stroke()
    gc.beginPath()
    gc.moveTo(-width / 2.0, height / 2.0)
    gc.lineTo(width / 2.0, -height / 2.0)
    gc.stroke()
    gc.beginPath()
    gc.moveTo(width / 2.0, height * 1.5)
    gc.lineTo(width * 1.5, height / 2.0)
    gc.stroke()
  }
}
