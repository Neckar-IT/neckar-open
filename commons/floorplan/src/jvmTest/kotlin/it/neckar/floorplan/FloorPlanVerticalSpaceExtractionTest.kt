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
import assertk.assertions.containsExactly
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import org.junit.jupiter.api.Test

/**
 * Vertical spaces (stairwells, voids, shafts) are detected from annotation text — never from layer
 * names — and linked across floors by their floor-local position: a stair sits at the same spot on
 * every floor it passes through.
 */
class FloorPlanVerticalSpaceExtractionTest {

  /**
   * Two floors side by side. A stair annotation sits at the same floor-local position on both floors
   * (inside Küche on floor 1, inside Schlafen on floor 2) and must become ONE confirmed vertical
   * space. A single `Luftraum` annotation exists only on floor 2 and must stay unconfirmed. An
   * unrelated note must produce nothing.
   */
  private val sample: String = buildString {
    appendLine("0"); appendLine("SECTION"); appendLine("2"); appendLine("ENTITIES")

    // First floor - rooms Küche and Bad.
    appendRoomOutline(layer = "00_Räume", minX = 0.0, minY = 0.0, maxX = 5.0, maxY = 5.0)
    appendMText(x = 2.5, y = 2.5, text = """Küche\P12.00m²""")
    appendRoomOutline(layer = "00_Räume", minX = 5.0, minY = 0.0, maxX = 10.0, maxY = 5.0)
    appendMText(x = 7.5, y = 2.5, text = """Bad\P6.00m²""")

    // First floor - ceiling-penetration annotation for the stair (arbitrary layer, arbitrary phrasing).
    appendMText(x = 2.0, y = 2.0, text = """BD/DD\PFür Treppe\PTYP KN42""")

    // Second floor - room Schlafen, offset far to the right.
    appendRoomOutline(layer = "00_Räume", minX = 100.0, minY = 0.0, maxX = 105.0, maxY = 5.0)
    appendMText(x = 102.5, y = 2.5, text = """Schlafen\P14.00m²""")

    // Second floor - the same stair at the same floor-local position, this time as single-line TEXT.
    appendText(x = 102.0, y = 2.0, text = "Treppe")

    // Second floor - a void that exists on this floor only.
    appendText(x = 104.0, y = 4.0, text = "Luftraum")

    // An unrelated note containing no vertical-space keyword.
    appendMText(x = 3.0, y = 4.0, text = "Rohbauhöhe wie Vertrag")

    appendLine("0"); appendLine("ENDSEC"); appendLine("0"); appendLine("EOF")
  }

  @Test
  fun linksStairAcrossFloorsIntoOneConfirmedVerticalSpace() {
    val extraction = FloorPlanRoomExtractor.extract(sample)

    val stair = extraction.verticalSpaces.single { it.keyword == "treppe" }
    assertThat(stair.confirmed).isTrue()
    assertThat(stair.footprints).hasSize(2)
    assertThat(stair.footprints.map { it.floorLabel }).containsExactly("Geschoss 1", "Geschoss 2")
    assertThat(stair.footprints.map { it.containingRoom }).containsExactly("Küche", "Schlafen")
  }

  @Test
  fun singleFloorCandidateStaysUnconfirmed() {
    val extraction = FloorPlanRoomExtractor.extract(sample)

    val void = extraction.verticalSpaces.single { it.keyword == "luftraum" }
    assertThat(void.confirmed).isFalse()
    assertThat(void.footprints).hasSize(1)
    assertThat(void.footprints.single().floorLabel).isEqualTo("Geschoss 2")
    assertThat(void.footprints.single().containingRoom).isEqualTo("Schlafen")
  }

  @Test
  fun textWithoutKeywordProducesNoVerticalSpace() {
    val extraction = FloorPlanRoomExtractor.extract(sample)

    assertThat(extraction.verticalSpaces).hasSize(2)
  }

  @Test
  fun anchorsAreFloorLocal() {
    val extraction = FloorPlanRoomExtractor.extract(sample)

    val stair = extraction.verticalSpaces.single { it.keyword == "treppe" }
    // Both floors: DXF anchor (2.0, 2.0) relative to the floor origin, y flipped (bbox maxY = 5.0).
    stair.footprints.forEach { footprint ->
      assertThat(footprint.anchor.x).isEqualTo(2.0)
      assertThat(footprint.anchor.y).isEqualTo(3.0)
    }
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

  private fun StringBuilder.appendMText(x: Double, y: Double, text: String) {
    appendLine("0"); appendLine("MTEXT")
    appendLine("8"); appendLine("00_Text")
    appendLine("10"); appendLine(x.toString())
    appendLine("20"); appendLine(y.toString())
    appendLine("1"); appendLine(text)
  }

  private fun StringBuilder.appendText(x: Double, y: Double, text: String) {
    appendLine("0"); appendLine("TEXT")
    appendLine("8"); appendLine("00_Text")
    appendLine("10"); appendLine(x.toString())
    appendLine("20"); appendLine(y.toString())
    appendLine("1"); appendLine(text)
  }
}
