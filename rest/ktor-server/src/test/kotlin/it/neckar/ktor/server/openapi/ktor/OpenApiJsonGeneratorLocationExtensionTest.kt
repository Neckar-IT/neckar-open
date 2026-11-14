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
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import it.neckar.ktor.collectRoutes
import it.neckar.open.serialization.isOpenApiEqualTo
import it.neckar.rest.openapi.documentation.DocumentationProvider
import it.neckar.rest.openapi.documentation.KotlinSourceInfo
import it.neckar.rest.openapi.OpenApiJsonGenerator
import it.neckar.rest.openapi.OpenApiMetaData
import it.neckar.rest.openapi.dsl.openApiRoute
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import it.neckar.open.test.utils.DisableLogging

/**
 *
 */
@DisableLogging
class OpenApiJsonGeneratorLocationExtensionTest {
  private lateinit var generator: OpenApiJsonGenerator

  @BeforeEach
  fun setUp() {
    generator = OpenApiJsonGenerator(documentationProvider = DocumentationProvider.None)
  }

  @Test
  fun testLocation() {
    val route = openApiRoute(
      "/foobar", HttpMethod.Get, emptyList(), sourceInformation = KotlinSourceInfo(
        fileName = "daFileName",
        lineNumber = 42,
        className = "daClassName",
        methodName = "daMethodName",
      )
    ) {
      response("the response") {
        body<String> {
          mediaType = ContentType.Text.Plain
          description = "A plain text response"
          noExamples()
        }
      }
    }

    assertThat(route.sourceInfo).isNotNull()
    assertThat(route.sourceInfo?.location).isEqualTo("daFileName:42")
  }

  @Test
  fun testAutoFetch() = testApplication {
    routing {
      get("foobar") {
        TODO()
      }.doc {
        response("the response") {
          body<String> {
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
    val apiRoute = docs.first()

    assertThat(apiRoute.sourceInfo).isNotNull()

    val jsonElement = OpenApiJsonGenerator(documentationProvider = DocumentationProvider.None).generate(docs, additionalTypes = emptyList(), metaData = OpenApiMetaData.default)
    assertThat(jsonElement).isOpenApiEqualTo(
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
                "operationId" : "GetFoobar",
                "x-category" : "default",
                "parameters" : [ ],
                "responses" : {
                  "200" : {
                    "description" : "the response",
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
