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

/**
 * The group codes from one code-0 marker (the record type) up to the next.
 */
internal class DxfRecord(
  val type: String,
  val codes: List<DxfGroupCode>,
) {
  fun string(code: Int): String? = codes.firstOrNull { it.code == code }?.value

  fun double(code: Int, default: Double): Double = codes.firstOrNull { it.code == code }?.doubleValue ?: default

  fun int(code: Int, default: Int): Int = codes.firstOrNull { it.code == code }?.intValue ?: default

  fun doubles(code: Int): List<Double> = codes.filter { it.code == code }.mapNotNull { it.doubleValue }

  /**
   * The point of the x code [xCode]; its y and z codes are 10 and 20 above it.
   */
  fun point(xCode: Int = 10, default: DxfPoint = DxfPoint(0.0, 0.0)): DxfPoint {
    return DxfPoint(
      x = double(xCode, default.x),
      y = double(xCode + 10, default.y),
      z = double(xCode + 20, default.z),
    )
  }

  fun hasPoint(xCode: Int): Boolean = codes.any { it.code == xCode }

  /**
   * Every point of the x code [xCode] in file order, each completed by the y and z codes that follow it.
   */
  fun points(xCode: Int): List<DxfPoint> {
    val points = mutableListOf<DxfPoint>()
    for (groupCode in codes) {
      val value = groupCode.doubleValue ?: continue
      when (groupCode.code) {
        xCode -> points.add(DxfPoint(value, 0.0))
        xCode + 10 -> if (points.isNotEmpty()) points[points.lastIndex] = points.last().copy(y = value)
        xCode + 20 -> if (points.isNotEmpty()) points[points.lastIndex] = points.last().copy(z = value)
      }
    }
    return points
  }
}
