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
import it.neckar.open.kotlin.lang.WhitespaceConfig
import org.junit.jupiter.api.Test

class WithUnitFormatTest {
  @Test
  fun `the delegate formats with the whitespace config of the caller`() {
    // The percentage format writes a small space before the percent sign, so it reads the config.
    // With the config dropped, one string mixed a narrow no-break space with a plain one.
    WithUnitFormat(PercentageFormat(decimalFormat2digits), "p.a.").let {
      assertThat(it.format(0.5, I18nConfiguration.Germany, WhitespaceConfig.Spaces)).isEqualTo("50,00 % p.a.")
    }
  }

  @Test
  fun testIt() {
    WithUnitFormat(decimalFormat, "daUnit").let {
      assertThat(it.format(17.01234, whitespaceConfig = WhitespaceConfig.Spaces)).isEqualTo("17.01 daUnit")
    }
  }

  @Test
  fun testCached() {
    var callCounter = 0

    val delegate = object : NumberFormat {
      override fun format(value: Double, i18nConfiguration: I18nConfiguration, whitespaceConfig: WhitespaceConfig): String {
        callCounter++
        return decimalFormat.format(value, i18nConfiguration, whitespaceConfig)
      }
    }

    val format = WithUnitFormat(delegate, "daUnit").cached()

    assertThat(callCounter).isEqualTo(0)
    assertThat(format.format(17.01234, whitespaceConfig = WhitespaceConfig.Spaces)).isEqualTo("17.01 daUnit")
    assertThat(callCounter).isEqualTo(1)

    assertThat(format.format(17.01234, whitespaceConfig = WhitespaceConfig.Spaces)).isEqualTo("17.01 daUnit")
    assertThat(callCounter).isEqualTo(1)
  }

  @Test
  fun testProvider() {
    var callCounter = 0

    val delegate = object : NumberFormat {
      override fun format(value: Double, i18nConfiguration: I18nConfiguration, whitespaceConfig: WhitespaceConfig): String {
        callCounter++
        return decimalFormat.format(value, i18nConfiguration, whitespaceConfig)
      }
    }

    var currentUnit: String? = "Unit0"

    val format = delegate.appendUnit {
      currentUnit
    }

    assertThat(callCounter).isEqualTo(0)
    assertThat(format.format(17.01234, whitespaceConfig = WhitespaceConfig.Spaces)).isEqualTo("17.01 Unit0")
    assertThat(callCounter).isEqualTo(1)
    assertThat(format.format(17.01234, whitespaceConfig = WhitespaceConfig.Spaces)).isEqualTo("17.01 Unit0")
    assertThat(callCounter).isEqualTo(1)

    //change the unit
    currentUnit = "Unit1"

    assertThat(callCounter).isEqualTo(1)
    assertThat(format.format(17.01234, whitespaceConfig = WhitespaceConfig.Spaces)).isEqualTo("17.01 Unit1")
    assertThat(callCounter).isEqualTo(2)
    assertThat(format.format(17.01234, whitespaceConfig = WhitespaceConfig.Spaces)).isEqualTo("17.01 Unit1")
    assertThat(callCounter).isEqualTo(2)

    //change the unit to null
    currentUnit = null

    assertThat(callCounter).isEqualTo(2)
    assertThat(format.format(17.01234, whitespaceConfig = WhitespaceConfig.Spaces)).isEqualTo("17.01")
    assertThat(callCounter).isEqualTo(3)
    assertThat(format.format(17.01234, whitespaceConfig = WhitespaceConfig.Spaces)).isEqualTo("17.01")
    assertThat(callCounter).isEqualTo(3)


    //Change *BACK* to initial value
    currentUnit = "Unit0"

    assertThat(callCounter).isEqualTo(3)
    assertThat(format.format(17.01234, whitespaceConfig = WhitespaceConfig.Spaces)).isEqualTo("17.01 Unit0")
    assertThat(callCounter).isEqualTo(3) //still cached
    assertThat(format.format(17.01234, whitespaceConfig = WhitespaceConfig.Spaces)).isEqualTo("17.01 Unit0")
    assertThat(callCounter).isEqualTo(3) //still cached
  }
}
