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
import it.neckar.open.test.utils.DisableLogging
import it.neckar.open.test.utils.isOpenApiEqualTo
import it.neckar.rest.filtering.FilterKey
import it.neckar.rest.filtering.HasTypedFilterKey
import it.neckar.rest.filtering.TypedFilterKey
import it.neckar.rest.filtering.instant
import it.neckar.rest.filtering.uuid
import it.neckar.rest.openapi.documentation.DocumentationProvider
import it.neckar.rest.openapi.OpenApiJsonGenerator
import it.neckar.rest.openapi.OpenApiMetaData
import it.neckar.rest.openapi.dsl.filteredBy
import it.neckar.rest.openapi.dsl.paginated
import org.junit.jupiter.api.Test

/**
 *
 */
@DisableLogging
class OpenApiJsonGeneratorParameterFilterTest {
  enum class MyFilterKeys(override val typedFilterKey: TypedFilterKey) : HasTypedFilterKey {
    Begin(FilterKey("begin").instant()),
    End(FilterKey("end").instant()),
    Product(FilterKey("filter-by-product").uuid()),
  }


  @Test
  fun testFilter() = testApplication {
    routing {
      get("foobar") {
      }.doc {
        summary = "Simple GET with filter parameters"
        filteredBy<MyFilterKeys>()

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
    assertThat(docs).hasSize(1)
    val route = docs.first()

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
                "summary" : "Simple GET with filter parameters",
                "operationId" : "GetFoobar",
                "x-category" : "default",
                "parameters" : [ {
                  "name" : "begin",
                  "in" : "query",
                  "required" : false,
                  "x-source-kotlin-type" : "it.neckar.ktor.server.openapi.ktor.OpenApiJsonGeneratorParameterFilterTest.MyFilterKeys",
                  "schema" : {
                    "type" : "string",
                    "format" : "date-time",
                    "x-source-kotlin-type" : "kotlin.String"
                  }
                }, {
                  "name" : "end",
                  "in" : "query",
                  "required" : false,
                  "x-source-kotlin-type" : "it.neckar.ktor.server.openapi.ktor.OpenApiJsonGeneratorParameterFilterTest.MyFilterKeys",
                  "schema" : {
                    "type" : "string",
                    "format" : "date-time",
                    "x-source-kotlin-type" : "kotlin.String"
                  }
                }, {
                  "name" : "filter-by-product",
                  "in" : "query",
                  "required" : false,
                  "x-source-kotlin-type" : "it.neckar.ktor.server.openapi.ktor.OpenApiJsonGeneratorParameterFilterTest.MyFilterKeys",
                  "schema" : {
                    "type" : "string",
                    "format" : "uuid",
                    "x-source-kotlin-type" : "kotlin.String"
                  }
                }, {
                  "name" : "scope",
                  "in" : "query",
                  "required" : false,
                  "description" : "Comma-separated list of filter keys that define the query scope. Scope filters affect totalElementsCount in pagination.",
                  "x-source-kotlin-type" : "it.neckar.ktor.server.openapi.ktor.OpenApiJsonGeneratorParameterFilterTest.MyFilterKeys",
                  "schema" : {
                    "type" : "string",
                    "enum" : [ "begin", "end", "filter-by-product" ],
                    "x-source-kotlin-type" : "kotlin.String"
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
            "schemas" : { }
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

        request {
          //Ensure does not overwrite paginated()
        }

        response("Some response") {
          description = "Empty Response"
          body<String> {
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

    val routes: List<Route> = application.collectRoutes()

    val docs = routes.openApiRoutes()

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
                    "description" : "Empty Response",
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
