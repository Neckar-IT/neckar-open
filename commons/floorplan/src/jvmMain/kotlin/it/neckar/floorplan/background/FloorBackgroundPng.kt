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

import java.awt.BasicStroke
import java.awt.Color
import java.awt.RenderingHints
import java.awt.geom.Ellipse2D
import java.awt.geom.Path2D
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

/**
 * Rasterizes a [FloorBackground] to a PNG of the size of its frame: antialiased ink on white.
 */
object FloorBackgroundPng {
  /**
   * The ink of the Python pipeline's background, a near black.
   */
  private val Ink: Color = Color(0x1a, 0x1a, 0x1a)

  fun render(background: FloorBackground): ByteArray {
    val image = BufferedImage(background.frame.widthPx, background.frame.heightPx, BufferedImage.TYPE_INT_RGB)
    val graphics = image.createGraphics()
    try {
      graphics.color = Color.WHITE
      graphics.fillRect(0, 0, image.width, image.height)
      graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
      graphics.color = Ink

      for (fill in background.fills) {
        val path = Path2D.Double(Path2D.WIND_EVEN_ODD)
        for (ring in fill.rings) {
          path.moveTo(ring[0], ring[1])
          for (index in 1 until ring.size / 2) {
            path.lineTo(ring[2 * index], ring[2 * index + 1])
          }
          path.closePath()
        }
        graphics.fill(path)
      }

      // One path per width: Java2D strokes a path of many segments far faster than many paths of one segment.
      for ((widthPx, strokes) in background.strokes.groupBy { it.widthPx }) {
        val path = Path2D.Double()
        for (stroke in strokes) {
          val coordinates = stroke.coordinates
          if (stroke.pointCount == 1) {
            graphics.fill(Ellipse2D.Double(coordinates[0] - widthPx / 2, coordinates[1] - widthPx / 2, widthPx, widthPx))
            continue
          }
          path.moveTo(coordinates[0], coordinates[1])
          for (index in 1 until stroke.pointCount) {
            path.lineTo(coordinates[2 * index], coordinates[2 * index + 1])
          }
        }
        graphics.stroke = BasicStroke(widthPx.toFloat(), BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND)
        graphics.draw(path)
      }
    } finally {
      graphics.dispose()
    }

    return ByteArrayOutputStream().use { output ->
      check(ImageIO.write(image, "png", output)) { "no PNG writer available" }
      output.toByteArray()
    }
  }
}
