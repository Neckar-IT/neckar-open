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
package it.neckar.open.kotlin.lang

import it.neckar.open.test.utils.RandomWithSeed
import org.junit.jupiter.api.Test
import kotlin.math.roundToInt

/**
 * This demo prints the generated random normal values
 */
class NormalizedDemo {
  @Test
  @RandomWithSeed(123)
  fun testNormalPlausibility() {
    val randomValuesCount = IntArray(2000) {
      randomNormal(100.0, 10.0).roundToInt()
    }.sorted()
      .groupingBy {
        it
      }.eachCount()

    for (i in 50..150) {
      val count = randomValuesCount[i] ?: 0

      print("$i".padStart(4))
      print("$count".padStart(4))
      print(": ")
      println("#".repeat(count))
    }
  }
}
