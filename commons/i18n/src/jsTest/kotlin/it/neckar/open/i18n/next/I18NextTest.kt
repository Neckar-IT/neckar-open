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
package it.neckar.open.i18n.next

import assertk.*
import assertk.assertions.*
import it.neckar.open.i18n.next.backend.http.I18nextHttpBackend
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import kotlin.js.json
import kotlin.test.Ignore
import kotlin.test.Test

/**
 *
 */
@Ignore
class I18NextTest {
  @Test
  fun testModules() {
    assertThat(i18next).isNotNull()
    assertThat(I18nextHttpBackend).isNotNull()
  }

  @Test
  fun testHttpBackend() = runTest {
    i18next
      .use(I18nextHttpBackend)
      .init(
        json(
          "backend" to json(
            "loadPath" to "/i18next/{{lng}}/{{ns}}.json"
          ),
          "lng" to "en",
          "fallbackLng" to "en",
          "defaultNS" to "translation",
          "ns" to arrayOf("i18next-translation-test"),
        )
      )
      .await()

    assertThat(i18next.t("foobar")).isEqualTo("foobar") //not translated!
    //TODO continue with translation(?)
  }

  @Test
  fun testNext() = runTest {
    val init = i18next.init()
    assertThat(init).isNotNull()
    init.await()
    assertThat(i18next.t("foobar")).isEqualTo("foobar")
  }

  @Test
  fun testInitialize() = runTest {
    i18next.init(
      json(
        "resources" to json(
          "en" to json(
            "translation" to json(
              "key" to "Hello World"
            )
          )
        ),
        "lng" to "en",
        "fallbackLng" to "en"
      )
    ).await()

    assertThat(i18next.t("key")).isEqualTo("Hello World")
  }
}
