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

import it.neckar.open.kotlin.lang.fastFor
import kotlin.math.floor


// Source: https://gist.github.com/alksily/7a85a1898e65c936f861ee93516e397d
/**
 * Perlin Random Generator can create a random value from one to three inputs.
 * There are two ways to compute a random value:
 * Option 1: Perlin.noise() accepts up to three inputs, besides optional settings, and computes a value
 * Option 2: Perlin.noiseOctave() accepts only one input, besides optional settings, and computes a value by layering the noise method multiple times.
 * */
data class Perlin(val seed: Double, val frequency: Double = 1.0, val amplitude: Double = 1.0, val size: Int = 35) {

  private val permutation: IntArray = intArrayOf(
    151, 160, 137, 91, 90, 15, 131, 13, 201,
    95, 96, 53, 194, 233, 7, 225, 140, 36, 103, 30, 69, 142, 8, 99,
    37, 240, 21, 10, 23, 190, 6, 148, 247, 120, 234, 75, 0, 26,
    197, 62, 94, 252, 219, 203, 117, 35, 11, 32, 57, 177, 33, 88,
    237, 149, 56, 87, 174, 20, 125, 136, 171, 168, 68, 175, 74,
    165, 71, 134, 139, 48, 27, 166, 77, 146, 158, 231, 83, 111,
    229, 122, 60, 211, 133, 230, 220, 105, 92, 41, 55, 46, 245, 40,
    244, 102, 143, 54, 65, 25, 63, 161, 1, 216, 80, 73, 209, 76,
    132, 187, 208, 89, 18, 169, 200, 196, 135, 130, 116, 188, 159,
    86, 164, 100, 109, 198, 173, 186, 3, 64, 52, 217, 226, 250,
    124, 123, 5, 202, 38, 147, 118, 126, 255, 82, 85, 212, 207,
    206, 59, 227, 47, 16, 58, 17, 182, 189, 28, 42, 223, 183, 170,
    213, 119, 248, 152, 2, 44, 154, 163, 70, 221, 153, 101, 155,
    167, 43, 172, 9, 129, 22, 39, 253, 19, 98, 108, 110, 79, 113,
    224, 232, 178, 185, 112, 104, 218, 246, 97, 228, 251, 34, 242,
    193, 238, 210, 144, 12, 191, 179, 162, 241, 81, 51, 145, 235,
    249, 14, 239, 107, 49, 192, 214, 31, 181, 199, 106, 157, 184,
    84, 204, 176, 115, 121, 50, 45, 127, 4, 150, 254, 138, 236,
    205, 93, 222, 114, 67, 29, 24, 72, 243, 141, 128, 195, 78, 66,
    215, 61, 156, 180
  )


  private val p: IntArray = IntArray(512) { permutation[it % 256] }

  /** Computes a perlin value from three values. A custom size value can be used to smooth out the curve. A Large size leads to a smoother curve.
   * @param x Double
   * @param y Double
   * @param z Double
   * @param size Int
   * */
  fun noise(x: Double, y: Double, z: Double, size: Int = this.size): Double {
    val xFreq = x * frequency
    val yFreq = y * frequency
    val zFreq = z * frequency

    var sizeCustom = size
    var value = 0.0
    val initialSize = sizeCustom.toDouble()
    while (sizeCustom >= 1) {
      value += smoothNoise(xFreq / sizeCustom, yFreq / sizeCustom, zFreq / sizeCustom) * sizeCustom
      sizeCustom = (sizeCustom / 2.0).toInt()
    }
    return ((value / initialSize) * amplitude + 1) / 2
  }

  /** Computes a perlin value from two inputs.
   * @param x Double
   * @param y Double
   * */
  fun noise(x: Double, y: Double): Double {
    val xFreq = x * frequency
    val yFreq = y * frequency
    var value = 0.0
    var size = size.toDouble()
    val initialSize = size
    while (size >= 1) {
      value += smoothNoise(xFreq / size, yFreq / size, 0f / size) * size
      size /= 2.0
    }
    return ((value / initialSize) * amplitude + 1) / 2
  }

  /** Computes a perlin value from one value. A custom size value can be used to smooth out the resulting curve. A Large size leads to a smoother curve.
   * @param x Double
   * @param size Int
   * */
  fun noise(x: Double, size: Int = this.size): Double {
    val xFreq = x * frequency

    var sizeCustom = size
    var value = 0.0
    val initialSize = sizeCustom.toDouble()
    while (sizeCustom >= 1) {
      value += smoothNoise(xFreq / sizeCustom) * sizeCustom
      sizeCustom = (sizeCustom / 2.0).toInt()
    }
    return ((value / initialSize) * amplitude + 1) / 2
  }


  /** Computes a perlin value from one input by using multiple noise curves (octaves) and combining the result of the individual octaves.
   * @param x Double
   * @param octaves Int: Number of computed octaves
   * @param persistence Double: How much influence on the end result the lower octaves have.
   * */
  fun noiseOctave(x: Double, octaves: Int = 7, persistence: Double = 0.5): Double {
    var total = 0.0
    var frequency = 1.0
    var amplitude = 1.0
    var maxValue = 0.0 // Used for normalizing result to 0.0 - 1.0

    octaves.fastFor { _ ->
      total += noise(x * frequency) * amplitude
      maxValue += amplitude
      amplitude *= persistence
      frequency *= 2.0
    }

    return total / maxValue
  }

  /** Smooths out the perlin curve. The curve becomes smoother if the method is called more often.
   * @param xInput Double
   * @param yInput Double
   * @param zInput Double
   * */
  fun smoothNoise(xInput: Double, yInput: Double = 0.0, zInput: Double = 0.0): Double {
    // Offset each coordinate by the seed value
    var x = xInput
    var y = yInput
    var z = zInput
    x += seed * 2
    y += seed * 2
    z += seed * 2
    val floorX: Int = floor(x).toInt() and 255 // FIND UNIT CUBE THAT
    val floorY: Int = floor(y).toInt() and 255 // CONTAINS POINT.
    val floorZ: Int = floor(z).toInt() and 255
    x -= floor(x) // FIND RELATIVE X,Y,Z
    y -= floor(y) // OF POINT IN CUBE.
    z -= floor(z)
    val u = fade(x) // COMPUTE FADE CURVES
    val v = fade(y) // FOR EACH OF X,Y,Z.
    val w = fade(z)
    val a = p[floorX] + floorY
    val aa = p[a] + floorZ
    val ab = p[a + 1] + floorZ // HASH COORDINATES OF
    val b = p[floorX + 1] + floorY
    val ba = p[b] + floorZ
    val bb = p[b + 1] + floorZ // THE 8 CUBE CORNERS,
    return lerp(
      w, lerp(
        v, lerp(
          u, grad(p[aa], x, y, z),  // AND ADD
          grad(p[ba], x - 1, y, z)
        ),  // BLENDED
        lerp(
          u, grad(p[ab], x, y - 1, z),  // RESULTS
          grad(p[bb], x - 1, y - 1, z)
        )
      ),  // FROM 8
      lerp(
        v, lerp(
          u, grad(p[aa + 1], x, y, z - 1),  // CORNERS
          grad(p[ba + 1], x - 1, y, z - 1)
        ),  // OF CUBE
        lerp(
          u, grad(p[ab + 1], x, y - 1, z - 1),
          grad(p[bb + 1], x - 1, y - 1, z - 1)
        )
      )
    )
  }


  private fun fade(t: Double): Double {
    return t * t * t * (t * (t * 6 - 15) + 10)
  }

  private fun lerp(t: Double, a: Double, b: Double): Double {
    return a + t * (b - a)
  }

  private fun grad(hash: Int, x: Double, y: Double, z: Double): Double {
    val h = hash and 15 // CONVERT LO 4 BITS OF HASH CODE
    val u = if (h < 8) x else y
    // INTO 12 GRADIENT DIRECTIONS.
    val v = if (h < 4) y else if (h == 12 || h == 14) x else z
    return (if (h and 1 == 0) u else -u) + if (h and 2 == 0) v else -v
  }
}
