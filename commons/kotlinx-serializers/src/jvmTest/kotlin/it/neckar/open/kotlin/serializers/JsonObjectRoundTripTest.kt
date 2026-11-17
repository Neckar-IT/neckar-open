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
import it.neckar.open.serialization.roundTrip
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

/**
 * The class at the centre of the proof of concept: strictly typed properties plus one free-form area
 * of arbitrary shape.
 */
@Serializable
private data class SensorReading(
  val sensorId: String,
  val celsius: Double,
  /**
   * The free-form area. Anything the caller wants to carry along - unknown at compile time, kept
   * verbatim through serialization and deserialization.
   */
  val extra: JsonObject = EmptyJsonObject,
)

/**
 * What an unconstrained [JsonObject] survives as the property of a strictly typed class: JSON in,
 * JSON out, byte for byte. Reaching into such an object is a different question and is answered by
 * [JsonObjectPathTest].
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class JsonObjectRoundTripTest {
  fun cases(): List<JsonObjectCases.Case> = JsonObjectCases.all

  @ParameterizedTest
  @MethodSource("cases")
  fun `the case survives the JSON round trip`(case: JsonObjectCases.Case) {
    roundTrip(JsonObjectCases.Envelope(case.value)) { null }
  }

  @ParameterizedTest
  @MethodSource("cases")
  fun `the case encodes to a stable string`(case: JsonObjectCases.Case) {
    val encoded: String = Json.encodeToString(JsonObjectCases.Envelope(case.value))
    val reEncoded: String = Json.encodeToString(Json.decodeFromString<JsonObjectCases.Envelope>(encoded))

    //Byte for byte - key order, number literals and escaping included
    assertThat(reEncoded).isEqualTo(encoded)
  }

  @Test
  fun `an empty free form area serializes as an empty object`() {
    roundTrip(SensorReading(sensorId = "temp-01", celsius = 21.5)) {
      //language=JSON
      """
        {
          "sensorId" : "temp-01",
          "celsius" : 21.5,
          "extra" : {}
        }
      """.trimIndent()
    }
  }

  @Test
  fun `the free form area keeps arbitrary nesting through a round trip`() {
    val reading = SensorReading(
      sensorId = "temp-01",
      celsius = 21.5,
      extra = buildJsonObject {
        put("collectedBy", "gateway-7")
        putJsonObject("vendor") {
          put("name", "Acme")
          put("firmware", "2.1.0")
          putJsonObject("build") {
            put("number", 4711)
            put("dirty", false)
            put("signedBy", JsonNull)
          }
        }
        putJsonArray("samples") {
          add(21.4)
          add(21.6)
          addJsonObject {
            put("value", 21.5)
            putJsonArray("flags") {
              add("interpolated")
              addJsonObject { put("reason", "gap") }
            }
          }
        }
      },
    )

    roundTrip(reading) {
      //language=JSON
      """
        {
          "sensorId" : "temp-01",
          "celsius" : 21.5,
          "extra" : {
            "collectedBy" : "gateway-7",
            "vendor" : {
              "name" : "Acme",
              "firmware" : "2.1.0",
              "build" : {
                "number" : 4711,
                "dirty" : false,
                "signedBy" : null
              }
            },
            "samples" : [
              21.4,
              21.6,
              {
                "value" : 21.5,
                "flags" : [
                  "interpolated",
                  { "reason" : "gap" }
                ]
              }
            ]
          }
        }
      """.trimIndent()
    }
  }

  @Test
  fun `a structure that no Kotlin class describes survives verbatim`() {
    //Nothing in the code knows these keys - they only exist in the incoming JSON
    //language=JSON
    val incoming = """
      {
        "sensorId": "temp-01",
        "celsius": 21.5,
        "extra": {
          "": "empty key",
          "ünïcode": ["ä", "ö", "ü"],
          "deep": {"a": {"b": {"c": {"d": {"e": [[[1]]]}}}}},
          "types": {"asString": "7", "asNumber": 7, "asBool": true, "asNull": null}
        }
      }
    """.trimIndent()

    val deserialized: SensorReading = Json.decodeFromString<SensorReading>(incoming)

    //Re-encoding produces the same document - no key is dropped, no type is coerced
    assertThat(Json.parseToJsonElement(Json.encodeToString(deserialized)))
      .isEqualTo(Json.parseToJsonElement(incoming))

    //The distinction between "7" and 7 is kept
    assertThat(deserialized.extra.getAt(JsonPath("types", "asString")).jsonPrimitive.isString).isTrue()
    assertThat(deserialized.extra.getAt(JsonPath("types", "asNumber")).jsonPrimitive.isString).isFalse()
  }

  @Test
  fun `arrays at the top level of a nested value are kept`() {
    val extra = JsonObject(mapOf("samples" to buildJsonArray { add(1); add(2); add(3) }))

    roundTrip(SensorReading(sensorId = "temp-01", celsius = 21.5, extra = extra)) {
      //language=JSON
      """
        {
          "sensorId" : "temp-01",
          "celsius" : 21.5,
          "extra" : {
            "samples" : [1, 2, 3]
          }
        }
      """.trimIndent()
    }
  }

}
