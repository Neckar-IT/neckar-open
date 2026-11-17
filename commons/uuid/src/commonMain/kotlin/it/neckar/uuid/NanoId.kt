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
package it.neckar.uuid

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.ln

/**
 * A class for generating unique String IDs.
 *
 * The implementations of the core logic in this class are based on NanoId, a JavaScript
 * library by Andrey Sitnik released under the MIT license. (https://github.com/ai/nanoid)
 */
@JvmInline
@Serializable
value class NanoId(val value: String) {
  override fun toString(): String {
    return value
  }

  companion object {
    /**
     * The default alphabet used by this class.
     * Creates url-friendly NanoId Strings using 64 unique symbols.
     */
    val DefaultAlphabet: CharArray = "_-0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray()

    /**
     * Alphabet consisting only of numbers and letters.
     */
    val AlphaNumeric: CharArray = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".toCharArray()


    /**
     * The default size used by this class.
     * Creates NanoId Strings with slightly more unique values than UUID v4.
     */
    const val DefaultSize: Int = 21

    /**
     * Static factory to retrieve a NanoId String.
     *
     * @param random   The random number generator.
     * @param alphabet The symbols used in the NanoId String.
     * @param size     The number of symbols in the NanoId String.
     * @return A randomly generated NanoId String.*
     */
    fun randomNanoId(
      random: kotlin.random.Random = it.neckar.open.kotlin.lang.random,
      alphabet: CharArray = DefaultAlphabet,
      size: Int = DefaultSize,
    ): NanoId {
      require(!(alphabet.isEmpty() || alphabet.size >= 256)) { "alphabet must contain between 1 and 255 symbols." }
      require(size > 0) { "size must be greater than zero." }

      val mask = (2 shl floor(ln((alphabet.size - 1).toDouble()) / ln(2.0)).toInt()) - 1
      val step = ceil(1.6 * mask * size / alphabet.size).toInt()

      return NanoId(buildString {
        while (length < size) {
          val bytes = ByteArray(step)
          random.nextBytes(bytes)

          for (i in 0 until step) {
            val alphabetIndex = bytes[i].toInt() and mask
            if (alphabetIndex < alphabet.size) {
              append(alphabet[alphabetIndex])
              if (length == size) {
                return NanoId(toString())
              }
            }
          }
        }
      })
    }

    /**
     * Creates a new generator
     */
    fun generator(
      size: Int = DefaultSize,
      alphabet: CharArray = DefaultAlphabet,
    ): NanoIdGenerator {
      return NanoIdGenerator(alphabet = alphabet, size = size)
    }
  }

  /**
   * Generator for Nano IDs
   */
  class NanoIdGenerator(
    private val random: kotlin.random.Random = it.neckar.open.kotlin.lang.random,
    val alphabet: CharArray = DefaultAlphabet,
    val size: Int = DefaultSize,
  ) {
    /**
     * Generates a new NanoId.
     */
    fun generate(): NanoId {
      return randomNanoId(random, alphabet, size)
    }
  }
}
