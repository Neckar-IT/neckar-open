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
import it.neckar.open.serialization.isJsonEqualTo
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import org.junit.jupiter.api.Test
import it.neckar.rest.openapi.documentation.DocumentationProvider
import it.neckar.rest.openapi.schema.SchemaDescriptor

/**
 *
 */
class DiscriminatedGenerationTest {
  @Test
  fun testBaseClass() {
    val json = buildJsonObject {
      OpenApiJsonGenerator(documentationProvider = DocumentationProvider.None)
        .buildSchemaType(SchemaDescriptor.of<FooInterface>() as SchemaDescriptor.ClassSchemaObject, registerCompositeType = {})
    }

    assertThat(json).isJsonEqualTo(
      //language=JSON
      $$"""
        {
          "DiscriminatedGenerationTest.FooInterface" : {
            "type" : "object",
            "x-source-kotlin-type" : "it.neckar.rest.openapi.DiscriminatedGenerationTest.FooInterface",
            "oneOf" : [ {
              "$ref" : "#/components/schemas/DiscriminatedGenerationTest.FooInterface.Foo.WithType"
            } ],
            "discriminator" : {
              "propertyName" : "type",
              "mapping" : {
                "Foo" : "#/components/schemas/DiscriminatedGenerationTest.FooInterface.Foo.WithType"
              }
            }
          }
        }
      """.trimIndent()
    )
  }

  @Test
  fun testSubTypeCalls() {
    val calledFor = mutableSetOf<SchemaDescriptor>()

    val json = buildJsonObject {
      OpenApiJsonGenerator(documentationProvider = DocumentationProvider.None)
        .buildSchemaType(SchemaDescriptor.of<FooInterface.Foo>() as SchemaDescriptor.DiscriminatedClassSchemaWrapper, registerCompositeType = { calledFor.add(it) })
    }

    //Only the composite type detected method is called twice!
    assertThat(json).isJsonEqualTo(
      //language=JSON
      $$"""{}""".trimIndent()
    )

    assertThat(calledFor).hasSize(2)
  }

  @Test
  fun testSubTypeBase() {
    val calledFor = mutableSetOf<SchemaDescriptor>()

    val json = buildJsonObject {
      val componentSchema = SchemaDescriptor.of<FooInterface.Foo>() as SchemaDescriptor.DiscriminatedClassSchemaWrapper

      OpenApiJsonGenerator(documentationProvider = DocumentationProvider.None)
        .buildSchemaType(componentSchema.baseSchema, registerCompositeType = { calledFor.add(it) })
    }

    //Only the composite type detected method is called twice!
    assertThat(json).isJsonEqualTo(
      //language=JSON
      $$"""
        {
          "DiscriminatedGenerationTest.FooInterface.Foo" : {
            "type" : "object",
            "x-source-kotlin-type" : "it.neckar.rest.openapi.DiscriminatedGenerationTest.FooInterface.Foo",
            "properties" : {
              "bar" : {
                "type" : "string"
              }
            },
            "required" : [ "bar" ]
          }
        }
      """.trimIndent()
    )

    assertThat(calledFor).hasSize(0)
  }

  @Test
  fun testSubTypeBaseWithType() {
    val calledFor = mutableSetOf<SchemaDescriptor>()

    val json = buildJsonObject {
      val componentSchema = SchemaDescriptor.of<FooInterface.Foo>() as SchemaDescriptor.DiscriminatedClassSchemaWrapper

      OpenApiJsonGenerator(documentationProvider = DocumentationProvider.None)
        .buildSchemaType(componentSchema.schemaWithDiscriminator, registerCompositeType = { calledFor.add(it) })
    }

    //Only the composite type detected method is called twice!
    assertThat(json).isJsonEqualTo(
      //language=JSON
      $$"""
        {
          "DiscriminatedGenerationTest.FooInterface.Foo.WithType" : {
            "type" : "object",
            "x-source-kotlin-type" : "it.neckar.rest.openapi.DiscriminatedGenerationTest.FooInterface.Foo",
            "allOf" : [ {
              "$ref" : "#/components/schemas/DiscriminatedGenerationTest.FooInterface.Foo"
            } ],
            "properties" : {
              "type" : {
                "type" : "string",
                "enum" : [ "Foo" ]
              }
            },
            "required" : [ "type" ]
          }
        }
      """.trimIndent()
    )

    assertThat(calledFor).hasSize(0)
  }

  @Test
  fun testBoth() {
    val calledFor = mutableSetOf<SchemaDescriptor>()

    val componentSchema = SchemaDescriptor.of<FooInterface>()
    val json = OpenApiJsonGenerator(documentationProvider = DocumentationProvider.None).generateComponentsSchemas(listOf(componentSchema))

    //Only the composite type detected method is called twice!
    assertThat(json).isJsonEqualTo(
      //language=JSON
      $$"""
        {
          "DiscriminatedGenerationTest.FooInterface" : {
            "type" : "object",
            "x-source-kotlin-type" : "it.neckar.rest.openapi.DiscriminatedGenerationTest.FooInterface",
            "oneOf" : [ {
              "$ref" : "#/components/schemas/DiscriminatedGenerationTest.FooInterface.Foo.WithType"
            } ],
            "discriminator" : {
              "propertyName" : "type",
              "mapping" : {
                "Foo" : "#/components/schemas/DiscriminatedGenerationTest.FooInterface.Foo.WithType"
              }
            }
          },
          "DiscriminatedGenerationTest.FooInterface.Foo.WithType" : {
            "type" : "object",
            "x-source-kotlin-type" : "it.neckar.rest.openapi.DiscriminatedGenerationTest.FooInterface.Foo",
            "allOf" : [ {
              "$ref" : "#/components/schemas/DiscriminatedGenerationTest.FooInterface.Foo"
            } ],
            "properties" : {
              "type" : {
                "type" : "string",
                "enum" : [ "Foo" ]
              }
            },
            "required" : [ "type" ]
          },
          "DiscriminatedGenerationTest.FooInterface.Foo" : {
            "type" : "object",
            "x-source-kotlin-type" : "it.neckar.rest.openapi.DiscriminatedGenerationTest.FooInterface.Foo",
            "properties" : {
              "bar" : {
                "type" : "string"
              }
            },
            "required" : [ "bar" ]
          }
        }
      """.trimIndent()
    )

    assertThat(calledFor).hasSize(0)
  }

  @Serializable
  private sealed interface FooInterface {

    @Serializable
    @SerialName("Foo")
    data class Foo(val bar: String) : FooInterface

  }
}
