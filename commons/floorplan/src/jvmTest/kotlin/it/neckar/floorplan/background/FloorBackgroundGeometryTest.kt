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

import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isCloseTo
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isLessThan
import assertk.assertions.messageContains
import it.neckar.dxf.DxfArc
import it.neckar.dxf.DxfBlock
import it.neckar.dxf.DxfDimension
import it.neckar.dxf.DxfDocument
import it.neckar.dxf.DxfEllipse
import it.neckar.dxf.DxfEntity
import it.neckar.dxf.DxfEntityProperties
import it.neckar.dxf.DxfHatch
import it.neckar.dxf.DxfHatchBoundaryPath
import it.neckar.dxf.DxfHatchEdge
import it.neckar.dxf.DxfHatchPatternLine
import it.neckar.dxf.DxfHeader
import it.neckar.dxf.DxfInsert
import it.neckar.dxf.DxfLayer
import it.neckar.dxf.DxfLine
import it.neckar.dxf.DxfLinetype
import it.neckar.dxf.DxfLineweight
import it.neckar.dxf.DxfLwPolyline
import it.neckar.dxf.DxfMText
import it.neckar.dxf.DxfPoint
import it.neckar.dxf.DxfPointEntity
import it.neckar.dxf.DxfPolyline
import it.neckar.dxf.DxfPolylineKind
import it.neckar.dxf.DxfSegmentWidth
import it.neckar.dxf.DxfSpline
import it.neckar.dxf.DxfText
import it.neckar.floorplan.FloorFrame
import it.neckar.floorplan.ModelBounds
import org.junit.jupiter.api.Test

/**
 * The frame shows the model square 0..10 m at 20 px per meter: x = 20 * X, y = 200 - 20 * Y.
 */
class FloorBackgroundGeometryTest {
  private val frame = FloorFrame(ModelBounds(0.0, 0.0, 10.0, 10.0), pixelsPerMeter = 20.0, widthPx = 200, heightPx = 200)

  private fun draw(vararg entities: DxfEntity, blocks: List<DxfBlock> = emptyList(), layers: List<DxfLayer> = emptyList(), linetypes: List<DxfLinetype> = emptyList()): FloorBackground {
    val document = DxfDocument(entities = entities.toList(), header = DxfHeader(insertionUnits = 6), layerTable = layers, linetypes = linetypes, blocks = blocks)
    return FloorBackgroundGeometry.draw(document, metersPerUnit = 1.0, frame = frame)
  }

  private fun line(layer: String, x0: Double, y0: Double, x1: Double, y1: Double, properties: DxfEntityProperties = DxfEntityProperties.Default): DxfLine =
    DxfLine(layer = layer, start = DxfPoint(x0, y0), end = DxfPoint(x1, y1), properties = properties)

  @Test
  fun aLineIsDrawnInThePixelsOfTheFrameAtOnePixelWidth() {
    val stroke = draw(line("WALLS", 1.0, 1.0, 9.0, 1.0)).strokes.single()
    assertThat(stroke.coordinates.toList()).isEqualTo(listOf(20.0, 180.0, 180.0, 180.0))
    assertThat(stroke.widthPx).isCloseTo(1.0, 1e-9)
  }

  @Test
  fun aLineweightAboveSevenTenthsOfAMillimeterWidensTheStroke() {
    val stroke = draw(line("WALLS", 1.0, 1.0, 9.0, 1.0, DxfEntityProperties(lineweight = DxfLineweight(100)))).strokes.single()
    assertThat(stroke.widthPx).isCloseTo(100.0 / 72.0, 1e-9)
  }

  @Test
  fun anEntityBeyondTheWindowAndItsPadIsLeftOut() {
    assertThat(draw(line("WALLS", 11.0, 1.0, 12.0, 1.0)).strokes).isEmpty()
  }

  @Test
  fun textsAndDimensionsAreLeftOut() {
    val background = draw(
      DxfText(layer = "TEXT", insertionPoint = DxfPoint(3.0, 4.0), text = "WOHNEN", height = 2.0),
      DxfMText(layer = "TEXT", insertionPoint = DxfPoint(3.0, 8.0), rawText = "Wohnen\\P12 m²", charHeight = 0.5),
      DxfDimension(layer = "DIMENSIONS", blockName = "*D1"),
      blocks = listOf(DxfBlock("*D1", DxfPoint(0.0, 0.0), listOf(line("DIMENSIONS", 0.0, 0.0, 5.0, 0.0)))),
    )
    assertThat(background.strokes).isEmpty()
    assertThat(background.fills).isEmpty()
  }

  @Test
  fun anOpenLineEndingAtATextOfItsLayerIsLeftOutAsItsLeader() {
    val text = DxfText(layer = "WALLS", insertionPoint = DxfPoint(3.0, 4.0), text = "WOHNEN", height = 2.0)
    assertThat(draw(text, line("WALLS", 1.0, 8.0, 4.0, 5.0)).strokes).isEmpty()
    assertThat(draw(text, line("OTHER", 1.0, 8.0, 4.0, 5.0)).strokes).hasSize(1)
  }

  @Test
  fun aBlockReferencePlacesItsContentAndLeavesOutItsText() {
    val block = DxfBlock(
      name = "DOOR",
      basePoint = DxfPoint(1.0, 0.0),
      entities = listOf(line("0", 1.0, 0.0, 2.0, 0.0), DxfText(layer = "0", insertionPoint = DxfPoint(1.0, 0.0), text = "T1", height = 1.0)),
    )
    val insert = DxfInsert(layer = "DOORS", blockName = "DOOR", insertionPoint = DxfPoint(5.0, 5.0), scale = DxfPoint(2.0, 2.0, 1.0), rotationDegrees = 90.0)
    val stroke = draw(insert, blocks = listOf(block)).strokes.single()
    val coordinates = stroke.coordinates.toList()
    // (1, 0) - base point -> (0, 0) -> (5, 5); (2, 0) -> (1, 0) scaled to (2, 0), rotated to (0, 2) -> (5, 7).
    listOf(100.0, 100.0, 100.0, 60.0).zip(coordinates).forEach { (expected, actual) -> assertThat(actual).isCloseTo(expected, 1e-9) }
  }

  @Test
  fun blockContentOnLayer0TakesTheLayerOfTheReference() {
    val layers = listOf(DxfLayer(name = "OFF", on = false), DxfLayer(name = "VISIBLE"))
    val block = DxfBlock("B", DxfPoint(0.0, 0.0), listOf(line("0", 1.0, 1.0, 2.0, 1.0), line("VISIBLE", 1.0, 2.0, 2.0, 2.0)))
    val background = draw(DxfInsert(layer = "OFF", blockName = "B", insertionPoint = DxfPoint(0.0, 0.0)), blocks = listOf(block), layers = layers)
    assertThat(background.strokes.map { it.coordinates[1] }).isEqualTo(listOf(160.0))
  }

  @Test
  fun aLinetypeSplitsTheLineIntoDashes() {
    val dashed = DxfLinetype(name = "DASHED", patternLength = 2.0, elements = listOf(1.0, -1.0), complex = false)
    val background = draw(line("WALLS", 0.0, 1.0, 8.0, 1.0, DxfEntityProperties(linetype = "DASHED")), linetypes = listOf(dashed))
    assertThat(background.strokes.map { it.coordinates[0] to it.coordinates[2] }).isEqualTo(listOf(0.0 to 20.0, 40.0 to 60.0, 80.0 to 100.0, 120.0 to 140.0))
  }

  @Test
  fun aBlockLinetypeComesFromTheReference() {
    val dashed = DxfLinetype(name = "DASHED", patternLength = 2.0, elements = listOf(1.0, -1.0), complex = false)
    val block = DxfBlock("B", DxfPoint(0.0, 0.0), listOf(line("0", 0.0, 1.0, 4.0, 1.0, DxfEntityProperties(linetype = DxfEntityProperties.ByBlock))))
    val insert = DxfInsert(layer = "A", blockName = "B", insertionPoint = DxfPoint(0.0, 0.0), properties = DxfEntityProperties(linetype = "DASHED"))
    assertThat(draw(insert, blocks = listOf(block), linetypes = listOf(dashed)).strokes.map { it.coordinates[0] to it.coordinates[2] }).isEqualTo(listOf(0.0 to 20.0, 40.0 to 60.0))
  }

  @Test
  fun aBulgedSegmentDrawsAnArc() {
    val polyline = DxfLwPolyline(layer = "WALLS", vertices = listOf(DxfPoint(1.0, 5.0), DxfPoint(9.0, 5.0)), closed = false, bulges = listOf(1.0, 0.0))
    val stroke = draw(polyline).strokes.single()
    val ys = stroke.coordinates.filterIndexed { index, _ -> index % 2 == 1 }
    // A counterclockwise half circle from (1, 5) to (9, 5) passes (5, 1).
    assertThat(ys.max()).isCloseTo(180.0, 0.1)
  }

  @Test
  fun aSolidHatchFillsItsBoundary() {
    val hatch = DxfHatch(
      layer = "H", solidFill = true, patternName = "SOLID", hatchStyle = 0, elevation = 0.0,
      boundaryPaths = listOf(DxfHatchBoundaryPath.Polyline(flags = 3, vertices = listOf(DxfPoint(1.0, 1.0), DxfPoint(3.0, 1.0), DxfPoint(3.0, 3.0)), bulges = listOf(0.0, 0.0, 0.0), closed = true)),
      patternLines = emptyList(),
    )
    val ring = draw(hatch).fills.single().rings.single()
    assertThat(ring.toList()).isEqualTo(listOf(20.0, 180.0, 60.0, 180.0, 60.0, 140.0, 20.0, 180.0))
  }

  @Test
  fun aPatternHatchDrawsItsLinesInsideTheBoundary() {
    val square = DxfHatchBoundaryPath.Polyline(flags = 3, vertices = listOf(DxfPoint(1.0, 1.0), DxfPoint(5.0, 1.0), DxfPoint(5.0, 5.0), DxfPoint(1.0, 5.0)), bulges = List(4) { 0.0 }, closed = true)
    val horizontal = DxfHatchPatternLine(angleDegrees = 0.0, basePoint = DxfPoint(0.0, 0.5), offset = DxfPoint(0.0, 1.0), dashes = emptyList())
    val hatch = DxfHatch(layer = "H", solidFill = false, patternName = "LINES", hatchStyle = 0, elevation = 0.0, boundaryPaths = listOf(square), patternLines = listOf(horizontal))
    val background = draw(hatch)
    assertThat(background.fills).isEmpty()
    // The lines Y = 1.5 to 4.5 cross the square from X = 1 to X = 5.
    assertThat(background.strokes.map { it.coordinates.toList() }.sortedBy { it[1] }).isEqualTo(
      listOf(110.0, 130.0, 150.0, 170.0).map { y -> listOf(20.0, y, 100.0, y) }
    )
  }

  @Test
  fun aDashedPatternLineDrawsItsDashesFromItsBasePoint() {
    val square = DxfHatchBoundaryPath.Polyline(flags = 3, vertices = listOf(DxfPoint(1.0, 1.0), DxfPoint(5.0, 1.0), DxfPoint(5.0, 2.0), DxfPoint(1.0, 2.0)), bulges = List(4) { 0.0 }, closed = true)
    val dashed = DxfHatchPatternLine(angleDegrees = 0.0, basePoint = DxfPoint(0.0, 1.5), offset = DxfPoint(0.0, 10.0), dashes = listOf(1.0, -1.0))
    val hatch = DxfHatch(layer = "H", solidFill = false, patternName = "DASHED", hatchStyle = 0, elevation = 0.0, boundaryPaths = listOf(square), patternLines = listOf(dashed))
    // Dashes X 0..1, 2..3, 4..5 clipped to X 1..5: the dash 0..1 ends at the boundary.
    assertThat(draw(hatch).strokes.map { it.coordinates[0] to it.coordinates[2] }).isEqualTo(listOf(40.0 to 60.0, 80.0 to 100.0))
  }

  @Test
  fun aPatternFarFromItsBasePointEnds() {
    val square = DxfHatchBoundaryPath.Polyline(flags = 3, vertices = listOf(DxfPoint(1.0, 1.0), DxfPoint(5.0, 1.0), DxfPoint(5.0, 2.0), DxfPoint(1.0, 2.0)), bulges = List(4) { 0.0 }, closed = true)
    val far = DxfHatchPatternLine(angleDegrees = 0.0, basePoint = DxfPoint(1e18, 1.5), offset = DxfPoint(0.0, 10.0), dashes = listOf(1.0, -1.0))
    val hatch = DxfHatch(layer = "H", solidFill = false, patternName = "FAR", hatchStyle = 0, elevation = 0.0, boundaryPaths = listOf(square), patternLines = listOf(far))
    // At 1e18 a double steps by 128, so the 4 units inside the boundary span at most two such steps.
    assertThat(draw(hatch).strokes.size).isLessThan(4)
  }

  @Test
  fun aLinetypeScaleThatIsNotANumberDrawsTheLineContinuous() {
    val dashed = DxfLinetype(name = "DASHED", patternLength = 2.0, elements = listOf(1.0, -1.0), complex = false)
    val background = draw(line("WALLS", 0.0, 1.0, 8.0, 1.0, DxfEntityProperties(linetype = "DASHED", linetypeScale = Double.NaN)), linetypes = listOf(dashed))
    assertThat(background.strokes).hasSize(1)
  }

  @Test
  fun patternLinesCloserThanATenThousandthUnitFillTheHatch() {
    val square = DxfHatchBoundaryPath.Polyline(flags = 3, vertices = listOf(DxfPoint(1.0, 1.0), DxfPoint(5.0, 1.0), DxfPoint(5.0, 5.0)), bulges = List(3) { 0.0 }, closed = true)
    val dense = DxfHatchPatternLine(angleDegrees = 0.0, basePoint = DxfPoint(0.0, 0.0), offset = DxfPoint(0.0, 0.00001), dashes = emptyList())
    val hatch = DxfHatch(layer = "H", solidFill = false, patternName = "DENSE", hatchStyle = 0, elevation = 0.0, boundaryPaths = listOf(square), patternLines = listOf(dense))
    assertThat(draw(hatch).fills).hasSize(1)
  }

  @Test
  fun aFloorAtThePointLimitIsDrawnAndOneAboveIsRefused() {
    val document = DxfDocument(entities = listOf(line("A", 1.0, 1.0, 2.0, 1.0), line("A", 1.0, 2.0, 2.0, 2.0)))
    // Two points per line plus one per entity drawn.
    assertThat(FloorBackgroundGeometry.draw(document, metersPerUnit = 1.0, frame = frame, maxPointCount = 6).strokes).hasSize(2)
    assertFailure { FloorBackgroundGeometry.draw(document, metersPerUnit = 1.0, frame = frame, maxPointCount = 5) }
      .isInstanceOf(FloorBackgroundTooComplexException::class)
  }

  @Test
  fun aMinsertAtTheCellLimitIsDrawnAndOneAboveIsLeftOut() {
    val block = DxfBlock("B", DxfPoint(0.0, 0.0), listOf(line("A", 1.0, 1.0, 2.0, 1.0)))
    val atLimit = DxfInsert(layer = "A", blockName = "B", insertionPoint = DxfPoint(0.0, 0.0), columnCount = 100, rowCount = FloorBackgroundGeometry.MaxGridCells / 100)
    assertThat(draw(atLimit, blocks = listOf(block)).strokes).hasSize(FloorBackgroundGeometry.MaxGridCells)
    assertThat(draw(atLimit.copy(columnCount = FloorBackgroundGeometry.MaxGridCells + 1, rowCount = 1), blocks = listOf(block)).strokes).isEmpty()
  }

  @Test
  fun aBlockReferencingItselfTwiceEndsAtThePointLimit() {
    val loop = DxfInsert(layer = "A", blockName = "LOOP", insertionPoint = DxfPoint(0.0, 0.0))
    val block = DxfBlock("LOOP", DxfPoint(0.0, 0.0), listOf(line("A", 1.0, 1.0, 2.0, 1.0), loop, loop))
    val document = DxfDocument(entities = listOf(loop), blocks = listOf(block))
    assertFailure { FloorBackgroundGeometry.draw(document, metersPerUnit = 1.0, frame = frame, maxPointCount = 10_000) }
      .isInstanceOf(FloorBackgroundTooComplexException::class)
  }

  @Test
  fun aSplineWithMoreControlPointsThanSegmentsIsDrawn() {
    val controlPoints = List(1025) { DxfPoint(1.0 + it * 0.005, 1.0) }
    val spline = DxfSpline(layer = "A", degree = 3, knots = emptyList(), controlPoints = controlPoints, weights = emptyList(), fitPoints = emptyList(), closed = false)
    val coordinates = draw(spline).strokes.single().coordinates
    // A clamped spline starts at its first and ends at its last control point.
    assertThat(coordinates.take(2)).isEqualTo(listOf(20.0, 180.0))
    assertThat(coordinates[coordinates.size - 2]).isCloseTo(20.0 + 1024 * 0.1, 1e-6)
  }

  @Test
  fun anEllipseWithHugeParametersEnds() {
    val ellipse = DxfEllipse(layer = "A", center = DxfPoint(5.0, 5.0), majorAxis = DxfPoint(1.0, 0.0), ratio = 0.5, startParameter = 1e300, endParameter = -1e300)
    assertThat(draw(ellipse).strokes).hasSize(1)
  }

  @Test
  fun aPolygonMeshNamingMoreVerticesThanItHasDrawsNothing() {
    val mesh = DxfPolyline(
      layer = "A",
      kind = DxfPolylineKind.PolygonMesh(rowCount = 65_536, columnCount = 65_536, closedRows = false, closedColumns = false),
      vertices = listOf(DxfPoint(1.0, 1.0)), bulges = listOf(0.0), widths = listOf(DxfSegmentWidth.Hairline), closed = false,
    )
    assertThat(draw(mesh).strokes).isEmpty()
  }

  @Test
  fun anArcWithAMirroredExtrusionMirrorsItsCenter() {
    val arc = DxfArc(layer = "A", center = DxfPoint(-5.0, 5.0), radius = 1.0, startAngleDegrees = 0.0, endAngleDegrees = 360.0, properties = DxfEntityProperties(extrusion = DxfPoint(0.0, 0.0, -1.0)))
    val xs = draw(arc).strokes.single().coordinates.filterIndexed { index, _ -> index % 2 == 0 }
    assertThat(xs.min()).isCloseTo(80.0, 0.1)
    assertThat(xs.max()).isCloseTo(120.0, 0.1)
  }

  @Test
  fun aPolylineSegmentWithAWidthIsFilledAsABand() {
    val polyline = DxfLwPolyline(layer = "A", vertices = listOf(DxfPoint(1.0, 5.0), DxfPoint(9.0, 5.0)), closed = false, widths = listOf(DxfSegmentWidth(1.0, 1.0), DxfSegmentWidth.Hairline))
    val ring = draw(polyline).fills.single().rings.single().toList()
    assertThat(ring).isEqualTo(listOf(20.0, 90.0, 180.0, 90.0, 180.0, 110.0, 20.0, 110.0))
  }

  @Test
  fun aPointWithoutAPositiveSizeIsDrawnOneUnitLarge() {
    val document = DxfDocument(entities = listOf(DxfPointEntity(layer = "A", location = DxfPoint(5.0, 5.0))), header = DxfHeader(pointDisplayMode = 3, pointDisplaySize = 0.0))
    val crosses = FloorBackgroundGeometry.draw(document, metersPerUnit = 1.0, frame = frame).strokes.map { it.coordinates.toList() }
    assertThat(crosses).isEqualTo(listOf(listOf(80.0, 120.0, 120.0, 80.0), listOf(80.0, 80.0, 120.0, 120.0)))
  }

  @Test
  fun aPolygonMeshWithoutRowsDrawsNothing() {
    val mesh = DxfPolyline(
      layer = "A",
      kind = DxfPolylineKind.PolygonMesh(rowCount = 0, columnCount = 2, closedRows = true, closedColumns = true),
      vertices = listOf(DxfPoint(1.0, 1.0), DxfPoint(2.0, 1.0)), bulges = listOf(0.0, 0.0), widths = List(2) { DxfSegmentWidth.Hairline }, closed = true,
    )
    assertThat(draw(mesh).strokes).isEmpty()
  }

  @Test
  fun aMinsertWithANegativeCountPlacesItsBlockOncePerColumn() {
    val block = DxfBlock("B", DxfPoint(0.0, 0.0), listOf(line("A", 1.0, 1.0, 2.0, 1.0)))
    val insert = DxfInsert(layer = "A", blockName = "B", insertionPoint = DxfPoint(0.0, 0.0), columnCount = 3, rowCount = -1, columnSpacing = 2.0)
    assertThat(draw(insert, blocks = listOf(block)).strokes).hasSize(3)
  }

  @Test
  fun aLongChainOfBlocksEndsAtTheDepthLimit() {
    val blocks = List(5000) { index ->
      val next = if (index < 4999) listOf(DxfInsert(layer = "A", blockName = "B${index + 1}", insertionPoint = DxfPoint(0.0, 0.0))) else emptyList()
      DxfBlock("B$index", DxfPoint(0.0, 0.0), listOf(line("A", 1.0, 1.0, 2.0, 1.0)) + next)
    }
    val background = draw(DxfInsert(layer = "A", blockName = "B0", insertionPoint = DxfPoint(0.0, 0.0)), blocks = blocks)
    assertThat(background.strokes).hasSize(FloorBackgroundGeometry.MaxBlockDepth)
  }

  @Test
  fun aHatchWithANotANumberVertexLeavesOutTheEdgesAtIt() {
    val square = DxfHatchBoundaryPath.Polyline(flags = 3, vertices = listOf(DxfPoint(1.0, 1.0), DxfPoint(5.0, 1.0), DxfPoint(Double.NaN, 3.0), DxfPoint(5.0, 5.0), DxfPoint(1.0, 5.0)), bulges = List(5) { 0.0 }, closed = true)
    val horizontal = DxfHatchPatternLine(angleDegrees = 0.0, basePoint = DxfPoint(0.0, 0.5), offset = DxfPoint(0.0, 1.0), dashes = emptyList())
    val hatch = DxfHatch(layer = "H", solidFill = false, patternName = "LINES", hatchStyle = 0, elevation = 0.0, boundaryPaths = listOf(square), patternLines = listOf(horizontal))
    // The two edges at the vertex drop out, so each line crosses the boundary once and pairs with nothing.
    assertThat(draw(hatch).strokes).isEmpty()
  }

  @Test
  fun aChainOfBlocksReferencingTheNextTwiceEndsAtThePointLimit() {
    val blocks = List(41) { index ->
      val next = DxfInsert(layer = "A", blockName = "B${index + 1}", insertionPoint = DxfPoint(0.0, 0.0))
      DxfBlock("B$index", DxfPoint(0.0, 0.0), listOf(line("A", 1.0, 1.0, 2.0, 1.0)) + if (index < 40) listOf(next, next) else emptyList())
    }
    val document = DxfDocument(entities = listOf(DxfInsert(layer = "A", blockName = "B0", insertionPoint = DxfPoint(0.0, 0.0))), blocks = blocks)
    assertFailure { FloorBackgroundGeometry.draw(document, metersPerUnit = 1.0, frame = frame) }.isInstanceOf(FloorBackgroundTooComplexException::class)
  }

  @Test
  fun hatchLinesCrossingTheBoundaryTooOftenRefuseTheFloor() {
    val square = DxfHatchBoundaryPath.Polyline(flags = 3, vertices = listOf(DxfPoint(1.0, 1.0), DxfPoint(9.0, 1.0), DxfPoint(9.0, 9.0), DxfPoint(1.0, 9.0)), bulges = List(4) { 0.0 }, closed = true)
    // Each family alone takes 4 edges plus 2 * 600 crossings, below the 1,996 steps of the budget; together they exceed it.
    val horizontal = DxfHatchPatternLine(angleDegrees = 0.0, basePoint = DxfPoint(0.0, 0.0), offset = DxfPoint(0.0, 8.0 / 600), dashes = emptyList())
    val vertical = horizontal.copy(angleDegrees = 90.0, offset = DxfPoint(-8.0 / 600, 0.0))
    val hatch = DxfHatch(layer = "H", solidFill = false, patternName = "FINE", hatchStyle = 0, elevation = 0.0, boundaryPaths = listOf(square), patternLines = listOf(horizontal, vertical))
    val document = DxfDocument(entities = listOf(hatch))
    assertFailure { FloorBackgroundGeometry.draw(document, metersPerUnit = 1.0, frame = frame, maxPointCount = 2_000) }
      .isInstanceOf(FloorBackgroundTooComplexException::class)
      .messageContains("steps")
  }

  @Test
  fun edgesOfAHatchJoinToOneLoopWhateverTheirDirection() {
    val edges = DxfHatchBoundaryPath.Edges(
      flags = 1,
      edges = listOf(
        DxfHatchEdge.Line(DxfPoint(1.0, 1.0), DxfPoint(3.0, 1.0)),
        DxfHatchEdge.Line(DxfPoint(3.0, 3.0), DxfPoint(3.0, 1.0)),
        DxfHatchEdge.Line(DxfPoint(3.0, 3.0), DxfPoint(1.0, 1.0)),
      ),
    )
    val hatch = DxfHatch(layer = "H", solidFill = true, patternName = "SOLID", hatchStyle = 0, elevation = 0.0, boundaryPaths = listOf(edges), patternLines = emptyList())
    val ring = draw(hatch).fills.single().rings.single().toList().chunked(2).toSet()
    assertThat(ring).isEqualTo(setOf(listOf(20.0, 180.0), listOf(60.0, 180.0), listOf(60.0, 140.0)))
  }

  @Test
  fun aBlockReferencingItselfEndsAtTheDepthLimit() {
    val block = DxfBlock("LOOP", DxfPoint(0.0, 0.0), listOf(line("A", 1.0, 1.0, 2.0, 1.0), DxfInsert(layer = "A", blockName = "LOOP", insertionPoint = DxfPoint(0.0, 0.0))))
    val background = draw(DxfInsert(layer = "A", blockName = "LOOP", insertionPoint = DxfPoint(0.0, 0.0)), blocks = listOf(block))
    assertThat(background.strokes).hasSize(FloorBackgroundGeometry.MaxBlockDepth)
  }
}
