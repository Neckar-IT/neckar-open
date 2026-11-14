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

/**
 * A parsed DXF entity of the `ENTITIES` section or of a block definition.
 *
 * Entity types without a class of their own are surfaced as [DxfUnknownEntity] so that no information about the layer
 * is silently dropped.
 */
sealed interface DxfEntity {
  /**
   * The layer the entity lives on (DXF group code 8). Defaults to `"0"` when the entity declares no layer.
   */
  val layer: String

  /**
   * Linetype, lineweight, visibility and extrusion of the entity.
   */
  val properties: DxfEntityProperties
}

/**
 * A lightweight polyline (`LWPOLYLINE`). Used for room outlines, walls and hatched areas.
 *
 * Its vertices lie in the object coordinate system of [DxfEntityProperties.extrusion] at the height [elevation].
 */
data class DxfLwPolyline(
  override val layer: String,
  /**
   * The polyline vertices in declaration order (group code 10/20 pairs).
   */
  val vertices: List<DxfPoint>,
  /**
   * Whether the polyline is closed (group code 70, bit 1). A closed polyline forms a polygon.
   */
  val closed: Boolean,
  /**
   * The bulge of the segment that starts at the vertex of the same index (group code 42): the tangent of a quarter of
   * the arc's included angle, positive counterclockwise, 0 for a straight segment.
   */
  val bulges: List<Double> = vertices.map { 0.0 },
  /**
   * The start and end width of the segment that starts at the vertex of the same index (group codes 40 and 41, or the
   * constant width 43); 0 draws a hairline.
   */
  val widths: List<DxfSegmentWidth> = vertices.map { DxfSegmentWidth.Hairline },
  val elevation: Double = 0.0,
  override val properties: DxfEntityProperties = DxfEntityProperties.Default,
) : DxfEntity

/**
 * A heavy polyline (`POLYLINE` with its `VERTEX` records up to `SEQEND`).
 */
data class DxfPolyline(
  override val layer: String,
  val kind: DxfPolylineKind,
  /**
   * The location of each `VERTEX` that is not a face record of a polyface mesh.
   */
  val vertices: List<DxfPoint>,
  /**
   * The bulge per vertex, see [DxfLwPolyline.bulges].
   */
  val bulges: List<Double>,
  val widths: List<DxfSegmentWidth>,
  /**
   * Group code 70, bit 1 of the `POLYLINE`; for a polygon mesh the closed flag of the M direction.
   */
  val closed: Boolean,
  override val properties: DxfEntityProperties = DxfEntityProperties.Default,
) : DxfEntity

/**
 * A formatted multi-line text (`MTEXT`). Used for room labels (`Wohnen/Essen 41.90m²`) and annotations.
 */
data class DxfMText(
  override val layer: String,
  /**
   * The insertion/anchor point of the text (group code 10/20).
   */
  val insertionPoint: DxfPoint,
  /**
   * The raw text including AutoCAD inline formatting codes (e.g. `{\fArial;...}`, `\P`).
   */
  val rawText: String,
  /**
   * The nominal height of a capital letter (group code 40).
   */
  val charHeight: Double = 1.0,
  /**
   * The width of the reference rectangle the text wraps in (group code 41); 0 when the text does not wrap.
   */
  val referenceWidth: Double = 0.0,
  /**
   * Where [insertionPoint] lies on the text box (group code 71): 1 top left, 2 top center, 3 top right, 4 middle left
   * up to 9 bottom right.
   */
  val attachmentPoint: Int = 1,
  /**
   * The direction of the text baseline in world coordinates (group code 11/21/31), or along [rotationDegrees].
   */
  val direction: DxfPoint? = null,
  val rotationDegrees: Double = 0.0,
  /**
   * The factor on the default line spacing of 5/3 of [charHeight] (group code 44).
   */
  val lineSpacingFactor: Double = 1.0,
  override val properties: DxfEntityProperties = DxfEntityProperties.Default,
) : DxfEntity {
  /**
   * The text with all inline formatting codes removed and `\P` turned into line breaks.
   */
  val text: String
    get() = cleanMTextFormatting(rawText)
}

/**
 * A single-line text (`TEXT`).
 */
data class DxfText(
  override val layer: String,
  /**
   * The first alignment point (group code 10/20), the start of the baseline of a left aligned text.
   */
  val insertionPoint: DxfPoint,
  val text: String,
  /**
   * The height of a capital letter (group code 40).
   */
  val height: Double = 1.0,
  val rotationDegrees: Double = 0.0,
  /**
   * The relative width of the characters (group code 41).
   */
  val widthFactor: Double = 1.0,
  /**
   * Group code 72: 0 left, 1 center, 2 right, 3 aligned, 4 middle, 5 fit.
   */
  val horizontalAlignment: Int = 0,
  /**
   * Group code 73: 0 baseline, 1 bottom, 2 middle, 3 top.
   */
  val verticalAlignment: Int = 0,
  /**
   * The second alignment point (group code 11/21), set for every alignment other than left on the baseline.
   */
  val alignmentPoint: DxfPoint? = null,
  override val properties: DxfEntityProperties = DxfEntityProperties.Default,
) : DxfEntity

/**
 * A straight line segment (`LINE`).
 */
data class DxfLine(
  override val layer: String,
  val start: DxfPoint,
  val end: DxfPoint,
  override val properties: DxfEntityProperties = DxfEntityProperties.Default,
) : DxfEntity

/**
 * A circle (`CIRCLE`); its center lies in the object coordinate system.
 */
data class DxfCircle(
  override val layer: String,
  val center: DxfPoint,
  val radius: Double,
  override val properties: DxfEntityProperties = DxfEntityProperties.Default,
) : DxfEntity

/**
 * A circular arc (`ARC`), counterclockwise from [startAngleDegrees] to [endAngleDegrees] in the object coordinate system.
 */
data class DxfArc(
  override val layer: String,
  val center: DxfPoint,
  val radius: Double,
  val startAngleDegrees: Double,
  val endAngleDegrees: Double,
  override val properties: DxfEntityProperties = DxfEntityProperties.Default,
) : DxfEntity

/**
 * An elliptic arc (`ELLIPSE`) in world coordinates: the point at parameter t is
 * `center + majorAxis * cos(t) + minorAxis * sin(t)`, the minor axis being the extrusion crossed with [majorAxis],
 * scaled to [ratio] times its length.
 */
data class DxfEllipse(
  override val layer: String,
  val center: DxfPoint,
  /**
   * The end point of the major axis relative to [center] (group code 11/21/31).
   */
  val majorAxis: DxfPoint,
  /**
   * The length of the minor axis over the length of the major axis (group code 40).
   */
  val ratio: Double,
  /**
   * Radians, group code 41; 0 for a full ellipse.
   */
  val startParameter: Double,
  /**
   * Radians, group code 42; 2π for a full ellipse.
   */
  val endParameter: Double,
  override val properties: DxfEntityProperties = DxfEntityProperties.Default,
) : DxfEntity

/**
 * A B-spline (`SPLINE`) in world coordinates.
 */
data class DxfSpline(
  override val layer: String,
  val degree: Int,
  val knots: List<Double>,
  val controlPoints: List<DxfPoint>,
  /**
   * One weight per control point for a rational spline, empty for a non-rational one.
   */
  val weights: List<Double>,
  /**
   * The points the spline passes through; a spline without control points is defined by these alone.
   */
  val fitPoints: List<DxfPoint>,
  /**
   * Group code 70, bit 1.
   */
  val closed: Boolean,
  override val properties: DxfEntityProperties = DxfEntityProperties.Default,
) : DxfEntity

/**
 * A point (`POINT`), drawn in the style of the header variables `$PDMODE` and `$PDSIZE`.
 */
data class DxfPointEntity(
  override val layer: String,
  val location: DxfPoint,
  override val properties: DxfEntityProperties = DxfEntityProperties.Default,
) : DxfEntity

/**
 * A filled triangle or quadrilateral (`SOLID`, `TRACE`) in the object coordinate system. DXF stores the corners in the
 * order 1, 2, 4, 3 of the outline; a triangle repeats its third corner.
 */
data class DxfSolid(
  override val layer: String,
  val corners: List<DxfPoint>,
  override val properties: DxfEntityProperties = DxfEntityProperties.Default,
) : DxfEntity

/**
 * A three- or four-sided face (`3DFACE`) in world coordinates, drawn as its outline.
 */
data class DxfFace3d(
  override val layer: String,
  /**
   * The four corners in outline order; a triangle repeats its third corner.
   */
  val corners: List<DxfPoint>,
  /**
   * Group code 70: bit n hides the edge that starts at corner n.
   */
  val invisibleEdges: Int,
  override val properties: DxfEntityProperties = DxfEntityProperties.Default,
) : DxfEntity

/**
 * A construction line (`XLINE`), infinite in both directions.
 */
data class DxfXLine(
  override val layer: String,
  val basePoint: DxfPoint,
  val direction: DxfPoint,
  override val properties: DxfEntityProperties = DxfEntityProperties.Default,
) : DxfEntity

/**
 * A dimension (`DIMENSION`); its lines, arrows and text are the content of the anonymous block [blockName].
 */
data class DxfDimension(
  override val layer: String,
  val blockName: String,
  override val properties: DxfEntityProperties = DxfEntityProperties.Default,
) : DxfEntity

/**
 * A block reference (`INSERT`): `rotate(rotationDegrees) * scale(scale) * (point - basePoint of the block) + insertionPoint`
 * in its object coordinate system; a `MINSERT` repeats it in [rowCount] times [columnCount] cells along the rotated axes.
 */
data class DxfInsert(
  override val layer: String,
  val blockName: String,
  val insertionPoint: DxfPoint,
  /**
   * The scale factors along x, y and z (group codes 41, 42, 43).
   */
  val scale: DxfPoint = DxfPoint(1.0, 1.0, 1.0),
  val rotationDegrees: Double = 0.0,
  val columnCount: Int = 1,
  val rowCount: Int = 1,
  val columnSpacing: Double = 0.0,
  val rowSpacing: Double = 0.0,
  /**
   * The `ATTRIB` records between the `INSERT` and its `SEQEND`.
   */
  val attributes: List<DxfAttribute> = emptyList(),
  override val properties: DxfEntityProperties = DxfEntityProperties.Default,
) : DxfEntity

/**
 * The value of a block attribute (`ATTRIB`) of an [DxfInsert], in world coordinates.
 */
data class DxfAttribute(
  override val layer: String,
  val tag: String,
  val text: String,
  val insertionPoint: DxfPoint,
  override val properties: DxfEntityProperties = DxfEntityProperties.Default,
) : DxfEntity

/**
 * Any entity type that is not modeled explicitly. Retains the layer and the raw DXF type name.
 */
data class DxfUnknownEntity(
  override val layer: String,
  val type: String,
  override val properties: DxfEntityProperties = DxfEntityProperties.Default,
) : DxfEntity
