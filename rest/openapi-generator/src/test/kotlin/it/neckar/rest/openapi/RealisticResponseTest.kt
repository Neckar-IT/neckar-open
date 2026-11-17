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
import it.neckar.open.serialization.isOpenApiEqualTo
import it.neckar.rest.RestResponse
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.Test
import kotlin.reflect.full.starProjectedType
import it.neckar.rest.openapi.documentation.DocumentationProvider
import it.neckar.rest.openapi.dsl.OpenApiRouteBuilder
import it.neckar.rest.openapi.schema.SchemaDescriptor
import it.neckar.rest.openapi.dsl.build

/**
 *
 */
class RealisticResponseTest {

  @Test
  fun testRealisticResponse() {
    val openApiRouteBuilder = OpenApiRouteBuilder(
      path = "/daPath", method = HttpMethod.Get, pathParameterNames = emptyList(), sourceInformation = null,
    ).apply {
      response("The response") {
        body<FooBarResponse> {
          description = "The response body"
          example("good", FooBarResponse.good(FooBar("foo", 42, null)), "Returns a foo value for 42")
          example("bad", FooBarResponse.bad(), "Example for a failure")
        }
      }
    }


    val route = openApiRouteBuilder.build()

    OpenApiJsonGenerator(documentationProvider = DocumentationProvider.None).collectTypesToGenerate(listOf(route)).let { typesToGenerate ->
      assertThat(typesToGenerate).hasSize(1)
      assertThat(typesToGenerate).containsExactly(SchemaDescriptor.of(FooBarResponse::class.starProjectedType))
    }


    val json = OpenApiJsonGenerator(documentationProvider = DocumentationProvider.None).generate(listOf(route), additionalTypes = emptyList(), metaData = OpenApiMetaData.default)

    assertThat(json).isOpenApiEqualTo(
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
            "/daPath" : {
              "get" : {
                "operationId" : "GetDaPath",
                "x-category" : "default",
                "parameters" : [ ],
                "responses" : {
                  "200" : {
                    "description" : "The response",
                    "content" : {
                      "application/json" : {
                        "schema" : {
                          "$ref" : "#/components/schemas/RealisticResponseTest.FooBarResponse"
                        },
                        "examples" : {
                          "good" : {
                            "summary" : "Returns a foo value for 42",
                            "x-source-kotlin-type" : "it.neckar.rest.openapi.RealisticResponseTest.FooBarResponse.Good",
                            "x-type-ref" : "#/components/schemas/RealisticResponseTest.FooBarResponse.Good.WithType",
                            "value" : {
                              "data" : {
                                "foo" : "foo",
                                "bar" : 42,
                                "bazNullable" : null,
                                "bazOption" : null
                              },
                              "type" : "good"
                            }
                          },
                          "bad" : {
                            "summary" : "Example for a failure",
                            "x-source-kotlin-type" : "it.neckar.rest.openapi.RealisticResponseTest.FooBarResponse.Failed",
                            "x-type-ref" : "#/components/schemas/RealisticResponseTest.FooBarResponse.Failed",
                            "value" : {
                              "type" : "bad/Failed"
                            }
                          }
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
              "RealisticResponseTest.FooBarResponse" : {
                "type" : "object",
                "x-source-kotlin-type" : "it.neckar.rest.openapi.RealisticResponseTest.FooBarResponse",
                "oneOf" : [ {
                  "$ref" : "#/components/schemas/RealisticResponseTest.FooBarResponse.Failed"
                }, {
                  "$ref" : "#/components/schemas/RealisticResponseTest.FooBarResponse.Good.WithType"
                } ],
                "discriminator" : {
                  "propertyName" : "type",
                  "mapping" : {
                    "bad/Failed" : "#/components/schemas/RealisticResponseTest.FooBarResponse.Failed",
                    "good" : "#/components/schemas/RealisticResponseTest.FooBarResponse.Good.WithType"
                  }
                }
              },
              "RealisticResponseTest.FooBarResponse.Good.WithType" : {
                "type" : "object",
                "x-source-kotlin-type" : "it.neckar.rest.openapi.RealisticResponseTest.FooBarResponse.Good",
                "allOf" : [ {
                  "$ref" : "#/components/schemas/RealisticResponseTest.FooBarResponse.Good"
                } ],
                "properties" : {
                  "type" : {
                    "type" : "string",
                    "enum" : [ "good" ]
                  }
                },
                "required" : [ "type" ]
              },
              "RealisticResponseTest.FooBarResponse.Good" : {
                "type" : "object",
                "x-source-kotlin-type" : "it.neckar.rest.openapi.RealisticResponseTest.FooBarResponse.Good",
                "properties" : {
                  "data" : {
                    "$ref" : "#/components/schemas/RealisticResponseTest.FooBar"
                  }
                },
                "required" : [ "data" ]
              },
              "RealisticResponseTest.FooBar" : {
                "type" : "object",
                "x-source-kotlin-type" : "it.neckar.rest.openapi.RealisticResponseTest.FooBar",
                "properties" : {
                  "foo" : {
                    "type" : "string"
                  },
                  "bar" : {
                    "type" : "integer"
                  },
                  "bazNullable" : {
                    "type" : [ "boolean", "null" ]
                  },
                  "bazOption" : {
                    "type" : [ "boolean", "null" ]
                  }
                },
                "required" : [ "foo", "bar", "bazNullable" ]
              },
              "RealisticResponseTest.FooBarResponse.Failed" : {
                "type" : "object",
                "x-source-kotlin-type" : "it.neckar.rest.openapi.RealisticResponseTest.FooBarResponse.Failed",
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
          }
        }
      """.trimIndent()
    )
  }

  /**
   * Response for
   */
  @Serializable
  private sealed interface FooBarResponse : RestResponse<FooBar> {

    @Serializable
    @SerialName("good")
    data class Good(override val data: FooBar) : FooBarResponse, RestResponse.GoodResponse<FooBar>

    @Serializable
    @SerialName("bad/Failed")
    data object Failed : FooBarResponse, RestResponse.BadResponse<FooBar>

    companion object {
      /**
       * Helper method to create a success object - with the correct type
       */
      fun good(data: FooBar): FooBarResponse = Good(data)

      /**
       * Helper method to create a failure object - with the correct type
       */
      fun bad(): FooBarResponse = Failed
    }
  }

  @Serializable
  private data class FooBar(
    val foo: String,
    val bar: Int,
    val bazNullable: Boolean?,
    val bazOption: Boolean? = null,
  )
}




