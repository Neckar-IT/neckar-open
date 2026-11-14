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
package it.neckar.open.serialization

import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotNull
import assertk.assertions.messageContains
import it.neckar.open.kotlin.serializers.JsonPath
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import org.junit.jupiter.api.Test
import org.opentest4j.AssertionFailedError

class AssertSerializationExtTest {

  // isJsonEqualTo on JsonElement

  @Test
  fun testIsJsonEqualToOnJsonElement() {
    val element: JsonElement = buildJsonObject {
      put("a", 1)
      put("b", "x")
    }

    assertThat(element).isJsonEqualTo(
      //language=JSON
      """
        { "a": 1, "b": "x" }
      """.trimIndent()
    )
  }

  @Test
  fun testIsJsonEqualToOnJsonElementMismatch() {
    val element: JsonElement = buildJsonObject { put("a", 1) }

    assertFailure {
      assertThat(element).isJsonEqualTo(
        //language=JSON
        """{ "a": 2 }"""
      )
    }.isInstanceOf(AssertionFailedError::class)
  }

  // asString

  @Test
  fun testAsString() {
    val element: JsonElement = JsonPrimitive("hello")
    assertThat(element).asString().isEqualTo("hello")
  }

  @Test
  fun testAsStringFromNumberPrimitive() {
    // JsonPrimitive.content always returns the textual form — explicit type checks happen elsewhere.
    val element: JsonElement = JsonPrimitive(42)
    assertThat(element).asString().isEqualTo("42")
  }

  @Test
  fun testAsStringRejectsObject() {
    val element: JsonElement = buildJsonObject { put("a", 1) }
    assertFailure {
      assertThat(element).asString()
    }.messageContains("Expected JsonPrimitive")
  }

  @Test
  fun testAsStringRejectsJsonNull() {
    val element: JsonElement = JsonNull
    assertFailure {
      assertThat(element).asString()
    }.messageContains("JsonNull")
  }

  // asInt

  @Test
  fun testAsInt() {
    val element: JsonElement = JsonPrimitive(42)
    assertThat(element).asInt().isEqualTo(42)
  }

  @Test
  fun testAsIntRejectsNonNumeric() {
    val element: JsonElement = JsonPrimitive("not a number")
    assertFailure {
      assertThat(element).asInt()
    }.messageContains("Int")
  }

  // asBoolean

  @Test
  fun testAsBoolean() {
    val element: JsonElement = JsonPrimitive(true)
    assertThat(element).asBoolean().isEqualTo(true)
  }

  @Test
  fun testAsBooleanRejectsNonBoolean() {
    val element: JsonElement = JsonPrimitive("yes")
    assertFailure {
      assertThat(element).asBoolean()
    }.messageContains("Boolean")
  }

  // requiredKeys

  @Test
  fun testRequiredKeys() {
    val schema: JsonObject = buildJsonObject {
      put("type", "object")
      putJsonArray("required") {
        add("id")
        add("name")
      }
    }

    assertThat(schema).requiredKeys().containsExactly("id", "name")
  }

  @Test
  fun testRequiredKeysAbsentReturnsEmpty() {
    val schema: JsonObject = buildJsonObject {
      put("type", "object")
    }

    assertThat(schema).requiredKeys().isEmpty()
  }

  @Test
  fun testRequiredKeysWrongShapeFails() {
    val schema: JsonObject = buildJsonObject {
      put("required", "not-an-array")
    }

    assertFailure {
      assertThat(schema).requiredKeys()
    }.messageContains("JsonArray")
  }

  // refs

  @Test
  fun testRefsExtractsAllRefValues() {
    val allOf: JsonArray = buildJsonArray {
      addJsonObject { put("\$ref", "#/components/schemas/A") }
      addJsonObject { put("\$ref", "#/components/schemas/B") }
    }

    assertThat(allOf).refs().containsExactly(
      "#/components/schemas/A",
      "#/components/schemas/B",
    )
  }

  @Test
  fun testRefsSkipsInlineSchemas() {
    // OpenAPI allOf may contain inline schema objects alongside $refs — refs() returns only the $refs.
    val allOf: JsonArray = buildJsonArray {
      addJsonObject { put("\$ref", "#/components/schemas/Base") }
      addJsonObject {
        put("type", "object")
        putJsonObject("properties") { put("name", buildJsonObject { put("type", "string") }) }
      }
    }

    assertThat(allOf).refs().containsExactly("#/components/schemas/Base")
  }

  @Test
  fun testRefsFailsOnAnElementThatIsNoObject() {
    val allOf: JsonArray = buildJsonArray { add("#/components/schemas/A") }

    assertFailure { assertThat(allOf).refs().isEmpty() }
      .messageContains("Expected JsonObject as schema in the array but was a string")
  }

  @Test
  fun testRefsFailsOnARefThatIsNoPrimitive() {
    val allOf: JsonArray = buildJsonArray { addJsonObject { putJsonObject("\$ref") { put("type", "string") } } }

    assertFailure { assertThat(allOf).refs().isEmpty() }
      .messageContains("Expected a string as '\$ref' but was an object")
  }

  @Test
  fun testRefsOnEmptyArray() {
    assertThat(JsonArray(emptyList())).refs().isEmpty()
  }

  // path (existing — sanity check)

  @Test
  fun testPathTraversesNestedObjects() {
    val doc: JsonObject = buildJsonObject {
      putJsonObject("a") {
        putJsonObject("b") {
          put("found", true)
        }
      }
    }

    assertThat(doc).path("a", "b").asJsonObjectContainsKey("found")
  }

  private fun assertk.Assert<JsonObject>.asJsonObjectContainsKey(key: String) =
    transform("keys") { it.keys }.contains(key)

  @Test
  fun testPathFailsOnMissingKeyWithDiagnosis() {
    val doc: JsonObject = buildJsonObject {
      putJsonObject("a") {
        put("here", 1)
      }
    }

    assertFailure {
      assertThat(doc).path("a", "missing")
    }.messageContains("missing")
  }

  // atPath (value-extracting)

  @Test
  fun testAtPathReturnsLeafObject() {
    val doc: JsonObject = buildJsonObject {
      putJsonObject("a") {
        putJsonObject("b") {
          put("leaf", "value")
        }
      }
    }

    val leaf = doc.atPath(JsonPath("a", "b"))
    assertThat(leaf).contains("leaf" to JsonPrimitive("value"))
  }

  @Test
  fun testAtPathFailsWithFullDiagnosisOnMissingKey() {
    val doc: JsonObject = buildJsonObject {
      putJsonObject("a") {
        put("here", 1)
      }
    }

    val failure = runCatching { doc.atPath(JsonPath("a", "missing", "deeper")) }.exceptionOrNull()
    assertThat(failure).isNotNull().messageContains("Missing key \"missing\" at path [\"a\"]")
    assertThat(failure!!).messageContains("here")  // available keys reported
  }

  @Test
  fun testAtPathFailsWhenIntermediateIsNotObject() {
    val doc: JsonObject = buildJsonObject {
      put("a", "not-an-object")
    }

    val failure = runCatching { doc.atPath(JsonPath("a", "b")) }.exceptionOrNull()
    assertThat(failure).isNotNull().messageContains("Expected JsonObject")
  }
}
