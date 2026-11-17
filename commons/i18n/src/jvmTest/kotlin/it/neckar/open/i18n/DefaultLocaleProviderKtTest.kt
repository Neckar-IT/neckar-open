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
import org.junit.jupiter.api.util.DefaultLocale
import java.util.Locale

class DefaultLocaleProviderKtTest {
  @DefaultLocale("de-DE")
  @Test
  fun testLocaleStuff() {
    assertThat(Locale.getDefault().language).isEqualTo("de")
    assertThat(Locale.getDefault().country).isEqualTo("DE")
    assertThat(Locale.getDefault().displayName).isEqualTo("Deutsch (Deutschland)")
    assertThat(Locale.getDefault().displayScript).isEqualTo("")
    assertThat(Locale.getDefault().displayVariant).isEqualTo("")
    assertThat(Locale.getDefault().toLanguageTag()).isEqualTo("de-DE")
    assertThat(Locale.getDefault().country).isEqualTo("DE")
    assertThat(Locale.getDefault().language).isEqualTo("de")
    assertThat(Locale.getDefault().toString()).isEqualTo("de_DE")
  }

  @Test
  internal fun testOnlyLanguage() {
    assertThat(it.neckar.open.i18n.Locale("en").convert().language).isEqualTo("en")
    assertThat(it.neckar.open.i18n.Locale("en").convert().country).isEqualTo("")
  }

  @DefaultLocale("de-DE")
  @Test
  fun testGetDefaultLocaleDeDE() {
    val locale = DefaultLocaleProvider().defaultLocale
    assertThat(locale.locale).isEqualTo("de-DE")
  }

  @DefaultLocale("en-US")
  @Test
  fun testGetDefaultLocaleEnUs() {
    val locale = DefaultLocaleProvider().defaultLocale
    assertThat(locale.locale).isEqualTo("en-US")
  }

  @DefaultLocale("en-US")
  @Test
  fun testConvertBack() {
    assertThat(it.neckar.open.i18n.Locale("de-DE").convert()).isEqualTo(Locale.GERMANY)
    assertThat(it.neckar.open.i18n.Locale("en-US").convert()).isEqualTo(Locale.US)
  }

  @Test
  fun testConvertRound() {
    testRoundTrip(Locale.GERMANY)
    testRoundTrip(Locale.US)
    testRoundTrip(Locale.GERMAN)
    testRoundTrip(Locale.ENGLISH)
  }

  private fun testRoundTrip(locale: Locale) {
    val converted = locale.convert()
    assertThat(converted.convert(), "Locale: <$locale>, Converted <$converted>").isEqualTo(locale)
  }
}
