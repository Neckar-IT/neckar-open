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

import it.neckar.geometry.Coordinates
import it.neckar.geometry.Polygon
import it.neckar.open.unit.other.px
import kotlinx.serialization.Serializable

/**
 * Configuration for [FloorPlanRoomExtractor].
 */
data class RoomExtractionConfig(
  /**
   * The DXF layer that carries the room-outline polylines. The German architecture template used by the
   * example plans names it `00_Räume`.
   */
  val roomLayer: String = "00_Räume",

  /**
   * A horizontal gap larger than `floorGapFactor` times the median room width starts a new floor. A
   * single DXF model space typically lays out the floors of a building side by side.
   *
   * The gap is measured in DXF units between the rightmost edge of all rooms placed so far and the left
   * edge of the next room from the left: a room reaching far to the right keeps the rooms it overlaps on
   * its own floor instead of splitting the floor behind it.
   */
  val floorGapFactor: Double = 1.0,

  /**
   * Scale applied when converting DXF units to the floor-local pixel coordinate system (px per DXF unit).
   * The default of 1.0 keeps the drawing's own units; callers align rooms to a concrete background image
   * by re-scaling once that image and its calibration are known.
   */
  val scale: Double = 1.0,

  /**
   * Keywords (case-insensitive, matched at word starts) that mark an annotation text as a vertical
   * connection: stairs, voids, shafts, elevators. Detection is text-based on purpose — no layer names,
   * no drawing-scheme assumptions; extend the list for further languages. Empty list disables
   * vertical-space extraction.
   */
  val verticalSpaceKeywords: List<String> = listOf(
    "treppe", "stair", "luftraum", "void", "aufzug", "elevator", "lift", "schacht", "shaft",
  ),

  /**
   * Two vertical-space candidates on different floors belong to the same vertical space when their
   * floor-local anchors are closer than this fraction of the floor's bounding-box diagonal. A stair
   * penetrates every floor at the same spot, so the tolerance only absorbs annotation-placement jitter.
   */
  val verticalStackingToleranceFraction: Double = 0.1,
)

/**
 * The affine transform from DXF model coordinates (y-up) to a floor's pixel coordinate system (y-down,
 * origin at the floor's top-left). Both the room outlines and any background geometry (walls, openings)
 * rendered for the same floor use this transform, so they are aligned by construction.
 */
@Serializable
data class FloorCalibration(
  /**
   * The bounding box of the floor's room outlines in DXF units - not the drawing's extents. Geometry the
   * room outlines do not cover (an annotation next to the outermost wall) therefore lies outside the box
   * and maps to a negative pixel coordinate or one beyond the floor's size.
   */
  val dxfMinX: Double,
  val dxfMinY: Double,
  val dxfMaxX: Double,
  val dxfMaxY: Double,
  /**
   * Pixels per DXF unit (the example plans use meters).
   */
  val scale: Double,
) {
  /**
   * Maps a DXF point to the floor's pixel coordinate system.
   */
  fun toPixel(x: Double, y: Double): Coordinates {
    return Coordinates.of(
      x = (x - dxfMinX) * scale,
      y = (dxfMaxY - y) * scale,
    )
  }
}

/**
 * One floor extracted from a DXF drawing, holding its rooms.
 *
 * The floor [label] is provisional (`Geschoss 1`, `Geschoss 2`, ... ordered left to right): the DXF
 * model space does not reliably associate a floor name with a room cluster.
 */
@Serializable
data class ExtractedFloor(
  val label: String,
  val rooms: List<ExtractedRoom>,
  /**
   * The DXF-to-pixel transform used for this floor's rooms.
   */
  val calibration: FloorCalibration,
)

/**
 * One room extracted from a DXF drawing: the cleaned [name], the [statedAreaSquareMeters] and the
 * outline [bounds] in the floor-local, y-down pixel coordinate system.
 *
 * [statedAreaSquareMeters] is what the room label claims, not the area of [bounds] - the two differ
 * where the label rounds or the outline omits a niche. [FloorPlanRoomExtractor] only turns an outline
 * into a room when its label states an area, so it is set for every room the extractor produces.
 */
@Serializable
data class ExtractedRoom(
  val name: String,
  val statedAreaSquareMeters: Double?,
  val bounds: @px Polygon,
)

/**
 * The complete extraction result for one DXF drawing: the [floors] with their rooms, plus the
 * [verticalSpaces] (stairwells, voids, shafts) that connect floors and therefore live outside the
 * per-floor room lists.
 */
@Serializable
data class FloorPlanExtraction(
  val floors: List<ExtractedFloor>,
  val verticalSpaces: List<ExtractedVerticalSpace>,
)

/**
 * The serialized `<name>.rooms.json` envelope written next to a DXF: the source file name plus flattened
 * counts for quick inspection, then the full [FloorPlanExtraction]. This is the single definition of the
 * on-disk sidecar shape consumed by the Python render pipeline in
 * `internal/open/commons/floorplan/tools/`; both the single-file CLI and the batch example generator
 * write it.
 */
@Serializable
data class FloorPlanExtractionResult(
  val sourceFile: String,
  val floorCount: Int,
  val roomCount: Int,
  val floors: List<ExtractedFloor>,
  val verticalSpaces: List<ExtractedVerticalSpace>,
) {
  companion object {
    fun of(sourceFile: String, extraction: FloorPlanExtraction): FloorPlanExtractionResult {
      return FloorPlanExtractionResult(
        sourceFile = sourceFile,
        floorCount = extraction.floors.size,
        roomCount = extraction.floors.sumOf { it.rooms.size },
        floors = extraction.floors,
        verticalSpaces = extraction.verticalSpaces,
      )
    }
  }
}

/**
 * A vertical connection through the building — a stairwell, void ("Luftraum"), shaft or elevator —
 * detected from annotation text and linked across floors by its floor-local position (a stair
 * penetrates every floor at the same spot).
 *
 * [confirmed] is true only when footprints were found on at least two floors; a single-floor match is
 * kept as an unconfirmed candidate so consumers can require review instead of trusting it blindly.
 */
@Serializable
data class ExtractedVerticalSpace(
  /**
   * Display label, e.g. `Treppe 1` (capitalized keyword plus running index per keyword).
   */
  val label: String,
  /**
   * The matched [RoomExtractionConfig.verticalSpaceKeywords] entry, lowercase.
   */
  val keyword: String,
  val confirmed: Boolean,
  val footprints: List<VerticalSpaceFootprint>,
)

/**
 * Where a vertical space touches one floor: the annotation [anchor] in the floor's pixel coordinate
 * system and the [containingRoom] (name of the tightest room polygon containing the anchor, if any).
 */
@Serializable
data class VerticalSpaceFootprint(
  val floorLabel: String,
  val anchor: @px Coordinates,
  val containingRoom: String?,
)
