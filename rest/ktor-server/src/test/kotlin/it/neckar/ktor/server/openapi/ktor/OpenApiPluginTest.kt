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
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import it.neckar.rest.test.*
import it.neckar.ktor.client.get
import it.neckar.ktor.client.getJson
import it.neckar.ktor.collectRoutes
import it.neckar.ktor.AuthenticationRequirement
import it.neckar.ktor.server.openapi.ktor.message.OpenApiCategoriesResponse
import it.neckar.open.http.Url
import it.neckar.open.test.utils.DisableLogging
import it.neckar.open.test.utils.isOpenApiEqualTo
import it.neckar.open.test.utils.removePropertiesStartingWith
import it.neckar.rest.openapi.OpenApiMetaData
import org.junit.jupiter.api.Test


@DisableLogging
class OpenApiPluginTest {
  @Test
  fun testSimple() = testApplication {
    install(OpenApiPlugin) {
      metadata = OpenApiMetaData(
        title = "Test API",
        description = "Da Test API",
      )
      authenticationRequirement = AuthenticationRequirement.None
      scalarUi = ScalarUiMode.Disabled
    }

    routing {
      get("foobar") {
      }.doc {
        summary = "A simple foobar endpoint"
        description = "This endpoint does nothing, but is used to test the OpenAPI plugin"
        noResponseBody()
      }
    }


    startApplication()

    assertThat(application.collectRoutes().map { it.toString() }).contains("/doc/openapi/(method:GET)")

    application.verifyOpenApiDocComplete()

    run {
      val response = client.get(Url.localhostHttp + OpenApiEndpoints.openapiUrl()) {
        accept(ContentType.Application.Json)
      }
      assertThat(response).isOk()

      val jsonResponse = response.bodyAsText()
      assertThat(jsonResponse).isOpenApiEqualTo(
        //language=JSON
        $$"""
        {
          "openapi": "3.1.0",
          "info": {
            "title": "Test API",
            "description": "Da Test API",
            "version": "1.0.0"
          },
          "servers": [],
          "paths": {
            "/doc/openapi": {
              "get": {
                "description": "Returns the OpenAPI documentation for the REST API in JSON format.",
                "summary": "Returns the OpenAPI documentation",
                "operationId": "GetOpenApiDocumentation",
                "x-category": "meta",
                "parameters": [],
                "responses": {
                  "200": {
                    "description": "OpenAPI JSON"
                  }
                }
              }
            },
            "/doc/openapi/categories": {
              "get": {
                "description": "Returns the available categories for the OpenAPI documentation.",
                "summary": "Returns the available categories",
                "operationId": "GetOpenApiCategories",
                "x-category": "meta",
                "parameters": [],
                "responses": {
                  "200": {
                    "description": "Responds with Good or Bad types",
                    "content": {
                      "application/json": {
                        "schema": {
                          "$ref": "#/components/schemas/OpenApiCategoriesResponse"
                        },
                        "examples": {
                          "good": {
                            "summary": "Good",
                            "x-source-kotlin-type": "it.neckar.ktor.server.openapi.ktor.message.OpenApiCategoriesResponse.Good",
                            "x-type-ref": "#/components/schemas/OpenApiCategoriesResponse.Good.WithType",
                            "value": {
                              "data": [
                                {
                                  "value": "default"
                                },
                                {
                                  "value": "admin"
                                },
                                {
                                  "value": "meta"
                                }
                              ],
                              "type": "good"
                            }
                          }
                        }
                      }
                    }
                  }
                }
              }
            },
            "/doc/openapi/{category}": {
              "get": {
                "description": "Returns the OpenAPI documentation for a specific category in JSON format.",
                "summary": "Returns the OpenAPI documentation for a specific category",
                "operationId": "GetOpenApiDocumentationByCategory",
                "x-category": "meta",
                "parameters": [
                  {
                    "name": "category",
                    "in": "path",
                    "required": true,
                    "description": "The name of the category to filter the routes by",
                    "schema": {
                      "type": "string",
                      "x-source-kotlin-type": "kotlin.String"
                    },
                    "examples": {
                      "default": {
                        "value": "default"
                      }
                    }
                  }
                ],
                "responses": {
                  "200": {
                    "description": "OpenAPI JSON"
                  }
                }
              }
            },
            "/doc/openapi/with-origin": {
              "get": {
                "description": "Returns the OpenAPI documentation with the servers list derived from the incoming request's origin (X-Forwarded-* headers or direct connection). Falls back to the direct connection values when no forwarding headers are present.",
                "summary": "Returns the OpenAPI documentation with auto-detected server URL",
                "operationId": "GetOpenApiDocumentationWithOrigin",
                "x-category": "meta",
                "parameters": [],
                "responses": {
                  "200": {
                    "description": "OpenAPI JSON"
                  }
                }
              }
            },
            "/foobar": {
              "get": {
                "description": "This endpoint does nothing, but is used to test the OpenAPI plugin",
                "summary": "A simple foobar endpoint",
                "operationId": "GetFoobar",
                "x-category": "default",
                "parameters": [],
                "responses": {
                  "204": {
                    "description": "Successful request with no content"
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
              "OpenApiCategoriesResponse": {
                "type": "object",
                "x-source-kotlin-type": "it.neckar.ktor.server.openapi.ktor.message.OpenApiCategoriesResponse",
                "oneOf": [
                  {
                    "$ref": "#/components/schemas/OpenApiCategoriesResponse.Good.WithType"
                  }
                ],
                "discriminator": {
                  "propertyName": "type",
                  "mapping": {
                    "good": "#/components/schemas/OpenApiCategoriesResponse.Good.WithType"
                  }
                }
              },
              "OpenApiCategoriesResponse.Good.WithType": {
                "type": "object",
                "x-source-kotlin-type": "it.neckar.ktor.server.openapi.ktor.message.OpenApiCategoriesResponse.Good",
                "allOf": [
                  {
                    "$ref": "#/components/schemas/OpenApiCategoriesResponse.Good"
                  }
                ],
                "properties": {
                  "type": {
                    "type": "string",
                    "enum": [
                      "good"
                    ]
                  }
                },
                "required": [
                  "type"
                ]
              },
              "OpenApiCategoriesResponse.Good": {
                "type": "object",
                "x-source-kotlin-type": "it.neckar.ktor.server.openapi.ktor.message.OpenApiCategoriesResponse.Good",
                "properties": {
                  "data": {
                    "type": "array",
                    "items": {
                      "$ref": "#/components/schemas/OpenApiMetaData.Category"
                    }
                  }
                },
                "required": [
                  "data"
                ]
              },
              "OpenApiMetaData.Category": {
                "type": "object",
                "x-source-kotlin-type": "it.neckar.rest.openapi.OpenApiMetaData.Category",
                "properties": {
                  "value": {
                    "type": "string"
                  }
                },
                "required": [
                  "value"
                ]
              }
            }
          }
        }
        """.trimIndent()
      )
    }
  }

  @Test
  fun testScalarHtmlEndpoint() = testApplication {
    install(OpenApiPlugin) {
      metadata = OpenApiMetaData(
        title = "Test API",
        description = "Da Test API",
      )
      authenticationRequirement = AuthenticationRequirement.None
    }

    routing {
      get("foobar") {
      }.doc {
        summary = "A simple foobar endpoint"
        noResponseBody()
      }
    }

    startApplication()

    run {
      val response = client.get(Url.localhostHttp + OpenApiEndpoints.apiUrl())
      assertThat(response).isOk()
      assertThat(response.headers["Content-Type"]).isNotNull().contains("text/html")

      val body = response.bodyAsText()
      assertThat(body).contains("<script id=\"api-reference\"")
      assertThat(body).contains("data-url=\"/doc/openapi/with-origin\"")
      assertThat(body).contains("@scalar/api-reference")
      assertThat(body).contains("<title>Test API</title>")
    }

    run {
      val response = client.get(Url.localhostHttp + OpenApiEndpoints.apiByCategoryUrl(OpenApiMetaData.Category.Default))
      assertThat(response).isOk()

      val body = response.bodyAsText()
      assertThat(body).contains("data-url=\"/doc/openapi/default\"")
      assertThat(body).contains("Test API — default")
    }
  }

  @Test
  fun testCategories() = testApplication {
    val client = createTestClient()

    install(OpenApiPlugin) {
      metadata = OpenApiMetaData(
        title = "Test API",
        description = "Da Test API",
      )
      authenticationRequirement = AuthenticationRequirement.None
      scalarUi = ScalarUiMode.Disabled
    }
    install(ContentNegotiation) {
      json()
    }

    routing {
      get("foobar") {
      }.doc {
        category = OpenApiMetaData.Category.Default
        noResponseBody()
      }

      get("foobar-admin") {
      }.doc {
        category = OpenApiMetaData.Category.Admin
        noResponseBody()
      }

      get("foobar-other-production") {
      }.doc {
        category = OpenApiMetaData.Category("other-production")
        noResponseBody()
      }
    }


    startApplication()

    run {
      val response = client.get(Url.localhostHttp + OpenApiEndpoints.openapiUrl()) {
        accept(ContentType.Application.Json)
      }
      assertThat(response).isOk()

      val jsonResponse = response.bodyAsText()
      assertThat(jsonResponse).isOpenApiEqualTo(
        //language=JSON
        $$"""
        {
          "openapi": "3.1.0",
          "info": {
            "title": "Test API",
            "description": "Da Test API",
            "version": "1.0.0"
          },
          "servers": [],
          "paths": {
            "/doc/openapi": {
              "get": {
                "description": "Returns the OpenAPI documentation for the REST API in JSON format.",
                "summary": "Returns the OpenAPI documentation",
                "operationId": "GetOpenApiDocumentation",
                "parameters": [],
                "responses": {
                  "200": {
                    "description": "OpenAPI JSON"
                  }
                }
              }
            },
            "/doc/openapi/categories": {
              "get": {
                "description": "Returns the available categories for the OpenAPI documentation.",
                "summary": "Returns the available categories",
                "operationId": "GetOpenApiCategories",
                "parameters": [],
                "responses": {
                  "200": {
                    "description": "Responds with Good or Bad types",
                    "content": {
                      "application/json": {
                        "schema": {
                          "$ref": "#/components/schemas/OpenApiCategoriesResponse"
                        },
                        "examples": {
                          "good": {
                            "summary": "Good",
                            "value": {
                              "data": [
                                {
                                  "value": "default"
                                },
                                {
                                  "value": "admin"
                                },
                                {
                                  "value": "meta"
                                }
                              ],
                              "type": "good"
                            }
                          }
                        }
                      }
                    }
                  }
                }
              }
            },
            "/doc/openapi/{category}": {
              "get": {
                "description": "Returns the OpenAPI documentation for a specific category in JSON format.",
                "summary": "Returns the OpenAPI documentation for a specific category",
                "operationId": "GetOpenApiDocumentationByCategory",
                "parameters": [
                  {
                    "name": "category",
                    "in": "path",
                    "required": true,
                    "description": "The name of the category to filter the routes by",
                    "schema": {
                      "type": "string"
                    },
                    "examples": {
                      "default": {
                        "value": "default"
                      }
                    }
                  }
                ],
                "responses": {
                  "200": {
                    "description": "OpenAPI JSON"
                  }
                }
              }
            },
            "/doc/openapi/with-origin": {
              "get": {
                "description": "Returns the OpenAPI documentation with the servers list derived from the incoming request's origin (X-Forwarded-* headers or direct connection). Falls back to the direct connection values when no forwarding headers are present.",
                "summary": "Returns the OpenAPI documentation with auto-detected server URL",
                "operationId": "GetOpenApiDocumentationWithOrigin",
                "parameters": [],
                "responses": {
                  "200": {
                    "description": "OpenAPI JSON"
                  }
                }
              }
            },
            "/foobar": {
              "get": {
                "operationId": "GetFoobar",
                "parameters": [],
                "responses": {
                  "204": {
                    "description": "Successful request with no content"
                  }
                }
              }
            },
            "/foobar-admin": {
              "get": {
                "operationId": "GetFoobarAdmin",
                "parameters": [],
                "responses": {
                  "204": {
                    "description": "Successful request with no content"
                  }
                }
              }
            },
            "/foobar-other-production": {
              "get": {
                "operationId": "GetFoobarOtherProduction",
                "parameters": [],
                "responses": {
                  "204": {
                    "description": "Successful request with no content"
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
              "OpenApiCategoriesResponse": {
                "type": "object",
                "oneOf": [
                  {
                    "$ref": "#/components/schemas/OpenApiCategoriesResponse.Good.WithType"
                  }
                ],
                "discriminator": {
                  "propertyName": "type",
                  "mapping": {
                    "good": "#/components/schemas/OpenApiCategoriesResponse.Good.WithType"
                  }
                }
              },
              "OpenApiCategoriesResponse.Good.WithType": {
                "type": "object",
                "allOf": [
                  {
                    "$ref": "#/components/schemas/OpenApiCategoriesResponse.Good"
                  }
                ],
                "properties": {
                  "type": {
                    "type": "string",
                    "enum": [
                      "good"
                    ]
                  }
                },
                "required": [
                  "type"
                ]
              },
              "OpenApiCategoriesResponse.Good": {
                "type": "object",
                "properties": {
                  "data": {
                    "type": "array",
                    "items": {
                      "$ref": "#/components/schemas/OpenApiMetaData.Category"
                    }
                  }
                },
                "required": [
                  "data"
                ]
              },
              "OpenApiMetaData.Category": {
                "type": "object",
                "properties": {
                  "value": {
                    "type": "string"
                  }
                },
                "required": [
                  "value"
                ]
              }
            }
          }
        }
        """.trimIndent()
      ) {
        this.removePropertiesStartingWith("x-")
      }
    }

    val categories = client.getJson<OpenApiCategoriesResponse>(Url.localhostHttp + OpenApiEndpoints.categoriesUrl()) {}.asGood().data
    assertThat(categories).all {
      hasSize(4)
      containsExactly(OpenApiMetaData.Category.Meta, OpenApiMetaData.Category.Default, OpenApiMetaData.Category.Admin, OpenApiMetaData.Category("other-production"))
    }
  }
}
