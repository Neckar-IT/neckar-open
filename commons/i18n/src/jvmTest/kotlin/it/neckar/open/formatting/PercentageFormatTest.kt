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
import it.neckar.open.kotlin.lang.WhitespaceConfig
import org.junit.jupiter.api.Test

class PercentageFormatTest {
  @Test
  internal fun testPrecision() {
    // example for 2 maximum fraction digits
    // 0.00001 -> 0.0%
    // 0.0001  -> 0.01%
    // 0.001   -> 0.1%
    // 0.01    -> 1%
    // 0.1     -> 10%
    // 1       -> 100%
    assertThat(PercentageFormat(DecimalFormat(2, 1, 2, true)).precision).isEqualTo(0.0001)
    assertThat(PercentageFormat(DecimalFormat(1, 1, 2, true)).precision).isEqualTo(0.001)
    assertThat(PercentageFormat(DecimalFormat(0, 1, 2, true)).precision).isEqualTo(0.01)
  }

  @Test
  fun testFormat() {
    assertThat((DecimalFormat(2, 1, 1, true).asPercentageFormat()).format(1.2123, whitespaceConfig = WhitespaceConfig.Spaces)).isEqualTo("121.23 %")
    assertThat((DecimalFormat(1, 1, 1, true).asPercentageFormat()).format(1.2123, whitespaceConfig = WhitespaceConfig.Spaces)).isEqualTo("121.2 %")
    assertThat((DecimalFormat(0, 1, 1, true).asPercentageFormat()).format(1.2123, whitespaceConfig = WhitespaceConfig.Spaces)).isEqualTo("121.2 %")
  }
}
