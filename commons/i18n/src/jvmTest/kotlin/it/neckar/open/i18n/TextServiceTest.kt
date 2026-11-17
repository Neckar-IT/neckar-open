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
import it.neckar.open.i18n.resolve.MapBasedTextResolver
import it.neckar.open.i18n.resolve.SimpleMapBasedTextResolver
import org.junit.jupiter.api.Test

/**
 *
 */
class TextServiceTest {
  @Test
  internal fun testCreate() {
    val service = SimpleMapBasedTextResolver().orFallbackText()
    assertThat(service[TextKey("asdf"), I18nConfiguration.Germany]).isEqualTo("asdf")
  }

  @Test
  fun testFallback() {
    val textService = TextService()
    textService.addTextKeyTextResolver()
    assertThat(textService[TextKey("asdf", "defa"), I18nConfiguration.US]).isEqualTo("asdf")
  }

  @Test
  fun testDef() {
    val textService = TextService()
    textService.addFallbackTextResolver()
    assertThat(textService[TextKey("asdf", "defa"), I18nConfiguration.US]).isEqualTo("defa")
  }

  @Test
  fun testTextService() {
    val textService = TextService()
    val textKey = TextKey("abc", "defa")

    //Use the text key as text
    assertThat(textService[textKey, I18nConfiguration.US]).isEqualTo(textKey.key)

    val provider1 = SimpleMapBasedTextResolver()
    val provider2 = MapBasedTextResolver()

    textService.addTextResolverAtFirst(provider2)
    textService.addTextResolverAtFirst(provider1)

    provider2.setText(Locale.US, textKey, "a")
    assertThat(textService[textKey, I18nConfiguration.US]).isEqualTo("a")
    assertThat(textService[textKey, I18nConfiguration.Germany]).isEqualTo(textKey.key)

    provider1.setText(textKey, "b")
    assertThat(textService[textKey, I18nConfiguration.US]).isEqualTo("b")
    assertThat(textService[textKey, I18nConfiguration.Germany]).isEqualTo("b")

    textService.removeTextResolver(provider1)
    assertThat(textService[textKey, I18nConfiguration.US]).isEqualTo("a")
    assertThat(textService[textKey, I18nConfiguration.Germany]).isEqualTo(textKey.key)
  }

  @Test
  fun testEnum() {
    val textService = TextService()

    assertThat(textService[MyEnum.Option1, I18nConfiguration.Germany]).isEqualTo("Option1")
    assertThat(textService[MyEnum.Option2, I18nConfiguration.Germany]).isEqualTo("Option2")

    assertThat(textService[MyEnum.Option1, I18nConfiguration.Germany, "cat1"]).isEqualTo("Option1.cat1")
    assertThat(textService[MyEnum.Option2, I18nConfiguration.Germany, "cat2"]).isEqualTo("Option2.cat2")
  }
}

enum class MyEnum {
  Option1, Option2
}
