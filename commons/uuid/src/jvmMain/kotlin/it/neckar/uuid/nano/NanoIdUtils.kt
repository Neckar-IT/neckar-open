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
package it.neckar.uuid.nano

import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.ln
import kotlin.random.Random

/**
 * A class for generating unique String IDs.
 */
object NanoIdUtils {
  /**
   * The default alphabet used by this class.
   */
  val DEFAULT_ALPHABET: CharArray = "_-0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray()

  /**
   * Creates a random Nano ID with the specified size, alphabet, and random number generator.
   */
  @JvmOverloads
  fun randomNanoId(random: Random = Random.Default, alphabet: CharArray = DEFAULT_ALPHABET, size: Int = 21): String {
    require(alphabet.size <= 256) { "Alphabet size must be less than or equal to 256." }
    require(size > 0) { "size must be greater than zero." }

    val mask = (2 shl floor(ln((alphabet.size - 1).toDouble()) / ln(2.0)).toInt()) - 1
    val step = ceil(1.6 * mask * size / alphabet.size).toInt()

    return buildString {
      while (true) {
        val bytes = ByteArray(step)
        random.nextBytes(bytes)
        for (i in 0 until step) {
          val alphabetIndex = bytes[i].toInt() and mask
          if (alphabetIndex < alphabet.size) {
            append(alphabet[alphabetIndex])
            if (this.length == size) {
              return@buildString
            }
          }
        }
      }
    }
  }
}
