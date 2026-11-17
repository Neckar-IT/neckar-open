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
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

internal operator fun DxfPoint.plus(other: DxfPoint): DxfPoint = DxfPoint(x + other.x, y + other.y, z + other.z)

internal operator fun DxfPoint.minus(other: DxfPoint): DxfPoint = DxfPoint(x - other.x, y - other.y, z - other.z)

internal operator fun DxfPoint.times(factor: Double): DxfPoint = DxfPoint(x * factor, y * factor, z * factor)

internal fun DxfPoint.cross(other: DxfPoint): DxfPoint = DxfPoint(
  y * other.z - z * other.y,
  z * other.x - x * other.z,
  x * other.y - y * other.x,
)

internal val DxfPoint.length: Double
  get() = sqrt(x * x + y * y + z * z)

internal fun DxfPoint.normalized(): DxfPoint {
  val length = length
  return if (length == 0.0) this else this * (1.0 / length)
}

/**
 * An affine map of 3D points: a 3x3 matrix ([m00] to [m22]) plus a translation ([tx], [ty], [tz]).
 */
internal class AffineTransform(
  private val m00: Double, private val m01: Double, private val m02: Double, private val tx: Double,
  private val m10: Double, private val m11: Double, private val m12: Double, private val ty: Double,
  private val m20: Double, private val m21: Double, private val m22: Double, private val tz: Double,
) {
  fun apply(point: DxfPoint): DxfPoint = DxfPoint(
    m00 * point.x + m01 * point.y + m02 * point.z + tx,
    m10 * point.x + m11 * point.y + m12 * point.z + ty,
    m20 * point.x + m21 * point.y + m22 * point.z + tz,
  )

  /**
   * The largest factor by which the transform stretches a length in the xy plane.
   */
  val maxPlanarScale: Double
    get() = maxOf(sqrt(m00 * m00 + m10 * m10 + m20 * m20), sqrt(m01 * m01 + m11 * m11 + m21 * m21))

  /**
   * The transform that applies [inner] first and this transform second.
   */
  fun after(inner: AffineTransform): AffineTransform = AffineTransform(
    m00 * inner.m00 + m01 * inner.m10 + m02 * inner.m20,
    m00 * inner.m01 + m01 * inner.m11 + m02 * inner.m21,
    m00 * inner.m02 + m01 * inner.m12 + m02 * inner.m22,
    m00 * inner.tx + m01 * inner.ty + m02 * inner.tz + tx,
    m10 * inner.m00 + m11 * inner.m10 + m12 * inner.m20,
    m10 * inner.m01 + m11 * inner.m11 + m12 * inner.m21,
    m10 * inner.m02 + m11 * inner.m12 + m12 * inner.m22,
    m10 * inner.tx + m11 * inner.ty + m12 * inner.tz + ty,
    m20 * inner.m00 + m21 * inner.m10 + m22 * inner.m20,
    m20 * inner.m01 + m21 * inner.m11 + m22 * inner.m21,
    m20 * inner.m02 + m21 * inner.m12 + m22 * inner.m22,
    m20 * inner.tx + m21 * inner.ty + m22 * inner.tz + tz,
  )

  companion object {
    val Identity: AffineTransform = AffineTransform(1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0)

    fun translation(offset: DxfPoint): AffineTransform =
      AffineTransform(1.0, 0.0, 0.0, offset.x, 0.0, 1.0, 0.0, offset.y, 0.0, 0.0, 1.0, offset.z)

    fun scale(factors: DxfPoint): AffineTransform =
      AffineTransform(factors.x, 0.0, 0.0, 0.0, 0.0, factors.y, 0.0, 0.0, 0.0, 0.0, factors.z, 0.0)

    fun rotationZ(radians: Double): AffineTransform {
      val cos = cos(radians)
      val sin = sin(radians)
      return AffineTransform(cos, -sin, 0.0, 0.0, sin, cos, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0)
    }

    /**
     * The map from the object coordinate system of [extrusion] to world coordinates, by the arbitrary axis algorithm
     * of the DXF reference.
     */
    fun objectToWorld(extrusion: DxfPoint): AffineTransform {
      val normal = extrusion.normalized()
      if (normal == DxfPoint(0.0, 0.0, 1.0)) {
        return Identity
      }
      val axisX = if (abs(normal.x) < ArbitraryAxisLimit && abs(normal.y) < ArbitraryAxisLimit) {
        DxfPoint(0.0, 1.0, 0.0).cross(normal)
      } else {
        DxfPoint(0.0, 0.0, 1.0).cross(normal)
      }.normalized()
      val axisY = normal.cross(axisX).normalized()
      return AffineTransform(
        axisX.x, axisY.x, normal.x, 0.0,
        axisX.y, axisY.y, normal.y, 0.0,
        axisX.z, axisY.z, normal.z, 0.0,
      )
    }

    private const val ArbitraryAxisLimit: Double = 1.0 / 64.0
  }
}
