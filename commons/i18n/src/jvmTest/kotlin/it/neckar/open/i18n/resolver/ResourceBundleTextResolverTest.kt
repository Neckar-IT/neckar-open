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
package it.neckar.open.i18n.resolver

import assertk.*
import assertk.assertions.*
import it.neckar.open.i18n.I18nConfiguration
import it.neckar.open.i18n.TextKey
import it.neckar.datetime.minimal.TimeZone
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.util.DefaultLocale


@DefaultLocale("fr-FR")
class ResourceBundleTextResolverTest {
  private val bundle: String = "it.neckar.open.i18n.test.testmessages"

  @Test
  fun testBundles() {
    val resolver = ResourceBundleTextResolver(bundle)
    assertThat(resolver).isNotNull()
    assertThat(resolver.bundleName).isEqualTo(bundle)
    assertThat(resolver.resolve(TextKey("ERROR_1"), I18nConfiguration.Germany)).isEqualTo("The Value 1 de: <{0}>")
    assertThat(resolver.resolve(TextKey("ERROR_1"), I18nConfiguration.US)).isEqualTo("The Value 1 en: <{0}>")
    assertThat(resolver.resolve(TextKey("ERROR_1"), I18nConfiguration.French)).isEqualTo("The Value 1 default: <{0}>")
  }
}

private val I18nConfiguration.Companion.French: I18nConfiguration
  get() {
    return I18nConfiguration(TimeZone.Berlin, it.neckar.open.i18n.Locale.France)
  }
