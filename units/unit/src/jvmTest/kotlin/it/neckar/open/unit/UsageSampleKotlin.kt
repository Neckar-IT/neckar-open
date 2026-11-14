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
package it.neckar.open.unit

import it.neckar.open.unit.other.dpi
import it.neckar.open.unit.other.px
import it.neckar.open.unit.si.cm
import it.neckar.open.unit.si.m
import it.neckar.open.unit.si.m2
import it.neckar.open.unit.si.mm
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

/**
 * Usage sample for Kotlin
 *
 */
class UsageSampleKotlin {
  @Test
  fun testArea1() {
    val length: @m Double = 7.3
    val width: @m Double = 12.0
    val area: @m2 Double = calculateArea(length, width)
    Assertions.assertEquals(87.6, area, 0.0)
  }

  @Test
  fun testArea2() {
    val length: @m Double = 7.3
    val width: @cm Double = 1200.0
    val area: @m2 Double = calculateAreaSpecial(length, width)
    Assertions.assertEquals(87.6, area, 0.0)
  }

  @Test
  fun testPxToMm() {
    val res: @dpi Int = 72
    Assertions.assertEquals(10.0, pixelsToMm(28, res.toDouble()), 0.2)
  }

  companion object {
    /**
     * This is a simple method that takes two length arguments of a given unit
     *
     * @param length the length
     * @param width  the width
     * @return the area
     */
    fun calculateArea(length: @m Double, width: @m Double): @m2 Double {
      return length * width
    }

    /**
     * This method converts one unit. The conversion has to be done manually.
     * But this is a strength! Not every unit can be converted losless. Therefore doing it manually if
     * necessary seems to be the better idea.
     *
     * @param length the length
     * @param width  the width
     * @return the area
     */
    fun calculateAreaSpecial(length: @m Double, width: @cm Double): @m2 Double {
      return length * width / 100.0
    }

    fun pixelsToMm(pixels: @px Int, resolution: @dpi Double): @mm Double {
      return pixels / resolution * 25.4
    }
  }
}
