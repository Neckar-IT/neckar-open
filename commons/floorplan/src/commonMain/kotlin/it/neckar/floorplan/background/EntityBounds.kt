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

import it.neckar.dxf.DxfArc
import it.neckar.dxf.DxfCircle
import it.neckar.dxf.DxfDocument
import it.neckar.dxf.DxfEllipse
import it.neckar.dxf.DxfEntity
import it.neckar.dxf.DxfFace3d
import it.neckar.dxf.DxfHatch
import it.neckar.dxf.DxfHatchBoundaryPath
import it.neckar.dxf.DxfHatchEdge
import it.neckar.dxf.DxfInsert
import it.neckar.dxf.DxfLine
import it.neckar.dxf.DxfLwPolyline
import it.neckar.dxf.DxfMText
import it.neckar.dxf.DxfPoint
import it.neckar.dxf.DxfPointEntity
import it.neckar.dxf.DxfPolyline
import it.neckar.dxf.DxfPolylineKind
import it.neckar.dxf.DxfSegmentWidth
import it.neckar.dxf.DxfSolid
import it.neckar.dxf.DxfSpline
import it.neckar.dxf.DxfText
import it.neckar.floorplan.ModelBounds
import kotlin.math.abs

/**
 * The world bounds of an entity, at least as large as what it draws; null for an entity that draws nothing measurable
 * (an `XLINE`, an empty block, an unknown type).
 */
internal class EntityBounds(private val document: DxfDocument) {
  /**
   * By block name and the depth it is measured at: the depth limit cuts a deeper measurement shorter.
   */
  private val blockBounds = mutableMapOf<Pair<String, Int>, ModelBounds?>()

  fun of(entity: DxfEntity): ModelBounds? = when (entity) {
    is DxfText -> TextBoxes.of(entity)
    is DxfMText -> TextBoxes.of(entity)
    else -> geometryBounds(entity, AffineTransform.Identity, 0)
  }

  /**
   * The bounds of what [entity] draws in the background, which leaves out every text.
   */
  private fun geometryBounds(entity: DxfEntity, transform: AffineTransform, depth: Int): ModelBounds? {
    val objectToWorld by lazy { transform.after(AffineTransform.objectToWorld(entity.properties.extrusion)) }
    return when (entity) {
      is DxfLine -> around(transform, listOf(entity.start, entity.end))
      is DxfLwPolyline -> around(objectToWorld, entity.vertices.map { it.copy(z = entity.elevation) }, polylineMargin(entity.vertices, entity.bulges, entity.widths))
      is DxfPolyline -> when (val kind = entity.kind) {
        is DxfPolylineKind.Polyline2d -> around(objectToWorld, entity.vertices.map { it.copy(z = kind.elevation) }, polylineMargin(entity.vertices, entity.bulges, entity.widths))
        else -> around(transform, entity.vertices)
      }

      is DxfCircle -> around(objectToWorld, listOf(entity.center), entity.radius)
      is DxfArc -> around(objectToWorld, listOf(entity.center), entity.radius)
      is DxfEllipse -> around(transform, listOf(entity.center), entity.majorAxis.length * maxOf(1.0, abs(entity.ratio)))
      is DxfSpline -> around(transform, entity.controlPoints + entity.fitPoints)
      // A point symbol reaches its size from the location, unscaled by a block reference.
      is DxfPointEntity -> around(listOf(transform.apply(entity.location)), document.header.pointDisplaySize.takeIf { it > 1.0 } ?: 1.0)
      is DxfSolid -> around(objectToWorld, entity.corners)
      is DxfFace3d -> around(transform, entity.corners)
      is DxfHatch -> hatchBounds(entity, objectToWorld)
      is DxfInsert -> insertBounds(entity, transform, depth)
      else -> null
    }
  }

  private fun hatchBounds(hatch: DxfHatch, objectToWorld: AffineTransform): ModelBounds? {
    val atElevation = objectToWorld.after(AffineTransform.translation(DxfPoint(0.0, 0.0, hatch.elevation)))
    return hatch.boundaryPaths.mapNotNull { path ->
      when (path) {
        is DxfHatchBoundaryPath.Polyline -> around(atElevation, path.vertices, bulgeMargin(path.vertices, path.bulges))
        is DxfHatchBoundaryPath.Edges -> path.edges.mapNotNull { edge ->
          when (edge) {
            is DxfHatchEdge.Line -> around(atElevation, listOf(edge.start, edge.end))
            is DxfHatchEdge.Arc -> around(atElevation, listOf(edge.center), edge.radius)
            is DxfHatchEdge.Ellipse -> around(atElevation, listOf(edge.center), edge.majorAxis.length * maxOf(1.0, abs(edge.ratio)))
            is DxfHatchEdge.Spline -> around(atElevation, edge.controlPoints + edge.fitPoints)
          }
        }.reduceOrNull { a, b -> a.union(b) }
      }
    }.reduceOrNull { a, b -> a.union(b) }
  }

  private fun insertBounds(insert: DxfInsert, transform: AffineTransform, depth: Int): ModelBounds? {
    val block = document.block(insert.blockName) ?: return null
    val local = blockBounds(block.name, depth) ?: return null
    val corners = listOf(
      DxfPoint(local.minX, local.minY), DxfPoint(local.maxX, local.minY), DxfPoint(local.maxX, local.maxY), DxfPoint(local.minX, local.maxY),
    )
    val lastColumn = BlockReference.columns(insert) - 1
    val lastRow = BlockReference.rows(insert) - 1
    val cells = listOf(0 to 0, lastColumn to lastRow, lastColumn to 0, 0 to lastRow)
    return around(cells.flatMap { (column, row) -> corners.map { transform.after(BlockReference.transform(insert, block, column, row)).apply(it) } }, 0.0)
  }

  /**
   * The bounds of a block's content in block coordinates at nesting [depth]; null for an empty block and beyond
   * [FloorBackgroundGeometry.MaxBlockDepth], where the drawing stops as well, cycles included.
   */
  private fun blockBounds(name: String, depth: Int): ModelBounds? {
    if (depth >= FloorBackgroundGeometry.MaxBlockDepth) {
      return null
    }
    val key = name.uppercase() to depth
    // containsKey, as an empty block caches null.
    if (blockBounds.containsKey(key)) {
      return blockBounds[key]
    }
    val bounds = document.block(name)?.entities
      ?.mapNotNull { geometryBounds(it, AffineTransform.Identity, depth + 1) }
      ?.reduceOrNull { a, b -> a.union(b) }
    blockBounds[key] = bounds
    return bounds
  }

  /**
   * How far a polyline reaches beyond the box of its vertices: its bulges plus half its widest segment.
   */
  private fun polylineMargin(vertices: List<DxfPoint>, bulges: List<Double>, widths: List<DxfSegmentWidth>): Double =
    bulgeMargin(vertices, bulges) + (widths.maxOfOrNull { maxOf(abs(it.start), abs(it.end)) } ?: 0.0) / 2

  /**
   * How far a bulged segment reaches beyond the box of its vertices: the sagitta, half the chord times the bulge, up to a
   * half circle; beyond, at most the chord times the bulge.
   */
  private fun bulgeMargin(vertices: List<DxfPoint>, bulges: List<Double>): Double {
    var margin = 0.0
    for (index in vertices.indices) {
      val bulge = abs(bulges.getOrElse(index) { 0.0 })
      val chord = (vertices[(index + 1) % vertices.size] - vertices[index]).length
      margin = maxOf(margin, if (bulge <= 1.0) chord * bulge / 2 else chord * bulge)
    }
    return margin
  }

  private fun around(transform: AffineTransform, points: List<DxfPoint>, margin: Double = 0.0): ModelBounds? {
    if (points.isEmpty()) {
      return null
    }
    val scaledMargin = margin * transform.maxPlanarScale
    return around(points.map { transform.apply(it) }, scaledMargin)
  }

  private fun around(worldPoints: List<DxfPoint>, margin: Double): ModelBounds? {
    return ModelBounds.around(worldPoints.map { it.x to it.y })?.expandedBy(margin)
  }
}
