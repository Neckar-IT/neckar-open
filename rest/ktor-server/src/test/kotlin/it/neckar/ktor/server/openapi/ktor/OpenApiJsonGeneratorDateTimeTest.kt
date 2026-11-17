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
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import it.neckar.datetime.minimal.LocalDate
import it.neckar.ktor.collectAllRoutes
import it.neckar.open.kotlin.serializers.DoubleAsIsoDateTimeSerializer
import it.neckar.open.serialization.isJsonEqualTo
import it.neckar.open.serialization.isOpenApiEqualTo
import it.neckar.open.unit.si.ms
import it.neckar.rest.openapi.documentation.DocumentationProvider
import it.neckar.rest.openapi.OpenApiJsonGenerator
import it.neckar.rest.openapi.OpenApiMetaData
import it.neckar.rest.openapi.schema.SchemaDescriptor
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import org.junit.jupiter.api.Test
import kotlin.time.Instant
import it.neckar.open.test.utils.DisableLogging
import it.neckar.open.test.utils.isOpenApiEqualTo

/**
 *
 */
@DisableLogging
class OpenApiJsonGeneratorDateTimeTest {
  @Test
  fun testNeckarLocalDateType() {
    val json = buildJsonObject {
      OpenApiJsonGenerator(documentationProvider = DocumentationProvider.None).buildComponentsSchemas(listOf(SchemaDescriptor.of<DataWithDate>()))
    }

    assertThat(json).isJsonEqualTo(
      //language=JSON
      """
        {
          "OpenApiJsonGeneratorDateTimeTest.DataWithDate" : {
            "type" : "object",
            "x-source-kotlin-type" : "it.neckar.ktor.server.openapi.ktor.OpenApiJsonGeneratorDateTimeTest.DataWithDate",
            "properties" : {
              "neckarItLocalDate" : {
                "type" : "string",
                "format" : "date"
              },
              "kotlinLocalDate" : {
                "type" : "string",
                "format" : "date"
              },
              "kotlinxDateTimeInstant" : {
                "type" : "string",
                "format" : "date-time"
              },
              "kotlinTimeInstant" : {
                "type" : "string",
                "format" : "date-time"
              }
            },
            "required" : [ "neckarItLocalDate", "kotlinLocalDate", "kotlinxDateTimeInstant", "kotlinTimeInstant" ]
          }
        }
      """.trimIndent()
    )
  }

  @Serializable
  private data class DataWithDate(
    val neckarItLocalDate: LocalDate,
    val kotlinLocalDate: kotlinx.datetime.LocalDate,
    val kotlinxDateTimeInstant: kotlinx.datetime.Instant,
    val kotlinTimeInstant: Instant,
  )


  @Test
  fun testDate() = testApplication {
    routing {
      get("foobar") {
        call.respond(MyData(123456789.0))
      }.doc {
        summary = "returns MyData"

        response("Returns a MyData") {
          body<MyData> {
            description = "contains a double as timestamp"
            example("the-example", MyData(123456789.0))
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

    val routes: List<Route> = application.collectAllRoutes()
    assertThat(routes).hasSize(3)

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
                "summary" : "returns MyData",
                "operationId" : "GetFoobar",
                "x-category" : "default",
                "parameters" : [ ],
                "responses" : {
                  "200" : {
                    "description" : "Returns a MyData",
                    "content" : {
                      "application/json" : {
                        "schema" : {
                          "$ref" : "#/components/schemas/OpenApiJsonGeneratorDateTimeTest.MyData"
                        },
                        "examples" : {
                          "the-example" : {
                            "summary" : "OpenApiJsonGeneratorDateTimeTest.MyData",
                            "x-source-kotlin-type" : "it.neckar.ktor.server.openapi.ktor.OpenApiJsonGeneratorDateTimeTest.MyData",
                            "x-type-ref" : "#/components/schemas/OpenApiJsonGeneratorDateTimeTest.MyData",
                            "value" : {
                              "date" : "1970-01-02T10:17:36.789Z"
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
              "OpenApiJsonGeneratorDateTimeTest.MyData" : {
                "type" : "object",
                "x-source-kotlin-type" : "it.neckar.ktor.server.openapi.ktor.OpenApiJsonGeneratorDateTimeTest.MyData",
                "properties" : {
                  "date" : {
                    "type" : "string",
                    "format" : "date-time"
                  }
                },
                "required" : [ "date" ]
              }
            }
          }
        }
      """.trimIndent()
    )
  }

  @Serializable
  private data class MyData(val date: @ms @Serializable(with = DoubleAsIsoDateTimeSerializer::class) Double)
}
