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
package it.neckar.open.formatting

import assertk.*
import assertk.assertions.*
import it.neckar.open.i18n.I18nConfiguration
import org.junit.jupiter.api.Test

/**
 */
class CachedFormatterTest {
  @Test
  internal fun testCachedWithOffset() {
    val i18nConfiguration = I18nConfiguration.Germany
    var currentOffset = 17.0

    val numberFormat = OffsetFormat(DecimalFormat(1, 1).cached(2)) { currentOffset }

    assertThat(numberFormat.currentCacheSize).isEqualTo(0)
    assertThat(numberFormat.format(1.0, i18nConfiguration)).isEqualTo("18,0")
    assertThat(numberFormat.currentCacheSize).isEqualTo(1)

    currentOffset = 10.0

    assertThat(numberFormat.format(2.0, i18nConfiguration)).isEqualTo("12,0")
    assertThat(numberFormat.currentCacheSize).isEqualTo(2)

    //Ask for the cached value again
    assertThat(numberFormat.format(1.0, i18nConfiguration)).isEqualTo("11,0")
    assertThat(numberFormat.currentCacheSize).isEqualTo(2)
  }

  @Test
  fun testIt() {
    val i18nConfiguration = I18nConfiguration.Germany
    val decimalFormat = DecimalFormat().cached(4)

    assertThat(decimalFormat.currentCacheSize).isEqualTo(0)
    assertThat(decimalFormat.format(1.0, i18nConfiguration)).isEqualTo("1")
    assertThat(decimalFormat.currentCacheSize).isEqualTo(1)
    assertThat(decimalFormat.format(2.0, i18nConfiguration)).isEqualTo("2")
    assertThat(decimalFormat.currentCacheSize).isEqualTo(2)
    assertThat(decimalFormat.format(3.0, i18nConfiguration)).isEqualTo("3")
    assertThat(decimalFormat.currentCacheSize).isEqualTo(3)

    //Check the same
    assertThat(decimalFormat.format(3.0, i18nConfiguration)).isEqualTo("3")
    assertThat(decimalFormat.currentCacheSize).isEqualTo(3)
    assertThat(decimalFormat.format(1.0, i18nConfiguration)).isEqualTo("1")
    assertThat(decimalFormat.currentCacheSize).isEqualTo(3)

    assertThat(decimalFormat.format(4.0, i18nConfiguration)).isEqualTo("4")
    assertThat(decimalFormat.currentCacheSize).isEqualTo(4)

    assertThat(decimalFormat.format(5.0, i18nConfiguration)).isEqualTo("5")
    assertThat(decimalFormat.currentCacheSize).isEqualTo(4)
  }

  @Test
  fun testMiniCache() {
    val i18nConfiguration = I18nConfiguration.Germany
    val decimalFormat = DecimalFormat().cached(1)

    assertThat(decimalFormat.currentCacheSize).isEqualTo(0)
    assertThat(decimalFormat.format(1.0, i18nConfiguration)).isEqualTo("1")
    assertThat(decimalFormat.currentCacheSize).isEqualTo(1)

    assertThat(decimalFormat.format(2.0, i18nConfiguration)).isEqualTo("2")
    assertThat(decimalFormat.currentCacheSize).isEqualTo(1)
  }

  @Test
  fun testCache2Locales() {
    val decimalFormat = DecimalFormat(1, 1)
      .cached(4)

    assertThat(decimalFormat.currentCacheSize).isEqualTo(0)
    assertThat(decimalFormat.format(1.0, I18nConfiguration.Germany)).isEqualTo("1,0")
    assertThat(decimalFormat.currentCacheSize).isEqualTo(1)
    assertThat(decimalFormat.format(1.0, I18nConfiguration.US)).isEqualTo("1.0")
    assertThat(decimalFormat.currentCacheSize).isEqualTo(2)

    assertThat(decimalFormat.format(2.0, I18nConfiguration.Germany)).isEqualTo("2,0")
    assertThat(decimalFormat.format(2.0, I18nConfiguration.US)).isEqualTo("2.0")
    assertThat(decimalFormat.currentCacheSize).isEqualTo(4)
  }
}
