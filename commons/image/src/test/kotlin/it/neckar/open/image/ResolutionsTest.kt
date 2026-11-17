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
package it.neckar.open.image

import assertk.*
import assertk.assertions.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.awt.Dimension
import java.util.Arrays
import kotlin.test.assertFailsWith

/**
 *
 */
class ResolutionsTest {
  private var resolutions: Resolutions? = null

  @BeforeEach
  @Throws(Exception::class)
  fun setUp() {
    resolutions = Resolutions()
  }

  @Test
  fun testScreenResolutions() {
    assertEquals(25, resolutions!!.getScreenResolutions().size.toLong())
    for (resolution in resolutions!!.getScreenResolutions()) {
      assertTrue(resolution.width > 0, resolution.toString())
      assertTrue(resolution.height > 0, resolution.toString())
    }
  }

  @Test
  fun testSorting() {
    val first = resolutions!!.getScreenResolutions()[0]
    assertTrue(first.compareTo(resolutions!!.getScreenResolutions()[1]) > 0)
    val screenResolutions = resolutions!!.getScreenResolutions()
    for (i in screenResolutions.indices) {
      val resolution = screenResolutions[i]
      for (j in i + 1 until screenResolutions.size) {
        assertTrue(resolution.compareTo(screenResolutions[j]) > 0)
      }
    }
  }

  @Test
  fun testNone() {
    assertFailsWith<IllegalArgumentException> {
      assertEquals(resolutions!!.getBest(-10, -10), Dimension(160, 160))
    }
    assertFailsWith<IllegalArgumentException> {
      assertEquals(resolutions!!.getBest(10, 10), Dimension(160, 160))
    }.also { illegalArgumentException ->
      assertThat(illegalArgumentException).hasMessage("No resolution found for 10/10")

    }
  }

  @Test
  fun testSizes() {
    assertEquals(Resolutions.LoRES, resolutions!!.getBest(160, 160))
    assertEquals(Resolutions.LoRES, resolutions!!.getBest(161, 161))
    assertEquals(Resolutions.FOUR_K, resolutions!!.getBest(Int.MAX_VALUE, Int.MAX_VALUE))
  }

  @Test
  fun testConstructor() {
    val small = Resolution(100, 100)
    resolutions = Resolutions(Arrays.asList(small))
    assertEquals(resolutions!!.getBest(160, 160), small)
    assertEquals(resolutions!!.getBest(161, 161), small)
    assertEquals(resolutions!!.getBest(Int.MAX_VALUE, Int.MAX_VALUE), small)
  }
}
