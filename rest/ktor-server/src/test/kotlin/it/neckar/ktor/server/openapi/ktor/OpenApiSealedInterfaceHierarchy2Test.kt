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
import it.neckar.ktor.collectAllRoutes
import it.neckar.open.test.utils.isJsonEqualTo
import it.neckar.open.test.utils.isOpenApiEqualTo
import it.neckar.rest.RestResponse
import it.neckar.rest.openapi.documentation.DocumentationProvider
import it.neckar.rest.openapi.OpenApiJsonGenerator
import it.neckar.rest.openapi.OpenApiMetaData
import it.neckar.rest.openapi.schema.SchemaDescriptor
import it.neckar.rest.openapi.schema.SchemaPropertyConstraints
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import it.neckar.open.test.utils.DisableLogging
import kotlin.reflect.full.starProjectedType

/**
 *
 */
@DisableLogging
class OpenApiSealedInterfaceHierarchy2Test {
  private lateinit var generator: OpenApiJsonGenerator

  @BeforeEach
  fun setUp() {
    generator = OpenApiJsonGenerator(documentationProvider = DocumentationProvider.None)
  }

  @Test
  fun testType() {
    val additionalTypes = mutableListOf<SchemaDescriptor>()

    val jsonObject = buildJsonObject {
      OpenApiJsonGenerator(documentationProvider = DocumentationProvider.None).buildSealedClassPropertyElement(SealedInterface.serializer().descriptor, null, SealedInterface::class.starProjectedType, SchemaPropertyConstraints.Required, { type: SchemaDescriptor ->
        additionalTypes.add(type)
      })
    }

    assertThat(jsonObject.toString()).isJsonEqualTo(
      //language=JSON
      $$"""
        {
          "$ref" : "#/components/schemas/OpenApiSealedInterfaceHierarchy2Test.SealedInterface"
        }
      """.trimIndent()
    )

    assertThat(additionalTypes).hasSize(1)
  }

  @Test
  fun testComplete() = testApplication {
    routing {

      get("foobar") {
        TODO()
      }.doc {
        respondingNoExample<MyTestResponse>()
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
                          "$ref" : "#/components/schemas/OpenApiSealedInterfaceHierarchy2Test.MyTestResponse"
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
              "OpenApiSealedInterfaceHierarchy2Test.MyTestResponse" : {
                "type" : "object",
                "x-source-kotlin-type" : "it.neckar.ktor.server.openapi.ktor.OpenApiSealedInterfaceHierarchy2Test.MyTestResponse",
                "oneOf" : [ {
                  "$ref" : "#/components/schemas/OpenApiSealedInterfaceHierarchy2Test.MyTestResponse.Failed.NotFound"
                }, {
                  "$ref" : "#/components/schemas/OpenApiSealedInterfaceHierarchy2Test.MyTestResponse.Good.WithType"
                } ],
                "discriminator" : {
                  "propertyName" : "type",
                  "mapping" : {
                    "bad/NotFound" : "#/components/schemas/OpenApiSealedInterfaceHierarchy2Test.MyTestResponse.Failed.NotFound",
                    "good" : "#/components/schemas/OpenApiSealedInterfaceHierarchy2Test.MyTestResponse.Good.WithType"
                  }
                }
              },
              "OpenApiSealedInterfaceHierarchy2Test.MyTestResponse.Good.WithType" : {
                "type" : "object",
                "x-source-kotlin-type" : "it.neckar.ktor.server.openapi.ktor.OpenApiSealedInterfaceHierarchy2Test.MyTestResponse.Good",
                "allOf" : [ {
                  "$ref" : "#/components/schemas/OpenApiSealedInterfaceHierarchy2Test.MyTestResponse.Good"
                } ],
                "properties" : {
                  "type" : {
                    "type" : "string",
                    "enum" : [ "good" ]
                  }
                },
                "required" : [ "type" ]
              },
              "OpenApiSealedInterfaceHierarchy2Test.MyTestResponse.Good" : {
                "type" : "object",
                "x-source-kotlin-type" : "it.neckar.ktor.server.openapi.ktor.OpenApiSealedInterfaceHierarchy2Test.MyTestResponse.Good",
                "properties" : {
                  "data" : {
                    "$ref" : "#/components/schemas/OpenApiSealedInterfaceHierarchy2Test.SealedInterface"
                  }
                },
                "required" : [ "data" ]
              },
              "OpenApiSealedInterfaceHierarchy2Test.SealedInterface" : {
                "type" : "object",
                "x-source-kotlin-type" : "it.neckar.ktor.server.openapi.ktor.OpenApiSealedInterfaceHierarchy2Test.SealedInterface",
                "oneOf" : [ {
                  "$ref" : "#/components/schemas/OpenApiSealedInterfaceHierarchy2Test.SealedInterface.SealedInterface1.Child1.Child1Object"
                }, {
                  "$ref" : "#/components/schemas/OpenApiSealedInterfaceHierarchy2Test.SealedInterface.SealedInterface2.Child2.Child2Object"
                } ],
                "discriminator" : {
                  "propertyName" : "type",
                  "mapping" : {
                    "child1Object" : "#/components/schemas/OpenApiSealedInterfaceHierarchy2Test.SealedInterface.SealedInterface1.Child1.Child1Object",
                    "child2Object" : "#/components/schemas/OpenApiSealedInterfaceHierarchy2Test.SealedInterface.SealedInterface2.Child2.Child2Object"
                  }
                }
              },
              "OpenApiSealedInterfaceHierarchy2Test.SealedInterface.SealedInterface2.Child2.Child2Object" : {
                "type" : "object",
                "x-source-kotlin-type" : "it.neckar.ktor.server.openapi.ktor.OpenApiSealedInterfaceHierarchy2Test.SealedInterface.SealedInterface2.Child2.Child2Object",
                "required" : [ "type" ],
                "properties" : {
                  "type" : {
                    "type" : "string",
                    "enum" : [ "child2Object" ]
                  }
                }
              },
              "OpenApiSealedInterfaceHierarchy2Test.SealedInterface.SealedInterface1.Child1.Child1Object" : {
                "type" : "object",
                "x-source-kotlin-type" : "it.neckar.ktor.server.openapi.ktor.OpenApiSealedInterfaceHierarchy2Test.SealedInterface.SealedInterface1.Child1.Child1Object",
                "required" : [ "type" ],
                "properties" : {
                  "type" : {
                    "type" : "string",
                    "enum" : [ "child1Object" ]
                  }
                }
              },
              "OpenApiSealedInterfaceHierarchy2Test.MyTestResponse.Failed.NotFound" : {
                "type" : "object",
                "x-source-kotlin-type" : "it.neckar.ktor.server.openapi.ktor.OpenApiSealedInterfaceHierarchy2Test.MyTestResponse.Failed.NotFound",
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


  private interface NotASealedInterface

  @Serializable
  private sealed interface SealedInterface : NotASealedInterface {
    @Serializable
    sealed interface SealedInterface1 : SealedInterface, NotASealedInterface {
      @Serializable
      sealed interface Child1 : SealedInterface1 {
        @Serializable
        @SerialName("child1Object")
        data object Child1Object : Child1
      }
    }

    @Serializable
    sealed interface SealedInterface2 : SealedInterface, NotASealedInterface {
      @Serializable
      sealed interface Child2 : SealedInterface2 {
        @Serializable
        @SerialName("child2Object")
        data object Child2Object : Child2
      }
    }
  }

  /**
   * Response for
   */
  @Serializable
  private sealed interface MyTestResponse : RestResponse<SealedInterface> {

    @Serializable
    @SerialName("good")
    data class Good(override val data: SealedInterface) : MyTestResponse, RestResponse.GoodResponse<SealedInterface>

    sealed interface Failed : MyTestResponse, RestResponse.BadResponse<SealedInterface> {
      @Serializable
      @SerialName("bad/NotFound")
      data object NotFound : Failed
    }
  }
}
