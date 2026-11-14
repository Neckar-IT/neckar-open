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
import it.neckar.rest.openapi.schema.hasSerializableSealedSuperType
import it.neckar.rest.openapi.schema.isDiscriminatedClassSchema

/**
 *
 */
class OpenApiSealedTypesGenerationTest {
  @Test
  fun testSingleSubclassBasic() {
    assertThat(FooBarInterface.FooImpl::class.hasSerializableSealedSuperType()).isTrue()

    assertThat(SchemaDescriptor.of<FooBarInterface.FooImpl>().isDiscriminatedClassSchema()).isTrue()

    val openApiJsonGenerator = OpenApiJsonGenerator(documentationProvider = DocumentationProvider.None)
    val json = buildJsonObject {
      openApiJsonGenerator.buildComponentsSchemas(
        listOf(
          SchemaDescriptor.of<FooBarInterface.FooImpl>(),
        )
      )
    }

    assertThat(json).isJsonEqualTo(
      //language=JSON
      $$"""
        {
          "OpenApiSealedTypesGenerationTest.FooBarInterface.FooImpl.WithType" : {
            "type" : "object",
            "x-source-kotlin-type" : "it.neckar.rest.openapi.OpenApiSealedTypesGenerationTest.FooBarInterface.FooImpl",
            "allOf" : [ {
              "$ref" : "#/components/schemas/OpenApiSealedTypesGenerationTest.FooBarInterface.FooImpl"
            } ],
            "properties" : {
              "type" : {
                "type" : "string",
                "enum" : [ "FooImpl" ]
              }
            },
            "required" : [ "type" ]
          },
          "OpenApiSealedTypesGenerationTest.FooBarInterface.FooImpl" : {
            "type" : "object",
            "x-source-kotlin-type" : "it.neckar.rest.openapi.OpenApiSealedTypesGenerationTest.FooBarInterface.FooImpl",
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
  }

  @Test
  fun testSingleSubclass() {
    assertThat(SchemaDescriptor.of<FooBarInterface.FooImpl>().isDiscriminatedClassSchema()).isTrue()

    val openApiJsonGenerator = OpenApiJsonGenerator(documentationProvider = DocumentationProvider.None)
    val json = openApiJsonGenerator.generateComponentsSchemas(
      listOf(
        SchemaDescriptor.of<FooBarInterface.FooImpl>(),
      )
    )

    assertThat(json).isJsonEqualTo(
      //language=JSON
      $$"""
        {
          "OpenApiSealedTypesGenerationTest.FooBarInterface.FooImpl.WithType" : {
            "type" : "object",
            "x-source-kotlin-type" : "it.neckar.rest.openapi.OpenApiSealedTypesGenerationTest.FooBarInterface.FooImpl",
            "allOf" : [ {
              "$ref" : "#/components/schemas/OpenApiSealedTypesGenerationTest.FooBarInterface.FooImpl"
            } ],
            "properties" : {
              "type" : {
                "type" : "string",
                "enum" : [ "FooImpl" ]
              }
            },
            "required" : [ "type" ]
          },
          "OpenApiSealedTypesGenerationTest.FooBarInterface.FooImpl" : {
            "type" : "object",
            "x-source-kotlin-type" : "it.neckar.rest.openapi.OpenApiSealedTypesGenerationTest.FooBarInterface.FooImpl",
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

  }

  @Test
  fun testGenerate() {
    val openApiJsonGenerator = OpenApiJsonGenerator(documentationProvider = DocumentationProvider.None)
    val json = openApiJsonGenerator.generateComponentsSchemas(
      listOf(
        SchemaDescriptor.of<FooBarInterface>(),
      )
    )

    assertThat(json).isJsonEqualTo(
      //language=JSON
      $$"""
        {
          "OpenApiSealedTypesGenerationTest.FooBarInterface" : {
            "type" : "object",
            "x-source-kotlin-type" : "it.neckar.rest.openapi.OpenApiSealedTypesGenerationTest.FooBarInterface",
            "oneOf" : [ {
              "$ref" : "#/components/schemas/OpenApiSealedTypesGenerationTest.FooBarInterface.BarImpl.WithType"
            }, {
              "$ref" : "#/components/schemas/OpenApiSealedTypesGenerationTest.FooBarInterface.FooImpl.WithType"
            } ],
            "discriminator" : {
              "propertyName" : "type",
              "mapping" : {
                "BarImpl" : "#/components/schemas/OpenApiSealedTypesGenerationTest.FooBarInterface.BarImpl.WithType",
                "FooImpl" : "#/components/schemas/OpenApiSealedTypesGenerationTest.FooBarInterface.FooImpl.WithType"
              }
            }
          },
          "OpenApiSealedTypesGenerationTest.FooBarInterface.FooImpl.WithType" : {
            "type" : "object",
            "x-source-kotlin-type" : "it.neckar.rest.openapi.OpenApiSealedTypesGenerationTest.FooBarInterface.FooImpl",
            "allOf" : [ {
              "$ref" : "#/components/schemas/OpenApiSealedTypesGenerationTest.FooBarInterface.FooImpl"
            } ],
            "properties" : {
              "type" : {
                "type" : "string",
                "enum" : [ "FooImpl" ]
              }
            },
            "required" : [ "type" ]
          },
          "OpenApiSealedTypesGenerationTest.FooBarInterface.FooImpl" : {
            "type" : "object",
            "x-source-kotlin-type" : "it.neckar.rest.openapi.OpenApiSealedTypesGenerationTest.FooBarInterface.FooImpl",
            "properties" : {
              "bar" : {
                "type" : "string"
              }
            },
            "required" : [ "bar" ]
          },
          "OpenApiSealedTypesGenerationTest.FooBarInterface.BarImpl.WithType" : {
            "type" : "object",
            "x-source-kotlin-type" : "it.neckar.rest.openapi.OpenApiSealedTypesGenerationTest.FooBarInterface.BarImpl",
            "allOf" : [ {
              "$ref" : "#/components/schemas/OpenApiSealedTypesGenerationTest.FooBarInterface.BarImpl"
            } ],
            "properties" : {
              "type" : {
                "type" : "string",
                "enum" : [ "BarImpl" ]
              }
            },
            "required" : [ "type" ]
          },
          "OpenApiSealedTypesGenerationTest.FooBarInterface.BarImpl" : {
            "type" : "object",
            "x-source-kotlin-type" : "it.neckar.rest.openapi.OpenApiSealedTypesGenerationTest.FooBarInterface.BarImpl",
            "properties" : {
              "foo" : {
                "type" : "integer"
              }
            },
            "required" : [ "foo" ]
          }
        }
      """.trimIndent()
    )
  }

  @Serializable
  private sealed interface FooBarInterface {
    @Serializable
    @SerialName("FooImpl")
    data class FooImpl(val bar: String) : FooBarInterface

    @Serializable
    @SerialName("BarImpl")
    data class BarImpl(val foo: Int) : FooBarInterface
  }
}
