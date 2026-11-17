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
package it.neckar.open.i18n

import assertk.*
import assertk.assertions.*
import org.junit.jupiter.api.Test

class LanguageCodeTest {
  @Test
  fun `accepts an ISO 639-1 code`() {
    assertThat(LanguageCode("de").value).isEqualTo("de")
    assertThat(LanguageCode.De.toString()).isEqualTo("de")
  }

  @Test
  fun `rejects anything that is not two lower-case letters`() {
    assertFailure { LanguageCode("DE") }.hasMessage("A language code must consist of two lower-case letters (ISO 639-1) but was [DE]")
    assertFailure { LanguageCode("de-DE") }.isInstanceOf(IllegalArgumentException::class)
    assertFailure { LanguageCode("deu") }.isInstanceOf(IllegalArgumentException::class)
    assertFailure { LanguageCode("") }.isInstanceOf(IllegalArgumentException::class)
    // "auto" is not a language — absence of a LanguageCode expresses auto-detection instead.
    assertFailure { LanguageCode("auto") }.isInstanceOf(IllegalArgumentException::class)
  }

  @Test
  fun `derives the language code from a locale, dropping the region`() {
    assertThat(Locale.Germany.languageCode).isEqualTo(LanguageCode.De)
    assertThat(Locale.US.languageCode).isEqualTo(LanguageCode.En)
    assertThat(Locale.France.languageCode).isEqualTo(LanguageCode.Fr)
    assertThat(Locale("de").languageCode).isEqualTo(LanguageCode.De)
  }
}
