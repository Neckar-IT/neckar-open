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
package it.neckar.rest.openapi

import assertk.*
import assertk.assertions.*
import it.neckar.open.kotlin.serializers.serializer
import it.neckar.open.serialization.roundTrip
import it.neckar.open.serialization.isJsonEqualTo
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.EmptySerializersModule
import kotlinx.serialization.serializer
import org.junit.jupiter.api.Test
import kotlin.reflect.typeOf
import it.neckar.rest.openapi.documentation.DocumentationProvider
import it.neckar.rest.openapi.schema.SchemaDescriptor

/**
 *
 */
class OpenApiJsonSerializerWithGenericsTest {
  @Test
  fun testSerializableFooBar() {
    roundTrip(FooBar(42)) {
      //language=Json
      """
        {
          "value": 42
        }
      """.trimIndent()
    }
    roundTrip(FooBar("bar")) {
      //language=Json
      """
        {
          "value": "bar"
        }
      """.trimIndent()
    }
  }

  @Test
  fun testMyResponseSerialization() {
    roundTrip(
      MyResponse(
        FooBar("myValue")
      )
    ) {
      //language=Json
      """
        {
          "foo": {
            "value": "myValue"
          }
        }
      """.trimIndent()
    }
  }

  @Test
  fun testRef() {
    assertThat(
      OpenApiJsonGenerator(documentationProvider = DocumentationProvider.None).generateSchemaTypeRef(
        SchemaDescriptor(MyResponse::class),
        emptyList()
      )
    ).isJsonEqualTo(
      //language=JSON
      $$"""
        {
          "schema" : {
            "$ref" : "#/components/schemas/OpenApiJsonSerializerWithGenericsTest.MyResponse"
          }
        }
      """.trimIndent()
    )
  }

  @Test
  fun createSchemaType() {
    val type = typeOf<FooBar<String>>()
    assertThat(type.toString()).isEqualTo("it.neckar.rest.openapi.OpenApiJsonSerializerWithGenericsTest.FooBar<kotlin.String>")
    val schema = SchemaDescriptor.of(type)
    assertThat(schema).isInstanceOf(SchemaDescriptor.ClassSchemaObject::class)
    assertThat((schema as SchemaDescriptor.ClassSchemaObject).type).isEqualTo(type)
  }

  @Test
  fun testGenerateOpenApiFooBar() {
    val schemaDescriptor = SchemaDescriptor.of(typeOf<FooBar<String>>())
    assertThat(schemaDescriptor.toString()).isEqualTo("ClassSchemaObject(referenceName=OpenApiJsonSerializerWithGenericsTest.FooBar<String>)")

    assertThat(
      OpenApiJsonGenerator(documentationProvider = DocumentationProvider.None).generateComponentsSchemas(
        listOf(schemaDescriptor),
      )
    ).isJsonEqualTo(
      //language=JSON
      $$"""
        {
          "OpenApiJsonSerializerWithGenericsTest.FooBar__String__" : {
            "type" : "object",
            "x-source-kotlin-type" : "it.neckar.rest.openapi.OpenApiJsonSerializerWithGenericsTest.FooBar",
            "properties" : {
              "value" : {
                "type" : "string"
              }
            },
            "required" : [ "value" ]
          }
        }
      """.trimIndent()
    )
  }

  @Test
  fun testGenerateOpenApi() {
    val responseSchemaDescriptor = SchemaDescriptor(MyResponse::class)
    assertThat(responseSchemaDescriptor).isInstanceOf(SchemaDescriptor.ClassSchemaObject::class)
    assertThat(responseSchemaDescriptor.toString()).isEqualTo("ClassSchemaObject(referenceName=OpenApiJsonSerializerWithGenericsTest.MyResponse)")

    val fooSchemaDescriptor = SchemaDescriptor.of(typeOf<FooBar<String>>())
    assertThat(fooSchemaDescriptor.toString()).isEqualTo("ClassSchemaObject(referenceName=OpenApiJsonSerializerWithGenericsTest.FooBar<String>)")

    assertThat(
      OpenApiJsonGenerator(documentationProvider = DocumentationProvider.None).generateComponentsSchemas(
        listOf(responseSchemaDescriptor),
      )
    ).isJsonEqualTo(
      //language=JSON
      $$"""
        {
          "OpenApiJsonSerializerWithGenericsTest.MyResponse" : {
            "type" : "object",
            "x-source-kotlin-type" : "it.neckar.rest.openapi.OpenApiJsonSerializerWithGenericsTest.MyResponse",
            "properties" : {
              "foo" : {
                "$ref" : "#/components/schemas/OpenApiJsonSerializerWithGenericsTest.FooBar__String__"
              }
            },
            "required" : [ "foo" ]
          },
          "OpenApiJsonSerializerWithGenericsTest.FooBar__String__" : {
            "type" : "object",
            "x-source-kotlin-type" : "it.neckar.rest.openapi.OpenApiJsonSerializerWithGenericsTest.FooBar",
            "properties" : {
              "value" : {
                "type" : "string"
              }
            },
            "required" : [ "value" ]
          }
        }
      """.trimIndent()
    )
  }

  @Test
  fun testFindSerializerFromType() {
    val type = typeOf<FooBar<String>>()
    val serializer = EmptySerializersModule().serializer(type)

    assertThat(serializer).isNotNull()
  }

  @Test
  fun testSerializerStuff() {
    val type = typeOf<FooBar<String>>()
    val serializer = type.serializer()

    assertThat(serializer).isNotNull()
  }

  @Serializable
  private data class MyResponse(
    val foo: FooBar<String>
  )

  @Serializable
  private data class FooBar<T>(
    val value: T
  )
}
