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
package it.neckar.floorplan

import assertk.assertThat
import assertk.assertions.containsExactlyInAnyOrder
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import org.junit.jupiter.api.Test

class FloorPlanRoomExtractorTest {

  /**
   * Two floors laid out side by side. The first floor has two labelled rooms (Küche, Bad), the second
   * floor has one (Schlafen). A large unlabelled polyline on the room layer spans the whole first floor
   * and must be filtered out (tightest-containing match), and a polyline on a different layer must be ignored.
   */
  private val sample: String = buildString {
    appendLine("0"); appendLine("SECTION"); appendLine("2"); appendLine("ENTITIES")

    // First floor - enclosing distractor outline on the room layer, without a label.
    appendRoomOutline(layer = "00_Räume", minX = 0.0, minY = 0.0, maxX = 10.0, maxY = 5.0)
    // First floor - room Küche.
    appendRoomOutline(layer = "00_Räume", minX = 0.0, minY = 0.0, maxX = 5.0, maxY = 5.0)
    appendRoomLabel(x = 2.5, y = 2.5, text = """Küche\P12.00m²""")
    // First floor - room Bad.
    appendRoomOutline(layer = "00_Räume", minX = 5.0, minY = 0.0, maxX = 10.0, maxY = 5.0)
    appendRoomLabel(x = 7.5, y = 2.5, text = """Bad\P6.00m²""")

    // Furniture on another layer - must be ignored.
    appendRoomOutline(layer = "00_Möbel", minX = 1.0, minY = 1.0, maxX = 2.0, maxY = 2.0)

    // Second floor - room Schlafen, offset far to the right.
    appendRoomOutline(layer = "00_Räume", minX = 100.0, minY = 0.0, maxX = 105.0, maxY = 5.0)
    appendRoomLabel(x = 102.5, y = 2.5, text = """Schlafen\P14.00m²""")

    appendLine("0"); appendLine("ENDSEC"); appendLine("0"); appendLine("EOF")
  }

  @Test
  fun splitsIntoTwoFloors() {
    val floors = FloorPlanRoomExtractor.extract(sample).floors
    assertThat(floors).hasSize(2)
    assertThat(floors[0].label).isEqualTo("Geschoss 1")
    assertThat(floors[1].label).isEqualTo("Geschoss 2")
  }

  @Test
  fun extractsLabelledRoomsOnly() {
    val floors = FloorPlanRoomExtractor.extract(sample).floors

    assertThat(floors[0].rooms.map { it.name }).containsExactlyInAnyOrder("Küche", "Bad")
    assertThat(floors[1].rooms.map { it.name }).containsExactlyInAnyOrder("Schlafen")
  }

  @Test
  fun readsStatedAreas() {
    val floors = FloorPlanRoomExtractor.extract(sample).floors

    val kueche = floors[0].rooms.single { it.name == "Küche" }
    assertThat(kueche.statedAreaSquareMeters).isEqualTo(12.0)
    assertThat(floors[1].rooms.single().statedAreaSquareMeters).isEqualTo(14.0)
  }

  @Test
  fun normalizesEachFloorToItsOwnOriginWithFlippedY() {
    val floors = FloorPlanRoomExtractor.extract(sample).floors

    // The second floor's room sits at x=100 in the DXF but is translated to the floor's own origin.
    val schlafen = floors[1].rooms.single()
    val boundingBox = schlafen.bounds.boundingBox
    assertThat(boundingBox.location.x).isEqualTo(0.0)
    assertThat(boundingBox.location.y).isEqualTo(0.0)
    assertThat(boundingBox.size.width).isEqualTo(5.0)
    assertThat(boundingBox.size.height).isEqualTo(5.0)
  }

  private fun StringBuilder.appendRoomOutline(layer: String, minX: Double, minY: Double, maxX: Double, maxY: Double) {
    appendLine("0"); appendLine("LWPOLYLINE")
    appendLine("8"); appendLine(layer)
    appendLine("90"); appendLine("4")
    appendLine("70"); appendLine("1")
    appendVertex(minX, minY)
    appendVertex(maxX, minY)
    appendVertex(maxX, maxY)
    appendVertex(minX, maxY)
  }

  private fun StringBuilder.appendVertex(x: Double, y: Double) {
    appendLine("10"); appendLine(x.toString())
    appendLine("20"); appendLine(y.toString())
  }

  private fun StringBuilder.appendRoomLabel(x: Double, y: Double, text: String) {
    appendLine("0"); appendLine("MTEXT")
    appendLine("8"); appendLine("00_Text")
    appendLine("10"); appendLine(x.toString())
    appendLine("20"); appendLine(y.toString())
    appendLine("1"); appendLine(text)
  }
}
