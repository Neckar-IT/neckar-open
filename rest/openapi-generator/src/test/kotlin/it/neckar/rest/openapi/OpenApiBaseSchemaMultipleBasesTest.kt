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
import it.neckar.open.annotations.OpenApiBaseSchema
import it.neckar.open.serialization.isJsonEqualTo
import it.neckar.open.serialization.path
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.jsonObject
import org.junit.jupiter.api.Test
import it.neckar.rest.openapi.documentation.DocumentationProvider
import it.neckar.rest.openapi.schema.SchemaDescriptor

/**
 * The data class ([MultiBaseItem]) implements two `@OpenApiBaseSchema` interfaces ([Identifiable],
 * [Named]). Both bases must be registered as separate component schemas; the data class
 * composes them via `allOf` with one `$ref` per base, plus its own `extra` property inlined.
 */
class OpenApiBaseSchemaMultipleBasesTest {
  private val jsonGenerator = OpenApiJsonGenerator(documentationProvider = DocumentationProvider.None)

  @Test
  fun testMultipleBasesCombinedViaAllOf() {
    val generated = jsonGenerator.generate(
      routes = emptyList(),
      additionalTypes = listOf(SchemaDescriptor.of(MultiBaseItem::class)),
      metaData = OpenApiMetaData.default,
    )

    assertThat(generated.jsonObject).path("components", "schemas").isJsonEqualTo(
      //language=JSON
      $$"""
        {
          "OpenApiBaseSchemaMultipleBasesTest.Identifiable" : {
            "type" : "object",
            "x-source-kotlin-type" : "it.neckar.rest.openapi.OpenApiBaseSchemaMultipleBasesTest.Identifiable",
            "properties" : {
              "id" : {
                "type" : "string"
              }
            },
            "required" : [ "id" ]
          },
          "OpenApiBaseSchemaMultipleBasesTest.Named" : {
            "type" : "object",
            "x-source-kotlin-type" : "it.neckar.rest.openapi.OpenApiBaseSchemaMultipleBasesTest.Named",
            "properties" : {
              "name" : {
                "type" : "string"
              }
            },
            "required" : [ "name" ]
          },
          "OpenApiBaseSchemaMultipleBasesTest.MultiBaseItem" : {
            "allOf" : [
              {
                "$ref" : "#/components/schemas/OpenApiBaseSchemaMultipleBasesTest.Identifiable"
              },
              {
                "$ref" : "#/components/schemas/OpenApiBaseSchemaMultipleBasesTest.Named"
              },
              {
                "type" : "object",
                "x-source-kotlin-type" : "it.neckar.rest.openapi.OpenApiBaseSchemaMultipleBasesTest.MultiBaseItem",
                "properties" : {
                  "extra" : {
                    "type" : "string"
                  }
                },
                "required" : [ "extra" ]
              }
            ]
          }
        }
      """.trimIndent()
    )
  }

  @OpenApiBaseSchema
  private interface Identifiable {
    val id: String
  }

  @OpenApiBaseSchema
  private interface Named {
    val name: String
  }

  @Serializable
  private data class MultiBaseItem(
    override val id: String,
    override val name: String,
    val extra: String,
  ) : Identifiable, Named
}
