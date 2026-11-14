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
package it.neckar.open.kotlin.serializers

import assertk.*
import assertk.assertions.*
import it.neckar.open.test.utils.JsonUtils
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.Period

/**
 */
internal class JavaDateTimeSerializerTest {
  @Test
  internal fun testLocalDate() {
    val obj = LocalDate.of(2021, 5, 4)
    val json = Json.encodeToString(LocalDateSerializer, obj)

    JsonUtils.assertJsonEquals("\"2021-05-04\"", json)

    val parsed = Json.decodeFromString(LocalDateSerializer, json)
    assertThat(parsed).isEqualTo(obj)
  }

  @Test
  internal fun testLocalTime() {
    val obj = LocalTime.of(14, 43, 5, 709_000_000)
    val json = Json.encodeToString(LocalTimeSerializer, obj)

    JsonUtils.assertJsonEquals("\"14:43:05.709\"", json)

    val parsed = Json.decodeFromString(LocalTimeSerializer, json)
    assertThat(parsed).isEqualTo(obj)
  }

  @Test
  internal fun testDuration() {
    val obj = Duration.ofMinutes(77)
    val json = Json.encodeToString(JavaTimeDurationSerializer, obj)

    JsonUtils.assertJsonEquals("\"PT1H17M\"", json)

    val parsed = Json.decodeFromString(JavaTimeDurationSerializer, json)
    assertThat(parsed).isEqualTo(obj)
  }

  @Test
  internal fun testPeriod() {
    val obj = Period.ofDays(44)
    val json = Json.encodeToString(PeriodSerializer, obj)

    JsonUtils.assertJsonEquals("\"P44D\"", json)

    val parsed = Json.decodeFromString(PeriodSerializer, json)
    assertThat(parsed).isEqualTo(obj)
  }
}
