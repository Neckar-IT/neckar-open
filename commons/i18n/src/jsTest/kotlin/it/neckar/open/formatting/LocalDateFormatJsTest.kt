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
import it.neckar.datetime.minimal.LocalDate
import it.neckar.datetime.minimal.TimeZone
import it.neckar.open.i18n.I18nConfiguration
import it.neckar.open.i18n.Locale
import kotlin.test.Test

/**
 *
 */
class LocalDateFormatJsTest {
  val germanTokyo: I18nConfiguration = I18nConfiguration(
    Locale.Germany,
    Locale.Germany,
    TimeZone.Tokyo
  )

  val englishTokyo: I18nConfiguration = I18nConfiguration(
    Locale.US,
    Locale.US,
    TimeZone.Tokyo
  )

  val englishNewYork: I18nConfiguration = I18nConfiguration(
    Locale.US,
    Locale.US,
    TimeZone.NewYork
  )

  @Test
  fun testIso() {
    val localDate = LocalDate(2019, 1, 7)

    assertThat(localDateFormatIso8601.format(localDate, germanTokyo)).isEqualTo("2019-01-07")
    assertThat(localDateFormatIso8601.format(localDate, englishNewYork)).isEqualTo("2019-01-07")
  }

  @Test
  fun testLocale() {
    assertThat(localDateFormat.format(LocalDate(2019, 1, 7), germanTokyo)).isEqualTo("7.1.2019")
    assertThat(localDateFormat.format(LocalDate(2019, 1, 7), englishNewYork)).isEqualTo("1/6/2019")
    assertThat(localDateFormat.format(LocalDate(2019, 1, 7), I18nConfiguration.Germany)).isEqualTo("7.1.2019")

    assertThat(localDateFormat.format(LocalDate(2024, 6, 12), germanTokyo)).isEqualTo("12.6.2024")
    assertThat(localDateFormat.format(LocalDate(2024, 6, 12), englishNewYork)).isEqualTo("6/11/2024")
  }
}
