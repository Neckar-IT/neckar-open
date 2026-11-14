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
import it.neckar.open.serialization.roundTripBson
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

/**
 * The free-form area stored as a nested BSON document - queryable in MongoDB.
 */
@Serializable
private data class StoredReading(
  val sensorId: String,
  val extra: JsonObject = EmptyJsonObject,
)

/**
 * What the free-form area does in BSON - the format MongoDB persistence uses in this repository.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class JsonObjectBsonTest {
  fun cases(): List<JsonObjectCases.Case> = JsonObjectCases.all

  @ParameterizedTest
  @MethodSource("cases")
  fun `the case survives the BSON round trip`(case: JsonObjectCases.Case) {
    roundTripBson(JsonObjectCases.Envelope(case.value)) { null }
  }

  @Test
  fun `the free form area becomes a nested BSON document`() {
    val reading = StoredReading(
      sensorId = "temp-01",
      extra = buildJsonObject {
        put("text", "7")
        put("count", 7)
        put("celsius", 21.5)
        put("valid", true)
        put("nothing", JsonNull)
        putJsonObject("vendor") { put("name", "Acme") }
        putJsonArray("samples") {
          add(1)
          add("two")
          addJsonObject { putJsonObject("deep") { put("x", "y") } }
        }
      },
    )

    roundTripBson(reading) {
      //language=JSON
      """
        {
          "sensorId": "temp-01",
          "extra": {
            "text": "7",
            "count": 7,
            "celsius": 21.5,
            "valid": true,
            "nothing": null,
            "vendor": { "name": "Acme" },
            "samples": [1, "two", { "deep": { "x": "y" } }]
          }
        }
      """.trimIndent()
    }
  }
}
