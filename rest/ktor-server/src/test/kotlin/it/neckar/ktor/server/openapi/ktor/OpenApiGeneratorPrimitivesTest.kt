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
import it.neckar.open.http.Url
import it.neckar.open.serialization.isJsonEqualTo
import it.neckar.open.serialization.isOpenApiEqualTo
import it.neckar.rest.RestResponse
import it.neckar.rest.openapi.documentation.DocumentationProvider
import it.neckar.rest.openapi.OpenApiJsonGenerator
import it.neckar.rest.openapi.OpenApiMetaData
import it.neckar.rest.openapi.schema.SchemaDescriptor
import it.neckar.uuid.NativeUuid
import it.neckar.uuid.ValueClassUuid
import it.neckar.uuid.randomUuid4
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import org.junit.jupiter.api.Test
import kotlin.uuid.Uuid
import it.neckar.open.test.utils.DisableLogging
import it.neckar.open.test.utils.isOpenApiEqualTo

@DisableLogging
class OpenApiGeneratorPrimitivesTest {
  @Test
  fun testUuid() {
    val json: JsonObject = buildJsonObject {
      val schema = SchemaDescriptor.of(WithUuid::class)
      OpenApiJsonGenerator(documentationProvider = DocumentationProvider.None).buildComponentsSchemas(listOf(schema))
    }

    assertThat(json).isJsonEqualTo(
      //language=JSON
      """
        {
          "OpenApiGeneratorPrimitivesTest.WithUuid" : {
            "type" : "object",
            "x-source-kotlin-type" : "it.neckar.ktor.server.openapi.ktor.OpenApiGeneratorPrimitivesTest.WithUuid",
            "properties" : {
              "id" : {
                "type" : "string",
                "format" : "uuid"
              }
            },
            "required" : [ "id" ]
          }
        }
      """.trimIndent()
    )
  }

  @Serializable
  private data class WithUuid(
    val id: Uuid
  )

  @Test
  fun testInnerId() {
    val json: JsonObject = buildJsonObject {
      val schema = SchemaDescriptor.of(WithId::class)
      OpenApiJsonGenerator(documentationProvider = DocumentationProvider.None).buildComponentsSchemas(listOf(schema))
    }

    assertThat(json).isJsonEqualTo(
      //language=JSON
      """
        {
          "OpenApiGeneratorPrimitivesTest.WithId" : {
            "type" : "object",
            "x-source-kotlin-type" : "it.neckar.ktor.server.openapi.ktor.OpenApiGeneratorPrimitivesTest.WithId",
            "properties" : {
              "id" : {
                "x-source-kotlin-type" : "it.neckar.ktor.server.openapi.ktor.OpenApiGeneratorPrimitivesTest.WithId.Id",
                "type" : "string",
                "format" : "uuid"
              }
            },
            "required" : [ "id" ]
          }
        }
      """.trimIndent()
    )
  }


  @Serializable
  private data class WithId(
    val id: Id
  ) {
    @Serializable
    @JvmInline
    value class Id(
      override val uuid: NativeUuid,
    ) : ValueClassUuid {

      override fun toString(): String {
        return uuid.toHexDashString()
      }

      companion object {
        /**
         * Creates a new random id
         */
        fun random(): Id = Id(randomUuid4())
      }
    }

  }

  @Test
  fun testNullableCollection() = testApplication {
    routing {
      get("a-string") {
        TODO()
      }.doc {
        response("Returns a String") {
          body<String> {
            noExamples()
          }
        }
      }
      get("a-uuid") {
        TODO()
      }.doc {
        response("Returns a Uuid") {
          body<Uuid> {
            noExamples()
          }
        }
      }
      get("data-scheme-url") {
        TODO()
      }.doc {
        response("Returns a simple URL") {
          body<Url.DataScheme> {
            noExamples()
          }
        }
      }
      get("url") {
        TODO()
      }.doc {
        response("Returns a simple URL") {
          body<Url> {
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
            "/a-string" : {
              "get" : {
                "operationId" : "GetAString",
                "x-category" : "default",
                "parameters" : [ ],
                "responses" : {
                  "200" : {
                    "description" : "Returns a String",
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
            },
            "/a-uuid" : {
              "get" : {
                "operationId" : "GetAUuid",
                "x-category" : "default",
                "parameters" : [ ],
                "responses" : {
                  "200" : {
                    "description" : "Returns a Uuid",
                    "content" : {
                      "application/json" : {
                        "schema" : {
                          "type" : "string",
                          "format" : "uuid",
                          "x-source-kotlin-type" : "kotlin.uuid.Uuid"
                        }
                      }
                    }
                  }
                }
              }
            },
            "/data-scheme-url" : {
              "get" : {
                "operationId" : "GetDataSchemeUrl",
                "x-category" : "default",
                "parameters" : [ ],
                "responses" : {
                  "200" : {
                    "description" : "Returns a simple URL",
                    "content" : {
                      "application/json" : {
                        "schema" : {
                          "type" : "string",
                          "format" : "uri",
                          "x-source-kotlin-type" : "it.neckar.open.http.Url.DataScheme"
                        }
                      }
                    }
                  }
                }
              }
            },
            "/url" : {
              "get" : {
                "operationId" : "GetUrl",
                "x-category" : "default",
                "parameters" : [ ],
                "responses" : {
                  "200" : {
                    "description" : "Returns a simple URL",
                    "content" : {
                      "application/json" : {
                        "schema" : {
                          "type" : "string",
                          "format" : "uri",
                          "x-source-kotlin-type" : "it.neckar.open.http.Url"
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

  /**
   * Response for
   */
  @Serializable
  private sealed interface MyTestResponse : RestResponse<List<MyDataClass?>> {

    @Serializable
    @SerialName("good")
    data class Good(override val data: List<MyDataClass?>) : MyTestResponse, RestResponse.GoodResponse<List<MyDataClass?>>

    @Serializable
    @SerialName("bad/Failed")
    data object Failed : MyTestResponse, RestResponse.BadResponse<List<MyDataClass?>>
  }

  @Serializable
  private data class MyDataClass(val value: String)
}
