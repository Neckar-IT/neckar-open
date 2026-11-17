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
import it.neckar.open.unit.si.ms
import it.neckar.time.Millis
import it.neckar.time.io.MillisAsIsoDateTimeSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.serializer
import org.junit.jupiter.api.Test

class MillisAsIsoDateTimeSerializerTest {
  @Test
  fun testJson() {
    roundTrip(Millis(0.0), MillisAsIsoDateTimeSerializer) {
      """
        "1970-01-01T00:00:00.000Z"
      """.trimIndent()
    }
    roundTrip(Millis(631351531531.0), MillisAsIsoDateTimeSerializer) {
      """
        "1990-01-03T07:25:31.531Z"
      """.trimIndent()
    }
    roundTrip(Millis(-631351531531.0), MillisAsIsoDateTimeSerializer) {
      """
        "1949-12-29T16:34:28.469Z"
      """.trimIndent()
    }
  }

  @Test
  fun testClassNullable() {
    roundTrip(TestClass(Millis(351531351.0)), serializer()) {
      """
        {
          "date" : "1970-01-05T01:38:51.351Z"
        }
      """.trimIndent()
    }

    roundTrip(TestClass(null)) {
      """{"date":null}"""
    }
  }

  @Test
  fun testBson() {
    roundTripBson(TestClass(Millis(35351351531351.0)), serializer()) {
      //language=JSON
      $$"""
        {
          "date" : {
            "$date" : "3090-03-29T03:52:11.351Z"
          }
        }
      """.trimIndent()
    }

    roundTrip(TestClass(null)) {
      """{"date":null}"""
    }
  }

  @Serializable
  data class TestClass(val date: @ms Millis?)
}
