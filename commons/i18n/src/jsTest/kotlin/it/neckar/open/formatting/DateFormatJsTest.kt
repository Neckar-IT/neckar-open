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
import it.neckar.open.i18n.Locale
import it.neckar.datetime.minimal.TimeZone
import it.neckar.open.unit.si.ms
import kotlin.test.Test

class DateFormatJsTest {

  val time: @ms Double = 1.546855919123123E12

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
  fun basics() {
    assertThat(time.formatUtc()).isEqualTo("2019-01-07T10:11:59.123Z")
  }

  @Test
  fun testIsoFormat() {
    assertThat(dateTimeFormatIso8601.format(time, germanTokyo)).isEqualTo("2019-01-07T10:11:59.123Z") //no timezone
    assertThat(dateTimeFormatIso8601.format(time, englishTokyo)).isEqualTo("2019-01-07T10:11:59.123Z")
    assertThat(dateTimeFormatIso8601.format(time, englishNewYork)).isEqualTo("2019-01-07T10:11:59.123Z")
  }

  @Test
  fun testYear() {
    assertThat(yearFormat.format(time, germanTokyo)).isEqualTo("2019")
    assertThat(yearFormat.format(time, englishTokyo)).isEqualTo("2019")
    assertThat(yearFormat.format(time, englishNewYork)).isEqualTo("2019")
  }

  @Test
  fun testYearMonth() {
    assertThat(yearMonthFormat.format(time, germanTokyo)).isEqualTo("Januar 2019")
    assertThat(yearMonthFormat.format(time, englishTokyo)).isEqualTo("January 2019")
    assertThat(yearMonthFormat.format(time, englishNewYork)).isEqualTo("January 2019")
  }

  @Test
  fun testIsoDate() {
    assertThat(dateFormatIso8601.format(time, germanTokyo)).isEqualTo("2019-01-07")
    assertThat(dateFormatIso8601.format(time, englishTokyo)).isEqualTo("2019-01-07")
    assertThat(dateFormatIso8601.format(time, englishNewYork)).isEqualTo("2019-01-07")
  }

  @Test
  fun testIsoTime() {
    assertThat(timeFormatIso8601.format(time, germanTokyo)).isEqualTo("19:11:59.123")
    assertThat(timeFormatIso8601.format(time, englishTokyo)).isEqualTo("19:11:59.123")
    assertThat(timeFormatIso8601.format(time, englishNewYork)).isEqualTo("05:11:59.123")
  }

  @Test
  fun testTimeFormat() {
    assertThat(timeFormat.format(time, germanTokyo)).isEqualTo("19:11:59")
    assertThat(timeFormat.format(time, englishTokyo)).isEqualTo("7:11:59 PM")
    assertThat(timeFormat.format(time, englishNewYork)).isEqualTo("5:11:59 AM")

    assertThat(timeFormatWithMillis.format(time, germanTokyo)).isEqualTo("19:11:59.123")
    assertThat(timeFormatWithMillis.format(time, englishTokyo)).isEqualTo("7:11:59.123 PM")
    assertThat(timeFormatWithMillis.format(time, englishNewYork)).isEqualTo("5:11:59.123 AM")
  }

  @Test
  fun testDateFormat() {
    assertThat(dateFormat.format(time, germanTokyo)).isEqualTo("7.1.2019")
    //TODO fix me!
    //assertThat(dateFormat.format(time, englishTokyo)).isEqualTo("Jan 7, 2019")
    //assertThat(dateFormat.format(time, englishNewYork)).isEqualTo("Jan 7, 2019")
  }

  @Test
  fun testDateTime() {
    //TODO fix me!
    //assertThat(dateTimeFormat.format(time, germanTokyo)).isEqualTo("7.1.2019, 19:11:59")
    //assertThat(dateTimeFormat.format(time, englishTokyo)).isEqualTo("Jan 7, 2019 7:11:59 PM")
    //assertThat(dateTimeFormat.format(time, englishNewYork)).isEqualTo("Jan 7, 2019 5:11:59 AM")
    //
    //assertThat(dateTimeFormatWithMillis.format(time, germanTokyo)).isEqualTo("07.01.2019 19:11:59.123")
    //assertThat(dateTimeFormatWithMillis.format(time, englishTokyo)).isEqualTo("Jan 7, 2019 7:11:59.123 PM")
    //assertThat(dateTimeFormatWithMillis.format(time, englishNewYork)).isEqualTo("Jan 7, 2019 5:11:59.123 AM")
  }
}
