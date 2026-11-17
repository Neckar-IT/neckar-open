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
import it.neckar.rest.RestResponse
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import org.junit.jupiter.api.Test
import it.neckar.rest.openapi.documentation.DocumentationProvider
import it.neckar.rest.openapi.schema.SchemaDescriptor

/**
 *
 */
class OpenApiRestResponseSchemaTest {
  @Test
  fun testResponseComponentSchemaRef() {
    val json = buildJsonObject {
      val generator = OpenApiJsonGenerator(documentationProvider = DocumentationProvider.None)

      with(this) {
        OpenApiJsonGenerator(documentationProvider = DocumentationProvider.None).putSchemaTypeOrReference(SchemaDescriptor.of(FooResponse::class))
      }
    }

    assertThat(json).isJsonEqualTo(
      //language=JSON
      $$"""
          {
            "schema" : {
              "$ref" : "#/components/schemas/OpenApiRestResponseSchemaTest.FooResponse"
            }
          }
      """.trimIndent()
    )
  }

  @Test
  fun testFailure() {
    val schemaDescriptor = SchemaDescriptor.of(FooResponse.Failed::class)
    assertThat(schemaDescriptor).isInstanceOf(SchemaDescriptor.ComponentSchema::class)
    assertThat(schemaDescriptor).isInstanceOf(SchemaDescriptor.ConstSchemaObject::class)

    schemaDescriptor as SchemaDescriptor.ConstSchemaObject

    val json = buildJsonObject {
      OpenApiJsonGenerator(documentationProvider = DocumentationProvider.None).buildSchemaTypeForObject(FooResponse.Failed::class, RequiredPropertiesMode.OnlyDiscriminator) {
        // Do nothing
      }
    }

    assertThat(json).isJsonEqualTo(
      //language=JSON
      $$"""
        {
          "type" : "object",
          "x-source-kotlin-type" : "it.neckar.rest.openapi.OpenApiRestResponseSchemaTest.FooResponse.Failed",
          "required" : [ "type" ],
          "properties" : {
            "type" : {
              "type" : "string",
              "enum" : [ "bad/Failed" ]
            }
          },
          "x-ignored-properties" : [ "isBad", "isGood" ]
        }
      """.trimIndent()
    )
  }

  @Test
  fun testFailure2() {
    val schemaDescriptor = SchemaDescriptor.of(FooResponse.Failed::class)
    assertThat(schemaDescriptor).isInstanceOf(SchemaDescriptor.ComponentSchema::class)
    assertThat(schemaDescriptor).isInstanceOf(SchemaDescriptor.ConstSchemaObject::class)

    schemaDescriptor as SchemaDescriptor.ConstSchemaObject

    val generator = OpenApiJsonGenerator(documentationProvider = DocumentationProvider.None)

    val json = buildJsonObject {
      generator.buildSchemaType(schemaDescriptor as SchemaDescriptor.ComponentSchema) {
        // Do nothing
      }
    }

    assertThat(json).isJsonEqualTo(
      //language=JSON
      $$"""
        {
          "OpenApiRestResponseSchemaTest.FooResponse.Failed" : {
            "type" : "object",
            "x-source-kotlin-type" : "it.neckar.rest.openapi.OpenApiRestResponseSchemaTest.FooResponse.Failed",
            "required" : [ "type" ],
            "properties" : {
              "type" : {
                "type" : "string",
                "enum" : [ "bad/Failed" ]
              }
            },
            "x-ignored-properties" : [ "isBad", "isGood" ]
          }
        }
      """.trimIndent()
    )
  }

  @Test
  fun testResponseComponentSchema() {
    val generator = OpenApiJsonGenerator(documentationProvider = DocumentationProvider.None)
    val json = generator.generateComponentsSchemas(listOf(SchemaDescriptor.of(FooResponse::class)))

    assertThat(json).isJsonEqualTo(
      //language=JSON
      $$"""
        {
          "OpenApiRestResponseSchemaTest.FooResponse" : {
            "type" : "object",
            "x-source-kotlin-type" : "it.neckar.rest.openapi.OpenApiRestResponseSchemaTest.FooResponse",
            "oneOf" : [ {
              "$ref" : "#/components/schemas/OpenApiRestResponseSchemaTest.FooResponse.Failed"
            }, {
              "$ref" : "#/components/schemas/OpenApiRestResponseSchemaTest.FooResponse.Good.WithType"
            } ],
            "discriminator" : {
              "propertyName" : "type",
              "mapping" : {
                "bad/Failed" : "#/components/schemas/OpenApiRestResponseSchemaTest.FooResponse.Failed",
                "good" : "#/components/schemas/OpenApiRestResponseSchemaTest.FooResponse.Good.WithType"
              }
            }
          },
          "OpenApiRestResponseSchemaTest.FooResponse.Good.WithType" : {
            "type" : "object",
            "x-source-kotlin-type" : "it.neckar.rest.openapi.OpenApiRestResponseSchemaTest.FooResponse.Good",
            "allOf" : [ {
              "$ref" : "#/components/schemas/OpenApiRestResponseSchemaTest.FooResponse.Good"
            } ],
            "properties" : {
              "type" : {
                "type" : "string",
                "enum" : [ "good" ]
              }
            },
            "required" : [ "type" ]
          },
          "OpenApiRestResponseSchemaTest.FooResponse.Good" : {
            "type" : "object",
            "x-source-kotlin-type" : "it.neckar.rest.openapi.OpenApiRestResponseSchemaTest.FooResponse.Good",
            "properties" : {
              "data" : {
                "$ref" : "#/components/schemas/OpenApiRestResponseSchemaTest.Foo"
              }
            },
            "required" : [ "data" ]
          },
          "OpenApiRestResponseSchemaTest.Foo" : {
            "type" : "object",
            "x-source-kotlin-type" : "it.neckar.rest.openapi.OpenApiRestResponseSchemaTest.Foo",
            "properties" : {
              "value" : {
                "type" : "integer"
              }
            },
            "required" : [ "value" ]
          },
          "OpenApiRestResponseSchemaTest.FooResponse.Failed" : {
            "type" : "object",
            "x-source-kotlin-type" : "it.neckar.rest.openapi.OpenApiRestResponseSchemaTest.FooResponse.Failed",
            "required" : [ "type" ],
            "properties" : {
              "type" : {
                "type" : "string",
                "enum" : [ "bad/Failed" ]
              }
            },
            "x-ignored-properties" : [ "isBad", "isGood" ]
          }
        }
      """.trimIndent()
    )
  }

  @Test
  fun testBuildComponentsSchemasForResponse() {
    val generator = OpenApiJsonGenerator(documentationProvider = DocumentationProvider.None)

    with(generator) {
      val json = buildJsonObject {
        buildComponentsSchemas(listOf(SchemaDescriptor.of(FooResponse::class)))
      }

      assertThat(json).isJsonEqualTo(
        //language=JSON
        $$"""
          {
            "OpenApiRestResponseSchemaTest.FooResponse" : {
              "type" : "object",
              "x-source-kotlin-type" : "it.neckar.rest.openapi.OpenApiRestResponseSchemaTest.FooResponse",
              "oneOf" : [ {
                "$ref" : "#/components/schemas/OpenApiRestResponseSchemaTest.FooResponse.Failed"
              }, {
                "$ref" : "#/components/schemas/OpenApiRestResponseSchemaTest.FooResponse.Good.WithType"
              } ],
              "discriminator" : {
                "propertyName" : "type",
                "mapping" : {
                  "bad/Failed" : "#/components/schemas/OpenApiRestResponseSchemaTest.FooResponse.Failed",
                  "good" : "#/components/schemas/OpenApiRestResponseSchemaTest.FooResponse.Good.WithType"
                }
              }
            },
            "OpenApiRestResponseSchemaTest.FooResponse.Good.WithType" : {
              "type" : "object",
              "x-source-kotlin-type" : "it.neckar.rest.openapi.OpenApiRestResponseSchemaTest.FooResponse.Good",
              "allOf" : [ {
                "$ref" : "#/components/schemas/OpenApiRestResponseSchemaTest.FooResponse.Good"
              } ],
              "properties" : {
                "type" : {
                  "type" : "string",
                  "enum" : [ "good" ]
                }
              },
              "required" : [ "type" ]
            },
            "OpenApiRestResponseSchemaTest.FooResponse.Good" : {
              "type" : "object",
              "x-source-kotlin-type" : "it.neckar.rest.openapi.OpenApiRestResponseSchemaTest.FooResponse.Good",
              "properties" : {
                "data" : {
                  "$ref" : "#/components/schemas/OpenApiRestResponseSchemaTest.Foo"
                }
              },
              "required" : [ "data" ]
            },
            "OpenApiRestResponseSchemaTest.Foo" : {
              "type" : "object",
              "x-source-kotlin-type" : "it.neckar.rest.openapi.OpenApiRestResponseSchemaTest.Foo",
              "properties" : {
                "value" : {
                  "type" : "integer"
                }
              },
              "required" : [ "value" ]
            },
            "OpenApiRestResponseSchemaTest.FooResponse.Failed" : {
              "type" : "object",
              "x-source-kotlin-type" : "it.neckar.rest.openapi.OpenApiRestResponseSchemaTest.FooResponse.Failed",
              "required" : [ "type" ],
              "properties" : {
                "type" : {
                  "type" : "string",
                  "enum" : [ "bad/Failed" ]
                }
              },
              "x-ignored-properties" : [ "isBad", "isGood" ]
            }
          }
        """.trimIndent()
      )
    }
  }

  @Serializable
  private data class Foo(val value: Int)

  /**
   * Response for
   */
  @Serializable
  @SerialName("FooResponse")
  private sealed interface FooResponse : RestResponse<Foo> {

    @Serializable
    @SerialName("good")
    data class Good(override val data: Foo) : FooResponse, RestResponse.GoodResponse<Foo>

    @Serializable
    @SerialName("bad/Failed")
    data object Failed : FooResponse, RestResponse.BadResponse<Foo>
  }

}
