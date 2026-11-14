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

import it.neckar.dxf.DxfDocument
import it.neckar.dxf.DxfLwPolyline
import it.neckar.dxf.DxfMText
import it.neckar.dxf.DxfParser
import it.neckar.dxf.DxfPoint
import it.neckar.dxf.DxfText
import it.neckar.geometry.Coordinates
import it.neckar.geometry.Polygon
import it.neckar.open.kotlin.lang.requireNotNull
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Extracts rooms from an Autodesk DXF floor-plan export.
 *
 * Strategy:
 * 1. Parse the DXF with [DxfParser].
 * 2. Collect room labels: every `MTEXT` whose text contains an area (`...m²`) yields a name + area + anchor.
 * 3. Collect candidate outlines: polylines on the configured room layer (the DXF closed flag is ignored).
 * 4. Match each label to the tightest (smallest-area) outline that contains the label anchor. This filters
 *    out the many non-room polylines on the room layer (hatches, sub-shapes) - only labelled areas become rooms.
 * 5. Cluster the matched rooms into floors by horizontal gaps (a building's floors are laid out side by side).
 * 6. Per floor, translate the outlines to the floor's top-left origin and flip the y axis (DXF is y-up,
 *    screen space is y-down), producing [ExtractedRoom] outlines in a floor-local pixel coordinate system.
 * 7. Detect vertical spaces (stairs, voids, shafts): annotation texts naming a vertical connection
 *    ([RoomExtractionConfig.verticalSpaceKeywords]) are assigned to floors and linked across floors by
 *    their floor-local position — a stair penetrates every floor at the same spot. Text-based on
 *    purpose: no layer names, no drawing-scheme assumptions.
 *
 * The absolute scale and alignment to a concrete background image is a separate calibration step; see
 * [RoomExtractionConfig.scale].
 */
object FloorPlanRoomExtractor {

  /**
   * Extracts the rooms from the ASCII DXF in [dxfContent].
   */
  fun extract(dxfContent: String, config: RoomExtractionConfig = RoomExtractionConfig()): FloorPlanExtraction {
    return extract(DxfParser.parse(dxfContent), config)
  }

  /**
   * Extracts the rooms from an already parsed [document].
   *
   * Only outlines carrying a label with a stated area become rooms - an unlabelled outline, and an
   * outline whose label states no area, are silently dropped. A drawing without a single such match
   * therefore yields an extraction with no floors and no vertical spaces rather than an error: a DXF
   * that does not follow the expected room-annotation scheme is a normal input here, not a failure.
   */
  fun extract(document: DxfDocument, config: RoomExtractionConfig = RoomExtractionConfig()): FloorPlanExtraction {
    val labels = document.entitiesOfType<DxfMText>().mapNotNull { RoomLabel.parseOrNull(it) }

    // Room outlines are polylines on the room layer with at least 3 vertices. The DXF "closed" flag is
    // intentionally not required: the architect's exports leave it unset even though the outlines form
    // closed areas, and ray-cast containment treats every vertex ring as implicitly closed.
    val outlines = document.entitiesOnLayer(config.roomLayer)
      .filterIsInstance<DxfLwPolyline>()
      .filter { it.vertices.size >= MinimumPolygonVertices }
      .map { RoomOutline(it.vertices) }

    val matchedRooms = labels.mapNotNull { label ->
      val tightestContainingOutline = outlines
        .filter { it.contains(label.anchor) }
        .minByOrNull { it.area }
        ?: return@mapNotNull null
      MatchedRoom(label, tightestContainingOutline)
    }

    if (matchedRooms.isEmpty()) {
      return FloorPlanExtraction(floors = emptyList(), verticalSpaces = emptyList())
    }

    val floorClusters = clusterIntoFloors(matchedRooms, config.floorGapFactor)
    val floors = floorClusters.mapIndexed { floorIndex, roomsOnFloor -> buildFloor(floorIndex, roomsOnFloor, config.scale) }
    val verticalSpaces = extractVerticalSpaces(document, floorClusters, floors, config)
    return FloorPlanExtraction(floors = floors, verticalSpaces = verticalSpaces)
  }

  /**
   * Splits the rooms into floors. Rooms are sorted left to right; a horizontal gap between the right edge
   * of the rooms seen so far and the left edge of the next room that exceeds [floorGapFactor] times the
   * median room width marks a floor boundary.
   */
  private fun clusterIntoFloors(rooms: List<MatchedRoom>, floorGapFactor: Double): List<List<MatchedRoom>> {
    val sorted = rooms.sortedBy { it.outline.minX }
    val medianWidth = rooms.map { it.outline.width }.sorted()[rooms.size / 2]
    val gapThreshold = medianWidth * floorGapFactor

    val floors = mutableListOf<MutableList<MatchedRoom>>()
    var currentFloor = mutableListOf(sorted.first())
    var currentFloorRightEdge = sorted.first().outline.maxX

    for (room in sorted.drop(1)) {
      val gap = room.outline.minX - currentFloorRightEdge
      if (gap > gapThreshold) {
        floors.add(currentFloor)
        currentFloor = mutableListOf()
      }
      currentFloor.add(room)
      currentFloorRightEdge = maxOf(currentFloorRightEdge, room.outline.maxX)
    }
    floors.add(currentFloor)

    return floors
  }

  /**
   * Transforms the floor's rooms into the floor-local, y-down pixel coordinate system and labels the floor.
   */
  private fun buildFloor(floorIndex: Int, rooms: List<MatchedRoom>, scale: Double): ExtractedFloor {
    val allVertices = rooms.flatMap { it.outline.vertices }
    val calibration = FloorCalibration(
      dxfMinX = allVertices.minOf { it.x },
      dxfMinY = allVertices.minOf { it.y },
      dxfMaxX = allVertices.maxOf { it.x },
      dxfMaxY = allVertices.maxOf { it.y },
      scale = scale,
    )

    val extractedRooms = rooms.map { matchedRoom ->
      val transformedVertices = matchedRoom.outline.vertices.map { vertex ->
        calibration.toPixel(vertex.x, vertex.y)
      }
      ExtractedRoom(
        name = matchedRoom.label.name,
        statedAreaSquareMeters = matchedRoom.label.areaSquareMeters,
        bounds = Polygon(transformedVertices),
      )
    }

    return ExtractedFloor(label = "Geschoss ${floorIndex + 1}", rooms = extractedRooms, calibration = calibration)
  }

  /**
   * Detects vertical spaces from annotation text (see class KDoc, step 7). Candidates on different
   * floors whose floor-local anchors coincide (within [RoomExtractionConfig.verticalStackingToleranceFraction]
   * of the floor diagonal) merge into one [ExtractedVerticalSpace]; a candidate without a partner on
   * another floor stays an unconfirmed single-footprint space.
   */
  private fun extractVerticalSpaces(
    document: DxfDocument,
    floorClusters: List<List<MatchedRoom>>,
    floors: List<ExtractedFloor>,
    config: RoomExtractionConfig,
  ): List<ExtractedVerticalSpace> {
    val keywordPatterns = config.verticalSpaceKeywords.map { keyword ->
      keyword.lowercase() to Regex("""(^|[^\p{L}])${Regex.escape(keyword)}""", RegexOption.IGNORE_CASE)
    }
    if (keywordPatterns.isEmpty()) {
      return emptyList()
    }

    val candidates = document.entities.mapNotNull { entity ->
      val (text, anchor) = when (entity) {
        is DxfMText -> entity.text to entity.insertionPoint
        is DxfText -> entity.text to entity.insertionPoint
        else -> return@mapNotNull null
      }
      val keyword = keywordPatterns.firstOrNull { (_, pattern) -> pattern.containsMatchIn(text) }?.first
        ?: return@mapNotNull null
      VerticalSpaceCandidate(keyword, anchor, floorIndexOf(anchor, floors))
    }.filter { it.floorIndex != null }

    // Greedy stacking: a candidate joins the first group with the same keyword whose footprints lie at
    // the same floor-local position; otherwise it starts a new group.
    val groups = mutableListOf<MutableList<LocatedCandidate>>()
    for (candidate in candidates.sortedBy { it.floorIndex }) {
      val floorIndex = candidate.floorIndex.requireNotNull { "floorIndex filtered above" }
      val floor = floors[floorIndex]
      val local = floor.calibration.toPixel(candidate.anchor.x, candidate.anchor.y)
      val tolerance = floorDiagonal(floor.calibration) * config.verticalStackingToleranceFraction
      val located = LocatedCandidate(candidate.keyword, floorIndex, local, candidate.anchor)

      val group = groups.firstOrNull { members ->
        members.first().keyword == candidate.keyword && members.any { it.local.distanceTo(local) <= tolerance }
      }
      if (group == null) {
        groups.add(mutableListOf(located))
      } else if (group.none { it.floorIndex == floorIndex }) {
        group.add(located)   // several annotations of the same stair on one floor collapse into one footprint
      }
    }

    val labelCounters = mutableMapOf<String, Int>()
    return groups.map { members ->
      val keyword = members.first().keyword
      val index = (labelCounters[keyword] ?: 0) + 1
      labelCounters[keyword] = index
      val footprints = members.sortedBy { it.floorIndex }.map { member ->
        VerticalSpaceFootprint(
          floorLabel = floors[member.floorIndex].label,
          anchor = member.local,
          containingRoom = floorClusters[member.floorIndex]
            .filter { it.outline.contains(member.dxfAnchor) }
            .minByOrNull { it.outline.area }
            ?.label?.name,
        )
      }
      ExtractedVerticalSpace(
        label = "${keyword.replaceFirstChar { it.uppercase() }} $index",
        keyword = keyword,
        confirmed = footprints.map { it.floorLabel }.distinct().size >= 2,
        footprints = footprints,
      )
    }
  }

  /**
   * The floor whose calibration bounding box (expanded by [FloorAssignMarginFraction] of its
   * diagonal) contains the DXF point; null when the annotation lies outside every floor.
   */
  private fun floorIndexOf(anchor: DxfPoint, floors: List<ExtractedFloor>): Int? {
    return floors.indices.firstOrNull { index ->
      val calibration = floors[index].calibration
      val margin = floorDiagonal(calibration) * FloorAssignMarginFraction
      anchor.x in (calibration.dxfMinX - margin)..(calibration.dxfMaxX + margin) &&
        anchor.y in (calibration.dxfMinY - margin)..(calibration.dxfMaxY + margin)
    }
  }

  private fun floorDiagonal(calibration: FloorCalibration): Double {
    val width = calibration.dxfMaxX - calibration.dxfMinX
    val height = calibration.dxfMaxY - calibration.dxfMinY
    return sqrt(width * width + height * height)
  }

  private const val MinimumPolygonVertices: Int = 3

  /**
   * Floor assignment tolerance: annotations may sit slightly outside the room-derived floor bounding box.
   */
  private const val FloorAssignMarginFraction: Double = 0.15
}

private data class VerticalSpaceCandidate(
  val keyword: String,
  val anchor: DxfPoint,
  val floorIndex: Int?,
)

private data class LocatedCandidate(
  val keyword: String,
  val floorIndex: Int,
  /**
   * The anchor in the floor's pixel coordinate system (floor-local, y-down).
   */
  val local: Coordinates,
  /**
   * The anchor in DXF model coordinates (y-up), used for room containment tests.
   */
  val dxfAnchor: DxfPoint,
)

/**
 * A room label parsed from an `MTEXT` entity: the room [name] (the text without the area line), the
 * stated [areaSquareMeters] and the [anchor] point used to locate the room outline.
 */
private data class RoomLabel(
  val name: String,
  val areaSquareMeters: Double?,
  val anchor: DxfPoint,
) {
  companion object {
    private val AREA = Regex("""([0-9]+[.,][0-9]+)\s*m²""")

    fun parseOrNull(mText: DxfMText): RoomLabel? {
      val text = mText.text
      val areaMatch = AREA.find(text) ?: return null

      val areaSquareMeters = areaMatch.groupValues[1].replace(',', '.').toDoubleOrNull()
      val name = text.lineSequence()
        .filterNot { AREA.containsMatchIn(it) }
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .joinToString(separator = " ")

      return RoomLabel(name = name, areaSquareMeters = areaSquareMeters, anchor = mText.insertionPoint)
    }
  }
}

/**
 * A room-outline polyline in DXF model coordinates, treated as an implicitly closed ring - the exports
 * leave the DXF closed flag unset (see [FloorPlanRoomExtractor.extract]).
 *
 * The geometric properties are computed once because every label is tested against every outline.
 */
private class RoomOutline(val vertices: List<DxfPoint>) {
  val minX: Double = vertices.minOf { it.x }
  val maxX: Double = vertices.maxOf { it.x }
  val width: Double = maxX - minX

  private val polygon: Polygon = Polygon(vertices.map { Coordinates.of(it.x, it.y) })

  /**
   * Shoelace area in squared DXF units. Used only to rank the outlines containing a label against each
   * other (tightest wins), so a self-intersecting ring skewing the value stays harmless.
   */
  val area: Double = shoelaceArea(vertices)

  fun contains(point: DxfPoint): Boolean {
    return polygon.containsRayCast(Coordinates.of(point.x, point.y))
  }

  private fun shoelaceArea(points: List<DxfPoint>): Double {
    var sum = 0.0
    for (index in points.indices) {
      val current = points[index]
      val next = points[(index + 1) % points.size]
      sum += current.x * next.y - next.x * current.y
    }
    return abs(sum) / 2.0
  }
}

private data class MatchedRoom(
  val label: RoomLabel,
  val outline: RoomOutline,
)
