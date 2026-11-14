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

/**
 * Contains common dimensions for different screens
 */
class Resolutions @JvmOverloads constructor(
  screenResolutions: Iterable<Resolution> = listOf(
    FOUR_K,
    QUXGA,
    QXGA,
    WUXGA,
    HD_1080,
    WSXGA_PLUS,
    UXGA,
    WSXGA,
    WXGA,
    SXGA,
    SXGA_960,
    WXGA_800,
    WXGA_768,
    HD_720,
    XGA_PLUS,
    XGA,
    SVGA,
    DVD,
    VGA,  //new Resolution( "", 480, 480 ),
    HVGA,
    WQVGA,
    QVGA,  //new Resolution( "", 256, 256 ),
    HQVGA,  //new Resolution( "", 208, 208 ),
    LoRES,
    QQVGA
  ),
) {

  private val screenResolutions: List<Resolution> = screenResolutions
    .toList()
    .sorted()
    .reversed()

  /**
   * Returns the common dimension that is the same or smaller
   *
   * @param maxWidth  the max width
   * @param maxHeight the max height
   * @return the best dimension for the max width/height
   */
  fun getBest(maxWidth: Int, maxHeight: Int): Resolution {
    for (resolution in screenResolutions) {
      if (maxWidth >= resolution.width && maxHeight >= resolution.height) {
        return resolution
      }
    }
    throw IllegalArgumentException("No resolution found for $maxWidth/$maxHeight")
  }

  /**
   * Returns the common dimensions
   *
   * @return the common dimensions
   */
  fun getScreenResolutions(): List<Resolution> {
    return screenResolutions
  }

  companion object {
    val A3_300: Resolution = Resolution(3508, 4961)

    val A4_300: Resolution = Resolution(2480, 3508)

    val A4_72: Resolution = Resolution(595, 842)

    val FOUR_K: Resolution = Resolution(4096, 2304)

    val QUXGA: Resolution = Resolution(3200, 2400)

    val WQXGA: Resolution = Resolution(2560, 1600)

    val WQHD: Resolution = Resolution(2560, 1440)

    val QXGA: Resolution = Resolution(2048, 1536)

    val WUXGA: Resolution = Resolution(1920, 1200)

    val HD_1080: Resolution = Resolution(1920, 1080)

    val WSXGA_PLUS: Resolution = Resolution(1680, 1050)

    val UXGA: Resolution = Resolution(1600, 1200)

    val WSXGA: Resolution = Resolution(1440, 900)

    val WXGA: Resolution = Resolution(1366, 768)

    val SXGA: Resolution = Resolution(1280, 1024)

    val SXGA_960: Resolution = Resolution(1280, 960)

    val WXGA_800: Resolution = Resolution(1280, 800)

    val WXGA_768: Resolution = Resolution(1280, 768)

    val HD_720: Resolution = Resolution(1280, 720)

    val XGA_PLUS: Resolution = Resolution(1152, 864)

    val XGA: Resolution = Resolution(1024, 768)

    val SVGA: Resolution = Resolution(800, 600)

    val WVGA: Resolution = Resolution(800, 480)

    val DVD: Resolution = Resolution(720, 576)

    val VGA: Resolution = Resolution(640, 480)

    val HVGA: Resolution = Resolution(480, 320)

    val WQVGA: Resolution = Resolution(400, 240)

    val QVGA: Resolution = Resolution(320, 240)

    val HQVGA: Resolution = Resolution(240, 160)

    val LoRES: Resolution = Resolution(160, 160)

    val QQVGA: Resolution = Resolution(160, 120)
  }
}
