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
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import org.junit.jupiter.api.Test

/**
 * The shape of one *known* part inside an otherwise unconstrained object - decoded on demand.
 */
@Serializable
private data class VendorInfo(
  val name: String,
  val firmware: String,
)

/**
 * Reaching into an object whose shape is not known at compile time: by path, and back out into a
 * typed class where a subtree does have a known shape.
 */
class JsonObjectPathTest {
  @Test
  fun `elements are addressed by path`() {
    val extra = buildJsonObject {
      putJsonObject("vendor") {
        putJsonObject("build") { put("number", 4711) }
      }
    }

    assertThat(extra.findAt(JsonPath("vendor", "build", "number"))).isEqualTo(JsonPrimitive(4711))
    assertThat(extra.findAt(JsonPath("vendor", "missing", "number"))).isNull()
    assertFailure { extra.findAt(JsonPath("vendor", "build", "number", "deeper")) }
      .isInstanceOf(IllegalArgumentException::class)
      .hasMessage("No element at path <vendor.build.number.deeper>: <vendor.build.number> holds a number, so <deeper> cannot be reached")
    assertFailure { buildJsonObject { put("vendor", JsonNull) }.findAt(JsonPath("vendor", "name")) }
      .hasMessage("No element at path <vendor.name>: <vendor> holds null, so <name> cannot be reached")

    assertFailure { extra.getAt(JsonPath("vendor", "missing")) }
      .messageContains("No element at path <vendor.missing>")
  }

  @Test
  fun `a known subtree can be decoded into a typed class`() {
    val extra = buildJsonObject {
      putJsonObject("vendor") {
        put("name", "Acme")
        put("firmware", "2.1.0")
      }
    }

    assertThat(extra.decodeAt<VendorInfo>(JsonPath("vendor"))).isEqualTo(VendorInfo(name = "Acme", firmware = "2.1.0"))
    assertThat(extra.findAndDecodeAt<VendorInfo>(JsonPath("manufacturer"))).isNull()
    assertFailure { extra.findAndDecodeAt<VendorInfo>(JsonPath("vendor", "name", "first")) }
      .isInstanceOf(IllegalArgumentException::class)
  }

  @Test
  fun `merging overwrites key by key`() {
    val base = buildJsonObject {
      put("keep", "original")
      putJsonObject("vendor") { put("name", "Acme") }
    }
    val overlay = buildJsonObject {
      putJsonObject("vendor") { put("firmware", "2.1.0") }
    }

    val merged: JsonObject = base.merge(overlay)

    assertThat(merged.findAt(JsonPath("keep"))).isEqualTo(JsonPrimitive("original"))
    //Shallow: the vendor object is replaced as a whole, "name" is gone
    assertThat(merged.getAt(JsonPath("vendor"))).isEqualTo(JsonObject(mapOf("firmware" to JsonPrimitive("2.1.0"))))
  }

  @Test
  fun `parse and Empty produce equal objects`() {
    assertThat(parseJsonObject("{}")).isEqualTo(EmptyJsonObject)
    assertThat(EmptyJsonObject.isEmpty()).isTrue()
    assertThat(parseJsonObject("""{"a": 1}""").isEmpty()).isFalse()
  }

  @Test
  fun `an array is not an object`() {
    assertFailure { parseJsonObject("""["a", "b"]""") }.isInstanceOf(IllegalArgumentException::class)
  }
}
