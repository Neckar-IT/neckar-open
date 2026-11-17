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

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 *
 */
class ResolutionTest {
  @Test
  fun testIt() {
    val resolution = Resolution(1024, 768)
    assertEquals(1024, resolution.width.toLong())
    assertEquals(768, resolution.height.toLong())
    assertEquals(AspectRatio.AR_4_3, resolution.aspectRatio)
  }

  @Test
  fun testAspectRatio() {
    assertEquals(AspectRatio.AR_1_1.ratio, 1.0 / 1.0, 0.0)
    assertEquals(AspectRatio.AR_3_2.ratio, 3.0 / 2.0, 0.0)
    assertEquals(AspectRatio.AR_4_3.ratio, 4.0 / 3.0, 0.0)
    assertEquals(AspectRatio.AR_5_3.ratio, 5.0 / 3.0, 0.0)
    assertEquals(AspectRatio.AR_5_4.ratio, 5.0 / 4.0, 0.0)
    assertEquals(AspectRatio.AR_16_9.ratio, 16.0 / 9.0, 0.0)
    assertEquals(AspectRatio.AR_16_10.ratio, 16.0 / 10.0, 0.0)
  }

  @Test
  fun testSelect() {
    assertEquals(AspectRatio.AR_4_3, AspectRatio[4.0, 3.0])
    assertEquals(AspectRatio.AR_4_3, AspectRatio[400.0, 300.0])
    assertEquals(AspectRatio.AR_4_3, AspectRatio[1024.0, 768.0])
    assertEquals(AspectRatio.AR_5_4, AspectRatio[1280.0, 1024.0])
    assertEquals(1.0, AspectRatio[1.0, 17.0].widthFactor, 0.0)
    assertEquals(17.0, AspectRatio[1.0, 17.0].heightFactor, 0.0)
  }

  @Test
  fun testNikon() {
    assertEquals(AspectRatio.AR_3_2, AspectRatio[4256.0, 2832.0]) //D3
    assertEquals(AspectRatio.AR_3_2, AspectRatio[6048.0, 4032.0]) //D3X
    assertEquals(AspectRatio.AR_3_2, AspectRatio[4256.0, 2832.0]) //D3s
    assertEquals(AspectRatio.AR_3_2, AspectRatio[4256.0, 2832.0]) //D700
  }

  @Test
  fun testCanonRes() {
    assertEquals(AspectRatio.AR_3_2, AspectRatio[3888.0, 2592.0]) //1000D
    assertEquals(AspectRatio.AR_3_2, AspectRatio[3072.0, 2048.0]) //300D
    assertEquals(AspectRatio.AR_3_2, AspectRatio[3456.0, 2304.0]) //350D
    assertEquals(AspectRatio.AR_3_2, AspectRatio[3888.0, 2592.0]) //400D
    assertEquals(AspectRatio.AR_3_2, AspectRatio[4272.0, 2848.0]) //450D
    assertEquals(AspectRatio.AR_3_2, AspectRatio[4752.0, 3168.0]) //500D
    assertEquals(AspectRatio.AR_3_2, AspectRatio[5184.0, 3456.0]) //550D
    assertEquals(AspectRatio.AR_3_2, AspectRatio[4752.0, 3168.0]) //50D
    assertEquals(AspectRatio.AR_3_2, AspectRatio[3888.0, 2592.0]) //40D
    assertEquals(AspectRatio.AR_3_2, AspectRatio[3504.0, 2336.0]) //30D
    assertEquals(AspectRatio.AR_3_2, AspectRatio[5184.0, 3456.0]) //7D
    assertEquals(AspectRatio.AR_3_2, AspectRatio[4368.0, 2912.0]) //5D
    assertEquals(AspectRatio.AR_3_2, AspectRatio[5616.0, 3744.0]) //5D2
    assertEquals(AspectRatio.AR_3_2, AspectRatio[3888.0, 2592.0]) //1D3
    assertEquals(AspectRatio.AR_3_2, AspectRatio[4896.0, 3264.0]) //1D4
    assertEquals(AspectRatio.AR_3_2, AspectRatio[2464.0, 1648.0]) //1D
    assertEquals(AspectRatio.AR_3_2, AspectRatio[4064.0, 2704.0]) //1Ds
    assertEquals(AspectRatio.AR_3_2, AspectRatio[4992.0, 3328.0]) //1Ds2
    assertEquals(AspectRatio.AR_3_2, AspectRatio[5616.0, 3744.0]) //1Ds3
  }

  @Test
  fun testComparable() {
    Assertions.assertTrue(Resolution(100, 100) == Resolution(100, 100))
    Assertions.assertTrue(Resolution(100, 100).compareTo(Resolution(100, 100)) == 0)
    Assertions.assertTrue(Resolution(100, 100).compareTo(Resolution(100, 101)) < 0)
    Assertions.assertTrue(Resolution(100, 100).compareTo(Resolution(101, 101)) < 0)
    Assertions.assertTrue(Resolution(100, 100).compareTo(Resolution(101, 100)) < 0)
    Assertions.assertTrue(Resolution(100, 100).compareTo(Resolution(100, 99)) > 0)
    Assertions.assertTrue(Resolution(100, 100).compareTo(Resolution(99, 99)) > 0)
    Assertions.assertTrue(Resolution(100, 100).compareTo(Resolution(99, 100)) > 0)
    Assertions.assertTrue(Resolution(100, 100).compareTo(Resolution(101, 99)) < 0)
    Assertions.assertTrue(Resolution(100, 100).compareTo(Resolution(99, 101)) > 0)
  }
}
