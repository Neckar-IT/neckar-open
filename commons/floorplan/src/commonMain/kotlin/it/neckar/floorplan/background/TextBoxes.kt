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

import it.neckar.dxf.DxfMText
import it.neckar.dxf.DxfPoint
import it.neckar.dxf.DxfText
import it.neckar.floorplan.ModelBounds
import it.neckar.open.kotlin.lang.toRadians
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/**
 * Estimates the box a text covers, from character widths in units of the cap height.
 *
 * The widths follow the fallback font of ezdxf, so a leader ending at a text is recognized where the Python pipeline
 * recognizes it.
 */
internal object TextBoxes {
  private const val DescenderPerCapHeight: Double = 0.2783
  private const val LineHeightPerCapHeight: Double = 1.0 + DescenderPerCapHeight

  /**
   * MTEXT places the baselines of two lines 5/3 cap heights apart at a line spacing factor of 1.
   */
  private const val LineSpacingPerCapHeight: Double = 1.67

  private const val NarrowCharacters: String = "iIl1.,:;!|'jtf()[]"

  fun of(text: DxfText): ModelBounds? {
    if (text.text.isBlank()) {
      return null
    }
    val height = text.height
    var width = estimatedWidth(text.text) * height * text.widthFactor
    var rotation = text.rotationDegrees.toRadians()
    var location = text.insertionPoint
    var shiftX = 0.0
    val alignmentPoint = text.alignmentPoint
    val aligned = text.horizontalAlignment == 3 || text.horizontalAlignment == 5
    if (aligned && alignmentPoint != null) {
      width = (alignmentPoint - text.insertionPoint).length
      rotation = atan2(alignmentPoint.y - text.insertionPoint.y, alignmentPoint.x - text.insertionPoint.x)
    } else if ((text.horizontalAlignment != 0 || text.verticalAlignment != 0) && alignmentPoint != null) {
      location = alignmentPoint
      shiftX = when (text.horizontalAlignment) {
        1, 4 -> -width / 2
        2 -> -width
        else -> 0.0
      }
    }
    val shiftY = when {
      text.horizontalAlignment == 4 -> -height / 2
      text.verticalAlignment == 1 -> DescenderPerCapHeight * height
      text.verticalAlignment == 2 -> -height / 2
      text.verticalAlignment == 3 -> -height
      else -> 0.0
    }
    return rotatedBox(location, rotation, shiftX, shiftX + width, shiftY - DescenderPerCapHeight * height, shiftY + height)
  }

  fun of(text: DxfMText): ModelBounds? {
    val lines = text.text.split('\n')
    if (lines.all { it.isBlank() }) {
      return null
    }
    val capHeight = text.charHeight
    val width = if (text.referenceWidth > 0.0) text.referenceWidth else lines.maxOf { estimatedWidth(it) } * capHeight
    val lineHeight = LineHeightPerCapHeight * capHeight
    val spacing = LineSpacingPerCapHeight * capHeight * text.lineSpacingFactor - lineHeight
    val height = lines.size * lineHeight + (lines.size - 1) * spacing

    val attachment = text.attachmentPoint.coerceIn(1, 9) - 1
    val shiftX = when (attachment % 3) {
      1 -> -width / 2
      2 -> -width
      else -> 0.0
    }
    val shiftY = when (attachment / 3) {
      1 -> height / 2
      2 -> height
      else -> 0.0
    }
    val direction = text.direction
    val rotation = if (direction != null && (direction.x != 0.0 || direction.y != 0.0)) atan2(direction.y, direction.x) else text.rotationDegrees.toRadians()
    return rotatedBox(text.insertionPoint, rotation, shiftX, shiftX + width, shiftY - height, shiftY)
  }

  /**
   * Character widths per cap height, summed over [text].
   */
  private fun estimatedWidth(text: String): Double = text.sumOf { character ->
    when {
      character in NarrowCharacters -> 0.31
      character == ' ' -> 0.38
      character.isUpperCase() || character in "mw" -> 1.0
      else -> 0.75
    }
  }

  private fun rotatedBox(origin: DxfPoint, rotation: Double, minX: Double, maxX: Double, minY: Double, maxY: Double): ModelBounds? {
    val cos = cos(rotation)
    val sin = sin(rotation)
    return ModelBounds.around(
      listOf(minX to minY, maxX to minY, maxX to maxY, minX to maxY).map { (x, y) ->
        origin.x + x * cos - y * sin to origin.y + x * sin + y * cos
      }
    )
  }
}
