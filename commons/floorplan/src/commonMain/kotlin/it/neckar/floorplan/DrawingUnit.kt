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
package it.neckar.floorplan

import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.sqrt

/**
 * The length of one drawing unit in meters.
 */
object DrawingUnit {
  /**
   * Meters per unit of each `$INSUNITS` code a floor plan uses.
   */
  private val MetersPerInsertionUnit: Map<Int, Double> = mapOf(
    1 to 0.0254, 2 to 0.3048, 4 to 0.001, 5 to 0.01, 6 to 1.0, 7 to 1000.0, 8 to 0.0000000254, 9 to 0.0000254,
    10 to 0.9144, 14 to 0.1,
  )

  /**
   * The units a room area can snap to: millimeter, centimeter, decimeter, meter.
   */
  private val MetricMetersPerUnit: List<Double> = listOf(0.001, 0.01, 0.1, 1.0)

  /**
   * Meters per unit of `$INSUNITS` [insertionUnits]; null for a drawing without a known unit.
   */
  fun metersPerUnit(insertionUnits: Int?): Double? = insertionUnits?.let { MetersPerInsertionUnit[it] }

  /**
   * Meters per unit from the median of stated area over outline area, snapped to mm, cm, dm or m; null without a stated
   * area. Metric only: a room under a sloped ceiling states less than its outline and would pull meters to yards.
   */
  fun metersPerUnitFromRoomAreas(rooms: List<StatedRoomArea>): Double? {
    val ratios = rooms.mapNotNull { room ->
      val outlineArea = room.outlineAreaInUnits
      val stated = room.statedAreaSquareMeters
      if (stated == null || stated == 0.0 || outlineArea <= 0.0) null else stated / outlineArea
    }.sorted()
    if (ratios.isEmpty()) {
      return null
    }
    val measured = sqrt(ratios[ratios.size / 2])
    return MetricMetersPerUnit.minBy { abs(ln(it / measured)) }
  }

  /**
   * A room label's stated area next to the outline it labels, in drawing units.
   */
  data class StatedRoomArea(
    val statedAreaSquareMeters: Double?,
    val outline: List<Pair<Double, Double>>,
  ) {
    val outlineAreaInUnits: Double
      get() = abs(outline.indices.sumOf { index ->
        val (x0, y0) = outline[index]
        val (x1, y1) = outline[(index + 1) % outline.size]
        x0 * y1 - x1 * y0
      }) / 2
  }
}
