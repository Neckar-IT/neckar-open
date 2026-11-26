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
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import kotlin.random.Random
import it.neckar.datetime.minimal.LocalDate as MinimalLocalDate
import kotlinx.datetime.LocalDate as KotlinxLocalDate

/**
 * [MinimalLocalDate] and [KotlinxLocalDate] serialize to the same ISO 8601 string in the whole
 * range of `kotlinx-datetime`, so the type can change on the wire and in MongoDB without a migration.
 */
class LocalDateSerializationEquivalenceTest {

  @Test
  fun `minimal and kotlinx LocalDate serialize identically from year -999999999 to 999999999`() {
    listOf(
      Triple(2026, 5, 14),
      Triple(2026, 3, 4),
      Triple(1000, 1, 1),
      Triple(999, 1, 1),
      Triple(0, 1, 1),
      Triple(-43, 3, 15),
      Triple(9999, 12, 31),
      Triple(10_000, 1, 1),
      Triple(-999_999_999, 1, 1),
      Triple(999_999_999, 12, 31),
    ).forEach { (year, month, day) ->
      assertSameJson(year, month, day)
    }
  }

  @Test
  fun `minimal and kotlinx LocalDate serialize identically across the range of kotlinx`() {
    val random = Random(6434)
    repeat(100_000) {
      val year = random.nextInt(-999_999_999, 1_000_000_000)
      val month = random.nextInt(1, 13)
      val day = random.nextInt(1, Month(month).daysInMonth(Year(year)).value + 1)
      assertSameJson(year, month, day)
    }
  }

  @Test
  fun `a value written by one type deserializes into the other`() {
    val writtenByMinimal = Json.encodeToString(MinimalLocalDate.serializer(), MinimalLocalDate(-43, 3, 15))
    assertThat(Json.decodeFromString(KotlinxLocalDate.serializer(), writtenByMinimal)).isEqualTo(KotlinxLocalDate(-43, 3, 15))

    val writtenByKotlinx = Json.encodeToString(KotlinxLocalDate.serializer(), KotlinxLocalDate(12_345, 5, 14))
    assertThat(Json.decodeFromString(MinimalLocalDate.serializer(), writtenByKotlinx)).isEqualTo(MinimalLocalDate(12_345, 5, 14))
  }

  private fun assertSameJson(year: Int, month: Int, day: Int) {
    val minimalJson = Json.encodeToString(MinimalLocalDate.serializer(), MinimalLocalDate(year, month, day))
    val kotlinxJson = Json.encodeToString(KotlinxLocalDate.serializer(), KotlinxLocalDate(year, month, day))
    assertThat(minimalJson).isEqualTo(kotlinxJson)
    assertThat(Json.decodeFromString(MinimalLocalDate.serializer(), kotlinxJson)).isEqualTo(MinimalLocalDate(year, month, day))
  }
}
