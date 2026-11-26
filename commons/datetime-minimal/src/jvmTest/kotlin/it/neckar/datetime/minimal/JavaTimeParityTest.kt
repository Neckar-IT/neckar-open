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

import assertk.assertThat
import assertk.assertions.isEqualTo
import org.junit.jupiter.api.Test
import java.time.LocalDateTime as JavaLocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.random.Random

/**
 * The calendar of [LocalDate] equals `java.time` in the whole range of `java.time`, ±999,999,999 years.
 */
class JavaTimeParityTest {
  private val javaMinEpochDay: Long = java.time.LocalDate.MIN.toEpochDay()
  private val javaMaxEpochDay: Long = java.time.LocalDate.MAX.toEpochDay()

  @Test
  fun everyDayFrom1600To2400() {
    val first = java.time.LocalDate.of(1600, 1, 1).toEpochDay()
    val last = java.time.LocalDate.of(2400, 12, 31).toEpochDay()
    for (epochDay in first..last) {
      assertSameDay(epochDay)
    }
  }

  @Test
  fun randomDaysAcrossTheRangeOfJavaTime() {
    val random = Random(6434)
    repeat(1_000_000) {
      assertSameDay(random.nextLong(javaMinEpochDay, javaMaxEpochDay + 1))
    }
    assertSameDay(javaMinEpochDay)
    assertSameDay(javaMaxEpochDay)
  }

  @Test
  fun plusMonthsAndPlusYears() {
    val random = Random(6434)
    repeat(100_000) {
      val start = java.time.LocalDate.ofEpochDay(random.nextLong(-1_000_000_000L, 1_000_000_000L))
      val months = random.nextLong(-1_000_000L, 1_000_000L)
      assertThat(start.toNeckarItLocalDate().plusMonths(months).toJava()).isEqualTo(start.plusMonths(months))
      assertThat(start.toNeckarItLocalDate().plusYears(months).toJava()).isEqualTo(start.plusYears(months))
    }
  }

  @Test
  fun transitionsAndOffsetsEqualZonedDateTime() {
    val zones = listOf("Europe/Berlin", "America/New_York", "Australia/Lord_Howe", "Asia/Kolkata")
    val random = Random(6434)
    zones.forEach { zoneId ->
      repeat(20_000) {
        val local = JavaLocalDateTime.of(random.nextInt(1850, 2031), random.nextInt(1, 13), random.nextInt(1, 29), random.nextInt(0, 24), random.nextInt(0, 60))
        val expected = ZonedDateTime.of(local, ZoneId.of(zoneId)).toInstant().toEpochMilli().toDouble()
        val minimal = LocalDateTime(local.toLocalDate().toNeckarItLocalDate(), local.toLocalTime().toNeckarItLocalTime())
        assertThat(minimal.toMillis(TimeZone(zoneId)), "$local $zoneId").isEqualTo(expected)
        assertThat(LocalDateTime.fromMillisCurrentTimeZone(expected, TimeZone(zoneId)).toMillis(TimeZone(zoneId)), "$local $zoneId").isEqualTo(expected)
      }
    }
  }

  private fun assertSameDay(epochDay: Long) {
    val expected = java.time.LocalDate.ofEpochDay(epochDay)
    val minimal = LocalDate.ofEpochDay(epochDay)
    assertThat(minimal.toJava()).isEqualTo(expected)
    assertThat(expected.toNeckarItLocalDate().toEpochDay()).isEqualTo(epochDay)
  }
}
