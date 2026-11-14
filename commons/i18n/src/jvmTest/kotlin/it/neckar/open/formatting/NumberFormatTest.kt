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
import it.neckar.open.formatting.NumberFormatPatternParser.parsePattern
import it.neckar.open.i18n.I18nConfiguration
import it.neckar.open.kotlin.lang.SpecialChars.Nnbsp
import org.junit.jupiter.api.Test
import kotlin.text.Typography.nbsp

class NumberFormatTest {
  @Test
  fun `prettyFormat writes the integer part through the locale`() {
    assertThat(1.5.prettyFormat(numberOfDecimals = 1, i18nConfiguration = I18nConfiguration.Germany)).isEqualTo("1 \u00bd")
    assertThat(0.75.prettyFormat(numberOfDecimals = 1, i18nConfiguration = I18nConfiguration.Germany)).isEqualTo("0 \u00be")
    assertThat(1234.5.prettyFormat(numberOfDecimals = 1, i18nConfiguration = I18nConfiguration.Germany)).isEqualTo("1.234 \u00bd")
  }

  @Test
  fun testPercentage0Digits() {
    assertThat(percentageFormat0digits.format(1.234567, I18nConfiguration.US)).isEqualTo("123$Nnbsp%")
  }

  @Test
  internal fun testDefaultLocaleEnglish() {
    val format = DecimalFormat(maximumFractionDigits = 1)
    assertThat(format.format(1.23, I18nConfiguration.US)).isEqualTo("1.2")
  }

  @Test
  internal fun testDefaultLocaleGerman() {
    val format = DecimalFormat(maximumFractionDigits = 1)
    assertThat(format.format(1.23, I18nConfiguration.Germany)).isEqualTo("1,2")
  }

  @Test
  fun testParseEmptyPattern() {
    val numberFormat1 = parsePattern("")
    assertThat(numberFormat1).isNotNull()
    assertThat(numberFormat1.useGrouping).isFalse()
    assertThat(numberFormat1.minimumIntegerDigits).isEqualTo(1)
    assertThat(numberFormat1.minimumFractionDigits).isEqualTo(0)
    assertThat(numberFormat1.maximumFractionDigits).isEqualTo(0)
  }

  @Test
  fun testParsePattern() {
    parsePattern("#").also {
      assertThat(it).isNotNull()
      assertThat(it.useGrouping).isFalse()
      assertThat(it.minimumIntegerDigits).isEqualTo(1)
      assertThat(it.minimumFractionDigits).isEqualTo(0)
      assertThat(it.maximumFractionDigits).isEqualTo(0)
    }
    parsePattern("0").also {
      assertThat(it).isNotNull()
      assertThat(it.useGrouping).isFalse()
      assertThat(it.minimumIntegerDigits).isEqualTo(1)
      assertThat(it.minimumFractionDigits).isEqualTo(0)
      assertThat(it.maximumFractionDigits).isEqualTo(0)
    }
    parsePattern("00.0").also {
      assertThat(it).isNotNull()
      assertThat(it.useGrouping).isFalse()
      assertThat(it.minimumIntegerDigits).isEqualTo(2)
      assertThat(it.minimumFractionDigits).isEqualTo(1)
      assertThat(it.maximumFractionDigits).isEqualTo(1)
    }
    parsePattern("#,0.00#").also {
      assertThat(it).isNotNull()
      assertThat(it.useGrouping).isTrue()
      assertThat(it.minimumIntegerDigits).isEqualTo(1)
      assertThat(it.minimumFractionDigits).isEqualTo(2)
      assertThat(it.maximumFractionDigits).isEqualTo(3)
    }
  }

}
