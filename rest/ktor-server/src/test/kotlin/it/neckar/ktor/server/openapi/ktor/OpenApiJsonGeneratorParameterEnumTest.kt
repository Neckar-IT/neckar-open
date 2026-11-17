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
import it.neckar.ktor.collectRoutes
import it.neckar.open.http.UrlParameterName
import it.neckar.open.test.utils.DisableLogging
import it.neckar.open.test.utils.isOpenApiEqualTo
import it.neckar.rest.openapi.documentation.DocumentationProvider
import it.neckar.rest.openapi.OpenApiJsonGenerator
import it.neckar.rest.openapi.OpenApiMetaData
import it.neckar.rest.openapi.dsl.paginated
import org.junit.jupiter.api.Test

/**
 *
 */
@DisableLogging
class OpenApiJsonGeneratorParameterEnumTest {
  enum class MyEnum {
    A, B, C
  }

  @Test
  fun testParamEnum() = testApplication {
    routing {
      get("foobar") {
      }.doc {
        summary = "Simple GET with enum parameters"

        request {
          parameterEnum(UrlParameterName("myEnum"), MyEnum::class)
        }
        noResponseBody()
      }
    }

    //The application will be available after startApplication() has been called
    lateinit var application: Application
    application {
      application = this
    }
    startApplication()


    assertThat(application).isNotNull()
    assertThat(application.attributes).isNotNull()

    val routes: List<Route> = application.collectRoutes()

    val docs = routes.openApiRoutes()
    assertThat(OpenApiJsonGenerator(documentationProvider = DocumentationProvider.None).generate(docs, additionalTypes = emptyList(), metaData = OpenApiMetaData.default).toString()).isOpenApiEqualTo(
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
              "get" : {
                "summary" : "Simple GET with enum parameters",
                "operationId" : "GetFoobar",
                "x-category" : "default",
                "parameters" : [ {
                  "name" : "myEnum",
                  "in" : "query",
                  "required" : false,
                  "x-source-kotlin-type" : "it.neckar.ktor.server.openapi.ktor.OpenApiJsonGeneratorParameterEnumTest.MyEnum",
                  "schema" : {
                    "$ref" : "#/components/schemas/OpenApiJsonGeneratorParameterEnumTest.MyEnum"
                  }
                } ],
                "responses" : {
                  "204" : {
                    "description" : "Successful request with no content"
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
              "OpenApiJsonGeneratorParameterEnumTest.MyEnum" : {
                "type" : "string",
                "enum" : [ "A", "B", "C" ]
              }
            }
          }
        }
      """.trimIndent()
    )
  }

  @Test
  fun testApi() = testApplication {
    routing {
      get("foobar") {
      }.doc {
        summary = "Simple GET with enum parameters"
        paginated()

        response("A simple response") {
          body<String> {
            description = "A simple response"
            noExamples()
          }
        }
      }
    }

    //The application will be available after startApplication() has been called
    lateinit var application: Application
    application {
      application = this
    }
    startApplication()


    assertThat(application).isNotNull()
    assertThat(application.attributes).isNotNull()

    val docs = application.openApiRoutes()

    assertThat(docs).hasSize(1)
    val doc = docs.first()
    assertThat(doc.request.parameters).hasSize(2)

    assertThat(OpenApiJsonGenerator(documentationProvider = DocumentationProvider.None).generate(docs, additionalTypes = emptyList(), metaData = OpenApiMetaData.default).toString()).isOpenApiEqualTo(
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
          "paths" : {
            "/foobar" : {
              "get" : {
                "summary" : "Simple GET with enum parameters",
                "operationId" : "GetFoobar",
                "x-category" : "default",
                "parameters" : [ {
                  "name" : "pageIndex",
                  "in" : "query",
                  "required" : false,
                  "schema" : {
                    "type" : "integer",
                    "x-source-kotlin-type" : "kotlin.Int"
                  }
                }, {
                  "name" : "pageSize",
                  "in" : "query",
                  "required" : false,
                  "schema" : {
                    "type" : "integer",
                    "x-source-kotlin-type" : "kotlin.Int"
                  }
                } ],
                "responses" : {
                  "200" : {
                    "description" : "A simple response",
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
            "schemas" : { }
          }
        }
      """.trimIndent()
    )
  }
}
