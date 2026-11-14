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
 * One edge of a [DxfHatchBoundaryPath.Edges].
 */
sealed interface DxfHatchEdge {
  data class Line(val start: DxfPoint, val end: DxfPoint) : DxfHatchEdge

  /**
   * A circular arc. DXF stores a clockwise arc by the angles 360 - end and 360 - start; [startAngleDegrees] and
   * [endAngleDegrees] are the counterclockwise angles of the same arc.
   */
  data class Arc(
    val center: DxfPoint,
    val radius: Double,
    val startAngleDegrees: Double,
    val endAngleDegrees: Double,
  ) : DxfHatchEdge

  /**
   * An elliptic arc; [majorAxis] is relative to [center], the angles are counterclockwise like those of [Arc].
   */
  data class Ellipse(
    val center: DxfPoint,
    val majorAxis: DxfPoint,
    val ratio: Double,
    val startAngleDegrees: Double,
    val endAngleDegrees: Double,
  ) : DxfHatchEdge

  data class Spline(
    val degree: Int,
    val knots: List<Double>,
    val controlPoints: List<DxfPoint>,
    val weights: List<Double>,
    val fitPoints: List<DxfPoint>,
  ) : DxfHatchEdge
}
