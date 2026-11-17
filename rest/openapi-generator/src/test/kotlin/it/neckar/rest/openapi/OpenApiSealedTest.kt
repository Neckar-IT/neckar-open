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
import io.ktor.http.*
import it.neckar.open.http.UrlPathSegments
import it.neckar.open.serialization.isJsonEqualTo
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.Test
import it.neckar.rest.openapi.documentation.DocumentationProvider
import it.neckar.rest.openapi.dsl.OpenApiRouteBuilder
import it.neckar.rest.openapi.schema.SchemaDescriptor
import it.neckar.rest.openapi.dsl.build

/**
 *
 */
class OpenApiSealedTest {
  @Test
  fun testSchemaType2() {
    // *NO* discriminator wrapper!!!!
    assertThat(SchemaDescriptor.of<FooInterface>()).isInstanceOf(SchemaDescriptor.ClassSchemaObject::class)

    SchemaDescriptor.of<FooInterface.Foo>().let {
      assertThat(it).isInstanceOf(SchemaDescriptor.DiscriminatedClassSchemaWrapper::class)
      it as SchemaDescriptor.DiscriminatedClassSchemaWrapper
      assertThat(it.baseSchema).isInstanceOf(SchemaDescriptor.ClassSchemaObject::class)
    }

    assertThat(SchemaDescriptor.of<FooInterface.Bar>()).isInstanceOf(SchemaDescriptor.DiscriminatedClassSchemaWrapper::class)
  }

  @Test
  fun testSchemaType() {
    assertThat(SchemaDescriptor(MyResponse::class)).isInstanceOf(SchemaDescriptor.ClassSchemaObject::class)

    SchemaDescriptor.of<FooInterface>().let {
      assertThat(it).isInstanceOf(SchemaDescriptor.ClassSchemaObject::class)
      it as SchemaDescriptor.ClassSchemaObject

      assertThat(it.getSimpleTypeName()).isEqualTo("OpenApiSealedTest.FooInterface")
    }
  }

  @Test
  fun testGenerateSchemaTypeRef() {
    val json = OpenApiJsonGenerator(documentationProvider = DocumentationProvider.None).generateSchemaTypeRef(
      SchemaDescriptor(MyResponse::class),
      emptyList()
    )

    assertThat(json).isJsonEqualTo(
      $$"""
        {
          "schema" : {
            "$ref" : "#/components/schemas/OpenApiSealedTest.MyResponse"
          }
        }
      """.trimIndent()
    )
  }

  @Test
  fun testGenerateOpenApi() {
    val json = OpenApiJsonGenerator(documentationProvider = DocumentationProvider.None).generateComponentsSchemas(
      listOf(
        SchemaDescriptor(MyResponse::class)
      )
    )

    assertThat(json).isJsonEqualTo(
      //language=JSON
      $$"""
        {
          "OpenApiSealedTest.MyResponse" : {
            "type" : "object",
            "x-source-kotlin-type" : "it.neckar.rest.openapi.OpenApiSealedTest.MyResponse",
            "properties" : {
              "foo" : {
                "$ref" : "#/components/schemas/OpenApiSealedTest.FooInterface"
              }
            },
            "required" : [ "foo" ]
          },
          "OpenApiSealedTest.FooInterface" : {
            "type" : "object",
            "x-source-kotlin-type" : "it.neckar.rest.openapi.OpenApiSealedTest.FooInterface",
            "oneOf" : [ {
              "$ref" : "#/components/schemas/OpenApiSealedTest.FooInterface.Bar.WithType"
            }, {
              "$ref" : "#/components/schemas/OpenApiSealedTest.FooInterface.Foo.WithType"
            } ],
            "discriminator" : {
              "propertyName" : "type",
              "mapping" : {
                "theBar" : "#/components/schemas/OpenApiSealedTest.FooInterface.Bar.WithType",
                "theFoo" : "#/components/schemas/OpenApiSealedTest.FooInterface.Foo.WithType"
              }
            }
          },
          "OpenApiSealedTest.FooInterface.Foo.WithType" : {
            "type" : "object",
            "x-source-kotlin-type" : "it.neckar.rest.openapi.OpenApiSealedTest.FooInterface.Foo",
            "allOf" : [ {
              "$ref" : "#/components/schemas/OpenApiSealedTest.FooInterface.Foo"
            } ],
            "properties" : {
              "type" : {
                "type" : "string",
                "enum" : [ "theFoo" ]
              }
            },
            "required" : [ "type" ]
          },
          "OpenApiSealedTest.FooInterface.Foo" : {
            "type" : "object",
            "x-source-kotlin-type" : "it.neckar.rest.openapi.OpenApiSealedTest.FooInterface.Foo",
            "properties" : {
              "s" : {
                "type" : "string"
              }
            },
            "required" : [ "s" ]
          },
          "OpenApiSealedTest.FooInterface.Bar.WithType" : {
            "type" : "object",
            "x-source-kotlin-type" : "it.neckar.rest.openapi.OpenApiSealedTest.FooInterface.Bar",
            "allOf" : [ {
              "$ref" : "#/components/schemas/OpenApiSealedTest.FooInterface.Bar"
            } ],
            "properties" : {
              "type" : {
                "type" : "string",
                "enum" : [ "theBar" ]
              }
            },
            "required" : [ "type" ]
          },
          "OpenApiSealedTest.FooInterface.Bar" : {
            "type" : "object",
            "x-source-kotlin-type" : "it.neckar.rest.openapi.OpenApiSealedTest.FooInterface.Bar",
            "properties" : {
              "i" : {
                "type" : "integer"
              }
            },
            "required" : [ "i" ]
          }
        }
      """.trimIndent()
    )
  }

  @Test
  fun testGenerateOpenApiRoute() {
    val json = OpenApiJsonGenerator(documentationProvider = DocumentationProvider.None).generate(
      listOf(
        OpenApiRouteBuilder(
          path = UrlPathSegments.parse("/my-path"),
          method = HttpMethod.Get,
          pathParameterNames = emptyList(),
          sourceInformation = null,
          category = OpenApiMetaData.Category.Default,
        ).apply {
          noRequestBody()
          response("MyResponse") {
            body<MyResponse> {
              noExamples()
            }
          }
        }.build()
      ),
      metaData = OpenApiMetaData.default,
    )

    assertThat(json).isJsonEqualTo(
      //language=JSON
      $$"""
        {
          "openapi" : "3.1.0",
          "info" : {
            "title" : "REST API",
            "description" : "REST API",
            "version" : "1.0.0"
          },
          "servers" : [ ],
          "paths" : {
            "/my-path" : {
              "get" : {
                "operationId" : "GetMyPath",
                "x-category" : "default",
                "parameters" : [ ],
                "responses" : {
                  "200" : {
                    "description" : "MyResponse",
                    "content" : {
                      "application/json" : {
                        "schema" : {
                          "$ref" : "#/components/schemas/OpenApiSealedTest.MyResponse"
                        }
                      }
                    }
                  }
                }
              }
            }
          },
          "security" : [ {
    "bearerAuth" : [ ]
  } ],
  "components" : {
            "securitySchemes" : {
              "bearerAuth" : {
                "type" : "http",
                "scheme" : "bearer",
                "bearerFormat" : "JWT"
              }
            },
            "schemas" : {
              "OpenApiSealedTest.MyResponse" : {
                "type" : "object",
                "x-source-kotlin-type" : "it.neckar.rest.openapi.OpenApiSealedTest.MyResponse",
                "properties" : {
                  "foo" : {
                    "$ref" : "#/components/schemas/OpenApiSealedTest.FooInterface"
                  }
                },
                "required" : [ "foo" ]
              },
              "OpenApiSealedTest.FooInterface" : {
                "type" : "object",
                "x-source-kotlin-type" : "it.neckar.rest.openapi.OpenApiSealedTest.FooInterface",
                "oneOf" : [ {
                  "$ref" : "#/components/schemas/OpenApiSealedTest.FooInterface.Bar.WithType"
                }, {
                  "$ref" : "#/components/schemas/OpenApiSealedTest.FooInterface.Foo.WithType"
                } ],
                "discriminator" : {
                  "propertyName" : "type",
                  "mapping" : {
                    "theBar" : "#/components/schemas/OpenApiSealedTest.FooInterface.Bar.WithType",
                    "theFoo" : "#/components/schemas/OpenApiSealedTest.FooInterface.Foo.WithType"
                  }
                }
              },
              "OpenApiSealedTest.FooInterface.Foo.WithType" : {
                "type" : "object",
                "x-source-kotlin-type" : "it.neckar.rest.openapi.OpenApiSealedTest.FooInterface.Foo",
                "allOf" : [ {
                  "$ref" : "#/components/schemas/OpenApiSealedTest.FooInterface.Foo"
                } ],
                "properties" : {
                  "type" : {
                    "type" : "string",
                    "enum" : [ "theFoo" ]
                  }
                },
                "required" : [ "type" ]
              },
              "OpenApiSealedTest.FooInterface.Foo" : {
                "type" : "object",
                "x-source-kotlin-type" : "it.neckar.rest.openapi.OpenApiSealedTest.FooInterface.Foo",
                "properties" : {
                  "s" : {
                    "type" : "string"
                  }
                },
                "required" : [ "s" ]
              },
              "OpenApiSealedTest.FooInterface.Bar.WithType" : {
                "type" : "object",
                "x-source-kotlin-type" : "it.neckar.rest.openapi.OpenApiSealedTest.FooInterface.Bar",
                "allOf" : [ {
                  "$ref" : "#/components/schemas/OpenApiSealedTest.FooInterface.Bar"
                } ],
                "properties" : {
                  "type" : {
                    "type" : "string",
                    "enum" : [ "theBar" ]
                  }
                },
                "required" : [ "type" ]
              },
              "OpenApiSealedTest.FooInterface.Bar" : {
                "type" : "object",
                "x-source-kotlin-type" : "it.neckar.rest.openapi.OpenApiSealedTest.FooInterface.Bar",
                "properties" : {
                  "i" : {
                    "type" : "integer"
                  }
                },
                "required" : [ "i" ]
              }
            }
          }
        }
      """.trimIndent()
    )
  }

  @Serializable
  private data class MyResponse(
    val foo: FooInterface
  )

  @Serializable
  private sealed interface FooInterface {
    @SerialName("theFoo")
    @Serializable
    data class Foo(val s: String) : FooInterface

    @SerialName("theBar")
    @Serializable
    data class Bar(val i: Int) : FooInterface
  }

}
