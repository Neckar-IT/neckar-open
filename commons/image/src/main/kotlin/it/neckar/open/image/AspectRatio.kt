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

import java.io.Serializable

/**
 * Represents an aspect ratio
 */
data class AspectRatio(
  val widthFactor: Double,
  val heightFactor: Double,
) : Serializable {

  /**
   * The ratio of width factor to height factor
   */
  val ratio: Double
    get() = widthFactor / heightFactor

  fun invert(): AspectRatio {
    return AspectRatio(heightFactor, widthFactor)
  }

  companion object {
    @JvmField
    val AR_1_1: AspectRatio = AspectRatio(1.0, 1.0)

    @JvmField
    val AR_3_2: AspectRatio = AspectRatio(3.0, 2.0)

    @JvmField
    val AR_4_3: AspectRatio = AspectRatio(4.0, 3.0)

    @JvmField
    val AR_5_3: AspectRatio = AspectRatio(5.0, 3.0)

    @JvmField
    val AR_5_4: AspectRatio = AspectRatio(5.0, 4.0)

    @JvmField
    val AR_16_9: AspectRatio = AspectRatio(16.0, 9.0)

    @JvmField
    val AR_16_10: AspectRatio = AspectRatio(16.0, 10.0)

    /**
     * Used for paper (A4)...
     */
    @JvmField
    val AR_SQ2_1: AspectRatio = AspectRatio(Math.sqrt(2.0), 1.0)

    @JvmField
    val PRE_DEFINED: List<AspectRatio> = listOf(AR_1_1, AR_3_2, AR_4_3, AR_5_3, AR_5_4, AR_16_9, AR_16_10)

    /**
     * The bounds used when finding the aspect ratio for a width/height
     */
    const val Bounds: Double = 0.005

    private const val serialVersionUID = -1083087626635613019L

    /**
     * Returns the predefined aspect ratio
     *
     * @param width  the width
     * @param height the height
     * @return the aspect ratio
     */
    @JvmStatic
    operator fun get(width: Double, height: Double): AspectRatio {
      val ratio = width / height
      for (aspectRatio in PRE_DEFINED) {
        val delta = aspectRatio.ratio - ratio
        if (Math.abs(delta) < Bounds) {
          return aspectRatio
        }
      }
      return AspectRatio(width, height)
    }
  }
}
