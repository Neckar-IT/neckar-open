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
 * A hatched or filled area (`HATCH`). Boundary paths and pattern lines lie in the object coordinate system at the
 * height [elevation].
 */
data class DxfHatch(
  override val layer: String,
  /**
   * Group code 70: the area is filled; otherwise the pattern lines are drawn.
   */
  val solidFill: Boolean,
  val patternName: String,
  /**
   * Group code 75: 0 nested (every path, islands alternate), 1 outermost, 2 ignore (external paths only).
   */
  val hatchStyle: Int,
  val elevation: Double,
  val boundaryPaths: List<DxfHatchBoundaryPath>,
  /**
   * The pattern lines, already scaled and rotated by the pattern scale and angle of the hatch.
   */
  val patternLines: List<DxfHatchPatternLine>,
  override val properties: DxfEntityProperties = DxfEntityProperties.Default,
) : DxfEntity
