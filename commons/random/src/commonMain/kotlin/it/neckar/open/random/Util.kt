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
package it.neckar.open.random

import kotlin.jvm.JvmOverloads
import kotlin.jvm.JvmStatic
import kotlin.math.PI
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.pow

object Util {
  const val TwoPi: Double = PI * 2.0
  private val INV_LOG_HALF = 1.0 / ln(0.5)

  @JvmStatic
  fun clamp(input: Double, min: Double, max: Double): Double = if (input < min) min else if (input > max) max else input

  @JvmStatic
  fun bias(b: Double, t: Double): Double = t.pow(ln(b) * INV_LOG_HALF)

  @JvmStatic
  fun lerp(t: Double, a: Double, b: Double): Double = a + t * (b - a)

  @JvmStatic
  fun gain(g: Double, t: Double): Double {
    val g1 = clamp(g, 0.0, 1.0)
    return if (t < 0.5) {
      bias(1.0 - g1, 2.0 * t) * 0.5
    } else {
      1.0 - bias(1.0 - g1, 2.0 - 2.0 * t) * 0.5
    }
  }

  @JvmStatic
  fun quinticBlend(t: Double): Double = t * t * t * (t * (t * 6 - 15) + 10)

  @JvmStatic
  fun sawtooth(x: Double, p: Double): Double = 2.0 * (x / p - floor(0.5 + x / p)) * 0.5 + 0.5

  class Vector3d @JvmOverloads constructor(var x: Double = 0.0, var y: Double = 0.0, var z: Double = 0.0)

}
