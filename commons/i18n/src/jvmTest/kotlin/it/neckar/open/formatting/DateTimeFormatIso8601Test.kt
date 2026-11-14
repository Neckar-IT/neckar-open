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
import it.neckar.open.time.formatUtcForDebug
import it.neckar.open.unit.si.ms
import org.junit.jupiter.api.Test
import java.time.format.DateTimeParseException

/**
 *
 */
class DateTimeFormatIso8601Test {
  @Test
  fun testFormatAlwaysUtc() {
    assertThat(DateTimeFormatIso8601().format(1000000000000.0, I18nConfiguration.US, WhitespaceConfig.NonBreaking)).isEqualTo("2001-09-09T01:46:40.000Z")
    assertThat(DateTimeFormatIso8601().format(1000000000000.0, I18nConfiguration.US, WhitespaceConfig.Spaces)).isEqualTo("2001-09-09T01:46:40.000Z")
    assertThat(DateTimeFormatIso8601().format(1000000000000.0, I18nConfiguration.Germany)).isEqualTo("2001-09-09T01:46:40.000Z")
    assertThat(DateTimeFormatIso8601().format(1000000000000.0, I18nConfiguration.GermanyUTC)).isEqualTo("2001-09-09T01:46:40.000Z")
  }

  @Test
  fun testFormatParse() {
    formatAndParse(1000000000000.0, "2001-09-09T01:46:40.000Z", I18nConfiguration.US)
    formatAndParse(1000000000000.0, "2001-09-09T01:46:40.000Z", I18nConfiguration.Germany)
    formatAndParse(1000000000000.0, "2001-09-09T01:46:40.000Z", I18nConfiguration.GermanyUTC)

    formatAndParse(2000000000000.0, "2033-05-18T03:33:20.000Z", I18nConfiguration.GermanyUTC)
    formatAndParse(0.0, "1970-01-01T00:00:00.000Z", I18nConfiguration.GermanyUTC)
  }

  @Test
  fun testParse() {
    assertThat(DateTimeFormatIso8601.parse("2033-05-18T03:33:20.000Z")).isEqualTo(2000000000000.0)
  }

  @Test
  fun testParseErrorHandling() {
    assertFailure {
      DateTimeFormatIso8601.parse("invalidStringCannotParse")
    }.all {
      hasMessage("Text 'invalidStringCannotParse' could not be parsed at index 0")
      isInstanceOf(DateTimeParseException::class)
    }
  }

  private fun formatAndParse(millis: @ms Double, expectedFormat: String, i18nConfiguration: I18nConfiguration) {
    assertThat(DateTimeFormatIso8601().format(millis, i18nConfiguration)).isEqualTo(expectedFormat)

    DateTimeFormatIso8601.parse(expectedFormat).let {
      assertThat(it.formatUtcForDebug()).isEqualTo(millis.formatUtcForDebug())
    }
  }
}
