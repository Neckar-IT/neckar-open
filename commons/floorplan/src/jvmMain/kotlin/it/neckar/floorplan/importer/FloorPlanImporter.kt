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
package it.neckar.floorplan.importer

import it.neckar.dxf.DxfParser
import it.neckar.floorplan.DrawingUnit
import it.neckar.floorplan.ExtractedFloor
import it.neckar.floorplan.ExtractedRoom
import it.neckar.floorplan.FloorFrame
import it.neckar.floorplan.FloorPlanRoomExtractor
import it.neckar.floorplan.ModelBounds
import it.neckar.floorplan.background.FloorBackgroundGeometry
import it.neckar.floorplan.background.FloorBackgroundPng
import it.neckar.geometry.Coordinates
import it.neckar.geometry.Polygon

/**
 * Imports the floors of a DXF in one process: rooms and background of every floor with rooms, as
 * `floorplan_import.py` of floorplan.neckar.it delivers them.
 */
object FloorPlanImporter {
  /**
   * Room outlines plus this margin frame a floor.
   */
  const val MarginMeters: Double = 1.0

  const val PixelsPerMeter: Double = 200.0

  /**
   * The floors with rooms of [dxf]; empty for a drawing without room outlines.
   *
   * @throws it.neckar.floorplan.background.FloorBackgroundTooComplexException when a floor is too complex to draw
   * @throws InterruptedException when the calling thread is interrupted, checked after parsing and per floor
   */
  fun import(dxf: String): List<ImportedFloorPlan> {
    val document = DxfParser.parse(dxf)
    checkNotInterrupted()
    val floors = FloorPlanRoomExtractor.extract(document).floors.filter { it.rooms.isNotEmpty() }
    val metersPerUnit = DrawingUnit.metersPerUnit(document.header.insertionUnits)
      ?: DrawingUnit.metersPerUnitFromRoomAreas(floors.flatMap { floor -> floor.rooms.map { DrawingUnit.StatedRoomArea(it.statedAreaSquareMeters, floor.outlineInUnits(it)) } })
      ?: 1.0

    return floors.map { floor ->
      checkNotInterrupted()
      val calibration = floor.calibration
      val bounds = ModelBounds(calibration.dxfMinX, calibration.dxfMinY, calibration.dxfMaxX, calibration.dxfMaxY).scaledBy(metersPerUnit)
      val frame = FloorFrame.around(bounds, MarginMeters, PixelsPerMeter)
      val rooms = floor.rooms.map { room ->
        val pixels = floor.outlineInUnits(room).map { (x, y) -> frame.toPixel(x * metersPerUnit, y * metersPerUnit) }
        ExtractedRoom(name = room.name, statedAreaSquareMeters = room.statedAreaSquareMeters, bounds = Polygon(pixels))
      }
      val background = FloorBackgroundGeometry.draw(document, metersPerUnit, frame)
      ImportedFloorPlan(label = floor.label, rooms = rooms, backgroundPng = FloorBackgroundPng.render(background))
    }
  }

  private fun checkNotInterrupted() {
    if (Thread.interrupted()) {
      throw InterruptedException("floor-plan import interrupted")
    }
  }

  /**
   * The outline of [room] in drawing units: the extractor stores it in the floor-local pixels of its calibration.
   */
  private fun ExtractedFloor.outlineInUnits(room: ExtractedRoom): List<Pair<Double, Double>> {
    return room.bounds.vertices().map { vertex: Coordinates ->
      vertex.x / calibration.scale + calibration.dxfMinX to calibration.dxfMaxY - vertex.y / calibration.scale
    }
  }
}
