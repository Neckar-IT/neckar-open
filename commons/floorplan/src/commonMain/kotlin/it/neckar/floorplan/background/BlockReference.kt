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

import it.neckar.dxf.DxfBlock
import it.neckar.dxf.DxfInsert
import it.neckar.dxf.DxfPoint
import it.neckar.open.kotlin.lang.toRadians
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

/**
 * The placement of block content by an [DxfInsert].
 */
internal object BlockReference {
  /**
   * The columns of the grid of [insert]: a `MINSERT` without a positive count places its block once.
   */
  fun columns(insert: DxfInsert): Int = max(insert.columnCount, 1)

  fun rows(insert: DxfInsert): Int = max(insert.rowCount, 1)

  /**
   * The map from the coordinates of [block] to the world coordinates of the cell [column], [row] of [insert]:
   * `objectToWorld(rotate(rotation) * scale * (point - basePoint) + insertionPoint + cellOffset)`.
   */
  fun transform(insert: DxfInsert, block: DxfBlock, column: Int, row: Int): AffineTransform {
    val rotation = insert.rotationDegrees.toRadians()
    val cellX = column * insert.columnSpacing
    val cellY = row * insert.rowSpacing
    val cellOffset = DxfPoint(cellX * cos(rotation) - cellY * sin(rotation), cellX * sin(rotation) + cellY * cos(rotation))
    return AffineTransform.objectToWorld(insert.properties.extrusion)
      .after(AffineTransform.translation(insert.insertionPoint + cellOffset))
      .after(AffineTransform.rotationZ(rotation))
      .after(AffineTransform.scale(insert.scale))
      .after(AffineTransform.translation(block.basePoint * -1.0))
  }
}
