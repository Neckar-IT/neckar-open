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
 * Tests for Schema-Level Examples support.
 *
 * Schema-Level Examples allow attaching examples directly to schema definitions
 * in `components/schemas`, rather than only at route-level (request/response bodies).
 *
 * Benefit: When a schema is used in multiple places, the example only needs to be
 * defined once and appears automatically everywhere the schema is referenced.
 */
class SchemaLevelExamplesTest {

  /**
   * Test that demonstrates the desired API for schema-level examples.
   *
   * The example should appear in the generated OpenAPI JSON under:
   * `components.schemas.MyDataClass.examples`
   */
  @Test
  fun `schema with example from example object`() {
    val jsonGenerator = OpenApiJsonGenerator(documentationProvider = DocumentationProvider.None)

    val schemaWithExamples = SchemaDescriptor.referenceObject<MyDataClass> {
      example("default", MyDataClassExample.Default) {
        summary = "Standard-Beispiel"
        description = "Ein typisches Beispiel für MyDataClass"
      }
    }

    val json = jsonGenerator.generateComponentsSchemas(listOf(schemaWithExamples))

    assertThat(json).isJsonEqualTo(
      //language=JSON
      $$"""
        {
          "SchemaLevelExamplesTest.MyDataClass" : {
            "type" : "object",
            "x-source-kotlin-type" : "it.neckar.rest.openapi.SchemaLevelExamplesTest.MyDataClass",
            "properties" : {
              "name" : {
                "type" : "string"
              },
              "value" : {
                "type" : "integer"
              }
            },
            "required" : [ "name", "value" ],
            "x-examples" : {
              "default" : {
                "summary" : "Standard-Beispiel",
                "description" : "Ein typisches Beispiel für MyDataClass",
                "x-source-kotlin-type" : "it.neckar.rest.openapi.SchemaLevelExamplesTest.MyDataClass",
                "value" : {
                  "name" : "Test",
                  "value" : 42
                }
              }
            }
          }
        }
      """.trimIndent()
    )
  }

  /**
   * Test that a schema without examples does not have an `examples` key in the output.
   * This verifies the baseline behavior.
   */
  @Test
  fun `schema without examples has no examples key`() {
    val jsonGenerator = OpenApiJsonGenerator(documentationProvider = DocumentationProvider.None)

    val schemaWithoutExamples = SchemaDescriptor.referenceObject<MyDataClass>()
    val json = jsonGenerator.generateComponentsSchemas(listOf(schemaWithoutExamples))

    assertThat(json).isJsonEqualTo(
      //language=JSON
      $$"""
        {
          "SchemaLevelExamplesTest.MyDataClass" : {
            "type" : "object",
            "x-source-kotlin-type" : "it.neckar.rest.openapi.SchemaLevelExamplesTest.MyDataClass",
            "properties" : {
              "name" : {
                "type" : "string"
              },
              "value" : {
                "type" : "integer"
              }
            },
            "required" : [ "name", "value" ]
          }
        }
      """.trimIndent()
    )
  }

  /**
   * Test with multiple examples on the same schema.
   */
  @Test
  fun `schema with multiple examples`() {
    val jsonGenerator = OpenApiJsonGenerator(documentationProvider = DocumentationProvider.None)

    val schemaWithExamples = SchemaDescriptor.referenceObject<MyDataClass> {
      example("minimal", MyDataClassExample.Minimal) {
        summary = "Minimales Beispiel"
      }
      example("typical", MyDataClassExample.Default) {
        summary = "Typisches Beispiel"
      }
      example("max-values", MyDataClassExample.MaxValues) {
        summary = "Maximale Werte"
      }
    }

    val json = jsonGenerator.generateComponentsSchemas(listOf(schemaWithExamples))

    assertThat(json).isJsonEqualTo(
      //language=JSON
      $$"""
        {
          "SchemaLevelExamplesTest.MyDataClass" : {
            "type" : "object",
            "x-source-kotlin-type" : "it.neckar.rest.openapi.SchemaLevelExamplesTest.MyDataClass",
            "properties" : {
              "name" : {
                "type" : "string"
              },
              "value" : {
                "type" : "integer"
              }
            },
            "required" : [ "name", "value" ],
            "x-examples" : {
              "minimal" : {
                "summary" : "Minimales Beispiel",
                "x-source-kotlin-type" : "it.neckar.rest.openapi.SchemaLevelExamplesTest.MyDataClass",
                "value" : {
                  "name" : "A",
                  "value" : 1
                }
              },
              "typical" : {
                "summary" : "Typisches Beispiel",
                "x-source-kotlin-type" : "it.neckar.rest.openapi.SchemaLevelExamplesTest.MyDataClass",
                "value" : {
                  "name" : "Test",
                  "value" : 42
                }
              },
              "max-values" : {
                "summary" : "Maximale Werte",
                "x-source-kotlin-type" : "it.neckar.rest.openapi.SchemaLevelExamplesTest.MyDataClass",
                "value" : {
                  "name" : "Maximum",
                  "value" : 2147483647
                }
              }
            }
          }
        }
      """.trimIndent()
    )
  }

  /**
   * Test data class.
   * Note: `private` visibility would be preferred, but kotlinx.serialization requires at least `internal`.
   */
  @Serializable
  internal data class MyDataClass(
    val name: String,
    val value: Int,
  )

  /**
   * Example instances for [MyDataClass].
   *
   * Following the project's Example object pattern (separate object, not companion object).
   * See [it.neckar.customer.company.CompanyExample] for reference.
   */
  private object MyDataClassExample {
    val Default: MyDataClass = MyDataClass(
      name = "Test",
      value = 42,
    )

    val Minimal: MyDataClass = MyDataClass(
      name = "A",
      value = 1,
    )

    val MaxValues: MyDataClass = MyDataClass(
      name = "Maximum",
      value = Int.MAX_VALUE,
    )
  }
}
