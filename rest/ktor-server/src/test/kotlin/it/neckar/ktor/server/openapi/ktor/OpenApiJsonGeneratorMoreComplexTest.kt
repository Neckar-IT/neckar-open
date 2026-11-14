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
package it.neckar.ktor.server.openapi.ktor

import assertk.*
import assertk.assertions.*
import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import it.neckar.open.http.UrlParameterName
import it.neckar.open.kotlin.serializers.AnySerializableRawSerializer
import it.neckar.open.kotlin.serializers.toStringPretty
import it.neckar.open.test.utils.DisableLogging
import it.neckar.open.test.utils.isJsonEqualTo
import it.neckar.open.test.utils.isOpenApiEqualTo
import it.neckar.rest.openapi.documentation.DocumentationProvider
import it.neckar.rest.openapi.OpenApiJsonGenerator
import it.neckar.rest.openapi.OpenApiMetaData
import it.neckar.rest.openapi.dsl.OpenApiRoute
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test

/**
 *
 */
@DisableLogging
class OpenApiJsonGeneratorMoreComplexTest {
  @Test
  fun testIt() {
    val encoded = Json.encodeToJsonElement(AnySerializableRawSerializer, FooBar("daFoor", 17))

    assertThat(encoded.toString()).isJsonEqualTo(
      //language=JSON
      """
        {
          "foo" : "daFoor",
          "bar" : 17
        }
      """.trimIndent()
    )
  }

  @Test
  fun testType() = testApplication {
    routing {
      put("foobar/{myId}") {
      }.doc {
        summary = "Simple PUT with parameters"

        request {
          parameterBoolean(UrlParameterName.verbose) {
            location = it.neckar.rest.openapi.dsl.OpenApiRequestParameter.Location.Query
            required = true

            example("the-example", true)
          }
          parameterInt(UrlParameterName("myId")) {
            example("the-example-1", 371)
            example("the-example-2", 42)
          }
          parameterDouble(UrlParameterName("myFactor")) {
            location = it.neckar.rest.openapi.dsl.OpenApiRequestParameter.Location.Header
            example("the-example-3" ,123.001)
          }

          body<FooBarRequest> {
            description = "A requested string"
            example("the-example-4", FooBarRequest(FooBar("foo", 42)))
            example("the-example-5", FooBarRequest(FooBar("foo2", 43)), "second example")
          }
        }

        response("foobar PUT response - a simple string") {
          body<FooBarResponse> {
            description = "The string that is returned"
            example("the-example-6", FooBarResponse(FooBar("foo2", 43)))
          }
        }
      }
    }

    lateinit var application: Application
    application {
      application = this
    }
    startApplication()

    val routes = application.openApiRoutes()
    assertThat(routes).hasSize(1)

    val firstRoute: OpenApiRoute = routes.first()

    assertThat(firstRoute.path.toString()).isEqualTo("/foobar/{myId}")

    val requestBody = firstRoute.request.body
    requireNotNull(requestBody)

    if (false) {
      println(OpenApiJsonGenerator(documentationProvider = DocumentationProvider.None).generate(routes, additionalTypes = emptyList(), metaData = OpenApiMetaData.default).toStringPretty())
    }

    assertThat(OpenApiJsonGenerator(documentationProvider = DocumentationProvider.None).generate(routes, additionalTypes = emptyList(), metaData = OpenApiMetaData.default).toString()).isOpenApiEqualTo(
      //language=JSON
      $$"""
        {
          "openapi": "3.1.0",
          "info": {
            "title": "REST API",
            "description": "REST API",
            "version": "1.0.0"
          },
          "servers": [],
          "paths": {
            "/foobar/{myId}": {
              "put": {
                "summary": "Simple PUT with parameters",
                "operationId": "PutMyIdForFoobar",
                "x-category": "default",
                "parameters": [
                  {
                    "name": "verbose",
                    "in": "query",
                    "required": true,
                    "schema": {
                      "type": "boolean",
                      "x-source-kotlin-type": "kotlin.Boolean"
                    },
                    "examples": {
                      "the-example": {
                        "value": true
                      }
                    }
                  },
                  {
                    "name": "myId",
                    "in": "path",
                    "required": true,
                    "schema": {
                      "type": "integer",
                      "x-source-kotlin-type": "kotlin.Int"
                    },
                    "examples": {
                      "the-example-1": {
                        "value": 371
                      },
                      "the-example-2": {
                        "value": 42
                      }
                    }
                  },
                  {
                    "name": "myFactor",
                    "in": "header",
                    "required": false,
                    "schema": {
                      "type": "number",
                      "format": "double",
                      "x-source-kotlin-type": "kotlin.Double"
                    },
                    "examples": {
                      "the-example-3": {
                        "value": 123.001
                      }
                    }
                  }
                ],
                "requestBody": {
                  "required": true,
                  "content": {
                    "application/json": {
                      "schema": {
                        "$ref": "#/components/schemas/OpenApiJsonGeneratorMoreComplexTest.FooBarRequest"
                      },
                      "examples": {
                        "the-example-4": {
                          "summary": "OpenApiJsonGeneratorMoreComplexTest.FooBarRequest",
                          "x-source-kotlin-type": "it.neckar.ktor.server.openapi.ktor.OpenApiJsonGeneratorMoreComplexTest.FooBarRequest",
                          "x-type-ref": "#/components/schemas/OpenApiJsonGeneratorMoreComplexTest.FooBarRequest",
                          "value": {
                            "fooBar": {
                              "foo": "foo",
                              "bar": 42
                            }
                          }
                        },
                        "the-example-5": {
                          "summary": "second example",
                          "x-source-kotlin-type": "it.neckar.ktor.server.openapi.ktor.OpenApiJsonGeneratorMoreComplexTest.FooBarRequest",
                          "x-type-ref": "#/components/schemas/OpenApiJsonGeneratorMoreComplexTest.FooBarRequest",
                          "value": {
                            "fooBar": {
                              "foo": "foo2",
                              "bar": 43
                            }
                          }
                        }
                      }
                    }
                  }
                },
                "responses": {
                  "200": {
                    "description": "foobar PUT response - a simple string",
                    "content": {
                      "application/json": {
                        "schema": {
                          "$ref": "#/components/schemas/OpenApiJsonGeneratorMoreComplexTest.FooBarResponse"
                        },
                        "examples": {
                          "the-example-6": {
                            "summary": "OpenApiJsonGeneratorMoreComplexTest.FooBarResponse",
                            "x-source-kotlin-type": "it.neckar.ktor.server.openapi.ktor.OpenApiJsonGeneratorMoreComplexTest.FooBarResponse",
                            "x-type-ref": "#/components/schemas/OpenApiJsonGeneratorMoreComplexTest.FooBarResponse",
                            "value": {
                              "fooBar": {
                                "foo": "foo2",
                                "bar": 43
                              }
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
          "security": [
            {
              "bearerAuth": []
            }
          ],
          "components": {
            "securitySchemes": {
              "bearerAuth": {
                "type": "http",
                "scheme": "bearer",
                "bearerFormat": "JWT"
              }
            },
            "schemas": {
              "OpenApiJsonGeneratorMoreComplexTest.FooBarRequest": {
                "type": "object",
                "x-source-kotlin-type": "it.neckar.ktor.server.openapi.ktor.OpenApiJsonGeneratorMoreComplexTest.FooBarRequest",
                "properties": {
                  "fooBar": {
                    "$ref": "#/components/schemas/OpenApiJsonGeneratorMoreComplexTest.FooBar"
                  }
                },
                "required": [
                  "fooBar"
                ]
              },
              "OpenApiJsonGeneratorMoreComplexTest.FooBar": {
                "type": "object",
                "x-source-kotlin-type": "it.neckar.ktor.server.openapi.ktor.OpenApiJsonGeneratorMoreComplexTest.FooBar",
                "properties": {
                  "foo": {
                    "type": "string"
                  },
                  "bar": {
                    "type": "integer"
                  }
                },
                "required": [
                  "foo",
                  "bar"
                ]
              },
              "OpenApiJsonGeneratorMoreComplexTest.FooBarResponse": {
                "type": "object",
                "x-source-kotlin-type": "it.neckar.ktor.server.openapi.ktor.OpenApiJsonGeneratorMoreComplexTest.FooBarResponse",
                "properties": {
                  "fooBar": {
                    "$ref": "#/components/schemas/OpenApiJsonGeneratorMoreComplexTest.FooBar"
                  }
                },
                "required": [
                  "fooBar"
                ]
              }
            }
          }
        }
      """.trimIndent()
    )
  }

  @Serializable
  data class FooBarRequest(
    val fooBar: FooBar,
  )

  @Serializable
  data class FooBarResponse(
    val fooBar: FooBar,
  )

  @Serializable
  data class FooBar(
    val foo: String,
    val bar: Int,
  )
}
