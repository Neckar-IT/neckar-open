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
package it.neckar.floorplan.background

import it.neckar.dxf.DxfHatch
import it.neckar.dxf.DxfHatchBoundaryPath
import it.neckar.dxf.DxfHatchEdge
import it.neckar.dxf.DxfHatchPatternLine
import it.neckar.dxf.DxfPoint
import it.neckar.open.kotlin.lang.toRadians
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin

/**
 * The boundary loops and pattern lines of a [DxfHatch] in its object coordinate system.
 */
internal object HatchGeometry {
  /**
   * Pattern lines closer than this, in drawing units, make the hatch draw as a solid fill.
   */
  private const val MinPatternLineDistance: Double = 1e-4

  /**
   * A pattern repeated more often between two boundary crossings draws as a continuous line.
   */
  private const val MaxPatternRepetitions: Double = 100_000.0

  /**
   * Endpoints of two edges closer than this connect.
   */
  private const val ConnectionTolerance: Double = 1e-9

  /**
   * The closed loops of the hatch: the boundary paths its hatch style fills, text boxes left out.
   */
  fun loops(hatch: DxfHatch, tolerance: Double): List<List<DxfPoint>> {
    val ignoreFrom = when (hatch.hatchStyle) {
      0 -> 3
      1 -> 2
      else -> 1
    }
    return hatch.boundaryPaths
      .sortedBy { pathTypeRank(it) }
      .filter { pathTypeRank(it) < ignoreFrom && it.isTextBox.not() }
      .flatMap { path ->
        when (path) {
          is DxfHatchBoundaryPath.Polyline -> listOf(polylineLoop(path, hatch.elevation, tolerance))
          is DxfHatchBoundaryPath.Edges -> edgeLoops(path, hatch.elevation, tolerance)
        }
      }
      .filter { it.size >= 2 }
  }

  private fun pathTypeRank(path: DxfHatchBoundaryPath): Int = when {
    path.isExternal -> 0
    path.isOutermost -> 1
    else -> 2
  }

  private fun polylineLoop(path: DxfHatchBoundaryPath.Polyline, elevation: Double, tolerance: Double): List<DxfPoint> {
    val vertices = path.vertices.map { it.copy(z = elevation) }
    if (vertices.isEmpty()) {
      return emptyList()
    }
    val points = mutableListOf(vertices.first())
    for (index in vertices.indices) {
      val next = vertices[(index + 1) % vertices.size]
      if (index == vertices.lastIndex && next == vertices[index]) {
        break
      }
      points.addAll(Tessellation.bulgeSegment(vertices[index], next, path.bulges.getOrElse(index) { 0.0 }, tolerance))
    }
    return points
  }

  /**
   * Chains the edges to loops: an edge joins the loop at whichever of its ends meets an end of the loop, reversed if
   * needed; an edge without a connection starts a new loop once the current one is closed.
   */
  private fun edgeLoops(path: DxfHatchBoundaryPath.Edges, elevation: Double, tolerance: Double): List<List<DxfPoint>> {
    val loops = mutableListOf<List<DxfPoint>>()
    // A deque, so an edge joins either end of the loop without copying it.
    var loop: ArrayDeque<DxfPoint>? = null
    for (edge in path.edges) {
      val segment = edgePoints(edge, tolerance).map { it.copy(z = elevation) }
      if (segment.size < 2) {
        continue
      }
      val current = loop
      when {
        current == null -> loop = ArrayDeque(segment)
        current.last().isCloseTo(segment.first()) -> current.addAll(segment.drop(1))
        current.last().isCloseTo(segment.last()) -> current.addAll(segment.reversed().drop(1))
        current.first().isCloseTo(segment.last()) -> segment.dropLast(1).asReversed().forEach { current.addFirst(it) }
        current.first().isCloseTo(segment.first()) -> segment.drop(1).forEach { current.addFirst(it) }
        current.first().isCloseTo(current.last()) -> {
          loops.add(current)
          loop = ArrayDeque(segment)
        }

        else -> current.addAll(segment)
      }
    }
    loop?.let { loops.add(it) }
    return loops
  }

  private fun DxfPoint.isCloseTo(other: DxfPoint): Boolean = abs(x - other.x) <= ConnectionTolerance && abs(y - other.y) <= ConnectionTolerance

  private fun edgePoints(edge: DxfHatchEdge, tolerance: Double): List<DxfPoint> = when (edge) {
    is DxfHatchEdge.Line -> listOf(edge.start, edge.end)

    is DxfHatchEdge.Arc -> {
      val span = Tessellation.angleSpanDegrees(edge.startAngleDegrees, edge.endAngleDegrees)
      if (edge.radius <= 0.0 || span == 0.0) emptyList() else Tessellation.arc(edge.center, edge.radius, edge.startAngleDegrees.toRadians(), span.toRadians(), tolerance)
    }

    is DxfHatchEdge.Ellipse -> {
      val minorAxis = DxfPoint(-edge.majorAxis.y, edge.majorAxis.x) * edge.ratio
      Tessellation.ellipse(
        center = edge.center,
        majorAxis = edge.majorAxis,
        minorAxis = minorAxis,
        startParameter = angleToParameter(edge.ratio, edge.startAngleDegrees.toRadians()),
        endParameter = angleToParameter(edge.ratio, edge.endAngleDegrees.toRadians()),
        tolerance = tolerance,
      )
    }

    is DxfHatchEdge.Spline -> if (edge.controlPoints.isNotEmpty()) {
      Tessellation.spline(edge.degree, edge.knots, edge.controlPoints, edge.weights, tolerance)
    } else {
      edge.fitPoints
    }
  }

  private fun angleToParameter(ratio: Double, angle: Double): Double {
    if (ratio == 0.0) {
      return angle
    }
    return atan2(sin(angle) / ratio, cos(angle))
  }

  /**
   * The pattern line segments inside [loops] in the object coordinate system, a dot as a segment of equal ends; null
   * when lines of one family lie closer than [MinPatternLineDistance], which draws as a solid fill.
   *
   * @throws FloorBackgroundTooComplexException when edges walked, crossings and dash steps exceed twice [maxSegments]
   */
  fun patternSegments(hatch: DxfHatch, loops: List<List<DxfPoint>>, maxSegments: Int): List<Pair<DxfPoint, DxfPoint>>? {
    val segments = mutableListOf<Pair<DxfPoint, DxfPoint>>()
    var stepCount = 0.0
    for (line in hatch.patternLines) {
      // A family of gaps alone draws nothing.
      if (line.dashes.isNotEmpty() && line.dashes.none { it >= 0.0 }) {
        continue
      }
      val directionX = cos(line.angleDegrees.toRadians())
      val directionY = sin(line.angleDegrees.toRadians())
      val lineDistance = directionX * line.offset.y - directionY * line.offset.x
      if (abs(lineDistance) < MinPatternLineDistance) {
        return null
      }
      stepCount += familySegments(line, directionX, directionY, lineDistance, loops, segments, maxSegments, 2.0 * maxSegments - stepCount, hatch.elevation)
      if (segments.size >= maxSegments) {
        break
      }
    }
    return segments
  }

  /**
   * Adds the segments of the family of [line] to [segments], at most about [maxSegments], and returns its steps: boundary
   * edges walked, crossings and dash steps, at most [maxSteps].
   */
  private fun familySegments(
    line: DxfHatchPatternLine,
    directionX: Double,
    directionY: Double,
    lineDistance: Double,
    loops: List<List<DxfPoint>>,
    segments: MutableList<Pair<DxfPoint, DxfPoint>>,
    maxSegments: Int,
    maxSteps: Double,
    elevation: Double,
  ): Double {
    val origin = line.basePoint
    // Signed distance of a point from the base line, in multiples of the line distance.
    fun lineNumber(point: DxfPoint): Double = (directionX * (point.y - origin.y) - directionY * (point.x - origin.x)) / lineDistance

    val crossings = mutableMapOf<Int, MutableList<Double>>()
    // Each family walks every boundary edge, so the edges count like crossings.
    var stepCount = loops.sumOf { it.size }.toDouble()
    requireWithin(stepCount, maxSteps)
    for (loop in loops) {
      for (index in loop.indices) {
        val a = loop[index]
        val b = loop[(index + 1) % loop.size]
        val numberA = lineNumber(a)
        val numberB = lineNumber(b)
        if (numberA == numberB || numberA.isFinite().not() || numberB.isFinite().not()) {
          continue
        }
        val low = minOf(numberA, numberB)
        val high = maxOf(numberA, numberB)
        stepCount += high - low
        requireWithin(stepCount, maxSteps)
        // Half-open: a line through a vertex counts once for the two edges that meet there.
        for (number in ceil(low).toInt() until ceil(high).toInt()) {
          val factor = (number - numberA) / (numberB - numberA)
          val x = a.x + (b.x - a.x) * factor
          val y = a.y + (b.y - a.y) * factor
          val lineOriginX = origin.x + line.offset.x * number
          val lineOriginY = origin.y + line.offset.y * number
          crossings.getOrPut(number) { mutableListOf() }.add(directionX * (x - lineOriginX) + directionY * (y - lineOriginY))
        }
      }
    }

    val patternLength = line.dashes.sumOf { abs(it) }
    for ((number, positions) in crossings) {
      positions.sort()
      val lineOriginX = origin.x + line.offset.x * number
      val lineOriginY = origin.y + line.offset.y * number
      fun pointAt(position: Double) = DxfPoint(lineOriginX + directionX * position, lineOriginY + directionY * position, elevation)

      for (pair in 0 until positions.size / 2) {
        val start = positions[2 * pair]
        val end = positions[2 * pair + 1]
        // A pattern of dots alone has no length to repeat; it draws continuous like a line without dashes.
        if (line.dashes.isEmpty() || patternLength == 0.0 || (end - start) / patternLength > MaxPatternRepetitions) {
          segments.add(pointAt(start) to pointAt(end))
        } else {
          // Every dash and gap of every repetition is a step, emitted or not.
          stepCount += ((end - start) / patternLength + 2) * line.dashes.size
          requireWithin(stepCount, maxSteps)
          dashSegments(line.dashes, patternLength, start, end) { from, to ->
            segments.add(pointAt(from) to pointAt(to))
            segments.size < maxSegments
          }
        }
        if (segments.size >= maxSegments) {
          return stepCount
        }
      }
    }
    return stepCount
  }

  private fun requireWithin(work: Double, maxWork: Double) {
    if (work > maxWork) {
      throw FloorBackgroundTooComplexException("the pattern lines of a hatch take more than ${maxWork.toLong()} steps")
    }
  }

  /**
   * Calls [emit] with the start and end position of every dash of [dashes], repeated from position 0, within [start] to
   * [end], until [emit] returns false.
   */
  private fun dashSegments(dashes: List<Double>, patternLength: Double, start: Double, end: Double, emit: (Double, Double) -> Boolean) {
    val firstPeriod = floor(start / patternLength)
    // Counted by index: far from the base point, adding one pattern length may not change the position at all.
    val periodCount = ceil(end / patternLength - firstPeriod).toLong().coerceIn(0, MaxPatternRepetitions.toLong() + 1)
    for (period in 0 until periodCount) {
      var position = (firstPeriod + period) * patternLength
      for (dash in dashes) {
        val dashEnd = position + abs(dash)
        if (dash >= 0.0) {
          val from = maxOf(position, start)
          val to = minOf(dashEnd, end)
          val more = when {
            dash == 0.0 && position in start..end -> emit(position, position)
            dash > 0.0 && to > from -> emit(from, to)
            else -> true
          }
          if (more.not()) {
            return
          }
        }
        position = dashEnd
      }
    }
  }
}
