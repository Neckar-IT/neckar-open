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
import it.neckar.open.http.UrlParameterName
import it.neckar.open.test.utils.isJsonEqualTo
import it.neckar.rest.RestResponse
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.Test
import it.neckar.rest.openapi.documentation.DocumentationProvider
import it.neckar.rest.openapi.documentation.KotlinSourceInfo
import it.neckar.rest.openapi.dsl.OpenApiExample
import it.neckar.rest.openapi.dsl.OpenApiHeader
import it.neckar.rest.openapi.dsl.OpenApiRequest
import it.neckar.rest.openapi.dsl.OpenApiRequestParameter
import it.neckar.rest.openapi.dsl.OpenApiResponse
import it.neckar.rest.openapi.dsl.OpenApiRoute
import it.neckar.rest.openapi.dsl.OpenApiSimpleBody
import it.neckar.rest.openapi.schema.SchemaDescriptor

/**
 *
 */
class OpenApiRouteTest {
  @Test
  fun testIt() {
    val response = OpenApiResponse(
      statusCode = HttpStatusCode.OK,
      description = "The response",
      body = OpenApiSimpleBody(
        type = SchemaDescriptor.of(MyTestResponse::class),
        required = true,
        mediaType = ContentType.Application.Json,
        examples = emptyList(),
      ),
      headers = listOf(
        OpenApiHeader(
          name = "X-My-Header",
          type = SchemaDescriptor.String,
          description = "My header",
          examples = listOf(OpenApiExample(OpenApiExample.Id("the-example"), "daExample", originalType = String::class)),
        ),
      )
    )

    val openApiRoute = OpenApiRoute(
      path = "/test",
      method = HttpMethod.Get,
      summary = "Test Endpoint",
      description = "A very long description of the endpoint",

      request = OpenApiRequest(
        body = OpenApiSimpleBody(
          type = SchemaDescriptor.of(MyRequestBodyClass::class),
          mediaType = ContentType.Application.Json,
          required = true,
          examples = listOf(
            OpenApiExample(
              exampleId = OpenApiExample.Id("the-example"),
              example = MyRequestBodyClass(17, "daValueForData"),
              summary = "Example 1",
              description = "This is an example of the body",
              originalType = MyRequestBodyClass::class,
            ),
            OpenApiExample(
              exampleId = OpenApiExample.Id("the-other-example"),
              example = MyRequestBodyClass(18, "daValueForData2"),
              originalType = MyRequestBodyClass::class,
            ),
          ),
        ),

        parameters = listOf(
          OpenApiRequestParameter(
            name = UrlParameterName("userId"),
            location = OpenApiRequestParameter.Location.Query,
            type = SchemaDescriptor.String,
            required = true,
          ),
        ),
      ),
      responses = listOf(response),
      sourceInfo = KotlinSourceInfo("the file", 17, "theClass", "theMethod"),
    )

    val generated = OpenApiJsonGenerator(documentationProvider = DocumentationProvider.None).generate(
      routes = listOf(openApiRoute),
      metaData = OpenApiMetaData.default,
      additionalTypes = emptyList(),
    )

    assertThat(generated.toString()).isJsonEqualTo(
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
                "description" : "A very long description of the endpoint",
                "summary" : "Test Endpoint",
                "operationId" : "GetTest",
                "x-category" : "default",
                "x-source-location" : {
                  "className" : "theClass",
                  "methodName" : "theMethod",
                  "location" : "the file:17"
                },
                "parameters" : [ {
                  "name" : "userId",
                  "in" : "query",
                  "required" : true,
                  "schema" : {
                    "type" : "string",
                    "x-source-kotlin-type" : "kotlin.String"
                  }
                } ],
                "requestBody" : {
                  "required" : true,
                  "content" : {
                    "application/json" : {
                      "schema" : {
                        "$ref" : "#/components/schemas/MyRequestBodyClass"
                      },
                      "examples" : {
                        "the-example" : {
                          "summary" : "Example 1",
                          "description" : "This is an example of the body",
                          "x-source-kotlin-type" : "it.neckar.rest.openapi.MyRequestBodyClass",
                          "x-type-ref" : "#/components/schemas/MyRequestBodyClass",
                          "value" : {
                            "id" : 17,
                            "data" : "daValueForData"
                          }
                        },
                        "the-other-example" : {
                          "x-source-kotlin-type" : "it.neckar.rest.openapi.MyRequestBodyClass",
                          "x-type-ref" : "#/components/schemas/MyRequestBodyClass",
                          "value" : {
                            "id" : 18,
                            "data" : "daValueForData2"
                          }
                        }
                      }
                    }
                  }
                },
                "responses" : {
                  "200" : {
                    "description" : "The response",
                    "headers" : {
                      "X-My-Header" : {
                        "description" : "My header",
                        "schema" : {
                          "type" : "string",
                          "x-source-kotlin-type" : "kotlin.String"
                        },
                        "examples" : {
                          "the-example" : {
                            "value" : "daExample"
                          }
                        }
                      }
                    },
                    "content" : {
                      "application/json" : {
                        "schema" : {
                          "$ref" : "#/components/schemas/MyTestResponse"
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
              "MyRequestBodyClass" : {
                "type" : "object",
                "x-source-kotlin-type" : "it.neckar.rest.openapi.MyRequestBodyClass",
                "properties" : {
                  "id" : {
                    "type" : "integer"
                  },
                  "data" : {
                    "type" : "string"
                  }
                },
                "required" : [ "id", "data" ]
              },
              "MyTestResponse" : {
                "type" : "object",
                "x-source-kotlin-type" : "it.neckar.rest.openapi.MyTestResponse",
                "oneOf" : [ {
                  "$ref" : "#/components/schemas/MyTestResponse.Failed"
                }, {
                  "$ref" : "#/components/schemas/MyTestResponse.Good.WithType"
                } ],
                "discriminator" : {
                  "propertyName" : "type",
                  "mapping" : {
                    "bad/Failed" : "#/components/schemas/MyTestResponse.Failed",
                    "good" : "#/components/schemas/MyTestResponse.Good.WithType"
                  }
                }
              },
              "MyTestResponse.Good.WithType" : {
                "type" : "object",
                "x-source-kotlin-type" : "it.neckar.rest.openapi.MyTestResponse.Good",
                "allOf" : [ {
                  "$ref" : "#/components/schemas/MyTestResponse.Good"
                } ],
                "properties" : {
                  "type" : {
                    "type" : "string",
                    "enum" : [ "good" ]
                  }
                },
                "required" : [ "type" ]
              },
              "MyTestResponse.Good" : {
                "type" : "object",
                "x-source-kotlin-type" : "it.neckar.rest.openapi.MyTestResponse.Good",
                "properties" : {
                  "data" : {
                    "type" : "string"
                  }
                },
                "required" : [ "data" ]
              },
              "MyTestResponse.Failed" : {
                "type" : "object",
                "x-source-kotlin-type" : "it.neckar.rest.openapi.MyTestResponse.Failed",
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
}

@Serializable
private data class MyRequestBodyClass(
  val id: Int,
  val data: String,
)

/**
 *
 */
@Serializable
sealed interface MyTestResponse : RestResponse<String> {

  @Serializable
  @SerialName("good")
  data class Good(override val data: String) : MyTestResponse, RestResponse.GoodResponse<String>

  @Serializable
  @SerialName("bad/Failed")
  data object Failed : MyTestResponse, RestResponse.BadResponse<String>
}
