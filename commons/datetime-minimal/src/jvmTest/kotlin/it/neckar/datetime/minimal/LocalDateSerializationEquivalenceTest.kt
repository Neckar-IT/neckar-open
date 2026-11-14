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
import it.neckar.datetime.minimal.LocalDate as MinimalLocalDate
import kotlinx.datetime.LocalDate as KotlinxLocalDate

/**
 * Proves that the minimal [MinimalLocalDate] and [KotlinxLocalDate] serialize to the *same* ISO
 * `yyyy-MM-dd` string for all realistic calendar days. This is the basis for swapping the type on
 * the wire and in MongoDB storage without a data migration: a document/JSON written with one type
 * reads back identically as the other.
 *
 * Known boundary: minimal `format()` does not zero-pad the year while kotlinx pads it to four digits,
 * so the two diverge for years below 1000. All real-world dates are >= 1000, so production is
 * unaffected — the boundary is pinned by [`diverge for years below 1000`] so it can never regress
 * silently.
 */
class LocalDateSerializationEquivalenceTest {

  @Test
  fun `minimal and kotlinx LocalDate serialize identically for realistic dates`() {
    listOf(
      Triple(2026, 5, 14), // the deliveryDate migration case
      Triple(2026, 1, 1),
      Triple(2026, 12, 31),
      Triple(2026, 3, 4), // single-digit month and day -> zero padding must match
      Triple(1000, 1, 1), // lowest realistic year
      Triple(9999, 12, 31),
    ).forEach { (year, month, day) ->
      val minimalJson = Json.encodeToString(MinimalLocalDate.serializer(), MinimalLocalDate(year, month, day))
      val kotlinxJson = Json.encodeToString(KotlinxLocalDate.serializer(), KotlinxLocalDate(year, month, day))
      assertThat(minimalJson).isEqualTo(kotlinxJson)
    }
  }

  @Test
  fun `a value written by one type deserializes into the other`() {
    val writtenByMinimal = Json.encodeToString(MinimalLocalDate.serializer(), MinimalLocalDate(2026, 5, 14))
    assertThat(Json.decodeFromString(KotlinxLocalDate.serializer(), writtenByMinimal)).isEqualTo(KotlinxLocalDate(2026, 5, 14))

    val writtenByKotlinx = Json.encodeToString(KotlinxLocalDate.serializer(), KotlinxLocalDate(2026, 5, 14))
    assertThat(Json.decodeFromString(MinimalLocalDate.serializer(), writtenByKotlinx)).isEqualTo(MinimalLocalDate(2026, 5, 14))
  }

  @Test
  fun `minimal and kotlinx diverge for years below 1000 (documented boundary)`() {
    assertThat(Json.encodeToString(MinimalLocalDate.serializer(), MinimalLocalDate(999, 1, 1))).isEqualTo("\"999-01-01\"")
    assertThat(Json.encodeToString(KotlinxLocalDate.serializer(), KotlinxLocalDate(999, 1, 1))).isEqualTo("\"0999-01-01\"")
  }
}
