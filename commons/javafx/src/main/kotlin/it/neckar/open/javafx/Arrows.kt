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
import javafx.scene.canvas.GraphicsContext
import javafx.scene.shape.LineTo
import javafx.scene.shape.MoveTo
import javafx.scene.shape.Path

/**
 * Helper class that creates arrows
 */
object Arrows {
  private const val ArrowWidth = 5.0
  private const val ArrowWidthHalf = ArrowWidth / 2.0
  private const val ArrowLength = 5.0

  @JvmStatic
  fun createArrowToTop(lineLength: @px Int): Path {
    return Path().apply {
      styleClass.add("arrow")
      elements.add(MoveTo(0.0, -ArrowLength)) //start @ top of arrow
      elements.add(LineTo(ArrowWidthHalf, 0.0)) //bottom right
      elements.add(LineTo(-ArrowWidthHalf, 0.0)) //bottom left
      elements.add(LineTo(0.0, -ArrowLength)) //back to top of arrow
      elements.add(MoveTo(0.0, 0.0)) //middle bottom
      elements.add(LineTo(0.0, lineLength.toDouble())) //middle bottom
    }
  }

  @JvmStatic
  fun addArrowPath(arrowLength: Double, lineTotalLength: @px Double, gc: GraphicsContext) {
    gc.beginPath()
    gc.moveTo(0.0, 0.0)
    gc.lineTo(arrowLength / 2.0, +arrowLength) //bottom right
    gc.lineTo(-(arrowLength / 2.0), +arrowLength) //bottom left
    gc.lineTo(0.0, 0.0) //back to top of arrow
    gc.lineTo(0.0, +arrowLength) //middle bottom
    gc.lineTo(0.0, lineTotalLength) //middle bottom
    gc.closePath()
  }
}
