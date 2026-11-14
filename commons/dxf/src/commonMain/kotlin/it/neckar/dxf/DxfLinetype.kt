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
package it.neckar.dxf

import kotlin.math.abs

/**
 * An entry of the `LTYPE` table.
 */
data class DxfLinetype(
  val name: String,
  /**
   * The length of one repetition of the pattern (group code 40).
   */
  val patternLength: Double,
  /**
   * The pattern elements (group code 49): a dash if positive, a gap if negative, a dot if 0.
   */
  val elements: List<Double>,
  /**
   * The pattern embeds text or shapes (group code 340).
   */
  val complex: Boolean,
) {
  /**
   * The pattern as alternating dash and gap lengths starting with a dash, a dash of 0 being a dot; empty for a
   * continuous line. A complex linetype draws as a continuous line.
   */
  fun dashesAndGaps(): List<Double> {
    if (complex || elements.size < 2 || patternLength <= 0.0) {
      return emptyList()
    }
    val merged = mergeConsecutiveElementsOfOneKind(elements)
    if (merged.size < 2) {
      return emptyList()
    }
    val sum = merged.sumOf { abs(it) }
    if (patternLength > sum) {
      merged.add(sum - patternLength)
    }
    if (merged.first() < 0.0) {
      val leadingGap = merged.removeAt(0)
      if (merged.last() < 0.0) {
        merged[merged.lastIndex] = merged.last() + leadingGap
      } else {
        merged.add(leadingGap)
      }
    }
    return merged.map { abs(it) }
  }

  private fun mergeConsecutiveElementsOfOneKind(elements: List<Double>): MutableList<Double> {
    val merged = mutableListOf<Double>()
    for (element in elements) {
      if (merged.isNotEmpty() && kindOf(merged.last()) == kindOf(element)) {
        merged[merged.lastIndex] = merged.last() + element
      } else {
        merged.add(element)
      }
    }
    return merged
  }

  private fun kindOf(element: Double): Int = when {
    element < 0.0 -> -1
    element > 0.0 -> 1
    else -> 0
  }
}
