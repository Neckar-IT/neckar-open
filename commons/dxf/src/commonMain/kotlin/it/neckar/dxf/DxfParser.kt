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

import kotlin.math.PI

/**
 * Parses ASCII DXF into a [DxfDocument]: header, `LAYER` and `LTYPE` tables, blocks and entities; a type without a class
 * of its own becomes [DxfUnknownEntity]. Codes are read per record, in file order only where DXF repeats them
 * (`LWPOLYLINE`, `SPLINE`, `HATCH`). Binary DXF and DWG are converted to ASCII DXF first.
 */
object DxfParser {

  private const val GroupCodeName: Int = 2
  private const val GroupCodeLayer: Int = 8
  private const val GroupCodeHeaderVariable: Int = 9
  private const val ClosedFlag: Int = 1

  /**
   * Parses the given DXF [content].
   */
  fun parse(content: CharSequence): DxfDocument {
    val sections = readSections(readRecords(content))

    val tableRecords = sections["TABLES"].orEmpty().drop(1)

    return DxfDocument(
      entities = assembleEntities(sections["ENTITIES"].orEmpty().drop(1)),
      header = sections["HEADER"]?.firstOrNull()?.let { readHeader(it) } ?: DxfHeader(),
      layerTable = tableRecords.filter { it.type == "LAYER" }.map { readLayer(it) },
      linetypes = tableRecords.filter { it.type == "LTYPE" }.map { readLinetype(it) },
      blocks = readBlocks(sections["BLOCKS"].orEmpty().drop(1)),
    )
  }

  /**
   * Turns the raw text into records: every group code 0 starts a record of the type its value names.
   * [lineSequence] normalizes CRLF/LF/CR.
   */
  private fun readRecords(content: CharSequence): List<DxfRecord> {
    val records = mutableListOf<DxfRecord>()
    var type: String? = null
    val codes = mutableListOf<DxfGroupCode>()

    val lines = content.lineSequence().iterator()
    while (lines.hasNext()) {
      val codeLine = lines.next()
      if (lines.hasNext().not()) break
      val valueLine = lines.next()

      val code = codeLine.trim().toIntOrNull() ?: continue
      if (code == 0) {
        type?.let { records.add(DxfRecord(it, codes.toList())) }
        type = valueLine.trim()
        codes.clear()
        if (type == "EOF") {
          return records
        }
      } else if (type != null) {
        codes.add(DxfGroupCode(code, valueLine))
      }
    }
    type?.let { records.add(DxfRecord(it, codes.toList())) }
    return records
  }

  /**
   * The records of each section by its name, starting with the `SECTION` record, which carries the variables of the
   * `HEADER`.
   */
  private fun readSections(records: List<DxfRecord>): Map<String, List<DxfRecord>> {
    val sections = mutableMapOf<String, MutableList<DxfRecord>>()
    var currentName: String? = null
    for (record in records) {
      when (record.type) {
        "SECTION" -> {
          val name = record.string(GroupCodeName)?.trim().orEmpty()
          sections.getOrPut(name) { mutableListOf() }.add(record)
          currentName = name
        }

        "ENDSEC" -> currentName = null
        else -> currentName?.let { sections.getValue(it).add(record) }
      }
    }
    return sections
  }

  private fun readHeader(section: DxfRecord): DxfHeader {
    var header = DxfHeader()
    var variable: String? = null
    for (groupCode in section.codes) {
      if (groupCode.code == GroupCodeHeaderVariable) {
        variable = groupCode.value.trim()
        continue
      }
      header = when (variable) {
        "\$INSUNITS" -> header.copy(insertionUnits = groupCode.intValue)
        "\$LTSCALE" -> header.copy(linetypeScale = groupCode.doubleValue ?: header.linetypeScale)
        "\$PDMODE" -> header.copy(pointDisplayMode = groupCode.intValue ?: header.pointDisplayMode)
        "\$PDSIZE" -> header.copy(pointDisplaySize = groupCode.doubleValue ?: header.pointDisplaySize)
        else -> header
      }
    }
    return header
  }

  private fun readLayer(record: DxfRecord): DxfLayer {
    return DxfLayer(
      name = record.string(GroupCodeName) .orEmpty(),
      on = record.int(62, 7) >= 0,
      frozen = record.int(70, 0) and 1 != 0,
      linetype = record.string(6) ?: "CONTINUOUS",
      lineweight = DxfLineweight(record.int(370, DxfLineweight.Default.hundredthsOfMillimeter)),
    )
  }

  private fun readLinetype(record: DxfRecord): DxfLinetype {
    return DxfLinetype(
      name = record.string(GroupCodeName) .orEmpty(),
      patternLength = record.double(40, 0.0),
      elements = record.doubles(49),
      complex = record.codes.any { it.code == 340 },
    )
  }

  private fun readBlocks(records: List<DxfRecord>): List<DxfBlock> {
    val blocks = mutableListOf<DxfBlock>()
    var index = 0
    while (index < records.size) {
      val begin = records[index]
      if (begin.type != "BLOCK") {
        index++
        continue
      }
      val end = (index + 1 until records.size).firstOrNull { records[it].type == "ENDBLK" } ?: records.size
      blocks.add(
        DxfBlock(
          name = begin.string(GroupCodeName) .orEmpty(),
          basePoint = begin.point(),
          entities = assembleEntities(records.subList(index + 1, end)),
        )
      )
      index = end + 1
    }
    return blocks
  }

  /**
   * Builds the entities of a run of entity records; a `POLYLINE` takes its `VERTEX` records, an `INSERT` its `ATTRIB`
   * records, both up to their `SEQEND`.
   */
  private fun assembleEntities(records: List<DxfRecord>): List<DxfEntity> {
    val entities = mutableListOf<DxfEntity>()
    var index = 0
    while (index < records.size) {
      val record = records[index]
      index++
      when (record.type) {
        "POLYLINE", "INSERT" -> {
          val followerType = if (record.type == "POLYLINE") "VERTEX" else "ATTRIB"
          val followers = mutableListOf<DxfRecord>()
          while (index < records.size && records[index].type == followerType) {
            followers.add(records[index])
            index++
          }
          if (index < records.size && records[index].type == "SEQEND") {
            index++
          }
          entities.add(if (record.type == "POLYLINE") readPolyline(record, followers) else readInsert(record, followers))
        }

        "VERTEX", "ATTRIB", "SEQEND" -> Unit
        else -> entities.add(buildEntity(record))
      }
    }
    return entities
  }

  private fun buildEntity(record: DxfRecord): DxfEntity {
    val layer = record.layer()
    val properties = record.entityProperties()

    return when (record.type) {
      "LWPOLYLINE" -> readLwPolyline(record)

      "MTEXT" -> DxfMText(
        layer = layer,
        insertionPoint = record.point(),
        rawText = record.readMText(),
        charHeight = record.double(40, 1.0),
        referenceWidth = record.double(41, 0.0),
        attachmentPoint = record.int(71, 1),
        direction = if (record.hasPoint(11)) record.point(11) else null,
        rotationDegrees = record.double(50, 0.0),
        lineSpacingFactor = record.double(44, 1.0),
        properties = properties,
      )

      "TEXT" -> DxfText(
        layer = layer,
        insertionPoint = record.point(),
        text = record.string(1) .orEmpty(),
        height = record.double(40, 1.0),
        rotationDegrees = record.double(50, 0.0),
        widthFactor = record.double(41, 1.0),
        horizontalAlignment = record.int(72, 0),
        verticalAlignment = record.int(73, 0),
        alignmentPoint = if (record.hasPoint(11)) record.point(11) else null,
        properties = properties,
      )

      "LINE" -> DxfLine(
        layer = layer,
        start = record.point(),
        end = record.point(11),
        properties = properties,
      )

      "CIRCLE" -> DxfCircle(layer = layer, center = record.point(), radius = record.double(40, 0.0), properties = properties)

      "ARC" -> DxfArc(
        layer = layer,
        center = record.point(),
        radius = record.double(40, 0.0),
        startAngleDegrees = record.double(50, 0.0),
        endAngleDegrees = record.double(51, 360.0),
        properties = properties,
      )

      "ELLIPSE" -> DxfEllipse(
        layer = layer,
        center = record.point(),
        majorAxis = record.point(11),
        ratio = record.double(40, 1.0),
        startParameter = record.double(41, 0.0),
        endParameter = record.double(42, 2 * PI),
        properties = properties,
      )

      "SPLINE" -> DxfSpline(
        layer = layer,
        degree = record.int(71, 3),
        knots = record.doubles(40),
        controlPoints = record.points(10),
        weights = record.doubles(41),
        fitPoints = record.points(11),
        closed = record.int(70, 0) and ClosedFlag != 0,
        properties = properties,
      )

      "POINT" -> DxfPointEntity(layer = layer, location = record.point(), properties = properties)

      "SOLID", "TRACE" -> DxfSolid(layer = layer, corners = record.corners(), properties = properties)

      "3DFACE" -> DxfFace3d(layer = layer, corners = record.corners(), invisibleEdges = record.int(70, 0), properties = properties)

      "XLINE" -> DxfXLine(layer = layer, basePoint = record.point(), direction = record.point(11), properties = properties)

      "DIMENSION" -> DxfDimension(layer = layer, blockName = record.string(GroupCodeName) .orEmpty(), properties = properties)

      "HATCH" -> DxfHatchReader(record.codes).read(layer, properties)

      else -> DxfUnknownEntity(layer = layer, type = record.type, properties = properties)
    }
  }

  private fun DxfRecord.layer(): String = string(GroupCodeLayer) ?: "0"

  private fun DxfRecord.entityProperties(): DxfEntityProperties {
    return DxfEntityProperties(
      linetype = string(6)?.trim() ?: DxfEntityProperties.ByLayer,
      linetypeScale = double(48, 1.0),
      lineweight = DxfLineweight(int(370, DxfLineweight.ByLayer.hundredthsOfMillimeter)),
      invisible = int(60, 0) != 0,
      paperSpace = int(67, 0) != 0,
      extrusion = point(210, DxfEntityProperties.DefaultExtrusion),
    )
  }

  /**
   * The four corners of a `SOLID`, `TRACE` or `3DFACE` (group codes 10 to 13); a missing fourth corner repeats the third.
   */
  private fun DxfRecord.corners(): List<DxfPoint> {
    val third = point(12)
    return listOf(point(10), point(11), third, point(13, third))
  }

  /**
   * Reads the vertices of an LWPOLYLINE in file order: group code 10 starts a vertex, the codes 20, 40, 41 and 42 up
   * to the next 10 belong to it.
   */
  private fun readLwPolyline(record: DxfRecord): DxfLwPolyline {
    val constantWidth = record.double(43, 0.0)
    val vertices = mutableListOf<DxfPoint>()
    val bulges = mutableListOf<Double>()
    val widths = mutableListOf<DxfSegmentWidth>()
    var pendingX: Double? = null

    for (groupCode in record.codes) {
      when (groupCode.code) {
        10 -> pendingX = groupCode.doubleValue
        20 -> {
          val x = pendingX ?: continue
          vertices.add(DxfPoint(x, groupCode.doubleValue ?: 0.0))
          bulges.add(0.0)
          widths.add(DxfSegmentWidth(constantWidth, constantWidth))
          pendingX = null
        }

        40 -> if (widths.isNotEmpty()) widths[widths.lastIndex] = widths.last().copy(start = groupCode.doubleValue ?: 0.0)
        41 -> if (widths.isNotEmpty()) widths[widths.lastIndex] = widths.last().copy(end = groupCode.doubleValue ?: 0.0)
        42 -> if (bulges.isNotEmpty()) bulges[bulges.lastIndex] = groupCode.doubleValue ?: 0.0
      }
    }

    return DxfLwPolyline(
      layer = record.layer(),
      vertices = vertices,
      closed = record.int(70, 0) and ClosedFlag == ClosedFlag,
      bulges = bulges,
      widths = widths,
      elevation = record.double(38, 0.0),
      properties = record.entityProperties(),
    )
  }

  private fun readPolyline(record: DxfRecord, vertexRecords: List<DxfRecord>): DxfPolyline {
    val flags = record.int(70, 0)
    val defaultStartWidth = record.double(40, 0.0)
    val defaultEndWidth = record.double(41, 0.0)

    val faceRecords = vertexRecords.filter { it.int(70, 0) and FaceRecordFlags == PolyfaceFaceFlag }
    val pointRecords = vertexRecords
      .filter { it.int(70, 0) and FaceRecordFlags != PolyfaceFaceFlag }
      .filter { it.int(70, 0) and SplineFrameControlPointFlag == 0 }

    val kind: DxfPolylineKind = when {
      flags and PolyfaceMeshFlag != 0 -> DxfPolylineKind.PolyfaceMesh(
        faces = faceRecords.map { face -> (71..74).map { face.int(it, 0) }.filter { it != 0 } },
      )

      flags and PolygonMeshFlag != 0 -> DxfPolylineKind.PolygonMesh(
        rowCount = record.int(71, 0),
        columnCount = record.int(72, 0),
        closedRows = flags and ClosedFlag != 0,
        closedColumns = flags and MeshClosedInNFlag != 0,
      )

      flags and Polyline3dFlag != 0 -> DxfPolylineKind.Polyline3d
      else -> DxfPolylineKind.Polyline2d(elevation = record.point().z)
    }

    return DxfPolyline(
      layer = record.layer(),
      kind = kind,
      vertices = pointRecords.map { it.point() },
      bulges = pointRecords.map { it.double(42, 0.0) },
      widths = pointRecords.map { DxfSegmentWidth(it.double(40, defaultStartWidth), it.double(41, defaultEndWidth)) },
      closed = flags and ClosedFlag != 0,
      properties = record.entityProperties(),
    )
  }

  private fun readInsert(record: DxfRecord, attributeRecords: List<DxfRecord>): DxfInsert {
    return DxfInsert(
      layer = record.layer(),
      blockName = record.string(GroupCodeName) .orEmpty(),
      insertionPoint = record.point(),
      scale = DxfPoint(record.double(41, 1.0), record.double(42, 1.0), record.double(43, 1.0)),
      rotationDegrees = record.double(50, 0.0),
      columnCount = record.int(70, 1),
      rowCount = record.int(71, 1),
      columnSpacing = record.double(44, 0.0),
      rowSpacing = record.double(45, 0.0),
      attributes = attributeRecords.map { attribute ->
        DxfAttribute(
          layer = attribute.layer(),
          tag = attribute.string(GroupCodeName) .orEmpty(),
          text = attribute.string(1) .orEmpty(),
          insertionPoint = attribute.point(),
          properties = attribute.entityProperties(),
        )
      },
      properties = record.entityProperties(),
    )
  }

  /**
   * Assembles the full MTEXT string. Long texts are split across multiple group-code-3 chunks followed
   * by a final group-code-1 chunk; concatenating them in document order reconstructs the original text.
   */
  private fun DxfRecord.readMText(): String {
    return codes.filter { it.code == 1 || it.code == 3 }.joinToString(separator = "") { it.value }
  }

  private const val Polyline3dFlag: Int = 8
  private const val PolygonMeshFlag: Int = 16
  private const val MeshClosedInNFlag: Int = 32
  private const val PolyfaceMeshFlag: Int = 64

  /**
   * The `VERTEX` flags that tell a face record of a polyface mesh (128 without 64) from one of its vertices (128 + 64).
   */
  private const val FaceRecordFlags: Int = 192
  private const val PolyfaceFaceFlag: Int = 128
  private const val SplineFrameControlPointFlag: Int = 16
}
