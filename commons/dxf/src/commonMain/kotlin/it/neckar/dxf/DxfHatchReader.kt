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
 * Reads the group codes of a `HATCH` in file order: their meaning depends on the position (code 72 is the bulge flag
 * of a polyline path and the type of an edge, code 10 the elevation, a vertex, an edge point or a seed point).
 */
internal class DxfHatchReader(private val codes: List<DxfGroupCode>) {
  private var index: Int = 0

  fun read(layer: String, properties: DxfEntityProperties): DxfHatch {
    val header = DxfRecord("HATCH", codes.takeWhile { it.code != BoundaryPathCountCode })
    index = codes.indexOfFirst { it.code == BoundaryPathCountCode }.let { if (it < 0) codes.size else it }
    val pathCount = count(take(BoundaryPathCountCode))
    val boundaryPaths = mutableListOf<DxfHatchBoundaryPath>()
    while (boundaryPaths.size < pathCount) {
      boundaryPaths.add(readBoundaryPath() ?: break)
    }

    val hatchStyle = skipTo(75)?.intValue ?: 0
    val solidFill = header.int(70, 0) != 0
    val patternLines = if (solidFill) emptyList() else readPatternLines()

    return DxfHatch(
      layer = layer,
      solidFill = solidFill,
      patternName = header.string(2)?.trim().orEmpty(),
      hatchStyle = hatchStyle,
      elevation = header.double(30, 0.0),
      boundaryPaths = boundaryPaths,
      patternLines = patternLines,
      properties = properties,
    )
  }

  private fun readBoundaryPath(): DxfHatchBoundaryPath? {
    val flags = skipTo(92)?.intValue ?: return null
    val path = if (flags and PolylinePathFlag != 0) readPolylinePath(flags) else readEdgePath(flags)
    val sourceCount = count(take(97))
    repeat(sourceCount) { skip(330) }
    return path
  }

  private fun readPolylinePath(flags: Int): DxfHatchBoundaryPath.Polyline {
    val hasBulge = (take(72)?.intValue ?: 0) != 0
    val closed = (take(73)?.intValue ?: 0) != 0
    val count = count(take(93))
    val vertices = readWhileAt(10, count) {
      DxfPoint(double(10), double(20)) to (if (hasBulge) take(42)?.doubleValue ?: 0.0 else 0.0)
    }
    return DxfHatchBoundaryPath.Polyline(flags = flags, vertices = vertices.map { it.first }, bulges = vertices.map { it.second }, closed = closed)
  }

  private fun readEdgePath(flags: Int): DxfHatchBoundaryPath.Edges {
    val count = count(take(93))
    val edges = readWhileAt(72, count) {
      when (take(72)?.intValue) {
        1 -> DxfHatchEdge.Line(start = DxfPoint(double(10), double(20)), end = DxfPoint(double(11), double(21)))
        2 -> readArcEdge()
        3 -> readEllipseEdge()
        4 -> readSplineEdge()
        else -> null
      }
    }.filterNotNull()
    return DxfHatchBoundaryPath.Edges(flags = flags, edges = edges)
  }

  private fun readArcEdge(): DxfHatchEdge.Arc {
    val center = DxfPoint(double(10), double(20))
    val radius = double(40)
    val (start, end) = counterclockwiseAngles(double(50), double(51), take(73)?.intValue ?: 1)
    return DxfHatchEdge.Arc(center = center, radius = radius, startAngleDegrees = start, endAngleDegrees = end)
  }

  private fun readEllipseEdge(): DxfHatchEdge.Ellipse {
    val center = DxfPoint(double(10), double(20))
    val majorAxis = DxfPoint(double(11), double(21))
    val ratio = double(40)
    val (start, end) = counterclockwiseAngles(double(50), double(51), take(73)?.intValue ?: 1)
    return DxfHatchEdge.Ellipse(center = center, majorAxis = majorAxis, ratio = ratio, startAngleDegrees = start, endAngleDegrees = end)
  }

  private fun counterclockwiseAngles(start: Double, end: Double, counterclockwise: Int): Pair<Double, Double> {
    return if (counterclockwise != 0) start to end else (360.0 - end) to (360.0 - start)
  }

  private fun readSplineEdge(): DxfHatchEdge.Spline {
    val degree = take(94)?.intValue ?: 3
    val rational = (take(73)?.intValue ?: 0) != 0
    skip(74)
    val knotCount = count(take(95))
    val controlCount = count(take(96))
    val knots = readWhileAt(40, knotCount) { double(40) }
    val weighted = readWhileAt(10, controlCount) {
      DxfPoint(double(10), double(20)) to (take(42)?.doubleValue ?: 1.0)
    }
    val controlPoints = weighted.map { it.first }
    val weights = if (rational) weighted.map { it.second } else emptyList()
    val fitCount = count(take(97))
    val fitPoints = readWhileAt(11, fitCount) { DxfPoint(double(11), double(21)) }
    listOf(12, 22, 13, 23).forEach { skip(it) }
    return DxfHatchEdge.Spline(degree = degree, knots = knots, controlPoints = controlPoints, weights = weights, fitPoints = fitPoints)
  }

  private fun readPatternLines(): List<DxfHatchPatternLine> {
    val count = count(skipTo(78) ?: return emptyList())
    return readWhileAt(53, count) {
      val angle = double(53)
      val basePoint = DxfPoint(double(43), double(44))
      val offset = DxfPoint(double(45), double(46))
      val dashCount = count(take(79))
      DxfHatchPatternLine(angleDegrees = angle, basePoint = basePoint, offset = offset, dashes = readWhileAt(49, dashCount) { double(49) })
    }
  }

  /**
   * Calls [read] up to [count] times, as long as the current group code is [code]: a count larger than the data reads
   * the data.
   */
  private inline fun <T> readWhileAt(code: Int, count: Int, read: () -> T): List<T> {
    val results = mutableListOf<T>()
    while (results.size < count && codes.getOrNull(index)?.code == code) {
      results.add(read())
    }
    return results
  }

  /**
   * The count a group code states, at most the codes left to read: a damaged file cannot make a loop outrun its data.
   */
  private fun count(groupCode: DxfGroupCode?): Int = (groupCode?.intValue ?: 0).coerceIn(0, codes.size - index)

  /**
   * Consumes and returns the current group code if it is [code].
   */
  private fun take(code: Int): DxfGroupCode? {
    val current = codes.getOrNull(index) ?: return null
    if (current.code != code) {
      return null
    }
    index++
    return current
  }

  /**
   * Consumes the current group code if it is [code].
   */
  private fun skip(code: Int) {
    if (codes.getOrNull(index)?.code == code) {
      index++
    }
  }

  private fun double(code: Int): Double = take(code)?.doubleValue ?: 0.0

  /**
   * Consumes the codes up to and including the next [code] and returns it; null without such a code.
   */
  private fun skipTo(code: Int): DxfGroupCode? {
    while (index < codes.size) {
      val current = codes[index]
      index++
      if (current.code == code) {
        return current
      }
    }
    return null
  }

  private companion object {
    const val BoundaryPathCountCode: Int = 91
    const val PolylinePathFlag: Int = 2
  }
}
