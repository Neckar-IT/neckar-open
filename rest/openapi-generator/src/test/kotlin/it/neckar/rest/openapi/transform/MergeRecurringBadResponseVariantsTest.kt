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
package it.neckar.rest.openapi.transform

import assertk.*
import assertk.assertions.*
import it.neckar.open.serialization.isJsonEqualTo
import it.neckar.rest.RestResponse
import it.neckar.rest.openapi.OpenApiJsonGenerator
import it.neckar.rest.openapi.OpenApiMetaData
import it.neckar.rest.openapi.documentation.DocumentationProvider
import it.neckar.rest.openapi.schema.SchemaDescriptor
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import org.junit.jupiter.api.Test

class MergeRecurringBadResponseVariantsTest {
  private val mergedSchemas: JsonObject = run {
    val schemas = OpenApiJsonGenerator(documentationProvider = DocumentationProvider.None).generateComponentsSchemas(
      listOf(SchemaDescriptor.of(UpsertFirstResponse::class), SchemaDescriptor.of(UpsertSecondResponse::class), SchemaDescriptor.of(ListFirstResponse::class))
    )
    val document = JsonObject(mapOf("components" to JsonObject(mapOf("schemas" to schemas))))
    transformedSchemas(document)
  }

  @Test
  fun `a recurring variant of one shape is one component`() {
    assertThat(mergedSchemas.getValue("BadResponse.VersionConflict")).isJsonEqualTo(
      //language=JSON
      """
        {
          "type": "object",
          "properties": {
            "actualVersion": { "type": "integer" }
          },
          "required": [ "actualVersion" ]
        }
      """.trimIndent()
    )
    assertThat(mergedSchemas.getValue("BadResponse.VersionConflict.WithType")).isJsonEqualTo(
      //language=JSON
      $$"""
        {
          "type": "object",
          "allOf": [ { "$ref": "#/components/schemas/BadResponse.VersionConflict" } ],
          "properties": {
            "type": { "type": "string", "enum": [ "bad/VersionConflict" ] }
          },
          "required": [ "type" ]
        }
      """.trimIndent()
    )
    assertThat(mergedSchemas.keys.filter { it.contains(".VersionConflict") || it.endsWith(".NotFound") }).containsOnly(
      "BadResponse.VersionConflict",
      "BadResponse.VersionConflict.WithType",
      "BadResponse.NotFound",
    )
  }

  @Test
  fun `each response keeps its union and mapping and points at the merged component`() {
    assertThat(mergedSchemas.getValue("MergeRecurringBadResponseVariantsTest.UpsertFirstResponse")).isJsonEqualTo(
      //language=JSON
      $$"""
        {
          "type": "object",
          "x-source-kotlin-type": "it.neckar.rest.openapi.transform.MergeRecurringBadResponseVariantsTest.UpsertFirstResponse",
          "oneOf": [
            { "$ref": "#/components/schemas/MergeRecurringBadResponseVariantsTest.UpsertFirstResponse.Good.WithType" },
            { "$ref": "#/components/schemas/MergeRecurringBadResponseVariantsTest.UpsertFirstResponse.Invalid.WithType" },
            { "$ref": "#/components/schemas/BadResponse.NotFound" },
            { "$ref": "#/components/schemas/MergeRecurringBadResponseVariantsTest.UpsertFirstResponse.Rejected.WithType" },
            { "$ref": "#/components/schemas/BadResponse.VersionConflict.WithType" }
          ],
          "discriminator": {
            "propertyName": "type",
            "mapping": {
              "good": "#/components/schemas/MergeRecurringBadResponseVariantsTest.UpsertFirstResponse.Good.WithType",
              "bad/Invalid": "#/components/schemas/MergeRecurringBadResponseVariantsTest.UpsertFirstResponse.Invalid.WithType",
              "bad/NotFound": "#/components/schemas/BadResponse.NotFound",
              "bad/Rejected": "#/components/schemas/MergeRecurringBadResponseVariantsTest.UpsertFirstResponse.Rejected.WithType",
              "bad/VersionConflict": "#/components/schemas/BadResponse.VersionConflict.WithType"
            }
          }
        }
      """.trimIndent()
    )
  }

  @Test
  fun `a variant name with several shapes and the good variant stay per response`() {
    assertThat(mergedSchemas.keys).containsAll(
      "MergeRecurringBadResponseVariantsTest.UpsertFirstResponse.Rejected",
      "MergeRecurringBadResponseVariantsTest.ListFirstResponse.Rejected",
      "MergeRecurringBadResponseVariantsTest.UpsertFirstResponse.Good",
      "MergeRecurringBadResponseVariantsTest.UpsertSecondResponse.Good",
    )
    assertThat(mergedSchemas.keys).doesNotContain("BadResponse.Rejected")
  }

  @Test
  fun `the generated document merges the recurring variants`() {
    val document = OpenApiJsonGenerator(documentationProvider = DocumentationProvider.None).generate(
      routes = emptyList(),
      additionalTypes = listOf(SchemaDescriptor.of(UpsertFirstResponse::class), SchemaDescriptor.of(UpsertSecondResponse::class)),
      metaData = OpenApiMetaData.default,
    )

    assertThat(document.jsonObject.schemas().keys.filter { it.endsWith(".NotFound") || it.contains(".VersionConflict") }).containsOnly("BadResponse.NotFound", "BadResponse.VersionConflict", "BadResponse.VersionConflict.WithType")
  }

  @Test
  fun `every reference points at a published component`() {
    val references = mergedSchemas.referencedComponentNames()

    assertThat(references).isNotEmpty()
    assertThat(mergedSchemas.keys).containsAll(*references.toTypedArray())
  }

  @Test
  fun `a property named like a documentation key is part of the shape`() {
    assertThat(mergedSchemas.keys).containsAll(
      "MergeRecurringBadResponseVariantsTest.UpsertFirstResponse.Invalid",
      "MergeRecurringBadResponseVariantsTest.UpsertSecondResponse.Invalid",
    )
    assertThat(mergedSchemas.keys).doesNotContain("BadResponse.Invalid")
  }

  @Test
  fun `references outside the schemas follow and agreeing documentation stays`() {
    val merged = MergeRecurringBadResponseVariants.transform(OpenApiDocument(documentWithTwoGoneVariants(secondDescription = "The entity is gone"))).json

    assertThat(merged).isJsonEqualTo(
      //language=JSON
      $$"""
        {
          "paths": {
            "/first": { "x-type-ref": "#/components/schemas/BadResponse.Gone" },
            "/second": { "x-type-ref": "#/components/schemas/BadResponse.Gone" }
          },
          "components": {
            "schemas": {
              "First": { "oneOf": [ { "$ref": "#/components/schemas/BadResponse.Gone" } ], "discriminator": { "propertyName": "type", "mapping": { "bad/Gone": "#/components/schemas/BadResponse.Gone" } } },
              "BadResponse.Gone": { "type": "object", "description": "The entity is gone", "properties": { "type": { "type": "string", "enum": [ "bad/Gone" ] } } },
              "Second": { "oneOf": [ { "$ref": "#/components/schemas/BadResponse.Gone" } ], "discriminator": { "propertyName": "type", "mapping": { "bad/Gone": "#/components/schemas/BadResponse.Gone" } } }
            }
          }
        }
      """.trimIndent()
    )
  }

  @Test
  fun `disagreeing documentation is left out`() {
    val merged = MergeRecurringBadResponseVariants.transform(OpenApiDocument(documentWithTwoGoneVariants(secondDescription = "Deleted"))).json

    assertThat(merged.schemas().getValue("BadResponse.Gone").jsonObject.keys).containsOnly("type", "properties")
  }

  @Test
  fun `an existing component under the merged name fails the generation`() {
    val document = documentWithTwoGoneVariants(secondDescription = "The entity is gone")
    val colliding = JsonObject(mapOf("components" to JsonObject(mapOf("schemas" to JsonObject(document.schemas() + ("BadResponse.Gone" to JsonObject(emptyMap())))))))

    assertFailure { MergeRecurringBadResponseVariants.transform(OpenApiDocument(colliding)).json }.all {
      isInstanceOf(IllegalStateException::class)
      messageContains("BadResponse.Gone")
      messageContains("First.Gone")
    }
  }

  @Test
  fun `a merged property named like a documentation key survives the merge`() {
    val document = Json.parseToJsonElement(
      //language=JSON
      $$"""
        {
          "components": {
            "schemas": {
              "First": { "oneOf": [ { "$ref": "#/components/schemas/First.Gone" } ], "discriminator": { "propertyName": "type", "mapping": { "bad/Gone": "#/components/schemas/First.Gone" } } },
              "First.Gone": { "type": "object", "properties": { "description": { "type": "string", "description": "Why it is gone" } } },
              "Second": { "oneOf": [ { "$ref": "#/components/schemas/Second.Gone" } ], "discriminator": { "propertyName": "type", "mapping": { "bad/Gone": "#/components/schemas/Second.Gone" } } },
              "Second.Gone": { "type": "object", "properties": { "description": { "type": "string", "description": "What removed it" } } }
            }
          }
        }
      """.trimIndent()
    ).jsonObject

    assertThat(transformedSchemas(document).getValue("BadResponse.Gone")).isJsonEqualTo(
      //language=JSON
      """
        { "type": "object", "properties": { "description": { "type": "string" } } }
      """.trimIndent()
    )
  }

  @Test
  fun `a recurring bad discriminator that names no class fails the generation`() {
    val document = Json.parseToJsonElement(
      documentWithTwoGoneVariants(secondDescription = "The entity is gone").toString().replace("bad/Gone", "bad/Gone/Twice")
    ).jsonObject

    assertFailure { MergeRecurringBadResponseVariants.transform(OpenApiDocument(document)).json }.all {
      isInstanceOf(IllegalStateException::class)
      messageContains("bad/Gone/Twice")
    }
  }

  @Test
  fun `a merged name carries the escaping of every component name`() {
    val document = Json.parseToJsonElement(
      documentWithTwoGoneVariants(secondDescription = "The entity is gone").toString().replace("Gone", "Größe")
    ).jsonObject

    assertThat(transformedSchemas(document).keys).contains("BadResponse.Gr%C3%B6%C3%9Fe")
  }

  @Test
  fun `a variant name answered by one response stays with it`() {
    val document = Json.parseToJsonElement(
      //language=JSON
      $$"""
        {
          "components": {
            "schemas": {
              "First": { "oneOf": [ { "$ref": "#/components/schemas/First.Gone" } ], "discriminator": { "propertyName": "type", "mapping": { "bad/Gone": "#/components/schemas/First.Gone" } } },
              "First.Gone": { "type": "object", "properties": { "type": { "type": "string", "enum": [ "bad/Gone" ] } } }
            }
          }
        }
      """.trimIndent()
    ).jsonObject

    assertThat(MergeRecurringBadResponseVariants.transform(OpenApiDocument(document)).json).isEqualTo(document)
  }

  private fun JsonElement.referencedComponentNames(): Set<String> {
    return when (this) {
      is JsonObject -> values.flatMapTo(mutableSetOf()) { it.referencedComponentNames() }
      is JsonArray -> flatMapTo(mutableSetOf()) { it.referencedComponentNames() }
      is JsonPrimitive -> if (isString && content.startsWith("#/components/schemas/")) setOf(content.removePrefix("#/components/schemas/")) else emptySet()
    }
  }

  private fun transformedSchemas(document: JsonObject): JsonObject {
    return MergeRecurringBadResponseVariants.transform(OpenApiDocument(document)).json.schemas()
  }

  private fun JsonObject.schemas(): JsonObject {
    return getValue("components").jsonObject.getValue("schemas").jsonObject
  }

  private fun documentWithTwoGoneVariants(secondDescription: String): JsonObject {
    return Json.parseToJsonElement(
      //language=JSON
      $$"""
        {
          "paths": {
            "/first": { "x-type-ref": "#/components/schemas/First.Gone" },
            "/second": { "x-type-ref": "#/components/schemas/Second.Gone" }
          },
          "components": {
            "schemas": {
              "First": { "oneOf": [ { "$ref": "#/components/schemas/First.Gone" } ], "discriminator": { "propertyName": "type", "mapping": { "bad/Gone": "#/components/schemas/First.Gone" } } },
              "First.Gone": { "type": "object", "description": "The entity is gone", "properties": { "type": { "type": "string", "enum": [ "bad/Gone" ] } } },
              "Second": { "oneOf": [ { "$ref": "#/components/schemas/Second.Gone" } ], "discriminator": { "propertyName": "type", "mapping": { "bad/Gone": "#/components/schemas/Second.Gone" } } },
              "Second.Gone": { "type": "object", "description": "$${secondDescription}", "properties": { "type": { "type": "string", "enum": [ "bad/Gone" ] } } }
            }
          }
        }
      """.trimIndent()
    ).jsonObject
  }

  @Serializable
  private sealed interface UpsertFirstResponse : RestResponse<String> {
    @Serializable
    @SerialName("good")
    data class Good(override val data: String) : UpsertFirstResponse, RestResponse.GoodResponse<String>

    @Serializable
    @SerialName("bad/NotFound")
    data object NotFound : UpsertFirstResponse, RestResponse.BadResponse<String>

    @Serializable
    @SerialName("bad/Rejected")
    data class Rejected(val reason: String) : UpsertFirstResponse, RestResponse.BadResponse<String>

    @Serializable
    @SerialName("bad/VersionConflict")
    data class VersionConflict(val actualVersion: Int) : UpsertFirstResponse, RestResponse.BadResponse<String>

    @Serializable
    @SerialName("bad/Invalid")
    data class Invalid(val description: String) : UpsertFirstResponse, RestResponse.BadResponse<String>
  }

  @Serializable
  private sealed interface UpsertSecondResponse : RestResponse<String> {
    @Serializable
    @SerialName("good")
    data class Good(override val data: String) : UpsertSecondResponse, RestResponse.GoodResponse<String>

    @Serializable
    @SerialName("bad/NotFound")
    data object NotFound : UpsertSecondResponse, RestResponse.BadResponse<String>

    @Serializable
    @SerialName("bad/VersionConflict")
    data class VersionConflict(val actualVersion: Int) : UpsertSecondResponse, RestResponse.BadResponse<String>

    @Serializable
    @SerialName("bad/Invalid")
    data class Invalid(val description: Int) : UpsertSecondResponse, RestResponse.BadResponse<String>
  }

  @Serializable
  private sealed interface ListFirstResponse : RestResponse<String> {
    @Serializable
    @SerialName("good")
    data class Good(override val data: String) : ListFirstResponse, RestResponse.GoodResponse<String>

    @Serializable
    @SerialName("bad/Rejected")
    data class Rejected(val count: Int) : ListFirstResponse, RestResponse.BadResponse<String>
  }
}
