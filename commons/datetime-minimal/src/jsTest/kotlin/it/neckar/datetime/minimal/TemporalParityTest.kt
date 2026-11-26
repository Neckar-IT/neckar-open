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
package it.neckar.datetime.minimal

import assertk.*
import assertk.assertions.*
import it.neckar.open.time.formatUtcForDebug
import kotlin.random.Random
import kotlin.test.Test

/**
 * [LocalDate] equals `Temporal.PlainDate`; the offset with and without `Temporal` is the same.
 */
class TemporalParityTest {
  /** -271821-04-19, the first day of `Temporal.PlainDate`. */
  private val temporalMinEpochDay: Long = -100_000_001L
  private val temporalMaxEpochDay: Long = 100_000_000L

  @Test
  fun everyDayFrom1600To2400() {
    for (epochDay in LocalDate(1600, 1, 1).toEpochDay()..LocalDate(2400, 12, 31).toEpochDay()) {
      assertSameDay(epochDay)
    }
  }

  @Test
  fun randomDaysAcrossTheRangeOfTemporal() {
    val random = Random(6434)
    repeat(1_000_000) {
      assertSameDay(random.nextLong(temporalMinEpochDay, temporalMaxEpochDay + 1))
    }
    assertSameDay(temporalMinEpochDay)
    assertSameDay(temporalMaxEpochDay)
  }

  @Test
  fun interopRoundTrip() {
    val bastilleDay = LocalDate(1789, 7, 14)
    assertThat(LocalDate.fromTemporal(bastilleDay.toTemporal())).isEqualTo(bastilleDay)
    val dateTime = LocalDateTime.of(-43, 3, 15, 11, 30, 5, 123)
    assertThat(LocalDateTime.fromTemporal(dateTime.toTemporal())).isEqualTo(dateTime)
    assertFailure { LocalDate(-271821, 4, 18).toTemporal() }.isInstanceOf<IllegalArgumentException>()
    assertFailure { LocalDate(275760, 9, 14).toTemporal() }.isInstanceOf<IllegalArgumentException>()
    assertThat(LocalDate.fromTemporal(LocalDate(275760, 9, 13).toTemporal())).isEqualTo(LocalDate(275760, 9, 13))
  }

  @Test
  fun offsetWithoutTemporalEqualsTemporal() {
    assertThat(hasTemporal()).isTrue()
    val zones = listOf("Europe/Berlin", "America/New_York", "Australia/Lord_Howe", "Asia/Kolkata").map { TimeZone(it) }
    val start = LocalDate(1850, 1, 1).toMillisAtStartOfDay(TimeZone.UTC)
    val end = LocalDate(2031, 1, 1).toMillisAtStartOfDay(TimeZone.UTC)
    val step = 3 * TimeConstants.MillisPerDay + 7 * TimeConstants.MillisPerHour
    val provider = DefaultTimeZoneOffsetProvider()

    zones.forEach { timeZone ->
      val instants = generateSequence(start) { it + step }.takeWhile { it < end }.toList()
      val withTemporal = instants.map { provider.offset(it, timeZone) }
      val withoutTemporal = withoutTemporal { instants.map { provider.offset(it, timeZone) } }
      assertThat(withoutTemporal, timeZone.zoneId).isEqualTo(withTemporal)
    }
  }

  @Test
  fun offsetsAndTransitionsWithAndWithoutTemporal() {
    val expectations: () -> Unit = {
      val provider = DefaultTimeZoneOffsetProvider()
      assertThat(provider.offset(LocalDateTime.of(1789, 7, 14, 12, 0, 0).toMillis(offset = 0.0), TimeZone.Berlin)).isEqualTo(3_208_000.0)
      assertThat(provider.offset(LocalDateTime.of(1789, 7, 14, 12, 0, 0).toMillis(offset = 0.0), TimeZone.NewYork)).isEqualTo(-17_762_000.0)
      assertThat(provider.offset(LocalDateTime.of(2026, 3, 29, 12, 0, 0).toMillis(offset = 0.0), TimeZone.Berlin)).isEqualTo(7_200_000.0)
      assertThat(provider.offset(LocalDateTime.of(2026, 3, 29, 12, 0, 0).toMillis(offset = 0.0), TimeZone.NewYork)).isEqualTo(-14_400_000.0)
      assertThat(provider.offset(LocalDateTime.of(2026, 10, 25, 12, 0, 0).toMillis(offset = 0.0), TimeZone.Berlin)).isEqualTo(3_600_000.0)
      assertThat(provider.offset(LocalDateTime.of(2026, 10, 25, 12, 0, 0).toMillis(offset = 0.0), TimeZone.NewYork)).isEqualTo(-14_400_000.0)
    }
    expectations()
    withoutTemporal(expectations)
  }

  @Test
  fun gapAndOverlapWithAndWithoutTemporal() {
    val expectations: () -> Unit = {
      assertThat(LocalDateTime.of(2026, 3, 29, 2, 30, 0).toMillis(TimeZone.Berlin).formatUtcForDebug()).isEqualTo("2026-03-29T01:30:00.000Z")
      assertThat(LocalDateTime.of(2026, 10, 25, 2, 30, 0).toMillis(TimeZone.Berlin).formatUtcForDebug()).isEqualTo("2026-10-25T00:30:00.000Z")
    }
    withUncachedProvider {
      expectations()
      withoutTemporal(expectations)
    }
  }

  private fun assertSameDay(epochDay: Long) {
    val expected = temporalPlainDateOfEpochDay(epochDay.toDouble())
    val minimal = LocalDate.ofEpochDay(epochDay)
    assertThat(minimal).isEqualTo(LocalDate.fromTemporal(expected))
    assertThat(LocalDate.fromTemporal(expected).toEpochDay()).isEqualTo(epochDay)
  }
}

private fun temporalPlainDateOfEpochDay(epochDay: Double): Temporal.PlainDate = js("Temporal.PlainDate.from('1970-01-01').add({ days: epochDay })")

private fun hasTemporal(): Boolean = js("typeof globalThis.Temporal === 'object'")

/**
 * Runs [block] with `globalThis.Temporal` removed, like in a browser without `Temporal`.
 */
private fun <T> withoutTemporal(block: () -> T): T {
  val temporal: dynamic = js("globalThis.Temporal")
  js("delete globalThis.Temporal")
  try {
    check(hasTemporal().not())
    return block()
  } finally {
    js("globalThis.Temporal = temporal")
  }
}

/**
 * Bypasses the cache of [timeZoneOffsetProvider], which would return offsets computed with `Temporal`.
 */
private fun withUncachedProvider(block: () -> Unit) {
  val previous = timeZoneOffsetProvider
  timeZoneOffsetProvider = DefaultTimeZoneOffsetProvider().cached(cacheSize = 1)
  try {
    block()
  } finally {
    timeZoneOffsetProvider = previous
  }
}
