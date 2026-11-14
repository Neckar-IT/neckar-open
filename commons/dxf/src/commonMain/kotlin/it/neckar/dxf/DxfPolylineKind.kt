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
 * The geometry a `POLYLINE` describes, from the flags of group code 70.
 */
sealed interface DxfPolylineKind {
  /**
   * A 2D polyline whose vertices lie in the object coordinate system at the elevation of the `POLYLINE`.
   */
  data class Polyline2d(val elevation: Double) : DxfPolylineKind

  /**
   * A 3D polyline whose vertices are world coordinates.
   */
  data object Polyline3d : DxfPolylineKind

  /**
   * A polyface mesh: each face names up to four vertices by their 1-based index; a negative index hides the edge that
   * starts at that vertex.
   */
  data class PolyfaceMesh(val faces: List<List<Int>>) : DxfPolylineKind

  /**
   * A polygon mesh of [rowCount] times [columnCount] vertices, stored row by row.
   */
  data class PolygonMesh(
    val rowCount: Int,
    val columnCount: Int,
    val closedRows: Boolean,
    val closedColumns: Boolean,
  ) : DxfPolylineKind
}
