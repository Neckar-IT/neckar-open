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
import io.ktor.http.*
import it.neckar.open.serialization.isJsonEqualTo
import it.neckar.rest.RestResponse
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.Test
import it.neckar.rest.openapi.documentation.DocumentationProvider
import it.neckar.rest.openapi.schema.SchemaDescriptor
import it.neckar.rest.openapi.dsl.openApiRoute

/**
 *
 */
class OpenApiTypeRefTest {

  @Test
  fun testNoContentResponse() {
    val route = openApiRoute("/foobar", HttpMethod.Put, emptyList()) {
      request {
        body<Nothing>(SchemaDescriptor.EmptySchema) {
          noExamples()
        }
      }
      response("The Response Body") {
        body<FooBarResponse> {
          noExamples()
        }
      }
    }


    val json = OpenApiJsonGenerator(documentationProvider = DocumentationProvider.None).generate(listOf(route), additionalTypes = emptyList(), metaData = OpenApiMetaData.default)

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
            "/foobar" : {
              "put" : {
                "operationId" : "PutFoobar",
                "x-category" : "default",
                "parameters" : [ ],
                "responses" : {
                  "200" : {
                    "description" : "The Response Body",
                    "content" : {
                      "application/json" : {
                        "schema" : {
                          "$ref" : "#/components/schemas/OpenApiTypeRefTest.FooBarResponse"
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
              "OpenApiTypeRefTest.FooBarResponse" : {
                "type" : "object",
                "x-source-kotlin-type" : "it.neckar.rest.openapi.OpenApiTypeRefTest.FooBarResponse",
                "oneOf" : [ {
                  "$ref" : "#/components/schemas/OpenApiTypeRefTest.FooBarResponse.Failed"
                }, {
                  "$ref" : "#/components/schemas/OpenApiTypeRefTest.FooBarResponse.Good.WithType"
                } ],
                "discriminator" : {
                  "propertyName" : "type",
                  "mapping" : {
                    "bad/Failed" : "#/components/schemas/OpenApiTypeRefTest.FooBarResponse.Failed",
                    "good" : "#/components/schemas/OpenApiTypeRefTest.FooBarResponse.Good.WithType"
                  }
                }
              },
              "OpenApiTypeRefTest.FooBarResponse.Good.WithType" : {
                "type" : "object",
                "x-source-kotlin-type" : "it.neckar.rest.openapi.OpenApiTypeRefTest.FooBarResponse.Good",
                "allOf" : [ {
                  "$ref" : "#/components/schemas/OpenApiTypeRefTest.FooBarResponse.Good"
                } ],
                "properties" : {
                  "type" : {
                    "type" : "string",
                    "enum" : [ "good" ]
                  }
                },
                "required" : [ "type" ]
              },
              "OpenApiTypeRefTest.FooBarResponse.Good" : {
                "type" : "object",
                "x-source-kotlin-type" : "it.neckar.rest.openapi.OpenApiTypeRefTest.FooBarResponse.Good",
                "properties" : {
                  "data" : {
                    "$ref" : "#/components/schemas/OpenApiTypeRefTest.FooBar"
                  }
                },
                "required" : [ "data" ]
              },
              "OpenApiTypeRefTest.FooBar" : {
                "type" : "object",
                "x-source-kotlin-type" : "it.neckar.rest.openapi.OpenApiTypeRefTest.FooBar",
                "properties" : {
                  "name" : {
                    "type" : "string"
                  },
                  "value" : {
                    "type" : "integer"
                  }
                },
                "required" : [ "name", "value" ]
              },
              "OpenApiTypeRefTest.FooBarResponse.Failed" : {
                "type" : "object",
                "x-source-kotlin-type" : "it.neckar.rest.openapi.OpenApiTypeRefTest.FooBarResponse.Failed",
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

      /**
       * Creates a success method for non-null results, failure for null results
       */
      fun createFor(data: FooBar?): FooBarResponse {
        return if (data != null) {
          good(data)
        } else {
          bad()
        }
      }
    }
  }

  @Serializable
  private data class FooBar(val name: String, val value: Int)
}
