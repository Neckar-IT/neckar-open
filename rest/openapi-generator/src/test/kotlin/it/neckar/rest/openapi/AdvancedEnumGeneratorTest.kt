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
package it.neckar.rest.openapi

import assertk.*
import assertk.assertions.*
import it.neckar.open.annotations.ExportAsObject
import it.neckar.open.serialization.isJsonEqualTo
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import org.junit.jupiter.api.Test
import it.neckar.rest.openapi.documentation.DocumentationProvider
import it.neckar.rest.openapi.schema.SchemaDescriptor
import it.neckar.rest.openapi.schema.isEnumEntry

/**
 *
 */
class AdvancedEnumGeneratorTest {
  @Test
  fun testSchemaRef() {
    val jsonGenerator = OpenApiJsonGenerator(documentationProvider = DocumentationProvider.None)
    val json = jsonGenerator.generateSchemaTypeRef(SchemaDescriptor.of(MyTestHost::class), emptyList())
    assertThat(json).isJsonEqualTo(
      //language=JSON
      $$"""
        {
          "schema" : {
            "$ref" : "#/components/schemas/AdvancedEnumGeneratorTest.MyTestHost"
          }
        }
      """.trimIndent()
    )
  }

  @Test
  fun testCompleteWithEnum() {
    val jsonGenerator = OpenApiJsonGenerator(documentationProvider = DocumentationProvider.None)

    val json = jsonGenerator.generate(emptyList(), listOf(SchemaDescriptor.enum(MyTestHost::class)), OpenApiMetaData("test", "foobar"))
    assertThat(json).isJsonEqualTo(
      //language=JSON
      """
        {
          "openapi" : "3.1.0",
          "info" : {
            "title" : "test",
            "description" : "foobar",
            "version" : "1.0.0"
          },
          "servers" : [ ],
          "paths" : { },
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
              "AdvancedEnumGeneratorTest.MyTestHost" : {
                "type" : "string",
                "enum" : [ "Localhost", "Dev" ]
              }
            }
          }
        }
      """.trimIndent()
    )
  }

  @Test
  fun testMyEnumEnumWithoutEntries() {
    val jsonGenerator = OpenApiJsonGenerator(documentationProvider = DocumentationProvider.None)

    val schemaObject = SchemaDescriptor.discriminatedEnum(MyTestEnum::class)

    buildJsonObject {
      jsonGenerator.buildSchemaType(schemaObject) {
        assertThat(it.isEnumEntry()).isTrue()
      }
    }.let { jsonObject ->
      assertThat(jsonObject).isJsonEqualTo(
        //language=JSON
        $$"""
          {
            "AdvancedEnumGeneratorTest.MyTestEnum" : {
              "type" : "object",
              "x-source-kotlin-type" : "it.neckar.rest.openapi.AdvancedEnumGeneratorTest.MyTestEnum",
              "oneOf" : [ {
                "$ref" : "#/components/schemas/AdvancedEnumGeneratorTest.MyTestEnum.A"
              }, {
                "$ref" : "#/components/schemas/AdvancedEnumGeneratorTest.MyTestEnum.B"
              }, {
                "$ref" : "#/components/schemas/AdvancedEnumGeneratorTest.MyTestEnum.C"
              } ],
              "discriminator" : {
                "propertyName" : "name",
                "mapping" : {
                  "A" : "#/components/schemas/AdvancedEnumGeneratorTest.MyTestEnum.A",
                  "B" : "#/components/schemas/AdvancedEnumGeneratorTest.MyTestEnum.B",
                  "C" : "#/components/schemas/AdvancedEnumGeneratorTest.MyTestEnum.C"
                }
              }
            }
          }
        """.trimIndent()
      )
    }
  }

  @Test
  fun testMyTestEnumEntries() {
    val jsonGenerator = OpenApiJsonGenerator(documentationProvider = DocumentationProvider.None)

    val schemaObject = SchemaDescriptor.discriminatedEnum(MyTestEnum::class)

    //Test for a single value!
    buildJsonObject {
      jsonGenerator.buildSchemaType(schemaObject) {
        //ignore
      }
    }.let { jsonObject ->
      assertThat(jsonObject).isJsonEqualTo(
        //language=JSON
          $$"""
            {
              "AdvancedEnumGeneratorTest.MyTestEnum" : {
                "type" : "object",
                "x-source-kotlin-type" : "it.neckar.rest.openapi.AdvancedEnumGeneratorTest.MyTestEnum",
                "oneOf" : [ {
                  "$ref" : "#/components/schemas/AdvancedEnumGeneratorTest.MyTestEnum.A"
                }, {
                  "$ref" : "#/components/schemas/AdvancedEnumGeneratorTest.MyTestEnum.B"
                }, {
                  "$ref" : "#/components/schemas/AdvancedEnumGeneratorTest.MyTestEnum.C"
                } ],
                "discriminator" : {
                  "propertyName" : "name",
                  "mapping" : {
                    "A" : "#/components/schemas/AdvancedEnumGeneratorTest.MyTestEnum.A",
                    "B" : "#/components/schemas/AdvancedEnumGeneratorTest.MyTestEnum.B",
                    "C" : "#/components/schemas/AdvancedEnumGeneratorTest.MyTestEnum.C"
                  }
                }
              }
            }
          """.trimIndent()
      )
    }

    val json = jsonGenerator.generate(routes = emptyList(), additionalTypes = listOf(schemaObject), metaData = OpenApiMetaData("test", "foobar"))


    //All together! with the enum type
    assertThat(json).isJsonEqualTo(
      //language=JSON
      $$"""
        {
          "openapi" : "3.1.0",
          "info" : {
            "title" : "test",
            "description" : "foobar",
            "version" : "1.0.0"
          },
          "servers" : [ ],
          "paths" : { },
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
              "AdvancedEnumGeneratorTest.MyTestEnum" : {
                "type" : "object",
                "x-source-kotlin-type" : "it.neckar.rest.openapi.AdvancedEnumGeneratorTest.MyTestEnum",
                "oneOf" : [ {
                  "$ref" : "#/components/schemas/AdvancedEnumGeneratorTest.MyTestEnum.A"
                }, {
                  "$ref" : "#/components/schemas/AdvancedEnumGeneratorTest.MyTestEnum.B"
                }, {
                  "$ref" : "#/components/schemas/AdvancedEnumGeneratorTest.MyTestEnum.C"
                } ],
                "discriminator" : {
                  "propertyName" : "name",
                  "mapping" : {
                    "A" : "#/components/schemas/AdvancedEnumGeneratorTest.MyTestEnum.A",
                    "B" : "#/components/schemas/AdvancedEnumGeneratorTest.MyTestEnum.B",
                    "C" : "#/components/schemas/AdvancedEnumGeneratorTest.MyTestEnum.C"
                  }
                }
              },
              "AdvancedEnumGeneratorTest.MyTestEnum.A" : {
                "type" : "object",
                "x-source-kotlin-type" : "it.neckar.rest.openapi.AdvancedEnumGeneratorTest.MyTestEnum",
                "required" : [ "foo", "name" ],
                "properties" : {
                  "foo" : {
                    "type" : "string",
                    "x-source-kotlin-type" : "kotlin.String",
                    "const" : "avalue"
                  },
                  "name" : {
                    "type" : "string",
                    "x-source-kotlin-type" : "kotlin.String",
                    "const" : "A"
                  }
                },
                "x-ignored-properties" : [ "ordinal" ]
              },
              "AdvancedEnumGeneratorTest.MyTestEnum.B" : {
                "type" : "object",
                "x-source-kotlin-type" : "it.neckar.rest.openapi.AdvancedEnumGeneratorTest.MyTestEnum",
                "required" : [ "foo", "name" ],
                "properties" : {
                  "foo" : {
                    "type" : "string",
                    "x-source-kotlin-type" : "kotlin.String",
                    "const" : "bvalue"
                  },
                  "name" : {
                    "type" : "string",
                    "x-source-kotlin-type" : "kotlin.String",
                    "const" : "B"
                  }
                },
                "x-ignored-properties" : [ "ordinal" ]
              },
              "AdvancedEnumGeneratorTest.MyTestEnum.C" : {
                "type" : "object",
                "x-source-kotlin-type" : "it.neckar.rest.openapi.AdvancedEnumGeneratorTest.MyTestEnum",
                "required" : [ "foo", "name" ],
                "properties" : {
                  "foo" : {
                    "type" : "string",
                    "x-source-kotlin-type" : "kotlin.String",
                    "const" : "cvalue"
                  },
                  "name" : {
                    "type" : "string",
                    "x-source-kotlin-type" : "kotlin.String",
                    "const" : "C"
                  }
                },
                "x-ignored-properties" : [ "ordinal" ]
              }
            }
          }
        }
      """.trimIndent()
    )
  }

  @Test
  fun testCompleteWithEnumEntries() {
    val jsonGenerator = OpenApiJsonGenerator(documentationProvider = DocumentationProvider.None)

    val schemaDescriptor = SchemaDescriptor.discriminatedEnum<MyTestHost>()
    assertThat(schemaDescriptor).isInstanceOf(SchemaDescriptor.ComponentEnumSchemaObject::class)


    buildJsonObject {
      jsonGenerator.buildSchemaType(schemaDescriptor.asDiscriminatedEnum()) {
        assertThat(it).isInstanceOf(SchemaDescriptor.EnumEntrySchema::class)
      }
    }.let { jsonObject ->
      assertThat(jsonObject).isJsonEqualTo(
        //language=JSON
        $$"""
          {
            "AdvancedEnumGeneratorTest.MyTestHost" : {
              "type" : "object",
              "x-source-kotlin-type" : "it.neckar.rest.openapi.AdvancedEnumGeneratorTest.MyTestHost",
              "oneOf" : [ {
                "$ref" : "#/components/schemas/AdvancedEnumGeneratorTest.MyTestHost.Localhost"
              }, {
                "$ref" : "#/components/schemas/AdvancedEnumGeneratorTest.MyTestHost.Dev"
              } ],
              "discriminator" : {
                "propertyName" : "name",
                "mapping" : {
                  "Localhost" : "#/components/schemas/AdvancedEnumGeneratorTest.MyTestHost.Localhost",
                  "Dev" : "#/components/schemas/AdvancedEnumGeneratorTest.MyTestHost.Dev"
                }
              }
            }
          }
        """.trimIndent()
      )
    }


    assertThat(jsonGenerator.generateComponentsSchemas(listOf(schemaDescriptor))).isJsonEqualTo(
      //language=JSON
      $$"""
        {
          "AdvancedEnumGeneratorTest.MyTestHost" : {
            "type" : "object",
            "x-source-kotlin-type" : "it.neckar.rest.openapi.AdvancedEnumGeneratorTest.MyTestHost",
            "oneOf" : [ {
              "$ref" : "#/components/schemas/AdvancedEnumGeneratorTest.MyTestHost.Localhost"
            }, {
              "$ref" : "#/components/schemas/AdvancedEnumGeneratorTest.MyTestHost.Dev"
            } ],
            "discriminator" : {
              "propertyName" : "name",
              "mapping" : {
                "Localhost" : "#/components/schemas/AdvancedEnumGeneratorTest.MyTestHost.Localhost",
                "Dev" : "#/components/schemas/AdvancedEnumGeneratorTest.MyTestHost.Dev"
              }
            }
          },
          "AdvancedEnumGeneratorTest.MyTestHost.Localhost" : {
            "type" : "object",
            "x-source-kotlin-type" : "it.neckar.rest.openapi.AdvancedEnumGeneratorTest.MyTestHost",
            "required" : [ "environment", "foo", "hostname", "name" ],
            "properties" : {
              "environment" : {
                "type" : "string",
                "x-source-kotlin-type" : "it.neckar.rest.openapi.AdvancedEnumGeneratorTest.MyTestHost.Environment",
                "const" : "Development"
              },
              "foo" : {
                "type" : "integer",
                "x-source-kotlin-type" : "kotlin.Int?",
                "const" : 17
              },
              "hostname" : {
                "type" : "string",
                "x-source-kotlin-type" : "kotlin.String",
                "const" : "localhost"
              },
              "name" : {
                "type" : "string",
                "x-source-kotlin-type" : "kotlin.String",
                "const" : "Localhost"
              }
            },
            "x-ignored-properties" : [ "ordinal" ]
          },
          "AdvancedEnumGeneratorTest.MyTestHost.Dev" : {
            "type" : "object",
            "x-source-kotlin-type" : "it.neckar.rest.openapi.AdvancedEnumGeneratorTest.MyTestHost",
            "required" : [ "environment", "hostname", "name" ],
            "properties" : {
              "environment" : {
                "type" : "string",
                "x-source-kotlin-type" : "it.neckar.rest.openapi.AdvancedEnumGeneratorTest.MyTestHost.Environment",
                "const" : "Development"
              },
              "hostname" : {
                "type" : "string",
                "x-source-kotlin-type" : "kotlin.String",
                "const" : "dev.app.turboangebot.de"
              },
              "name" : {
                "type" : "string",
                "x-source-kotlin-type" : "kotlin.String",
                "const" : "Dev"
              }
            },
            "x-ignored-properties" : [ "ordinal" ]
          }
        }
      """.trimIndent()
    )


    val json = jsonGenerator.generate(routes = emptyList(), additionalTypes = listOf(schemaDescriptor), metaData = OpenApiMetaData("test", "foobar"))
    assertThat(json).isJsonEqualTo(
      //language=JSON
      $$"""
        {
          "openapi" : "3.1.0",
          "info" : {
            "title" : "test",
            "description" : "foobar",
            "version" : "1.0.0"
          },
          "servers" : [ ],
          "paths" : { },
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
              "AdvancedEnumGeneratorTest.MyTestHost" : {
                "type" : "object",
                "x-source-kotlin-type" : "it.neckar.rest.openapi.AdvancedEnumGeneratorTest.MyTestHost",
                "oneOf" : [ {
                  "$ref" : "#/components/schemas/AdvancedEnumGeneratorTest.MyTestHost.Localhost"
                }, {
                  "$ref" : "#/components/schemas/AdvancedEnumGeneratorTest.MyTestHost.Dev"
                } ],
                "discriminator" : {
                  "propertyName" : "name",
                  "mapping" : {
                    "Localhost" : "#/components/schemas/AdvancedEnumGeneratorTest.MyTestHost.Localhost",
                    "Dev" : "#/components/schemas/AdvancedEnumGeneratorTest.MyTestHost.Dev"
                  }
                }
              },
              "AdvancedEnumGeneratorTest.MyTestHost.Localhost" : {
                "type" : "object",
                "x-source-kotlin-type" : "it.neckar.rest.openapi.AdvancedEnumGeneratorTest.MyTestHost",
                "required" : [ "environment", "foo", "hostname", "name" ],
                "properties" : {
                  "environment" : {
                    "type" : "string",
                    "x-source-kotlin-type" : "it.neckar.rest.openapi.AdvancedEnumGeneratorTest.MyTestHost.Environment",
                    "const" : "Development"
                  },
                  "foo" : {
                    "type" : "integer",
                    "x-source-kotlin-type" : "kotlin.Int?",
                    "const" : 17
                  },
                  "hostname" : {
                    "type" : "string",
                    "x-source-kotlin-type" : "kotlin.String",
                    "const" : "localhost"
                  },
                  "name" : {
                    "type" : "string",
                    "x-source-kotlin-type" : "kotlin.String",
                    "const" : "Localhost"
                  }
                },
                "x-ignored-properties" : [ "ordinal" ]
              },
              "AdvancedEnumGeneratorTest.MyTestHost.Dev" : {
                "type" : "object",
                "x-source-kotlin-type" : "it.neckar.rest.openapi.AdvancedEnumGeneratorTest.MyTestHost",
                "required" : [ "environment", "hostname", "name" ],
                "properties" : {
                  "environment" : {
                    "type" : "string",
                    "x-source-kotlin-type" : "it.neckar.rest.openapi.AdvancedEnumGeneratorTest.MyTestHost.Environment",
                    "const" : "Development"
                  },
                  "hostname" : {
                    "type" : "string",
                    "x-source-kotlin-type" : "kotlin.String",
                    "const" : "dev.app.turboangebot.de"
                  },
                  "name" : {
                    "type" : "string",
                    "x-source-kotlin-type" : "kotlin.String",
                    "const" : "Dev"
                  }
                },
                "x-ignored-properties" : [ "ordinal" ]
              }
            }
          }
        }
      """.trimIndent()
    )
  }

  @Test
  fun testEnumAsSimpleEnum() {
    assertThat(OpenApiJsonGenerator(documentationProvider = DocumentationProvider.None).generateComponentsSchemas(listOf(SchemaDescriptor.enum(MyTestHost::class))))
      .isJsonEqualTo(
        //language=JSON
        $$"""
        {
          "AdvancedEnumGeneratorTest.MyTestHost" : {
            "type" : "string",
            "enum" : [ "Localhost", "Dev" ]
          }
        }
      """.trimIndent()
      )
  }

  @Test
  fun testEnum() {
    val jsonGenerator = OpenApiJsonGenerator(documentationProvider = DocumentationProvider.None)

    val json = jsonGenerator.generateComponentsSchemas(listOf(SchemaDescriptor.of(MyTestHost::class)))
    assertThat(json).isJsonEqualTo(
      //language=JSON
      $$"""
        {
          "AdvancedEnumGeneratorTest.MyTestHost" : {
            "type" : "object",
            "x-source-kotlin-type" : "it.neckar.rest.openapi.AdvancedEnumGeneratorTest.MyTestHost",
            "oneOf" : [ {
              "$ref" : "#/components/schemas/AdvancedEnumGeneratorTest.MyTestHost.Localhost"
            }, {
              "$ref" : "#/components/schemas/AdvancedEnumGeneratorTest.MyTestHost.Dev"
            } ],
            "discriminator" : {
              "propertyName" : "name",
              "mapping" : {
                "Localhost" : "#/components/schemas/AdvancedEnumGeneratorTest.MyTestHost.Localhost",
                "Dev" : "#/components/schemas/AdvancedEnumGeneratorTest.MyTestHost.Dev"
              }
            }
          },
          "AdvancedEnumGeneratorTest.MyTestHost.Localhost" : {
            "type" : "object",
            "x-source-kotlin-type" : "it.neckar.rest.openapi.AdvancedEnumGeneratorTest.MyTestHost",
            "required" : [ "environment", "foo", "hostname", "name" ],
            "properties" : {
              "environment" : {
                "type" : "string",
                "x-source-kotlin-type" : "it.neckar.rest.openapi.AdvancedEnumGeneratorTest.MyTestHost.Environment",
                "const" : "Development"
              },
              "foo" : {
                "type" : "integer",
                "x-source-kotlin-type" : "kotlin.Int?",
                "const" : 17
              },
              "hostname" : {
                "type" : "string",
                "x-source-kotlin-type" : "kotlin.String",
                "const" : "localhost"
              },
              "name" : {
                "type" : "string",
                "x-source-kotlin-type" : "kotlin.String",
                "const" : "Localhost"
              }
            },
            "x-ignored-properties" : [ "ordinal" ]
          },
          "AdvancedEnumGeneratorTest.MyTestHost.Dev" : {
            "type" : "object",
            "x-source-kotlin-type" : "it.neckar.rest.openapi.AdvancedEnumGeneratorTest.MyTestHost",
            "required" : [ "environment", "hostname", "name" ],
            "properties" : {
              "environment" : {
                "type" : "string",
                "x-source-kotlin-type" : "it.neckar.rest.openapi.AdvancedEnumGeneratorTest.MyTestHost.Environment",
                "const" : "Development"
              },
              "hostname" : {
                "type" : "string",
                "x-source-kotlin-type" : "kotlin.String",
                "const" : "dev.app.turboangebot.de"
              },
              "name" : {
                "type" : "string",
                "x-source-kotlin-type" : "kotlin.String",
                "const" : "Dev"
              }
            },
            "x-ignored-properties" : [ "ordinal" ]
          }
        }
      """.trimIndent()
    )
  }


  @ExportAsObject
  @Serializable
  private enum class MyTestHost(
    /**
     * The hostname
     */
    val hostname: String,

    val environment: Environment,

    val foo: Int?,
  ) {
    /**
     * For development on the local machine
     */
    Localhost(hostname = "localhost", environment = Environment.Development, foo = 17),

    /**
     * Contains the main branch of the service.
     */
    Dev(hostname = "dev.app.turboangebot.de", environment = Environment.Development, foo = null),
    ;

    /**
     * The environment where the service has been started in
     */
    enum class Environment {
      /**
       * Production - use the production database
       */
      Production,

      /**
       * (Local) development (usually during development on localhost)
       */
      Development,
      ;
    }
  }

  @Serializable
  private enum class MyTestEnum(val foo: String) {
    A("avalue"), B("bvalue"), C("cvalue")
  }
}
