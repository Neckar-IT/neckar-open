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
import it.neckar.rest.openapi.dsl.OpenApiRouteBuilder
import it.neckar.rest.openapi.dsl.build

class SummaryResponseTest {
  @Test
  fun testEmpty() {
    val json = OpenApiJsonGenerator(documentationProvider = DocumentationProvider.None).generate(emptyList(), additionalTypes = emptyList(), metaData = OpenApiMetaData.default)

    assertThat(json).isJsonEqualTo(
      //language=JSON
      """
        {
          "openapi" : "3.1.0",
            "info" : {
            "title" : "REST API",
            "description" : "REST API",
            "version" : "1.0.0"
          },
          "servers" : [ ],
          "paths" : { },
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
            "schemas" : { }
          }
        }
      """.trimIndent()
    )
  }

  @Test
  fun testResponseDefaultComment() {
    val route = OpenApiRouteBuilder("/test", HttpMethod.Get, emptyList(), sourceInformation = null).apply {
      summary = "summary on route"
      responding<MyRestResponse> {
        noExamples()
      }
    }.build()


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
            "/test" : {
              "get" : {
                "summary" : "summary on route",
                "operationId" : "GetTest",
                "x-category" : "default",
                "parameters" : [ ],
                "responses" : {
                  "200" : {
                    "description" : "Responds with Good or Bad types",
                    "content" : {
                      "application/json" : {
                        "schema" : {
                          "$ref" : "#/components/schemas/SummaryResponseTest.MyRestResponse"
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
              "SummaryResponseTest.MyRestResponse" : {
                "type" : "object",
                "x-source-kotlin-type" : "it.neckar.rest.openapi.SummaryResponseTest.MyRestResponse",
                "oneOf" : [ {
                  "$ref" : "#/components/schemas/SummaryResponseTest.MyRestResponse.Failed"
                }, {
                  "$ref" : "#/components/schemas/SummaryResponseTest.MyRestResponse.Good.WithType"
                } ],
                "discriminator" : {
                  "propertyName" : "type",
                  "mapping" : {
                    "bad/Failed" : "#/components/schemas/SummaryResponseTest.MyRestResponse.Failed",
                    "good" : "#/components/schemas/SummaryResponseTest.MyRestResponse.Good.WithType"
                  }
                }
              },
              "SummaryResponseTest.MyRestResponse.Good.WithType" : {
                "type" : "object",
                "x-source-kotlin-type" : "it.neckar.rest.openapi.SummaryResponseTest.MyRestResponse.Good",
                "allOf" : [ {
                  "$ref" : "#/components/schemas/SummaryResponseTest.MyRestResponse.Good"
                } ],
                "properties" : {
                  "type" : {
                    "type" : "string",
                    "enum" : [ "good" ]
                  }
                },
                "required" : [ "type" ]
              },
              "SummaryResponseTest.MyRestResponse.Good" : {
                "type" : "object",
                "x-source-kotlin-type" : "it.neckar.rest.openapi.SummaryResponseTest.MyRestResponse.Good",
                "properties" : {
                  "data" : {
                    "type" : "string"
                  }
                },
                "required" : [ "data" ]
              },
              "SummaryResponseTest.MyRestResponse.Failed" : {
                "type" : "object",
                "x-source-kotlin-type" : "it.neckar.rest.openapi.SummaryResponseTest.MyRestResponse.Failed",
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

  @Test
  fun testRequestDefaultComment() {
    val route = OpenApiRouteBuilder("/test", HttpMethod.Put, emptyList(), null).apply {
      summary = "summary on route"
      requestBody<DaRequest> {
        noExamples()
      }
      response("test string") {
        body<String> {
        }
      }
    }.build()


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
            "/test" : {
              "put" : {
                "summary" : "summary on route",
                "operationId" : "PutTest",
                "x-category" : "default",
                "parameters" : [ ],
                "requestBody" : {
                  "required" : true,
                  "content" : {
                    "application/json" : {
                      "schema" : {
                        "$ref" : "#/components/schemas/SummaryResponseTest.DaRequest"
                      }
                    }
                  }
                },
                "responses" : {
                  "200" : {
                    "description" : "test string",
                    "content" : {
                      "text/plain" : {
                        "schema" : {
                          "type" : "string",
                          "x-source-kotlin-type" : "kotlin.String"
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
              "SummaryResponseTest.DaRequest" : {
                "type" : "object",
                "x-source-kotlin-type" : "it.neckar.rest.openapi.SummaryResponseTest.DaRequest",
                "properties" : {
                  "name" : {
                    "type" : "string"
                  },
                  "age" : {
                    "type" : "integer"
                  }
                },
                "required" : [ "name", "age" ]
              }
            }
          }
        }
      """.trimIndent()
    )
  }

  @Serializable
  data class DaRequest(
    val name: String,
    val age: Int,
  )

  /**
   * Response for
   */
  @Serializable
  sealed interface MyRestResponse : RestResponse<String> {

    @Serializable
    @SerialName("good")
    data class Good(override val data: String) : MyRestResponse, RestResponse.GoodResponse<String>

    @Serializable
    @SerialName("bad/Failed")
    data object Failed : MyRestResponse, RestResponse.BadResponse<String>

    companion object {
      /**
       * Helper method to create a success object - with the correct type
       */
      fun good(data: String): MyRestResponse = Good(data)

      /**
       * Helper method to create a failure object - with the correct type
       */
      fun bad(): MyRestResponse = Failed
    }
  }

}
