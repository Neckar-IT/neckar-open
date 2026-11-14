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

import it.neckar.dxf.DxfPoint
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.atan
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Approximates curves by polylines whose chords deviate from the curve by at most a tolerance.
 */
internal object Tessellation {
  /**
   * The most chords one curve is split into, so a huge circle stays affordable.
   */
  private const val MaxSegments: Int = 4096

  private const val MaxSplineDegree: Int = 11

  /**
   * The points of the arc around [center] from [startRadians] counterclockwise over [spanRadians], both ends included.
   */
  fun arc(center: DxfPoint, radius: Double, startRadians: Double, spanRadians: Double, tolerance: Double): List<DxfPoint> {
    val segments = segmentCount(radius, spanRadians, tolerance)
    return (0..segments).map { index ->
      val angle = startRadians + spanRadians * index / segments
      DxfPoint(center.x + radius * cos(angle), center.y + radius * sin(angle), center.z)
    }
  }

  /**
   * The counterclockwise span from [start] to [end] in degrees: 0 for equal angles, 360 for a full turn.
   */
  fun angleSpanDegrees(start: Double, end: Double): Double {
    if (start == end) {
      return 0.0
    }
    val startModulo = start.mod(360.0)
    val endModulo = end.mod(360.0)
    if (startModulo == endModulo) {
      return 360.0
    }
    return if (endModulo < startModulo) endModulo + 360.0 - startModulo else endModulo - startModulo
  }

  /**
   * The points after [start] up to and including [end] of a polyline segment with [bulge] (the tangent of a quarter of
   * its included angle).
   */
  fun bulgeSegment(start: DxfPoint, end: DxfPoint, bulge: Double, tolerance: Double): List<DxfPoint> {
    val chordX = end.x - start.x
    val chordY = end.y - start.y
    val chord = sqrt(chordX * chordX + chordY * chordY)
    if (bulge == 0.0 || chord == 0.0) {
      return listOf(end)
    }
    val includedAngle = 4 * atan(bulge)
    val radius = chord / (2 * sin(abs(includedAngle) / 2))
    // The center lies on the bisector of the chord, left of it for a counterclockwise arc below a half circle.
    val offset = (1 - bulge * bulge) / (4 * bulge)
    val center = DxfPoint((start.x + end.x) / 2 - chordY * offset, (start.y + end.y) / 2 + chordX * offset, start.z)
    val startAngle = atan2(start.y - center.y, start.x - center.x)
    return arc(center, radius, startAngle, includedAngle, tolerance).drop(1).dropLast(1) + end
  }

  /**
   * The points of the elliptic arc `center + majorAxis * cos(t) + minorAxis * sin(t)` for t from [startParameter] to
   * [endParameter].
   */
  fun ellipse(center: DxfPoint, majorAxis: DxfPoint, minorAxis: DxfPoint, startParameter: Double, endParameter: Double, tolerance: Double): List<DxfPoint> {
    if (startParameter.isFinite().not() || endParameter.isFinite().not()) {
      return emptyList()
    }
    val span = (endParameter - startParameter).mod(2 * PI).let { if (it == 0.0) 2 * PI else it }
    val segments = segmentCount(max(majorAxis.length, minorAxis.length), span, tolerance)
    return (0..segments).map { index ->
      val parameter = startParameter + span * index / segments
      center + majorAxis * cos(parameter) + minorAxis * sin(parameter)
    }
  }

  /**
   * The points of a non-uniform rational B-spline; without valid knots the knots are clamped and uniform.
   */
  fun spline(degree: Int, knots: List<Double>, controlPoints: List<DxfPoint>, weights: List<Double>, tolerance: Double): List<DxfPoint> {
    // A degree beyond any CAD use costs its square per point; such a spline draws as its control polygon.
    if (controlPoints.size < 2 || degree < 1 || degree > MaxSplineDegree) {
      return controlPoints
    }
    val order = min(degree, controlPoints.size - 1) + 1
    val knotVector = if (knots.size == controlPoints.size + order && knots.zipWithNext().all { (a, b) -> b >= a }) knots else clampedUniformKnots(controlPoints.size, order)
    val weightVector = if (weights.size == controlPoints.size) weights else controlPoints.map { 1.0 }
    val first = knotVector[order - 1]
    val last = knotVector[controlPoints.size]
    if (last <= first) {
      return controlPoints
    }
    val polygonLength = controlPoints.zipWithNext().sumOf { (a, b) -> (b - a).length }
    val segments = (polygonLength / max(tolerance * 8, 1e-12)).toInt().coerceIn(min(controlPoints.size * 4, MaxSegments), MaxSegments)
    // The parameter only grows, so the knot span moves forward instead of being searched for each point.
    var span = order - 1
    return (0..segments).map { index ->
      val parameter = first + (last - first) * index / segments
      while (span < controlPoints.size - 1 && parameter >= knotVector[span + 1]) {
        span++
      }
      deBoor(order, knotVector, controlPoints, weightVector, parameter, span)
    }
  }

  private fun clampedUniformKnots(count: Int, order: Int): List<Double> {
    val inner = count - order
    return List(order) { 0.0 } + (1..inner).map { it.toDouble() / (inner + 1) } + List(order) { 1.0 }
  }

  private fun deBoor(order: Int, knots: List<Double>, controlPoints: List<DxfPoint>, weights: List<Double>, parameter: Double, span: Int): DxfPoint {
    val degree = order - 1
    val x = DoubleArray(order)
    val y = DoubleArray(order)
    val z = DoubleArray(order)
    val w = DoubleArray(order)
    for (j in 0..degree) {
      val point = controlPoints[span - degree + j]
      val weight = weights[span - degree + j]
      x[j] = point.x * weight
      y[j] = point.y * weight
      z[j] = point.z * weight
      w[j] = weight
    }
    for (r in 1..degree) {
      for (j in degree downTo r) {
        val left = knots[j + span - degree]
        val right = knots[j + 1 + span - r]
        val alpha = if (right == left) 0.0 else (parameter - left) / (right - left)
        x[j] = (1 - alpha) * x[j - 1] + alpha * x[j]
        y[j] = (1 - alpha) * y[j - 1] + alpha * y[j]
        z[j] = (1 - alpha) * z[j - 1] + alpha * z[j]
        w[j] = (1 - alpha) * w[j - 1] + alpha * w[j]
      }
    }
    val weight = if (w[degree] == 0.0) 1.0 else w[degree]
    return DxfPoint(x[degree] / weight, y[degree] / weight, z[degree] / weight)
  }

  /**
   * The chords an arc of [radius] over [spanRadians] needs so that none deviates more than [tolerance] from it.
   */
  private fun segmentCount(radius: Double, spanRadians: Double, tolerance: Double): Int {
    if (radius <= tolerance) {
      return ceil(abs(spanRadians) / (PI / 2)).toInt().coerceIn(1, MaxSegments)
    }
    val stepAngle = 2 * acos(1 - tolerance / radius)
    return ceil(abs(spanRadians) / stepAngle).toInt().coerceIn(1, MaxSegments)
  }
}
