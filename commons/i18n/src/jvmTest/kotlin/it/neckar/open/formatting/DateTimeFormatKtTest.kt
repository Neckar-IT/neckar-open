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
import it.neckar.open.kotlin.lang.SpecialChars
import it.neckar.open.kotlin.lang.WhitespaceConfig
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.util.DefaultLocale

class DateTimeFormatKtTest {
  @DefaultLocale("en-US")
  @Test
  fun testFormatDuration() {
    assertThat(1001.0.formatAsDuration(WhitespaceConfig.Spaces)).isEqualTo("1 s 1.0 ms")
    assertThat(123123123123123.0.formatAsDuration(WhitespaceConfig.Spaces)).isEqualTo("1,425,036 days 3 h 32 min 3 s 123.0 ms")

    assertThat(1001.0.formatAsDuration()).isEqualTo("1${SpecialChars.Nnbsp}s${SpecialChars.Nbsp}1.0${SpecialChars.Nnbsp}ms")
    assertThat(123123123123123.0.formatAsDuration()).isEqualTo("1,425,036${SpecialChars.Nnbsp}days${SpecialChars.Nbsp}3${SpecialChars.Nnbsp}h${SpecialChars.Nbsp}32${SpecialChars.Nnbsp}min${SpecialChars.Nbsp}3${SpecialChars.Nnbsp}s${SpecialChars.Nbsp}123.0${SpecialChars.Nnbsp}ms")
  }
}
