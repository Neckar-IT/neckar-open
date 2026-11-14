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
import it.neckar.open.annotations.OpenApiBaseSchema
import it.neckar.open.serialization.isJsonEqualTo
import it.neckar.open.serialization.path
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.jsonObject
import org.junit.jupiter.api.Test
import it.neckar.rest.openapi.documentation.DocumentationProvider
import it.neckar.rest.openapi.schema.SchemaDescriptor

/**
 * Tests `allOf` composition for `@OpenApiBaseSchema`-annotated interfaces.
 */
class OpenApiBaseSchemaTest {
  private val jsonGenerator = OpenApiJsonGenerator(documentationProvider = DocumentationProvider.None)

  @Test
  fun testAllOfComposition() {
    val generated = jsonGenerator.generate(
      routes = emptyList(),
      additionalTypes = listOf(SchemaDescriptor.of(ItemDetails::class)),
      metaData = OpenApiMetaData.default,
    )

    assertThat(generated.jsonObject).path("components", "schemas").isJsonEqualTo(
      //language=JSON
      $$"""
        {
          "OpenApiBaseSchemaTest.ItemLike" : {
            "type" : "object",
            "x-source-kotlin-type" : "it.neckar.rest.openapi.OpenApiBaseSchemaTest.ItemLike",
            "properties" : {
              "id" : {
                "type" : "string"
              },
              "name" : {
                "type" : "string"
              }
            },
            "required" : [ "id", "name" ]
          },
          "OpenApiBaseSchemaTest.ItemDetails" : {
            "allOf" : [
              {
                "$ref" : "#/components/schemas/OpenApiBaseSchemaTest.ItemLike"
              },
              {
                "type" : "object",
                "x-source-kotlin-type" : "it.neckar.rest.openapi.OpenApiBaseSchemaTest.ItemDetails",
                "properties" : {
                  "description" : {
                    "type" : [
                      "string",
                      "null"
                    ]
                  },
                  "price" : {
                    "type" : "number",
                    "format" : "double"
                  }
                },
                "required" : [ "description", "price" ]
              }
            ]
          }
        }
      """.trimIndent()
    )
  }

  @Test
  fun testFlatSchemaWhenNoOwnProperties() {
    val generated = jsonGenerator.generate(
      routes = emptyList(),
      additionalTypes = listOf(SchemaDescriptor.of(ItemSummary::class)),
      metaData = OpenApiMetaData.default,
    )

    // ItemSummary has the same properties as ItemLike — should stay flat (no allOf)
    assertThat(generated.jsonObject).path("components", "schemas", "OpenApiBaseSchemaTest.ItemSummary")
      .transform("keys") { it.keys }.all {
        doesNotContain("allOf")
        contains("type")
        contains("properties")
      }
  }

  @Test
  fun testSchemaDescriptorOfBaseInterface() {
    val descriptor = SchemaDescriptor.of(ItemLike::class)
    assertThat(descriptor).isInstanceOf(SchemaDescriptor.InterfaceBaseSchema::class)

    val interfaceSchema = descriptor as SchemaDescriptor.InterfaceBaseSchema
    assertThat(interfaceSchema.propertyNames).containsExactlyInAnyOrder("id", "name")
  }

  @OpenApiBaseSchema
  private interface ItemLike {
    val id: String
    val name: String
  }

  @Serializable
  private data class ItemDetails(
    override val id: String,
    override val name: String,
    val description: String?,
    val price: Double,
  ) : ItemLike

  @Serializable
  private data class ItemSummary(
    override val id: String,
    override val name: String,
  ) : ItemLike
}
