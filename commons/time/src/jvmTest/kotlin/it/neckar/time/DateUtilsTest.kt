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
package it.neckar.time

import assertk.*
import assertk.assertions.*
import it.neckar.open.time.DateUtils
import it.neckar.open.time.DateUtils.formatDurationWords
import it.neckar.open.time.DurationMillisExample
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.util.DefaultLocale
import java.time.Duration
import java.util.Locale

/**
 */
@DefaultLocale("en-US")
class DateUtilsTest {
  @Test
  fun humanReadableDuration() {
    assertThat(formatDurationWords(1000)).isEqualTo("1 second")
    assertThat(formatDurationWords((1000 + 1000 * 61).toLong())).isEqualTo("1 minute 2 seconds")
  }

  @Test
  fun format() {
    assertThat(DateUtils.formatDurationHHmmSSmmm((1000 + 1000 * 61).toLong())).isEqualTo("00:01:02.000")
  }

  @Test
  fun durationWorldsNotFollowing() {
    assertThat(formatDurationWords(Duration.ofHours(1).plusMinutes(30))).isEqualTo("1 hour 30 minutes")
    assertThat(formatDurationWords(Duration.ofHours(1))).isEqualTo("1 hour")
  }

  @Test
  fun duration() {
    assertThat(DateUtils.formatDurationWordsWithSeconds((1000 + 1000 * 61).toLong())).isEqualTo("1min 02s")
    assertThat(DateUtils.formatDurationWordsWithSeconds(Duration.ofDays(3).toMillis())).isEqualTo("72h 00min 00s")
    assertThat(DateUtils.formatDurationWordsWithSeconds(Duration.ofDays(3).toMillis() + 1000)).isEqualTo("72h 00min 01s")
    assertThat(DateUtils.formatDurationWordsWithSeconds(Duration.ofDays(13).toMillis() + 1000)).isEqualTo("312h 00min 01s")
    assertThat(DateUtils.formatDurationWordsWithSeconds(Duration.ofMinutes(3).toMillis() + 1000)).isEqualTo("3min 01s")
    assertThat(DateUtils.formatDurationWordsWithSeconds(Duration.ofSeconds(1).toMillis())).isEqualTo("1s")
    assertThat(DateUtils.formatDurationWordsWithSeconds(Duration.ofMinutes(1).toMillis())).isEqualTo("1min 00s")
    assertThat(DateUtils.formatDurationWordsWithSeconds(Duration.ofHours(1).toMillis())).isEqualTo("1h 00min 00s")
    assertThat(DateUtils.formatDurationWordsWithSeconds(Duration.ofDays(1).toMillis())).isEqualTo("24h 00min 00s")
    Locale.setDefault(Locale.US)
    assertThat(DateUtils.formatDurationWordsWithSeconds(Duration.ofDays(100).toMillis())).isEqualTo("2,400h 00min 00s")
    Locale.setDefault(Locale.GERMAN)
    assertThat(DateUtils.formatDurationWordsWithSeconds(Duration.ofDays(100).toMillis())).isEqualTo("2.400h 00min 00s")
  }

  @Test
  fun formatHourMinute() {
    assertThat(DateUtils.formatDurationHHmm(Duration.ofHours(1).plusMinutes(10))).isEqualTo("01:10")
    assertThat(DateUtils.formatDurationHHmm(Duration.ofHours(18).plusMinutes(10))).isEqualTo("18:10")
    assertThat(DateUtils.formatDurationHHmm(Duration.ofHours(118).plusMinutes(10))).isEqualTo("118:10")
    val formatted = DateUtils.formatDurationHHmm(Duration.ofHours(118).plusMinutes(10))
    assertThat(DateUtils.parseDurationHHmm(formatted)).isEqualTo(Duration.ofHours(118).plusMinutes(10))
  }

  @Test
  fun testMOre() {
    assertThat(formatDurationWords(90061000L, Locale.US)).isEqualTo("1 day 1 hour 1 minute 1 second")
  }

  @Test
  fun `should remove zeros and handle plurals correctly`() {
    assertThat(formatDurationWords(3661000L, Locale.US)).isEqualTo("1 hour 1 minute 1 second")
  }

  @Test
  fun `should handle plurals correctly`() {
    assertThat(formatDurationWords(1000L, Locale.US)).isEqualTo("1 second")
  }

  @Test
  fun `should handle negative duration correctly`() {
    assertThat(formatDurationWords(-1000L, Locale.US)).isEqualTo("-1000 ms")
  }

  @Test
  fun `should handle zero duration correctly`() {
    assertThat(formatDurationWords(0L, Locale.US)).isEqualTo("0 seconds")
  }

  @Test
  fun `should return correct format for different durations`() {
    assertThat(formatDurationWords(3661000L, Locale.US)).isEqualTo("1 hour 1 minute 1 second")
    assertThat(formatDurationWords(61000L, Locale.US)).isEqualTo("1 minute 1 second")
    assertThat(formatDurationWords(1000L, Locale.US)).isEqualTo("1 second")
    assertThat(formatDurationWords(0L, Locale.US)).isEqualTo("0 seconds")
    assertThat(formatDurationWords(-1000L, Locale.US)).isEqualTo("-1000 ms")
    assertThat(formatDurationWords(86400000L, Locale.US)).isEqualTo("1 day")
    assertThat(formatDurationWords(3600000L, Locale.US)).isEqualTo("1 hour")
    assertThat(formatDurationWords(60000L, Locale.US)).isEqualTo("1 minute")

    assertThat(DurationMillisExample.MillisPerDay + DurationMillisExample.MillisPerHour + DurationMillisExample.MillisPerMinute + DurationMillisExample.MillisPerSecond).isEqualTo(9.0061E7)
    assertThat(formatDurationWords(90061000L, Locale.US)).isEqualTo("1 day 1 hour 1 minute 1 second")
    assertThat(formatDurationWords(86461000L, Locale.US)).isEqualTo("1 day 1 minute 1 second")
    assertThat(formatDurationWords(90061000L * 2, Locale.US)).isEqualTo("2 days 2 hours 2 minutes 2 seconds")

    assertThat(formatDurationWords(36061000L, Locale.US)).isEqualTo("10 hours 1 minute 1 second")
    assertThat(formatDurationWords(1201000L, Locale.US)).isEqualTo("20 minutes 1 second")
    assertThat(formatDurationWords(90061000L, Locale.US)).isEqualTo("1 day 1 hour 1 minute 1 second")
  }
}

