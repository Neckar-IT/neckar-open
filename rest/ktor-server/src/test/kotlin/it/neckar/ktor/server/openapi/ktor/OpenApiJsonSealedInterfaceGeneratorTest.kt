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
import it.neckar.open.serialization.isOpenApiEqualTo
import it.neckar.rest.openapi.documentation.DocumentationProvider
import it.neckar.rest.openapi.OpenApiJsonGenerator
import it.neckar.rest.openapi.OpenApiMetaData
import it.neckar.rest.openapi.schema.SchemaDescriptor
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import it.neckar.open.test.utils.DisableLogging
import it.neckar.open.test.utils.isOpenApiEqualTo

/**
 *
 */
@DisableLogging
class OpenApiJsonSealedInterfaceGeneratorTest {
  private lateinit var generator: OpenApiJsonGenerator

  @BeforeEach
  fun setUp() {
    generator = OpenApiJsonGenerator(documentationProvider = DocumentationProvider.None)
  }

  @Test
  fun testGenerateSchemaType() {
    val schemaType = SchemaDescriptor.of(MySealedInterface::class)
    val json = generator.generateSchemaTypeRef(schemaType, emptyList())

    assertThat(json).isOpenApiEqualTo(
      //language=JSON
      $$"""
        {
          "schema" : {
            "$ref" : "#/components/schemas/OpenApiJsonSealedInterfaceGeneratorTest.MySealedInterface"
          }
        }
      """.trimIndent()
    )
  }

  @Test
  fun testComplete() = testApplication {
    routing {
      get("foobar") {
        TODO()
      }.doc {
        response("response using a sealed interface") {
          body<MySealedInterface> {
            description = "The sealed interface"
            example("foo", MySealedInterface.Foo, "da foo summary")
            example("bar", MySealedInterface.Bar, "da bar summary")
            example("my-dc", MySealedInterface.MyDC(MyDataClass("daName")), "da data class entry summary")
          }
        }
      }
    }

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
                "operationId" : "GetFoobar",
                "x-category" : "default",
                "parameters" : [ ],
                "responses" : {
                  "200" : {
                    "description" : "response using a sealed interface",
                    "content" : {
                      "application/json" : {
                        "schema" : {
                          "$ref" : "#/components/schemas/OpenApiJsonSealedInterfaceGeneratorTest.MySealedInterface"
                        },
                        "examples" : {
                          "foo" : {
                            "summary" : "da foo summary",
                            "x-source-kotlin-type" : "it.neckar.ktor.server.openapi.ktor.OpenApiJsonSealedInterfaceGeneratorTest.MySealedInterface.Foo",
                            "x-type-ref" : "#/components/schemas/OpenApiJsonSealedInterfaceGeneratorTest.MySealedInterface.Foo",
                            "value" : {
                              "type" : "foo"
                            }
                          },
                          "bar" : {
                            "summary" : "da bar summary",
                            "x-source-kotlin-type" : "it.neckar.ktor.server.openapi.ktor.OpenApiJsonSealedInterfaceGeneratorTest.MySealedInterface.Bar",
                            "x-type-ref" : "#/components/schemas/OpenApiJsonSealedInterfaceGeneratorTest.MySealedInterface.Bar",
                            "value" : {
                              "type" : "bar"
                            }
                          },
                          "my-dc" : {
                            "summary" : "da data class entry summary",
                            "x-source-kotlin-type" : "it.neckar.ktor.server.openapi.ktor.OpenApiJsonSealedInterfaceGeneratorTest.MySealedInterface.MyDC",
                            "x-type-ref" : "#/components/schemas/OpenApiJsonSealedInterfaceGeneratorTest.MySealedInterface.MyDC.WithType",
                            "value" : {
                              "daDataClass" : {
                                "name" : "daName"
                              },
                              "type" : "mydc"
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
              "OpenApiJsonSealedInterfaceGeneratorTest.MySealedInterface" : {
                "type" : "object",
                "x-source-kotlin-type" : "it.neckar.ktor.server.openapi.ktor.OpenApiJsonSealedInterfaceGeneratorTest.MySealedInterface",
                "oneOf" : [ {
                  "$ref" : "#/components/schemas/OpenApiJsonSealedInterfaceGeneratorTest.MySealedInterface.Bar"
                }, {
                  "$ref" : "#/components/schemas/OpenApiJsonSealedInterfaceGeneratorTest.MySealedInterface.Foo"
                }, {
                  "$ref" : "#/components/schemas/OpenApiJsonSealedInterfaceGeneratorTest.MySealedInterface.MyDC.WithType"
                } ],
                "discriminator" : {
                  "propertyName" : "type",
                  "mapping" : {
                    "bar" : "#/components/schemas/OpenApiJsonSealedInterfaceGeneratorTest.MySealedInterface.Bar",
                    "foo" : "#/components/schemas/OpenApiJsonSealedInterfaceGeneratorTest.MySealedInterface.Foo",
                    "mydc" : "#/components/schemas/OpenApiJsonSealedInterfaceGeneratorTest.MySealedInterface.MyDC.WithType"
                  }
                }
              },
              "OpenApiJsonSealedInterfaceGeneratorTest.MySealedInterface.MyDC.WithType" : {
                "type" : "object",
                "x-source-kotlin-type" : "it.neckar.ktor.server.openapi.ktor.OpenApiJsonSealedInterfaceGeneratorTest.MySealedInterface.MyDC",
                "allOf" : [ {
                  "$ref" : "#/components/schemas/OpenApiJsonSealedInterfaceGeneratorTest.MySealedInterface.MyDC"
                } ],
                "properties" : {
                  "type" : {
                    "type" : "string",
                    "enum" : [ "mydc" ]
                  }
                },
                "required" : [ "type" ]
              },
              "OpenApiJsonSealedInterfaceGeneratorTest.MySealedInterface.MyDC" : {
                "type" : "object",
                "x-source-kotlin-type" : "it.neckar.ktor.server.openapi.ktor.OpenApiJsonSealedInterfaceGeneratorTest.MySealedInterface.MyDC",
                "properties" : {
                  "daDataClass" : {
                    "$ref" : "#/components/schemas/OpenApiJsonSealedInterfaceGeneratorTest.MyDataClass"
                  }
                },
                "required" : [ "daDataClass" ]
              },
              "OpenApiJsonSealedInterfaceGeneratorTest.MyDataClass" : {
                "type" : "object",
                "x-source-kotlin-type" : "it.neckar.ktor.server.openapi.ktor.OpenApiJsonSealedInterfaceGeneratorTest.MyDataClass",
                "properties" : {
                  "name" : {
                    "type" : "string"
                  }
                },
                "required" : [ "name" ]
              },
              "OpenApiJsonSealedInterfaceGeneratorTest.MySealedInterface.Foo" : {
                "type" : "object",
                "x-source-kotlin-type" : "it.neckar.ktor.server.openapi.ktor.OpenApiJsonSealedInterfaceGeneratorTest.MySealedInterface.Foo",
                "required" : [ "type" ],
                "properties" : {
                  "type" : {
                    "type" : "string",
                    "enum" : [ "foo" ]
                  }
                }
              },
              "OpenApiJsonSealedInterfaceGeneratorTest.MySealedInterface.Bar" : {
                "type" : "object",
                "x-source-kotlin-type" : "it.neckar.ktor.server.openapi.ktor.OpenApiJsonSealedInterfaceGeneratorTest.MySealedInterface.Bar",
                "required" : [ "type" ],
                "properties" : {
                  "type" : {
                    "type" : "string",
                    "enum" : [ "bar" ]
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
  fun testCompleteNoExamples() = testApplication {
    routing {
      get("foobar") {
        TODO()
      }.doc {
        response("response using a sealed interface") {
          body<MySealedInterface> {
            description = "The sealed interface"
            noExamples()
          }
        }
      }
    }

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
                "operationId" : "GetFoobar",
                "x-category" : "default",
                "parameters" : [ ],
                "responses" : {
                  "200" : {
                    "description" : "response using a sealed interface",
                    "content" : {
                      "application/json" : {
                        "schema" : {
                          "$ref" : "#/components/schemas/OpenApiJsonSealedInterfaceGeneratorTest.MySealedInterface"
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
              "OpenApiJsonSealedInterfaceGeneratorTest.MySealedInterface" : {
                "type" : "object",
                "x-source-kotlin-type" : "it.neckar.ktor.server.openapi.ktor.OpenApiJsonSealedInterfaceGeneratorTest.MySealedInterface",
                "oneOf" : [ {
                  "$ref" : "#/components/schemas/OpenApiJsonSealedInterfaceGeneratorTest.MySealedInterface.Bar"
                }, {
                  "$ref" : "#/components/schemas/OpenApiJsonSealedInterfaceGeneratorTest.MySealedInterface.Foo"
                }, {
                  "$ref" : "#/components/schemas/OpenApiJsonSealedInterfaceGeneratorTest.MySealedInterface.MyDC.WithType"
                } ],
                "discriminator" : {
                  "propertyName" : "type",
                  "mapping" : {
                    "bar" : "#/components/schemas/OpenApiJsonSealedInterfaceGeneratorTest.MySealedInterface.Bar",
                    "foo" : "#/components/schemas/OpenApiJsonSealedInterfaceGeneratorTest.MySealedInterface.Foo",
                    "mydc" : "#/components/schemas/OpenApiJsonSealedInterfaceGeneratorTest.MySealedInterface.MyDC.WithType"
                  }
                }
              },
              "OpenApiJsonSealedInterfaceGeneratorTest.MySealedInterface.MyDC.WithType" : {
                "type" : "object",
                "x-source-kotlin-type" : "it.neckar.ktor.server.openapi.ktor.OpenApiJsonSealedInterfaceGeneratorTest.MySealedInterface.MyDC",
                "allOf" : [ {
                  "$ref" : "#/components/schemas/OpenApiJsonSealedInterfaceGeneratorTest.MySealedInterface.MyDC"
                } ],
                "properties" : {
                  "type" : {
                    "type" : "string",
                    "enum" : [ "mydc" ]
                  }
                },
                "required" : [ "type" ]
              },
              "OpenApiJsonSealedInterfaceGeneratorTest.MySealedInterface.MyDC" : {
                "type" : "object",
                "x-source-kotlin-type" : "it.neckar.ktor.server.openapi.ktor.OpenApiJsonSealedInterfaceGeneratorTest.MySealedInterface.MyDC",
                "properties" : {
                  "daDataClass" : {
                    "$ref" : "#/components/schemas/OpenApiJsonSealedInterfaceGeneratorTest.MyDataClass"
                  }
                },
                "required" : [ "daDataClass" ]
              },
              "OpenApiJsonSealedInterfaceGeneratorTest.MyDataClass" : {
                "type" : "object",
                "x-source-kotlin-type" : "it.neckar.ktor.server.openapi.ktor.OpenApiJsonSealedInterfaceGeneratorTest.MyDataClass",
                "properties" : {
                  "name" : {
                    "type" : "string"
                  }
                },
                "required" : [ "name" ]
              },
              "OpenApiJsonSealedInterfaceGeneratorTest.MySealedInterface.Foo" : {
                "type" : "object",
                "x-source-kotlin-type" : "it.neckar.ktor.server.openapi.ktor.OpenApiJsonSealedInterfaceGeneratorTest.MySealedInterface.Foo",
                "required" : [ "type" ],
                "properties" : {
                  "type" : {
                    "type" : "string",
                    "enum" : [ "foo" ]
                  }
                }
              },
              "OpenApiJsonSealedInterfaceGeneratorTest.MySealedInterface.Bar" : {
                "type" : "object",
                "x-source-kotlin-type" : "it.neckar.ktor.server.openapi.ktor.OpenApiJsonSealedInterfaceGeneratorTest.MySealedInterface.Bar",
                "required" : [ "type" ],
                "properties" : {
                  "type" : {
                    "type" : "string",
                    "enum" : [ "bar" ]
                  }
                }
              }
            }
          }
        }
      """.trimIndent()
    )
  }

  @Serializable
  private sealed interface MySealedInterface {
    @Serializable
    @SerialName("foo")
    data object Foo : MySealedInterface

    @Serializable
    @SerialName("bar")
    @Suppress("ConvertObjectToDataObject")
    object Bar : MySealedInterface

    @Serializable
    @SerialName("mydc")
    data class MyDC(val daDataClass: MyDataClass) : MySealedInterface
  }

  @Serializable
  data class MyDataClass(val name: String)
}
