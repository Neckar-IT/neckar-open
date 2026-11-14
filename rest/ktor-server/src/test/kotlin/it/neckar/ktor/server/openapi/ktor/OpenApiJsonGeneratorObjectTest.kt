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
import it.neckar.rest.openapi.documentation.DocumentationProvider
import it.neckar.rest.openapi.OpenApiJsonGenerator
import it.neckar.rest.openapi.OpenApiMetaData
import it.neckar.rest.openapi.schema.SchemaDescriptor
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.Test

/**
 *
 */
@DisableLogging
class OpenApiJsonGeneratorObjectTest {
  @Test
  fun testSimple() = testApplication {
    routing {
      get("foobar") {
        TODO()
      }.doc {
        summary = "Returns an object"

        response("foobar response - an object") {
          body<FooBar> {
            description = "the object itself"
            example("the-example", FooBar)
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
                "summary" : "Returns an object",
                "operationId" : "GetFoobar",
                "x-category" : "default",
                "parameters" : [ ],
                "responses" : {
                  "200" : {
                    "description" : "foobar response - an object",
                    "content" : {
                      "application/json" : {
                        "schema" : {
                          "$ref" : "#/components/schemas/OpenApiJsonGeneratorObjectTest.FooBar"
                        },
                        "examples" : {
                          "the-example" : {
                            "summary" : "OpenApiJsonGeneratorObjectTest.FooBar",
                            "x-source-kotlin-type" : "it.neckar.ktor.server.openapi.ktor.OpenApiJsonGeneratorObjectTest.FooBar",
                            "x-type-ref" : "#/components/schemas/OpenApiJsonGeneratorObjectTest.FooBar",
                            "value" : { }
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
              "OpenApiJsonGeneratorObjectTest.FooBar" : {
                "type" : "object",
                "x-source-kotlin-type" : "it.neckar.ktor.server.openapi.ktor.OpenApiJsonGeneratorObjectTest.FooBar",
                "properties" : {
                  "Bar" : {
                    "type" : "integer",
                    "x-source-kotlin-type" : "kotlin.Int",
                    "const" : 42
                  },
                  "Foo" : {
                    "type" : "string",
                    "x-source-kotlin-type" : "kotlin.String",
                    "const" : "foo"
                  }
                }
              }
            }
          }
        }
      """.trimIndent()
    )
  }

  @Test
  fun testMessageWinsOverAdditionalType() = testApplication {
    routing {
      get("foobar") {
        TODO()
      }.doc {
        response("foobar response - an object") {
          body<FooBar> {
            example("the-example", FooBar)
          }
        }
      }
    }

    lateinit var application: Application
    application {
      application = this
    }
    startApplication()

    val docs = application.collectRoutes().openApiRoutes()
    val document = OpenApiJsonGenerator(documentationProvider = DocumentationProvider.None)
      .generate(docs, additionalTypes = listOf(SchemaDescriptor.of(FooBar::class)), metaData = OpenApiMetaData.default)

    val fooBarSchema = document.jsonObject.getValue("components").jsonObject.getValue("schemas").jsonObject.getValue("OpenApiJsonGeneratorObjectTest.FooBar").jsonObject
    assertThat(fooBarSchema.containsKey("required")).isFalse()
  }

  @Test
  fun testGetValue() {
    assertThat(FooBar::Foo.get()).isEqualTo("foo")
    assertThat(FooBar::Bar.get()).isEqualTo(42)
  }

  @Serializable
  object FooBar {
    const val Foo: String = "foo"
    const val Bar: Int = 42
  }
}
