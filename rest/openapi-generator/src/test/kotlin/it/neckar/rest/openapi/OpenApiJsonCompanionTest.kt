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
import it.neckar.open.serialization.isJsonEqualTo
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.Test
import it.neckar.rest.openapi.documentation.DocumentationProvider
import it.neckar.rest.openapi.schema.SchemaDescriptor

/**
 *
 */
class OpenApiJsonCompanionTest {
  @Test
  fun testClassItself() {
    val jsonGenerator = OpenApiJsonGenerator(documentationProvider = DocumentationProvider.None)
    val schemaType = SchemaDescriptor.of(Foobar::class)

    jsonGenerator.generateComponentsSchemas(
      listOf(
        SchemaDescriptor.of(Foobar::class),
      )
    ).let {
      assertThat(it).isJsonEqualTo(
        //language=JSON
        """
          {
            "OpenApiJsonCompanionTest.Foobar" : {
              "type" : "object",
              "x-source-kotlin-type" : "it.neckar.rest.openapi.OpenApiJsonCompanionTest.Foobar",
              "properties" : {
                "value" : {
                  "type" : "string"
                }
              },
              "required" : [ "value" ]
            }
          }
        """.trimIndent()
      )
    }
  }

  @Test
  fun testCompanionConstants() {
    val jsonGenerator = OpenApiJsonGenerator(documentationProvider = DocumentationProvider.None)

    assertThat(
      jsonGenerator.generateComponentsSchemas(
        listOf(
          SchemaDescriptor.of(Foobar.Companion::class),
        )
      )
    ).isJsonEqualTo(
      //language=JSON
      """
          {
            "OpenApiJsonCompanionTest.Foobar.Companion" : {
              "type" : "object",
              "x-source-kotlin-type" : "it.neckar.rest.openapi.OpenApiJsonCompanionTest.Foobar.Companion",
              "required" : [ "Foobar1", "Foobar2", "Foobar3" ],
              "properties" : {
                "Foobar1" : {
                  "type" : "string",
                  "x-source-kotlin-type" : "it.neckar.rest.openapi.OpenApiJsonCompanionTest.Foobar",
                  "const" : "Foobar(value=constant1)"
                },
                "Foobar2" : {
                  "type" : "string",
                  "x-source-kotlin-type" : "it.neckar.rest.openapi.OpenApiJsonCompanionTest.Foobar",
                  "const" : "Foobar(value=constant2)"
                },
                "Foobar3" : {
                  "type" : "string",
                  "x-source-kotlin-type" : "it.neckar.rest.openapi.OpenApiJsonCompanionTest.Foobar",
                  "const" : "Foobar(value=constant3)"
                }
              }
            }
          }
        """.trimIndent()
    )
  }

  @Test
  fun testComplete() {
    val jsonGenerator = OpenApiJsonGenerator(documentationProvider = DocumentationProvider.None)

    assertThat(
      jsonGenerator.generate(
        routes = emptyList(),
        additionalTypes = listOf(SchemaDescriptor.of(Foobar.Companion::class)),
        metaData = OpenApiMetaData.default,
      )
    ).isJsonEqualTo(
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
              "OpenApiJsonCompanionTest.Foobar.Companion" : {
                "type" : "object",
                "x-source-kotlin-type" : "it.neckar.rest.openapi.OpenApiJsonCompanionTest.Foobar.Companion",
                "required" : [ "Foobar1", "Foobar2", "Foobar3" ],
                "properties" : {
                  "Foobar1" : {
                    "type" : "string",
                    "x-source-kotlin-type" : "it.neckar.rest.openapi.OpenApiJsonCompanionTest.Foobar",
                    "const" : "Foobar(value=constant1)"
                  },
                  "Foobar2" : {
                    "type" : "string",
                    "x-source-kotlin-type" : "it.neckar.rest.openapi.OpenApiJsonCompanionTest.Foobar",
                    "const" : "Foobar(value=constant2)"
                  },
                  "Foobar3" : {
                    "type" : "string",
                    "x-source-kotlin-type" : "it.neckar.rest.openapi.OpenApiJsonCompanionTest.Foobar",
                    "const" : "Foobar(value=constant3)"
                  }
                }
              }
            }
          }
        }
      """.trimIndent()
    )
  }

  //2025-10-18 private does not work because of
  @Serializable
  internal data class Foobar(val value: String) {
    internal companion object {
      val Foobar1 = Foobar("constant1")
      val Foobar2 = Foobar("constant2")
      val Foobar3 = Foobar("constant3")
    }
  }
}
