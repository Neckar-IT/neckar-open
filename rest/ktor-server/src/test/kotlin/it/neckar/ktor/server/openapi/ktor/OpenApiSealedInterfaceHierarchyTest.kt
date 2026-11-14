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
import it.neckar.ktor.collectAllRoutes
import it.neckar.open.test.utils.isOpenApiEqualTo
import it.neckar.rest.RestResponse
import it.neckar.rest.openapi.documentation.DocumentationProvider
import it.neckar.rest.openapi.OpenApiJsonGenerator
import it.neckar.rest.openapi.OpenApiMetaData
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import it.neckar.open.test.utils.DisableLogging

/**
 *
 */
@DisableLogging
class OpenApiSealedInterfaceHierarchyTest {
  private lateinit var generator: OpenApiJsonGenerator

  @BeforeEach
  fun setUp() {
    generator = OpenApiJsonGenerator(documentationProvider = DocumentationProvider.None)
  }

  @Test
  fun testIt() = testApplication {
    routing {

      get("foobar") {
        call.respond(MyTestResponse.Good(listOf(MyDataClass("first"), null, MyDataClass("third"))))
      }.doc {
        responding<MyTestResponse>{
          noExamples()
        }
      }
    }

    //The application will be available after startApplication() has been called
    lateinit var application: Application
    application {
      application = this
    }
    startApplication()


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
                "operationId" : "GetFoobar",
                "x-category" : "default",
                "parameters" : [ ],
                "responses" : {
                  "200" : {
                    "description" : "Responds with Good or Bad types",
                    "content" : {
                      "application/json" : {
                        "schema" : {
                          "$ref" : "#/components/schemas/OpenApiSealedInterfaceHierarchyTest.MyTestResponse"
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
              "OpenApiSealedInterfaceHierarchyTest.MyTestResponse" : {
                "type" : "object",
                "x-source-kotlin-type" : "it.neckar.ktor.server.openapi.ktor.OpenApiSealedInterfaceHierarchyTest.MyTestResponse",
                "oneOf" : [ {
                  "$ref" : "#/components/schemas/OpenApiSealedInterfaceHierarchyTest.MyTestResponse.Failed.NotFound"
                }, {
                  "$ref" : "#/components/schemas/OpenApiSealedInterfaceHierarchyTest.MyTestResponse.Good.WithType"
                } ],
                "discriminator" : {
                  "propertyName" : "type",
                  "mapping" : {
                    "bad/NotFound" : "#/components/schemas/OpenApiSealedInterfaceHierarchyTest.MyTestResponse.Failed.NotFound",
                    "good" : "#/components/schemas/OpenApiSealedInterfaceHierarchyTest.MyTestResponse.Good.WithType"
                  }
                }
              },
              "OpenApiSealedInterfaceHierarchyTest.MyTestResponse.Good.WithType" : {
                "type" : "object",
                "x-source-kotlin-type" : "it.neckar.ktor.server.openapi.ktor.OpenApiSealedInterfaceHierarchyTest.MyTestResponse.Good",
                "allOf" : [ {
                  "$ref" : "#/components/schemas/OpenApiSealedInterfaceHierarchyTest.MyTestResponse.Good"
                } ],
                "properties" : {
                  "type" : {
                    "type" : "string",
                    "enum" : [ "good" ]
                  }
                },
                "required" : [ "type" ]
              },
              "OpenApiSealedInterfaceHierarchyTest.MyTestResponse.Good" : {
                "type" : "object",
                "x-source-kotlin-type" : "it.neckar.ktor.server.openapi.ktor.OpenApiSealedInterfaceHierarchyTest.MyTestResponse.Good",
                "properties" : {
                  "data" : {
                    "type" : "array",
                    "items" : {
                      "oneOf" : [ {
                        "$ref" : "#/components/schemas/OpenApiSealedInterfaceHierarchyTest.MyDataClass"
                      }, {
                        "type" : "null"
                      } ]
                    }
                  }
                },
                "required" : [ "data" ]
              },
              "OpenApiSealedInterfaceHierarchyTest.MyDataClass" : {
                "type" : "object",
                "x-source-kotlin-type" : "it.neckar.ktor.server.openapi.ktor.OpenApiSealedInterfaceHierarchyTest.MyDataClass",
                "properties" : {
                  "value" : {
                    "type" : "string"
                  }
                },
                "required" : [ "value" ]
              },
              "OpenApiSealedInterfaceHierarchyTest.MyTestResponse.Failed.NotFound" : {
                "type" : "object",
                "x-source-kotlin-type" : "it.neckar.ktor.server.openapi.ktor.OpenApiSealedInterfaceHierarchyTest.MyTestResponse.Failed.NotFound",
                "required" : [ "type" ],
                "properties" : {
                  "type" : {
                    "type" : "string",
                    "enum" : [ "bad/NotFound" ]
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


  /**
   * Response for
   */
  @Serializable
  private sealed interface MyTestResponse : RestResponse<List<MyDataClass?>> {

    @Serializable
    @SerialName("good")
    data class Good(override val data: List<MyDataClass?>) : MyTestResponse, RestResponse.GoodResponse<List<MyDataClass?>>

    sealed interface Failed : MyTestResponse, RestResponse.BadResponse<List<MyDataClass?>> {

      @Serializable
      @SerialName("bad/NotFound")
      data object NotFound : Failed
    }

  }

  @Serializable
  private data class MyDataClass(val value: String)

}
