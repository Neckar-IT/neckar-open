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

import java.awt.Dimension
import java.io.Serializable

/**
 * Represents a resolution.
 * Resolutions can be compared using the width as first criteria and the height as second.
 *
 *
 * This means that 100/1 is larger than 99/Integer.MAX
 */
data class Resolution(
  val width: Int,
  val height: Int,
) : Comparable<Resolution>, Serializable {

  val aspectRatio: AspectRatio = AspectRatio[width.toDouble(), height.toDouble()]

  /**
   * Creates a new resolution based on the dimension
   */
  constructor(dimension: Dimension) : this(dimension.width, dimension.height) {}

  override fun compareTo(other: Resolution): Int {
    return if (width != other.width) {
      Integer.valueOf(width).compareTo(other.width)
    } else Integer.valueOf(height).compareTo(other.height)
  }

  override fun toString(): String {
    return "($width/$height)"
  }

  /**
   * Converts the resolution to a dimensino
   *
   * @return a dimension with the same width/height
   */
  fun toDimension(): Dimension {
    return Dimension(width, height)
  }

  companion object {
    private const val serialVersionUID = -8344104794069328642L
  }
}
