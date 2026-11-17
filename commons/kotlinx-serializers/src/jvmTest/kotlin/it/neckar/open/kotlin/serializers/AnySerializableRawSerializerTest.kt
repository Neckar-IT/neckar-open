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
import it.neckar.open.serialization.isJsonEqualTo
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import it.neckar.open.test.utils.isJsonEqualTo

/**
 *
 */
class AnySerializableRawSerializerTest {
  @Suppress("JSON_FORMAT_REDUNDANT")
  @Test
  fun testDefaults() {
    assertThat(Json {
    }.encodeToString<FooBarWithOptional>(FooBarWithOptional("foo", null))).isJsonEqualTo(
      //language=JSON
      """
        {
          "foo" : "foo",
          "bar" : null
        }
      """.trimIndent()
    )

    assertThat(Json {
      encodeDefaults = false
      explicitNulls = false
    }.encodeToString<FooBarWithOptional>(FooBarWithOptional("foo", null))).isJsonEqualTo(
      //language=JSON
      """
        {
          "foo" : "foo"
        }
      """.trimIndent()
    )

    assertThat(Json {
      encodeDefaults = true
      explicitNulls = true
    }.encodeToString<FooBarWithOptional>(FooBarWithOptional("foo", null))).isJsonEqualTo(
      //language=JSON
      """
        {
          "foo" : "foo",
          "bar" : null,
          "barOptional" : 7,
          "barOptionalNullable" : null
        }
      """.trimIndent()
    )

    assertThat(Json {
      encodeDefaults = false
      explicitNulls = true
    }.encodeToString<FooBarWithOptional>(FooBarWithOptional("foo", null))).isJsonEqualTo(
      //language=JSON
      """
        {
          "foo" : "foo",
          "bar" : null
        }
      """.trimIndent()
    )

    assertThat(Json {
      encodeDefaults = true
      explicitNulls = false
    }.encodeToString<FooBarWithOptional>(FooBarWithOptional("foo", null))).isJsonEqualTo(
      //language=JSON
      """
        {
          "foo" : "foo",
          "barOptional" : 7
        }
      """.trimIndent()
    )


    AnySerializableRawSerializer.encodeToJsonElement(FooBarWithOptional("foo", 42), includeOptionals = false).let { jsonElement ->
      assertThat(jsonElement).isJsonEqualTo(
        //language=JSON
        """
        {
          "foo" : "foo",
          "bar" : 42
        }
      """.trimIndent()
      )
    }

    AnySerializableRawSerializer.encodeToJsonElement(FooBarWithOptional("foo", 42), true).let { jsonElement ->
      assertThat(jsonElement).isJsonEqualTo(
        //language=JSON
        """
          {
            "foo" : "foo",
            "bar" : 42,
            "barOptional" : 7,
            "barOptionalNullable" : null
          }
        """.trimIndent()
      )
    }
  }

  @Test
  fun testEncodeObjectToString() {
    val json = Json.encodeToString(AnySerializableRawSerializer, FooBar("foo", 42))

    assertThat(json).isJsonEqualTo(
      //language=JSON
      """
        {
          "foo" : "foo",
          "bar" : 42
        }
      """.trimIndent()
    )
  }

  @Test
  fun testEncodeObjectToJsonElement() {
    val json = Json.encodeToJsonElement(AnySerializableRawSerializer, FooBar("foo", 42))

    assertThat(json).isJsonEqualTo(
      //language=JSON
      """
        {
          "foo" : "foo",
          "bar" : 42
        }
      """.trimIndent()
    )
  }

  @Test
  fun testPrimitives() {
    assertThat(Json.encodeToJsonElement(String.serializer(), "bar")).isJsonEqualTo(
      //language=JSON
      """
        "bar"
      """.trimIndent()
    )
  }

  @Test
  fun testPrimitivesAnySerializableRawSerializer() {
    assertThat(Json.encodeToJsonElement(AnySerializableRawSerializer, "bar")).isJsonEqualTo(
      //language=JSON
      """
        "bar"
      """.trimIndent()
    )
  }

  @Test
  fun testPrimitive() {
    Json.encodeToString(AnySerializableRawSerializer, "Hello, world").let { jsonElement ->
      assertThat(jsonElement).isJsonEqualTo(
        //language=JSON
        """
        "Hello, world"
      """.trimIndent()
      )
    }
  }

  @Serializable
  private data class FooBar(
    val foo: String,
    val bar: Int,
  )

  @Serializable
  private data class FooBarWithOptional(
    val foo: String,
    val bar: Int?,
    val barOptional: Int = 7,
    val barOptionalNullable: Int? = null,
  )
}
