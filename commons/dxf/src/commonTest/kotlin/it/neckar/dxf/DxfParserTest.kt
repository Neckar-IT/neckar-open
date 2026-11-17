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
import kotlin.test.assertTrue

class DxfParserTest {

  /**
   * A minimal DXF with an ENTITIES section (a closed room polyline, a room label, a wall line) and a
   * BLOCKS section whose entity must be ignored. Group codes are written without indentation on purpose -
   * the parser trims them.
   */
  private val sample: String = """
0
SECTION
2
ENTITIES
0
LWPOLYLINE
8
00_Räume
90
4
70
1
10
0.0
20
0.0
10
10.0
20
0.0
10
10.0
20
5.0
10
0.0
20
5.0
0
MTEXT
8
00_Text
10
3.0
20
2.0
1
{\fBahnschrift Light|b0|i0|c0|p34;Wohnen/Essen\P41.90m²}
0
LINE
8
00_Wände
10
0.0
20
0.0
11
10.0
21
0.0
0
ENDSEC
0
SECTION
2
BLOCKS
0
MTEXT
8
SHOULD_BE_IGNORED
1
ignore me
0
ENDSEC
0
EOF
""".trimIndent()

  @Test
  fun blockSectionEntitiesStayOutOfTheEntities() {
    val document = DxfParser.parse(sample)
    assertEquals(3, document.entities.size, "block-section entities must be ignored")
    assertTrue("SHOULD_BE_IGNORED" !in document.usedLayerNames)
  }

  @Test
  fun parsesClosedPolylineWithVertices() {
    val document = DxfParser.parse(sample)
    val polyline = document.entitiesOfType<DxfLwPolyline>().single()

    assertEquals("00_Räume", polyline.layer)
    assertTrue(polyline.closed)
    assertEquals(4, polyline.vertices.size)
    assertEquals(DxfPoint(0.0, 0.0), polyline.vertices.first())
    assertEquals(DxfPoint(10.0, 5.0), polyline.vertices[2])
  }

  @Test
  fun parsesMTextLabelWithCleanedText() {
    val document = DxfParser.parse(sample)
    val label = document.entitiesOfType<DxfMText>().single()

    assertEquals("00_Text", label.layer)
    assertEquals(DxfPoint(3.0, 2.0), label.insertionPoint)
    assertEquals("Wohnen/Essen\n41.90m²", label.text)
  }

  @Test
  fun parsesLineWithStartAndEnd() {
    val document = DxfParser.parse(sample)
    val line = document.entitiesOfType<DxfLine>().single()

    assertEquals("00_Wände", line.layer)
    assertEquals(DxfPoint(0.0, 0.0), line.start)
    assertEquals(DxfPoint(10.0, 0.0), line.end)
  }

  @Test
  fun entitiesCanBeFilteredByLayer() {
    val document = DxfParser.parse(sample)
    assertEquals(1, document.entitiesOnLayer("00_Räume").size)
    assertEquals(setOf("00_Räume", "00_Text", "00_Wände"), document.usedLayerNames)
  }

  @Test
  fun handlesCarriageReturnLineEndings() {
    val crlf = sample.replace("\n", "\r\n")
    val document = DxfParser.parse(crlf)
    assertEquals(3, document.entities.size)
    assertEquals("Wohnen/Essen\n41.90m²", document.entitiesOfType<DxfMText>().single().text)
  }
}
