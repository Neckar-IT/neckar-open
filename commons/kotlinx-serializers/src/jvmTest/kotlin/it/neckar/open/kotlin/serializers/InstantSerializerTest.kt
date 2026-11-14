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

import it.neckar.open.serialization.roundTrip
import it.neckar.open.serialization.roundTripBson
import it.neckar.open.time.TimeInstantExample
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.Test
import kotlin.time.Instant

/**
 *
 */
class InstantSerializerTest {
  @Test
  fun testJsonDefault() {
    val date = TimeInstantExample._2026_09_30_17_38_12_123Z

    roundTrip(
      Data(
        instantDefault = date,
        instant = date,
      )
    ) {
      //language=JSON
      """
        {
          "instantDefault" : "2026-09-30T17:38:12Z",
          "instant" : "2026-09-30T17:38:12Z"
        }
      """.trimIndent()
    }
  }

  /**
   * This test shows that the *default* serializer does not use BSON DateTime, but a string representation
   */
  @Test
  fun testBsonDefault() {
    val date = TimeInstantExample._2026_09_30_17_38_12_123Z

    roundTripBson(
      Data(
        instantDefault = date,
        instant = date,
      )
    ) {
      //language=JSON
      $$"""
        {
          "instantDefault" : "2026-09-30T17:38:12Z",
          "instant" : {
            "$date" : "2026-09-30T17:38:12Z"
          }
        }
      """.trimIndent()
    }
  }

  @Serializable
  private data class Data(
    val instantDefault: Instant,

    @Serializable(with = InstantSerializer::class)
    val instant: Instant,
  )
}
