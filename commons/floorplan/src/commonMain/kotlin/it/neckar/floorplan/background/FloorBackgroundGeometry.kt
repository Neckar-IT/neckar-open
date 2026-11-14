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
import it.neckar.dxf.DxfAttribute
import it.neckar.dxf.DxfCircle
import it.neckar.dxf.DxfDimension
import it.neckar.dxf.DxfDocument
import it.neckar.dxf.DxfEllipse
import it.neckar.dxf.DxfEntity
import it.neckar.dxf.DxfEntityProperties
import it.neckar.dxf.DxfFace3d
import it.neckar.dxf.DxfHatch
import it.neckar.dxf.DxfInsert
import it.neckar.dxf.DxfLine
import it.neckar.dxf.DxfLineweight
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
import it.neckar.dxf.DxfUnknownEntity
import it.neckar.floorplan.FloorFrame
import it.neckar.floorplan.ModelBounds
import it.neckar.open.kotlin.lang.toRadians
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sqrt

/**
 * The drawing of one floor as the background of its rooms: every layer in black, without annotation and without the
 * leaders of its texts. The rules follow `render_background` in `internal/open/commons/floorplan/tools/floorplan_pipeline.py`,
 * the reference of the pixel comparison in the task testImport of floorplan.neckar.it.
 */
object FloorBackgroundGeometry {
  /**
   * An entity whose bounds reach this far beyond the window is drawn into it.
   */
  const val EntityPadMeters: Double = 0.5

  /**
   * The distance from a text box within which the end of an open line marks the line as the leader of that text.
   */
  const val LeaderMarginMeters: Double = 0.15

  /**
   * The most work one floor may cost: every entity visited, every point of a stroke or fill, inside the image or not,
   * and every vertex of a hatch boundary.
   */
  const val MaxPointCount: Int = 5_000_000

  /**
   * Block references nested deeper are left out.
   */
  const val MaxBlockDepth: Int = 32

  /**
   * A `MINSERT` with more cells is left out.
   */
  const val MaxGridCells: Int = 10_000

  /**
   * The background of the floor in [frame] of a drawing whose unit is [metersPerUnit] meters.
   *
   * @throws FloorBackgroundTooComplexException when the floor costs more than [maxPointCount], see [MaxPointCount]
   */
  fun draw(document: DxfDocument, metersPerUnit: Double, frame: FloorFrame, maxPointCount: Int = MaxPointCount): FloorBackground {
    require(metersPerUnit > 0.0) { "metersPerUnit must be positive but was $metersPerUnit" }
    val windowUnits = frame.windowMeters.scaledBy(1 / metersPerUnit)
    val reach = windowUnits.expandedBy(EntityPadMeters / metersPerUnit)

    val entityBounds = EntityBounds(document)
    val candidates = document.modelSpaceEntities.mapNotNull { entity ->
      entityBounds.of(entity)?.takeIf { it.intersects(reach) }?.let { entity to it }
    }

    val drawer = Drawer(document, metersPerUnit, frame, maxPointCount)
    for (entity in withoutAnnotation(candidates, LeaderMarginMeters / metersPerUnit)) {
      drawer.draw(entity, AffineTransform.Identity, null, 0)
    }
    return FloorBackground(frame, drawer.strokes, drawer.fills)
  }

  /**
   * The entities of [candidates] without the leaders of the texts among them; [Drawer.draw] leaves out the texts.
   */
  private fun withoutAnnotation(candidates: List<Pair<DxfEntity, ModelBounds>>, leaderMargin: Double): List<DxfEntity> {
    val textBoxesByLayer = candidates
      .filter { (entity, _) -> entity is DxfText || entity is DxfMText }
      .groupBy({ (entity, _) -> entity.layer }, { (_, bounds) -> bounds.expandedBy(leaderMargin) })

    return candidates.map { it.first }.filter { entity ->
      val boxes = textBoxesByLayer[entity.layer] ?: return@filter true
      openEnds(entity).none { (x, y) -> boxes.any { box -> x >= box.minX && x <= box.maxX && y >= box.minY && y <= box.maxY } }
    }
  }

  private val AnnotationTypeNames: Set<String> = setOf("ATTDEF", "ARC_DIMENSION", "LEADER", "MLEADER", "MULTILEADER", "TOLERANCE")

  private fun isAnnotation(entity: DxfEntity): Boolean = when (entity) {
    is DxfText, is DxfMText, is DxfAttribute, is DxfDimension -> true
    is DxfUnknownEntity -> entity.type in AnnotationTypeNames
    else -> false
  }

  /**
   * The ends of an open LINE, LWPOLYLINE or POLYLINE in the coordinates the entity stores; none for any other entity.
   */
  private fun openEnds(entity: DxfEntity): List<Pair<Double, Double>> = when (entity) {
    is DxfLine -> listOf(entity.start.x to entity.start.y, entity.end.x to entity.end.y)
    is DxfLwPolyline -> if (entity.closed || entity.vertices.isEmpty()) emptyList() else listOf(entity.vertices.first(), entity.vertices.last()).map { it.x to it.y }
    is DxfPolyline -> if (entity.closed || entity.vertices.isEmpty()) emptyList() else listOf(entity.vertices.first(), entity.vertices.last()).map { it.x to it.y }
    else -> emptyList()
  }

  /**
   * The layer, linetype and lineweight an entity draws with, after the references to its layer and block.
   */
  private class Style(
    val layer: String,
    val linetypeName: String,
    val dashesAndGaps: List<Double>,
    val lineweightMillimeters: Double,
  )

  private class Drawer(
    private val document: DxfDocument,
    private val metersPerUnit: Double,
    private val frame: FloorFrame,
    private val maxPointCount: Int,
  ) {
    val strokes: MutableList<FloorBackground.Stroke> = mutableListOf()
    val fills: MutableList<FloorBackground.Fill> = mutableListOf()

    private val pixelsPerUnit: Double = frame.pixelsPerMeter * metersPerUnit

    /**
     * The deviation of a tessellated curve from the curve, in drawing units: a quarter pixel.
     */
    private val worldTolerance: Double = 0.25 / pixelsPerUnit

    private var pointCount: Int = 0
    private val scaledPatterns = mutableMapOf<Pair<List<Double>, Double>, DoubleArray?>()

    fun draw(entity: DxfEntity, transform: AffineTransform, parent: Style?, depth: Int) {
      // Block references multiply their content, so the entities drawn count against the point limit too.
      count(1)
      if (isAnnotation(entity)) {
        return
      }
      val style = resolve(entity, parent)
      if (entity is DxfInsert) {
        if (entity.properties.invisible.not()) {
          drawInsert(entity, transform, style, depth)
        }
        return
      }
      val layer = document.layer(style.layer)
      if (entity.properties.invisible || (layer != null && layer.visible.not())) {
        return
      }
      val objectToWorld = transform.after(AffineTransform.objectToWorld(entity.properties.extrusion))
      val linetypeScale = entity.properties.linetypeScale
      when (entity) {
        is DxfLine -> stroke(listOf(transform.apply(entity.start), transform.apply(entity.end)), style, linetypeScale)
        is DxfLwPolyline -> polyline(entity.vertices.map { it.copy(z = entity.elevation) }, entity.bulges, entity.widths, entity.closed, objectToWorld, style, linetypeScale)
        is DxfPolyline -> drawPolyline(entity, transform, objectToWorld, style)
        is DxfCircle -> stroke(Tessellation.arc(entity.center, entity.radius, 0.0, FullTurn, localTolerance(objectToWorld)).map { objectToWorld.apply(it) }, style, linetypeScale)
        is DxfArc -> {
          val span = Tessellation.angleSpanDegrees(entity.startAngleDegrees, entity.endAngleDegrees)
          if (span > 0.0) {
            val points = Tessellation.arc(entity.center, entity.radius, entity.startAngleDegrees.toRadians(), span.toRadians(), localTolerance(objectToWorld))
            stroke(points.map { objectToWorld.apply(it) }, style, linetypeScale)
          }
        }

        is DxfEllipse -> {
          val minorAxis = entity.properties.extrusion.normalized().cross(entity.majorAxis) * entity.ratio
          val points = Tessellation.ellipse(entity.center, entity.majorAxis, minorAxis, entity.startParameter, entity.endParameter, localTolerance(transform))
          stroke(points.map { transform.apply(it) }, style, linetypeScale)
        }

        is DxfSpline -> {
          val points = if (entity.controlPoints.isNotEmpty()) {
            Tessellation.spline(entity.degree, entity.knots, entity.controlPoints, entity.weights, localTolerance(transform))
          } else {
            entity.fitPoints
          }
          stroke(points.map { transform.apply(it) }, style, linetypeScale)
        }

        is DxfPointEntity -> drawPoint(entity, transform, style)
        is DxfSolid -> fill(listOf(listOf(0, 1, 3, 2).map { objectToWorld.apply(entity.corners[it]) }))
        is DxfFace3d -> drawFace(entity, transform, style)
        is DxfHatch -> drawHatch(entity, objectToWorld, style)
        else -> Unit
      }
    }

    private fun resolve(entity: DxfEntity, parent: Style?): Style {
      val layerName = if (entity.layer == "0" && parent != null) parent.layer else entity.layer
      val layer = document.layer(layerName)
      val linetypeName = entity.properties.linetype.uppercase()
      val (resolvedName, dashesAndGaps) = when (linetypeName) {
        DxfEntityProperties.ByLayer -> {
          val name = (layer?.linetype ?: "CONTINUOUS").uppercase()
          name to document.linetype(name)?.dashesAndGaps().orEmpty()
        }

        DxfEntityProperties.ByBlock -> (parent?.linetypeName ?: "STANDARD") to parent?.dashesAndGaps.orEmpty()
        else -> linetypeName to document.linetype(linetypeName)?.dashesAndGaps().orEmpty()
      }
      val lineweight = entity.properties.lineweight
      val lineweightMillimeters = when {
        lineweight.isByLayer -> layer?.lineweight?.millimeters ?: DxfLineweight.DefaultMillimeters
        lineweight.isByBlock -> parent?.lineweightMillimeters ?: DxfLineweight.DefaultMillimeters
        else -> lineweight.millimeters
      }
      return Style(layerName, resolvedName, dashesAndGaps, lineweightMillimeters)
    }

    private fun drawInsert(insert: DxfInsert, transform: AffineTransform, style: Style, depth: Int) {
      val block = document.block(insert.blockName) ?: return
      val columns = BlockReference.columns(insert)
      val rows = BlockReference.rows(insert)
      if (depth >= MaxBlockDepth || columns.toLong() * rows > MaxGridCells) {
        return
      }
      for (row in 0 until rows) {
        for (column in 0 until columns) {
          val cellTransform = transform.after(BlockReference.transform(insert, block, column, row))
          for (entity in block.entities) {
            draw(entity, cellTransform, style, depth + 1)
          }
        }
      }
    }

    private fun drawPolyline(polyline: DxfPolyline, transform: AffineTransform, objectToWorld: AffineTransform, style: Style) {
      val linetypeScale = polyline.properties.linetypeScale
      when (val kind = polyline.kind) {
        is DxfPolylineKind.Polyline2d -> polyline(polyline.vertices.map { it.copy(z = kind.elevation) }, polyline.bulges, polyline.widths, polyline.closed, objectToWorld, style, linetypeScale)
        DxfPolylineKind.Polyline3d -> {
          val points = polyline.vertices.map { transform.apply(it) }
          stroke(if (polyline.closed && points.isNotEmpty()) points + points.first() else points, style, linetypeScale)
        }

        is DxfPolylineKind.PolyfaceMesh -> for (face in kind.faces) {
          for (index in face.indices) {
            val start = face[index]
            val end = face[(index + 1) % face.size]
            val from = polyline.vertices.getOrNull(abs(start) - 1) ?: continue
            val to = polyline.vertices.getOrNull(abs(end) - 1) ?: continue
            if (start > 0) {
              stroke(listOf(transform.apply(from), transform.apply(to)), style, linetypeScale)
            }
          }
        }

        is DxfPolylineKind.PolygonMesh -> {
          val grid = polyline.vertices.map { transform.apply(it) }
          if (kind.rowCount < 1 || kind.columnCount < 1 || grid.size.toLong() < kind.rowCount.toLong() * kind.columnCount) {
            // A damaged mesh: no rows, no columns or fewer vertices than they name.
            return
          }
          for (row in 0 until kind.rowCount) {
            val points = (0 until kind.columnCount).map { grid[row * kind.columnCount + it] }
            stroke(if (kind.closedColumns) points + points.first() else points, style, linetypeScale)
          }
          for (column in 0 until kind.columnCount) {
            val points = (0 until kind.rowCount).map { grid[it * kind.columnCount + column] }
            stroke(if (kind.closedRows) points + points.first() else points, style, linetypeScale)
          }
        }
      }
    }

    /**
     * A 2D polyline in the object coordinate system: hairline segments as strokes, segments with a width as filled bands.
     */
    private fun polyline(
      vertices: List<DxfPoint>,
      bulges: List<Double>,
      widths: List<DxfSegmentWidth>,
      closed: Boolean,
      objectToWorld: AffineTransform,
      style: Style,
      linetypeScale: Double,
    ) {
      if (vertices.isEmpty()) {
        return
      }
      val tolerance = localTolerance(objectToWorld)
      val segmentCount = if (closed) vertices.size else vertices.size - 1
      if (widths.all { it.isHairline }) {
        val points = mutableListOf(vertices.first())
        for (index in 0 until segmentCount) {
          points.addAll(Tessellation.bulgeSegment(vertices[index], vertices[(index + 1) % vertices.size], bulges.getOrElse(index) { 0.0 }, tolerance))
          requireBudget(points.size.toDouble())
        }
        stroke(points.map { objectToWorld.apply(it) }, style, linetypeScale)
        return
      }
      for (index in 0 until segmentCount) {
        val start = vertices[index]
        val centerLine = listOf(start) + Tessellation.bulgeSegment(start, vertices[(index + 1) % vertices.size], bulges.getOrElse(index) { 0.0 }, tolerance)
        val width = widths.getOrElse(index) { DxfSegmentWidth.Hairline }
        if (width.isHairline) {
          stroke(centerLine.map { objectToWorld.apply(it) }, style, linetypeScale)
        } else {
          fill(listOf(band(centerLine, width).map { objectToWorld.apply(it) }))
        }
      }
    }

    /**
     * The outline of [centerLine] widened from [DxfSegmentWidth.start] to [DxfSegmentWidth.end]: the left edge forward,
     * the right edge back.
     */
    private fun band(centerLine: List<DxfPoint>, width: DxfSegmentWidth): List<DxfPoint> {
      val length = centerLine.zipWithNext().sumOf { (a, b) -> (b - a).length }
      val left = mutableListOf<DxfPoint>()
      val right = mutableListOf<DxfPoint>()
      var travelled = 0.0
      for (index in centerLine.indices) {
        if (index > 0) {
          travelled += (centerLine[index] - centerLine[index - 1]).length
        }
        val before = centerLine[maxOf(index - 1, 0)]
        val after = centerLine[minOf(index + 1, centerLine.lastIndex)]
        val tangent = (after - before).normalized()
        val normal = DxfPoint(-tangent.y, tangent.x)
        val halfWidth = (width.start + (width.end - width.start) * (if (length == 0.0) 0.0 else travelled / length)) / 2
        left.add(centerLine[index] + normal * halfWidth)
        right.add(centerLine[index] - normal * halfWidth)
      }
      return left + right.reversed()
    }

    private fun drawPoint(point: DxfPointEntity, transform: AffineTransform, style: Style) {
      if (style.layer.equals("defpoints", ignoreCase = true)) {
        return
      }
      val center = transform.apply(point.location)
      val mode = document.header.pointDisplayMode
      // ezdxf draws a relative size (0 or negative) at one drawing unit.
      val size = document.header.pointDisplaySize.takeIf { it > 0.0 } ?: 1.0
      val linetypeScale = point.properties.linetypeScale
      fun symmetricLine(dx: Double, dy: Double) = stroke(listOf(center - DxfPoint(dx, dy), center + DxfPoint(dx, dy)), style, linetypeScale)

      when (mode and 7) {
        0 -> dot(center)
        2 -> {
          symmetricLine(size, 0.0)
          symmetricLine(0.0, size)
        }

        3 -> {
          symmetricLine(size, size)
          symmetricLine(size, -size)
        }

        4 -> stroke(listOf(center, center + DxfPoint(0.0, size / 2)), style, linetypeScale)
      }
      if (mode and 64 != 0) {
        val half = size / 2
        stroke(listOf(DxfPoint(-half, -half), DxfPoint(half, -half), DxfPoint(half, half), DxfPoint(-half, half), DxfPoint(-half, -half)).map { center + it }, style, linetypeScale)
      }
      if (mode and 32 != 0) {
        stroke(Tessellation.arc(center, size / 2, 0.0, FullTurn, worldTolerance), style, linetypeScale)
      }
    }

    private fun drawFace(face: DxfFace3d, transform: AffineTransform, style: Style) {
      val corners = face.corners.map { transform.apply(it) }
      val linetypeScale = face.properties.linetypeScale
      if (face.invisibleEdges and 15 == 0) {
        stroke(corners + corners.first(), style, linetypeScale)
        return
      }
      for (index in 0 until 4) {
        if (face.invisibleEdges and (1 shl index) == 0) {
          stroke(listOf(corners[index], corners[(index + 1) % 4]), style, linetypeScale)
        }
      }
    }

    private fun drawHatch(hatch: DxfHatch, objectToWorld: AffineTransform, style: Style) {
      val loops = HatchGeometry.loops(hatch, localTolerance(objectToWorld))
      if (loops.isEmpty()) {
        return
      }
      // The boundary is tessellated whether filled or hatched, so its points count once here.
      count(loops.sumOf { it.size })
      val worldLoops = loops.map { loop -> loop.map { objectToWorld.apply(it) } }
      if (hatch.solidFill) {
        fill(worldLoops, counted = true)
        return
      }
      if (hatch.patternLines.isEmpty()) {
        return
      }
      val segments = HatchGeometry.patternSegments(hatch, loops, ((maxPointCount.toLong() - pointCount) / 2 + 1).coerceAtMost(Int.MAX_VALUE.toLong()).toInt())
      if (segments == null) {
        fill(worldLoops, counted = true)
        return
      }
      val widthPx = widthPx(style)
      for ((start, end) in segments) {
        if (start == end) {
          dot(objectToWorld.apply(start))
        } else {
          count(2)
          addStroke(pixelCoordinates(worldCoordinates(listOf(objectToWorld.apply(start), objectToWorld.apply(end)))), widthPx)
        }
      }
    }

    private fun localTolerance(transform: AffineTransform): Double {
      val scale = transform.maxPlanarScale
      return if (scale > 0.0) worldTolerance / scale else worldTolerance
    }

    /**
     * Draws a polyline in world coordinates with the dashes of the linetype of [style].
     */
    private fun stroke(worldPoints: List<DxfPoint>, style: Style, linetypeScale: Double) {
      if (worldPoints.size < 2) {
        return
      }
      // Every point counts, inside the image or not, so a drawing cannot buy unbounded work off the image.
      val pattern = scaledPattern(style, linetypeScale)
      val widthPx = widthPx(style)
      val worldCoordinates = worldCoordinates(worldPoints)
      if (pattern == null) {
        count(worldPoints.size)
        addStroke(pixelCoordinates(worldCoordinates), widthPx)
      } else {
        requireBudget(polylineLength(worldCoordinates) / pattern.sum() * pattern.size + worldPoints.size)
        LinetypeDasher(pattern).dashes(worldCoordinates).forEach { dash ->
          count(dash.size / 2)
          addStroke(pixelCoordinates(dash), widthPx)
        }
      }
    }

    /**
     * The dash and gap lengths of the linetype of [style] in drawing units: the pattern times the entity's linetype
     * scale times `$LTSCALE`, each at least [MinDashLength]. Null for a continuous line, for a scale that is not a finite
     * number and for a pattern shorter than two pixels, whose dashes would merge into one.
     */
    private fun scaledPattern(style: Style, linetypeScale: Double): DoubleArray? {
      return scaledPatterns.getOrPut(style.dashesAndGaps to linetypeScale) {
        val scale = linetypeScale * document.header.linetypeScale
        val pattern = style.dashesAndGaps.map { max(it * scale, MinDashLength) }
        val even = if (pattern.size % 2 == 1) pattern.dropLast(1) else pattern
        // A scale that is not a finite number draws the line continuous.
        if (even.size < 2 || even.all { it.isFinite() }.not() || even.sum() * pixelsPerUnit < 2.0) null else even.toDoubleArray()
      }
    }

    /**
     * ezdxf passes the lineweight in millimeters to matplotlib as points, at least one pixel; this takes the same width.
     */
    private fun widthPx(style: Style): Double = max(style.lineweightMillimeters, MinLineweightPoints) * PixelsPerPoint

    private fun dot(worldPoint: DxfPoint) {
      count(1)
      addStroke(pixelCoordinates(doubleArrayOf(worldPoint.x, worldPoint.y)), DotWidthPx)
    }

    /**
     * Fills [worldRings]; [counted] when the caller has counted their points already.
     */
    private fun fill(worldRings: List<List<DxfPoint>>, counted: Boolean = false) {
      val rings = worldRings.filter { it.size >= 3 }.map { pixelCoordinates(worldCoordinates(it)) }
      if (counted.not()) {
        count(rings.sumOf { it.size / 2 })
      }
      if (rings.isEmpty() || rings.none { isInImage(it, 0.0) }) {
        return
      }
      fills.add(FloorBackground.Fill(rings))
    }

    private fun addStroke(pixelCoordinates: DoubleArray, widthPx: Double) {
      if (isInImage(pixelCoordinates, widthPx).not()) {
        return
      }
      strokes.add(FloorBackground.Stroke(pixelCoordinates, widthPx))
    }

    /**
     * Throws before work of [points] points is done that the limit would refuse anyway.
     */
    private fun requireBudget(points: Double) {
      if (pointCount + points > maxPointCount) {
        throw FloorBackgroundTooComplexException("the background of the floor needs more than $maxPointCount points")
      }
    }

    private fun count(points: Int) {
      pointCount += points
      if (pointCount > maxPointCount) {
        throw FloorBackgroundTooComplexException("the background of the floor needs more than $maxPointCount points")
      }
    }

    private fun polylineLength(coordinates: DoubleArray): Double = (1 until coordinates.size / 2).sumOf { index ->
      sqrt((coordinates[2 * index] - coordinates[2 * index - 2]).let { it * it } + (coordinates[2 * index + 1] - coordinates[2 * index - 1]).let { it * it })
    }

    /**
     * x and y of each world point in turn.
     */
    private fun worldCoordinates(worldPoints: List<DxfPoint>): DoubleArray =
      DoubleArray(worldPoints.size * 2) { index -> if (index % 2 == 0) worldPoints[index / 2].x else worldPoints[index / 2].y }

    /**
     * World coordinates as pixel coordinates, both as x and y of each point in turn.
     */
    private fun pixelCoordinates(worldCoordinates: DoubleArray): DoubleArray {
      val window = frame.windowMeters
      return DoubleArray(worldCoordinates.size) { index ->
        if (index % 2 == 0) {
          (worldCoordinates[index] * metersPerUnit - window.minX) * frame.pixelsPerMeter
        } else {
          (window.maxY - worldCoordinates[index] * metersPerUnit) * frame.pixelsPerMeter
        }
      }
    }

    private fun isInImage(coordinates: DoubleArray, margin: Double): Boolean {
      var minX = Double.MAX_VALUE
      var minY = Double.MAX_VALUE
      var maxX = -Double.MAX_VALUE
      var maxY = -Double.MAX_VALUE
      for (index in 0 until coordinates.size / 2) {
        minX = minOf(minX, coordinates[2 * index])
        maxX = maxOf(maxX, coordinates[2 * index])
        minY = minOf(minY, coordinates[2 * index + 1])
        maxY = maxOf(maxY, coordinates[2 * index + 1])
      }
      return maxX >= -margin && minX <= frame.widthPx + margin && maxY >= -margin && minY <= frame.heightPx + margin
    }
  }

  /**
   * The shortest dash or gap of a linetype, in drawing units.
   */
  private const val MinDashLength: Double = 0.1

  /**
   * Lineweights are drawn as points (1/72 inch) at 100 pixels per inch, at least one pixel wide.
   */
  private const val MinLineweightPoints: Double = 0.72
  private const val PixelsPerPoint: Double = 100.0 / 72.0

  private const val DotWidthPx: Double = 1.6

  private const val FullTurn: Double = 2 * kotlin.math.PI
}
