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
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import org.junit.jupiter.api.Test

class DrawingUnitTest {
  private val square4000 = listOf(0.0 to 0.0, 4000.0 to 0.0, 4000.0 to 4000.0, 0.0 to 4000.0)

  @Test
  fun theInsertionUnitsNameTheMeters() {
    assertThat(DrawingUnit.metersPerUnit(4)).isEqualTo(0.001)
    assertThat(DrawingUnit.metersPerUnit(6)).isEqualTo(1.0)
    assertThat(DrawingUnit.metersPerUnit(0)).isNull()
    assertThat(DrawingUnit.metersPerUnit(null)).isNull()
  }

  @Test
  fun roomsWithoutAStatedAreaLeaveTheUnitOpen() {
    val rooms = listOf(
      DrawingUnit.StatedRoomArea(statedAreaSquareMeters = null, outline = square4000),
      DrawingUnit.StatedRoomArea(statedAreaSquareMeters = 6.0, outline = listOf(0.0 to 0.0, 1.0 to 0.0, 2.0 to 0.0)),
    )
    assertThat(DrawingUnit.metersPerUnitFromRoomAreas(rooms)).isNull()
  }

  @Test
  fun oneMislabelledRoomOfThreeKeepsTheMillimeter() {
    val rooms = listOf(16.0, 16.0, 1600.0).map { DrawingUnit.StatedRoomArea(statedAreaSquareMeters = it, outline = square4000) }
    assertThat(DrawingUnit.metersPerUnitFromRoomAreas(rooms)).isEqualTo(0.001)
  }

  @Test
  fun aMeterDrawingWithSlopedCeilingRoomsStaysInMeters() {
    val square4 = listOf(0.0 to 0.0, 4.0 to 0.0, 4.0 to 4.0, 0.0 to 4.0)
    val rooms = List(3) { DrawingUnit.StatedRoomArea(statedAreaSquareMeters = 13.6, outline = square4) }
    assertThat(DrawingUnit.metersPerUnitFromRoomAreas(rooms)).isEqualTo(1.0)
  }
}
