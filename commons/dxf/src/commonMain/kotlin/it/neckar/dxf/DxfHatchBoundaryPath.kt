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
 * One boundary loop of a [DxfHatch].
 */
sealed interface DxfHatchBoundaryPath {
  /**
   * Group code 92: bit 1 external, bit 2 polyline, bit 4 derived, bit 8 text box, bit 16 outermost.
   */
  val flags: Int

  val isExternal: Boolean
    get() = flags and ExternalFlag != 0

  val isOutermost: Boolean
    get() = flags and OutermostFlag != 0

  /**
   * The path frames a text and is no part of the hatched area.
   */
  val isTextBox: Boolean
    get() = flags and TextBoxFlag != 0

  /**
   * A boundary given as a polyline with bulges.
   */
  data class Polyline(
    override val flags: Int,
    val vertices: List<DxfPoint>,
    val bulges: List<Double>,
    val closed: Boolean,
  ) : DxfHatchBoundaryPath

  /**
   * A boundary given as a sequence of edges.
   */
  data class Edges(
    override val flags: Int,
    val edges: List<DxfHatchEdge>,
  ) : DxfHatchBoundaryPath

  companion object {
    const val ExternalFlag: Int = 1
    const val TextBoxFlag: Int = 8
    const val OutermostFlag: Int = 16
  }
}
