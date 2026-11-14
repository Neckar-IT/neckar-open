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

import javafx.scene.image.Image
import javafx.scene.image.ImageView

/**
 * Returns the aspect ratio of an image (width/height)
 */
private val Image.aspectRatio: Double
  get() {
    return width / this.height
  }

/**
 * An image view that can be resized
 */
class ImageViewResizable() : ImageView() {
  init {
    isPreserveRatio = true
  }

  constructor(image: Image) : this() {
    this.image = image
  }

  override fun minWidth(height: Double): Double {
    return 40.0
  }

  override fun prefWidth(height: Double): Double {
    val image = image ?: return minWidth(height)

    if (height <= 0.0) {
      return image.width
    }

    return (height * image.aspectRatio).coerceAtMost(image.width)
  }

  override fun maxWidth(height: Double): Double {
    return 16384.0
  }

  override fun minHeight(width: Double): Double {
    return 40.0
  }

  override fun prefHeight(width: Double): Double {
    val image = image ?: return minHeight(width)

    if (width <= 0.0) {
      return image.height
    }

    return (width / image.aspectRatio).coerceAtMost(image.height)
  }

  override fun maxHeight(width: Double): Double {
    return 16384.0
  }

  override fun isResizable(): Boolean {
    return true
  }

  override fun resize(width: Double, height: Double) {
    fitWidth = width
    fitHeight = height
  }
}
