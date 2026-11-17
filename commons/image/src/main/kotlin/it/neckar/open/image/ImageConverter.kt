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
package it.neckar.open.image

import java.awt.Color
import java.awt.Dimension
import java.awt.Image
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.awt.image.RenderedImage

/**
 *
 * ImageConverter class.
 *
 */
object ImageConverter {
  /**
   * Calculates the new dimension of the image
   *
   * @param original           the original image
   * @param originalDPI the original resolution
   * @param targetDPI   the target resolution
   * @return the dimension
   */
  fun calculateNewDimensions(original: RenderedImage, originalDPI: DPI, targetDPI: DPI): Dimension {
    val newWidth = original.width * targetDPI.dpi / originalDPI.dpi
    val newHeight = original.height * targetDPI.dpi / originalDPI.dpi
    return Dimension(newWidth, newHeight)
  }

  /**
   * Resizes the image to the given size. The result has no alpha channel: a transparent pixel of [original] shows
   * [background].
   */
  fun resize(original: Image, targetDimension: Dimension, background: Color = Color.BLACK): BufferedImage {
    val resized = BufferedImage(targetDimension.width, targetDimension.height, BufferedImage.TYPE_INT_RGB)
    val graphics2D = resized.createGraphics()
    try {
      graphics2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC)
      graphics2D.color = background
      graphics2D.fillRect(0, 0, targetDimension.width, targetDimension.height)
      graphics2D.drawImage(original, 0, 0, targetDimension.width, targetDimension.height, null)
    } finally {
      graphics2D.dispose()
    }
    return resized
  }

  /**
   *
   * resize
   *
   * @param original           a BufferedImage object.
   * @param originalDPI a DPI object.
   * @param targetDPI   a DPI object.
   * @return a BufferedImage object.
   */
  fun resize(original: BufferedImage, originalDPI: DPI, targetDPI: DPI): BufferedImage {
    return resize(original, calculateNewDimensions(original, originalDPI, targetDPI))
  }
}
