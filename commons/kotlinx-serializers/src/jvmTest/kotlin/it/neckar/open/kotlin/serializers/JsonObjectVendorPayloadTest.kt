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
import it.neckar.open.serialization.roundTripBson
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import org.junit.jupiter.api.Test

/**
 * A report with three modelled fields and one area holding whatever the wallbox vendor sends.
 */
@Serializable
private data class ChargingReport(
  val reportId: String,
  val wallboxSerial: String,
  val vendorPayload: JsonObject = EmptyJsonObject,
)

/**
 * A part of the payload whose shape is known - decoded on demand.
 */
@Serializable
private data class Firmware(
  val version: String,
  val channels: List<String>,
  val signature: String?,
)

/**
 * A realistic payload: six levels deep, arrays of objects, empty containers, mixed number types.
 */
//language=JSON
private val vendorPayloadJson = """
  {
    "schemaVersion": 3,
    "device": {
      "manufacturer": "Acme",
      "model": "WB-11",
      "firmware": { "version": "2.1.0", "channels": ["stable", "beta"], "signature": null },
      "capabilities": { "phases": 3, "maxCurrentAmpere": 16, "rfid": true }
    },
    "sessions": [
      {
        "id": "s-1",
        "energyKwh": 11.04,
        "phases": [
          { "index": 1, "currentAmpere": 15.9 },
          { "index": 2, "currentAmpere": 16.1 }
        ],
        "errors": []
      },
      {
        "id": "s-2",
        "energyKwh": 0.4,
        "phases": [],
        "errors": [
          { "code": "E42", "details": { "raw": { "register": "0x1A", "value": 4711 } } }
        ]
      }
    ],
    "vendorSpecific": { "notes": "", "registers": { "0x1A": [1, 2, 3] } }
  }
""".trimIndent()

private val report = ChargingReport(
  reportId = "r-4711",
  wallboxSerial = "WB-11-0001",
  vendorPayload = parseJsonObject(vendorPayloadJson),
)

/**
 * The free-form area on a realistic document - the case the proof of concept is actually about.
 */
class JsonObjectVendorPayloadTest {
  @Test
  fun `the whole payload survives the JSON round trip`() {
    roundTrip(report) {
      //language=JSON
      """
        {
          "reportId": "r-4711",
          "wallboxSerial": "WB-11-0001",
          "vendorPayload": $vendorPayloadJson
        }
      """.trimIndent()
    }
  }

  @Test
  fun `known parts of the payload decode into typed classes`() {
    assertThat(report.vendorPayload.decodeAt<Firmware>(JsonPath("device", "firmware")))
      .isEqualTo(Firmware(version = "2.1.0", channels = listOf("stable", "beta"), signature = null))

    assertThat(report.vendorPayload.findAndDecodeAt<Firmware>(JsonPath("device", "bootloader"))).isNull()
  }

  @Test
  fun `unknown parts stay reachable by path`() {
    assertThat(report.vendorPayload.getAt(JsonPath("device", "capabilities", "phases"))).isEqualTo(JsonPrimitive(3))
    assertThat(report.vendorPayload.findAt(JsonPath("vendorSpecific", "notes"))).isEqualTo(JsonPrimitive(""))

    //A path walks objects only - an array is a leaf and is unwrapped by hand
    val sessions: JsonArray = report.vendorPayload.getAt(JsonPath("sessions")).jsonArray
    assertThat(sessions).hasSize(2)
    assertThat(sessions[1].jsonObject["errors"]?.jsonArray?.first()?.jsonObject?.get("code")).isEqualTo(JsonPrimitive("E42"))

    //Empty containers are values, not absences
    assertThat(report.vendorPayload.getAt(JsonPath("sessions")).jsonArray[0].jsonObject["errors"]).isEqualTo(JsonArray(emptyList()))
  }

  /**
   * Every measured value is fractional on purpose - an integral double would be damaged by BSON, see
   * `JsonBsonEncoderNumberTypeTest`.
   */
  @Test
  fun `the payload survives the BSON round trip`() {
    roundTripBson(report) { null }
  }

  @Test
  fun `an overlay merges into the payload`() {
    val merged: JsonObject = report.vendorPayload.merge(
      buildJsonObject {
        put("schemaVersion", 4)
        putJsonObject("receivedBy") { put("gateway", "gw-7") }
      }
    )

    assertThat(merged.getAt(JsonPath("schemaVersion"))).isEqualTo(JsonPrimitive(4))
    assertThat(merged.getAt(JsonPath("receivedBy", "gateway"))).isEqualTo(JsonPrimitive("gw-7"))
    //Untouched keys keep their value
    assertThat(merged.getAt(JsonPath("device", "model"))).isEqualTo(JsonPrimitive("WB-11"))
  }
}
