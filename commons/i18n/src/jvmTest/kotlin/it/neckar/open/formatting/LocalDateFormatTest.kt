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
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.util.DefaultLocale

/**
 *
 */
class LocalDateFormatTest {
  @Test
  fun testIso() {
    assertThat(localDateFormatIso8601.format(LocalDate(2024, 6, 12), I18nConfiguration.Germany)).isEqualTo("2024-06-12")
  }

  /**
   * A localized short pattern can carry a text field: the Japanese calendar's is `GGGGGy/M/d`, and
   * the era text comes from the locale the formatter was built with.
   */
  @Test
  @DefaultLocale("de-DE")
  fun `the date is rendered with the locale of its pattern, not the default one`() {
    val japaneseCalendar = I18nConfiguration(
      textLocale = it.neckar.open.i18n.Locale("ja-JP-u-ca-japanese"),
      formatLocale = it.neckar.open.i18n.Locale("ja-JP-u-ca-japanese"),
      timeZone = TimeZone.Berlin,
    )

    assertThat(localDateFormat.format(LocalDate(2024, 1, 5), japaneseCalendar)).isEqualTo("AD2024/1/5")
  }

  @Test
  fun testLocale() {
    assertThat(localDateFormat.format(LocalDate(2024, 6, 12), I18nConfiguration.Germany)).isEqualTo("12.06.24")
    assertThat(localDateFormat.format(LocalDate(2024, 6, 12), I18nConfiguration.US)).isEqualTo("6/12/24")
  }
}
