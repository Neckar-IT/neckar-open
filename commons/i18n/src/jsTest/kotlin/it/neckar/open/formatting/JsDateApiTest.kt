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
import it.neckar.open.i18n.Locale
import it.neckar.datetime.minimal.TimeZone
import kotlin.js.Date
import kotlin.test.Test

class JsDateApiTest {
  @Test
  fun testIt() {
    val timestamp = 12312313512323.0
    val date = Date(timestamp)

    assertThat(timestamp.formatUtc()).isEqualTo("2360-02-29T15:05:12.323Z")
    assertThat(date.toUTCString()).isEqualTo("Mon, 29 Feb 2360 15:05:12 GMT")

    //TODO!!! Attention does not work in other TimeZones
    if (false) {
      assertThat(date.getTimezoneOffset()).isEqualTo(-60)
    }

    assertThat(date.toLocaleDateString(Locale.US.locale, localeOptions(TimeZone.NewYork))).isEqualTo("2/29/2360")
    assertThat(date.toLocaleTimeString(Locale.US.locale, localeOptions(TimeZone.NewYork))).isEqualTo("10:05:12 AM")

    assertThat(date.toLocaleDateString(Locale.Germany.locale, localeOptions(TimeZone.NewYork))).isEqualTo("29.2.2360")
    assertThat(date.toLocaleTimeString(Locale.Germany.locale, localeOptions(TimeZone.NewYork))).isEqualTo("10:05:12")

    assertThat(date.toLocaleDateString(Locale.Germany.locale, localeOptions(TimeZone.Tokyo))).isEqualTo("1.3.2360")
    assertThat(date.toLocaleTimeString(Locale.Germany.locale, localeOptions(TimeZone.Tokyo))).isEqualTo("00:05:12")
  }
}
