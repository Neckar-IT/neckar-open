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

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.addJsonArray
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

/**
 * The cases both the JSON and the BSON round trip run through.
 */
object JsonObjectCases {
  /**
   * Carries a free-form area and nothing else.
   */
  @Serializable
  data class Envelope(val area: JsonObject = EmptyJsonObject)

  /**
   * One free-form area to round trip. Every case must come back unchanged - in every format.
   */
  data class Case(
    val description: String,
    val value: JsonObject,
  ) {
    override fun toString(): String = description
  }

  val all: List<Case> = listOf(
    Case("empty area", EmptyJsonObject),
    Case("null value", buildJsonObject { put("value", JsonNull) }),
    Case("empty string", buildJsonObject { put("value", "") }),
    Case("empty key", buildJsonObject { put("", "value") }),
    Case("empty nested object", buildJsonObject { putJsonObject("nested") { } }),
    Case("empty array", buildJsonObject { putJsonArray("items") { } }),
    Case(
      "array of primitives",
      buildJsonObject {
        putJsonArray("items") {
          add(1)
          add("two")
          add(true)
          add(JsonNull)
          add(2.5)
        }
      },
    ),
    Case(
      "array of arrays",
      buildJsonObject {
        putJsonArray("matrix") {
          addJsonArray {
            add(1)
            add(2)
          }
          addJsonArray { }
        }
      },
    ),
    Case(
      "array of objects",
      buildJsonObject {
        putJsonArray("items") {
          addJsonObject { put("a", 1) }
          addJsonObject { }
        }
      },
    ),
    Case("booleans", buildJsonObject { put("yes", true); put("no", false) }),
    Case(
      "int boundaries",
      buildJsonObject {
        put("zero", 0)
        put("negative", -42)
        put("max", Int.MAX_VALUE)
        put("min", Int.MIN_VALUE)
      },
    ),
    Case(
      "long boundaries",
      buildJsonObject {
        put("max", Long.MAX_VALUE)
        put("min", Long.MIN_VALUE)
        put("beyondDoublePrecision", 9007199254740993L)
      },
    ),
    Case(
      "doubles",
      buildJsonObject {
        put("half", 0.5)
        put("negative", -1.25)
        put("verySmall", 1.0E-10)
      },
    ),
    Case("number as string and as number", buildJsonObject { put("asString", "7"); put("asNumber", 7) }),
    Case("string with escapes", buildJsonObject { put("text", "line1\nline2\t\"quoted\" \\ backslash") }),
    Case("unicode", buildJsonObject { put("ünïcode", "äöü 😀") }),
    Case("dot in key", buildJsonObject { put("a.b", 1) }),
    Case("dollar prefixed key", buildJsonObject { put($$"$set", 1) }),
    Case("deeply nested", JsonObject(nested(depth = 20))),
    Case("many keys", JsonObject((1..50).associate { "key$it" to JsonPrimitive(it) })),
    Case("keys in non alphabetical order", buildJsonObject { put("b", 1); put("a", 2) }),
  )

  /**
   * An object nested [depth] levels deep, with the depth as the leaf value.
   */
  private fun nested(depth: Int): JsonObject {
    var current: JsonObject = buildJsonObject { put("leaf", depth) }

    repeat(depth) {
      val child = current
      current = buildJsonObject { put("level", child) }
    }

    return current
  }
}
