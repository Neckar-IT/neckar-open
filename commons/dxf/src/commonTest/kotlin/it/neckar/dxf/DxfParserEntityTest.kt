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

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class DxfParserEntityTest {

  /**
   * A DXF of the given group codes, one code and one value per pair.
   */
  private fun dxf(vararg codes: Pair<Int, Any>): String = codes.joinToString("\n") { (code, value) -> "$code\n$value" } + "\n0\nEOF\n"

  private fun entities(vararg codes: Pair<Int, Any>): List<DxfEntity> {
    return DxfParser.parse(dxf(0 to "SECTION", 2 to "ENTITIES", *codes, 0 to "ENDSEC")).entities
  }

  @Test
  fun readsTheHeaderVariablesOfTheRenderer() {
    val document = DxfParser.parse(
      dxf(
        0 to "SECTION", 2 to "HEADER",
        9 to "\$ACADVER", 1 to "AC1027",
        9 to "\$INSUNITS", 70 to 6,
        9 to "\$LTSCALE", 40 to 0.02,
        9 to "\$PDMODE", 70 to 35,
        9 to "\$PDSIZE", 40 to 0.1,
        0 to "ENDSEC",
      )
    )
    assertEquals(DxfHeader(insertionUnits = 6, linetypeScale = 0.02, pointDisplayMode = 35, pointDisplaySize = 0.1), document.header)
  }

  @Test
  fun readsLayersAndLinetypes() {
    val document = DxfParser.parse(
      dxf(
        0 to "SECTION", 2 to "TABLES",
        0 to "TABLE", 2 to "LTYPE",
        0 to "LTYPE", 2 to "STRICHLINIE", 70 to 0, 72 to 65, 73 to 2, 40 to 19.05, 49 to 12.7, 74 to 0, 49 to -6.35, 74 to 0,
        0 to "ENDTAB",
        0 to "TABLE", 2 to "LAYER",
        0 to "LAYER", 2 to "Wände", 70 to 1, 62 to -7, 6 to "STRICHLINIE", 370 to 50,
        0 to "ENDTAB",
        0 to "ENDSEC",
      )
    )
    assertEquals(DxfLayer(name = "Wände", on = false, frozen = true, linetype = "STRICHLINIE", lineweight = DxfLineweight(50)), document.layer("WÄNDE"))
    val linetype = assertNotNull(document.linetype("strichlinie"))
    assertEquals(listOf(12.7, 6.35), linetype.dashesAndGaps().map { kotlin.math.round(it * 1000) / 1000 }.take(2))
  }

  @Test
  fun aPatternStartingWithAGapEndsWithIt() {
    val linetype = DxfLinetype(name = "GAP_FIRST", patternLength = 3.0, elements = listOf(-1.0, 2.0), complex = false)
    assertEquals(listOf(2.0, 1.0), linetype.dashesAndGaps())
  }

  @Test
  fun aComplexLinetypeDrawsContinuous() {
    val linetype = DxfLinetype(name = "BATTING", patternLength = 3.0, elements = listOf(1.0, -2.0), complex = true)
    assertEquals(emptyList(), linetype.dashesAndGaps())
  }

  @Test
  fun readsBlockDefinitionsWithBasePointAndEntities() {
    val document = DxfParser.parse(
      dxf(
        0 to "SECTION", 2 to "BLOCKS",
        0 to "BLOCK", 8 to "0", 2 to "DOOR", 70 to 0, 10 to 1.0, 20 to 2.0, 30 to 0.0,
        0 to "LINE", 8 to "0", 10 to 0.0, 20 to 0.0, 11 to 1.0, 21 to 0.0,
        0 to "ATTDEF", 8 to "0", 2 to "NAME",
        0 to "ENDBLK",
        0 to "ENDSEC",
      )
    )
    val block = assertNotNull(document.block("door"))
    assertEquals(DxfPoint(1.0, 2.0), block.basePoint)
    assertEquals(listOf("LINE", "ATTDEF"), block.entities.map { if (it is DxfUnknownEntity) it.type else "LINE" })
    assertTrue(document.entities.isEmpty())
  }

  @Test
  fun readsTheCommonPropertiesOfAnEntity() {
    val line = entities(
      0 to "LINE", 8 to "A", 6 to "Hidden", 48 to 0.5, 370 to 35, 60 to 1, 67 to 1, 210 to 0.0, 220 to 0.0, 230 to -1.0,
      10 to 0.0, 20 to 0.0, 11 to 1.0, 21 to 1.0,
    ).single()
    assertEquals(
      DxfEntityProperties(linetype = "Hidden", linetypeScale = 0.5, lineweight = DxfLineweight(35), invisible = true, paperSpace = true, extrusion = DxfPoint(0.0, 0.0, -1.0)),
      line.properties,
    )
  }

  @Test
  fun modelSpaceEntitiesLeaveOutThePaperSpace() {
    val document = DxfParser.parse(
      dxf(
        0 to "SECTION", 2 to "ENTITIES",
        0 to "LINE", 8 to "MODEL", 10 to 0.0, 20 to 0.0, 11 to 1.0, 21 to 1.0,
        0 to "XLINE", 8 to "PAPER", 67 to 1, 10 to 0.0, 20 to 0.0, 11 to 1.0, 21 to 0.0,
        0 to "ENDSEC",
      )
    )
    assertEquals(listOf("MODEL"), document.modelSpaceEntities.map { it.layer })
    assertEquals(2, document.entities.size)
  }

  @Test
  fun readsBulgesAndWidthsOfALightweightPolyline() {
    val polyline = assertIs<DxfLwPolyline>(
      entities(
        0 to "LWPOLYLINE", 8 to "A", 90 to 3, 70 to 0, 43 to 0.2, 38 to 1.5,
        10 to 0.0, 20 to 0.0, 42 to 1.0,
        10 to 2.0, 20 to 0.0, 40 to 0.5, 41 to 0.1,
        10 to 2.0, 20 to 2.0,
      ).single()
    )
    assertEquals(listOf(DxfPoint(0.0, 0.0), DxfPoint(2.0, 0.0), DxfPoint(2.0, 2.0)), polyline.vertices)
    assertEquals(listOf(1.0, 0.0, 0.0), polyline.bulges)
    assertEquals(listOf(DxfSegmentWidth(0.2, 0.2), DxfSegmentWidth(0.5, 0.1), DxfSegmentWidth(0.2, 0.2)), polyline.widths)
    assertEquals(1.5, polyline.elevation)
  }

  @Test
  fun aPolylineTakesItsVerticesUpToSeqend() {
    val parsed = entities(
      0 to "POLYLINE", 8 to "A", 66 to 1, 70 to 1, 10 to 0.0, 20 to 0.0, 30 to 2.0,
      0 to "VERTEX", 8 to "A", 10 to 0.0, 20 to 0.0, 42 to 0.5,
      0 to "VERTEX", 8 to "A", 10 to 3.0, 20 to 0.0,
      0 to "VERTEX", 8 to "A", 10 to 3.0, 20 to 3.0,
      0 to "SEQEND", 8 to "A",
      0 to "LINE", 8 to "B", 10 to 0.0, 20 to 0.0, 11 to 1.0, 21 to 1.0,
    )
    assertEquals(2, parsed.size)
    val polyline = assertIs<DxfPolyline>(parsed.first())
    assertEquals(DxfPolylineKind.Polyline2d(elevation = 2.0), polyline.kind)
    assertTrue(polyline.closed)
    assertEquals(listOf(DxfPoint(0.0, 0.0), DxfPoint(3.0, 0.0), DxfPoint(3.0, 3.0)), polyline.vertices)
    assertEquals(listOf(0.5, 0.0, 0.0), polyline.bulges)
    assertIs<DxfLine>(parsed[1])
  }

  @Test
  fun aPolyfaceMeshKeepsItsFacesApartFromItsVertices() {
    val polyline = assertIs<DxfPolyline>(
      entities(
        0 to "POLYLINE", 8 to "A", 66 to 1, 70 to 64, 71 to 3, 72 to 1,
        0 to "VERTEX", 8 to "A", 70 to 192, 10 to 0.0, 20 to 0.0, 30 to 0.0,
        0 to "VERTEX", 8 to "A", 70 to 192, 10 to 1.0, 20 to 0.0, 30 to 0.0,
        0 to "VERTEX", 8 to "A", 70 to 192, 10 to 0.0, 20 to 1.0, 30 to 0.0,
        0 to "VERTEX", 8 to "A", 70 to 128, 71 to 1, 72 to -2, 73 to 3,
        0 to "SEQEND",
      ).single()
    )
    assertEquals(DxfPolylineKind.PolyfaceMesh(faces = listOf(listOf(1, -2, 3))), polyline.kind)
    assertEquals(3, polyline.vertices.size)
  }

  @Test
  fun anInsertTakesItsAttributesUpToSeqend() {
    val insert = assertIs<DxfInsert>(
      entities(
        0 to "INSERT", 8 to "A", 66 to 1, 2 to "DOOR", 10 to 5.0, 20 to 6.0, 41 to 2.0, 42 to -1.0, 50 to 90.0, 70 to 3, 71 to 2, 44 to 1.5, 45 to 2.5,
        0 to "ATTRIB", 8 to "TEXT", 2 to "NAME", 1 to "T1", 10 to 5.0, 20 to 6.0,
        0 to "SEQEND", 8 to "A",
      ).single()
    )
    assertEquals("DOOR", insert.blockName)
    assertEquals(DxfPoint(5.0, 6.0), insert.insertionPoint)
    assertEquals(DxfPoint(2.0, -1.0, 1.0), insert.scale)
    assertEquals(90.0, insert.rotationDegrees)
    assertEquals(listOf(3, 2, 1.5, 2.5), listOf(insert.columnCount, insert.rowCount, insert.columnSpacing, insert.rowSpacing))
    assertEquals(listOf("NAME" to "T1"), insert.attributes.map { it.tag to it.text })
  }

  @Test
  fun readsCirclesArcsEllipsesAndSplines() {
    val parsed = entities(
      0 to "CIRCLE", 8 to "A", 10 to 1.0, 20 to 2.0, 40 to 3.0,
      0 to "ARC", 8 to "A", 10 to 1.0, 20 to 2.0, 40 to 3.0, 50 to 10.0, 51 to 80.0,
      0 to "ELLIPSE", 8 to "A", 10 to 0.0, 20 to 0.0, 11 to 4.0, 21 to 0.0, 40 to 0.5, 41 to 0.0, 42 to 3.14,
      0 to "SPLINE", 8 to "A", 70 to 8, 71 to 2, 72 to 6, 73 to 3,
      40 to 0.0, 40 to 0.0, 40 to 0.0, 40 to 1.0, 40 to 1.0, 40 to 1.0,
      10 to 0.0, 20 to 0.0, 30 to 0.0, 10 to 1.0, 20 to 1.0, 30 to 0.0, 10 to 2.0, 20 to 0.0, 30 to 0.0,
    )
    assertEquals(DxfCircle(layer = "A", center = DxfPoint(1.0, 2.0), radius = 3.0), parsed[0])
    assertEquals(DxfArc(layer = "A", center = DxfPoint(1.0, 2.0), radius = 3.0, startAngleDegrees = 10.0, endAngleDegrees = 80.0), parsed[1])
    assertEquals(DxfEllipse(layer = "A", center = DxfPoint(0.0, 0.0), majorAxis = DxfPoint(4.0, 0.0), ratio = 0.5, startParameter = 0.0, endParameter = 3.14), parsed[2])
    val spline = assertIs<DxfSpline>(parsed[3])
    assertEquals(2, spline.degree)
    assertEquals(listOf(0.0, 0.0, 0.0, 1.0, 1.0, 1.0), spline.knots)
    assertEquals(listOf(DxfPoint(0.0, 0.0), DxfPoint(1.0, 1.0), DxfPoint(2.0, 0.0)), spline.controlPoints)
  }

  @Test
  fun readsPointsSolidsFacesConstructionLinesAndDimensions() {
    val parsed = entities(
      0 to "POINT", 8 to "A", 10 to 1.0, 20 to 2.0,
      0 to "SOLID", 8 to "A", 10 to 0.0, 20 to 0.0, 11 to 1.0, 21 to 0.0, 12 to 0.0, 22 to 1.0,
      0 to "3DFACE", 8 to "A", 10 to 0.0, 20 to 0.0, 11 to 1.0, 21 to 0.0, 12 to 1.0, 22 to 1.0, 13 to 0.0, 23 to 1.0, 70 to 2,
      0 to "XLINE", 8 to "A", 10 to 0.0, 20 to 0.0, 11 to 1.0, 21 to 0.0,
      0 to "DIMENSION", 8 to "A", 2 to "*D1",
    )
    assertEquals(DxfPointEntity(layer = "A", location = DxfPoint(1.0, 2.0)), parsed[0])
    assertEquals(DxfPoint(0.0, 1.0), assertIs<DxfSolid>(parsed[1]).corners[3], "a triangle repeats its third corner")
    assertEquals(2, assertIs<DxfFace3d>(parsed[2]).invisibleEdges)
    assertIs<DxfXLine>(parsed[3])
    assertEquals("*D1", assertIs<DxfDimension>(parsed[4]).blockName)
  }

  @Test
  fun readsTheBoundaryPathsAndPatternLinesOfAHatch() {
    val hatch = assertIs<DxfHatch>(
      entities(
        0 to "HATCH", 8 to "H", 10 to 0.0, 20 to 0.0, 30 to 0.5, 210 to 0.0, 220 to 0.0, 230 to 1.0,
        2 to "ANSI31", 70 to 0, 71 to 1, 91 to 2,
        // A polyline path with bulges.
        92 to 3, 72 to 1, 73 to 1, 93 to 2, 10 to 0.0, 20 to 0.0, 42 to 1.0, 10 to 2.0, 20 to 0.0, 42 to 1.0, 97 to 1, 330 to "AB",
        // An edge path: a line and a clockwise arc.
        92 to 16, 93 to 2,
        72 to 1, 10 to 0.0, 20 to 0.0, 11 to 4.0, 21 to 0.0,
        72 to 2, 10 to 2.0, 20 to 0.0, 40 to 2.0, 50 to 0.0, 51 to 180.0, 73 to 0,
        97 to 0,
        75 to 1, 76 to 1, 52 to 0.0, 41 to 1.0, 77 to 0, 78 to 1,
        53 to 45.0, 43 to 0.0, 44 to 0.0, 45 to -0.1, 46 to 0.1, 79 to 2, 49 to 0.2, 49 to -0.1,
        98 to 1, 10 to 1.0, 20 to 1.0,
      ).single()
    )
    assertEquals("ANSI31", hatch.patternName)
    assertEquals(0.5, hatch.elevation)
    assertEquals(1, hatch.hatchStyle)
    assertEquals(
      DxfHatchBoundaryPath.Polyline(flags = 3, vertices = listOf(DxfPoint(0.0, 0.0), DxfPoint(2.0, 0.0)), bulges = listOf(1.0, 1.0), closed = true),
      hatch.boundaryPaths[0],
    )
    val edges = assertIs<DxfHatchBoundaryPath.Edges>(hatch.boundaryPaths[1])
    assertTrue(edges.isOutermost)
    assertEquals(DxfHatchEdge.Line(DxfPoint(0.0, 0.0), DxfPoint(4.0, 0.0)), edges.edges[0])
    assertEquals(DxfHatchEdge.Arc(center = DxfPoint(2.0, 0.0), radius = 2.0, startAngleDegrees = 180.0, endAngleDegrees = 360.0), edges.edges[1])
    assertEquals(listOf(DxfHatchPatternLine(angleDegrees = 45.0, basePoint = DxfPoint(0.0, 0.0), offset = DxfPoint(-0.1, 0.1), dashes = listOf(0.2, -0.1))), hatch.patternLines)
  }

  @Test
  fun readsEllipseAndRationalSplineEdgesOfAHatch() {
    val hatch = assertIs<DxfHatch>(
      entities(
        0 to "HATCH", 8 to "H", 2 to "SOLID", 70 to 1, 71 to 0, 91 to 1,
        92 to 1, 93 to 2,
        72 to 3, 10 to 0.0, 20 to 0.0, 11 to 2.0, 21 to 0.0, 40 to 0.5, 50 to 0.0, 51 to 180.0, 73 to 1,
        72 to 4, 94 to 2, 73 to 1, 74 to 0, 95 to 6, 96 to 3,
        40 to 0.0, 40 to 0.0, 40 to 0.0, 40 to 1.0, 40 to 1.0, 40 to 1.0,
        10 to -2.0, 20 to 0.0, 42 to 1.0, 10 to 0.0, 20 to -1.0, 42 to 0.5, 10 to 2.0, 20 to 0.0, 42 to 1.0,
        97 to 0,
        75 to 0, 76 to 1, 98 to 0,
      ).single()
    )
    val edges = assertIs<DxfHatchBoundaryPath.Edges>(hatch.boundaryPaths.single()).edges
    assertEquals(DxfHatchEdge.Ellipse(center = DxfPoint(0.0, 0.0), majorAxis = DxfPoint(2.0, 0.0), ratio = 0.5, startAngleDegrees = 0.0, endAngleDegrees = 180.0), edges[0])
    val spline = assertIs<DxfHatchEdge.Spline>(edges[1])
    assertEquals(listOf(1.0, 0.5, 1.0), spline.weights)
    assertEquals(listOf(DxfPoint(-2.0, 0.0), DxfPoint(0.0, -1.0), DxfPoint(2.0, 0.0)), spline.controlPoints)
  }

  @Test
  fun aHatchStatingMoreVerticesThanItHoldsReadsWhatItHolds() {
    val hatch = assertIs<DxfHatch>(
      entities(
        0 to "HATCH", 8 to "H", 2 to "SOLID", 70 to 1, 71 to 0, 91 to 2_000_000_000,
        92 to 2, 72 to 0, 73 to 1, 93 to 2_000_000_000, 10 to 0.0, 20 to 0.0, 10 to 1.0, 20 to 0.0,
      ).single()
    )
    assertEquals(listOf(DxfPoint(0.0, 0.0), DxfPoint(1.0, 0.0)), assertIs<DxfHatchBoundaryPath.Polyline>(hatch.boundaryPaths.single()).vertices)
  }

  @Test
  fun aSolidHatchHasNoPatternLines() {
    val hatch = assertIs<DxfHatch>(
      entities(
        0 to "HATCH", 8 to "H", 2 to "SOLID", 70 to 1, 71 to 0, 91 to 1,
        92 to 2, 72 to 0, 73 to 1, 93 to 3, 10 to 0.0, 20 to 0.0, 10 to 1.0, 20 to 0.0, 10 to 0.0, 20 to 1.0, 97 to 0,
        75 to 0, 76 to 1, 98 to 0,
      ).single()
    )
    assertTrue(hatch.solidFill)
    assertEquals(emptyList(), hatch.patternLines)
    assertEquals(3, assertIs<DxfHatchBoundaryPath.Polyline>(hatch.boundaryPaths.single()).vertices.size)
  }
}
