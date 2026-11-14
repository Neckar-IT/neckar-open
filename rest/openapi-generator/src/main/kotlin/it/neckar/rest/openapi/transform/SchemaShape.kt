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

import it.neckar.open.kotlin.lang.checkNotNull
import it.neckar.rest.openapi.AdditionalOpenApiKeys
import it.neckar.rest.openapi.DocumentationKeyFilter
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * Compares and merges schemas by what they say about the JSON on the wire, ignoring their documentation.
 */
object SchemaShape {
  /**
   * Leaves out prose, the `examples` and the Kotlin type reference; a unit and the `deprecated` flag stay.
   * An `example`, `default`, `const` or `enum` value is data and stays whole, fields called `description` included.
   */
  private val ShapeFilter: DocumentationKeyFilter = DocumentationKeyFilter(
    documentationKeys = setOf(
      "description",
      "examples",
      AdditionalOpenApiKeys.DeprecatedReason,
      AdditionalOpenApiKeys.EnumDescriptions,
      AdditionalOpenApiKeys.SourceRef.KotlinType,
      AdditionalOpenApiKeys.ValueClassMeta.GermanTerm,
    ),
    nameMapKeys = DocumentationKeyFilter.SchemaNameMapKeys,
    verbatimKeys = DocumentationKeyFilter.SchemaVerbatimKeys,
  )

  /**
   * The shape of [schema]: equal for two schemas that differ only in documentation; a `$ref` compares by the component it names.
   */
  fun of(schema: JsonElement): JsonElement {
    return ShapeFilter.withoutDocumentation(schema)
  }

  /**
   * Merges schemas of one shape: a documentation key survives where every schema carries the same value.
   */
  fun merge(schemas: List<JsonElement>): JsonElement {
    require(schemas.isNotEmpty()) { "Nothing to merge" }
    require(schemas.map { of(it) }.distinct().size == 1) { "Only schemas of one shape merge: $schemas" }
    return merge(schemas, isNameMap = false)
  }

  private fun merge(schemas: List<JsonElement>, isNameMap: Boolean): JsonElement {
    return when (val first = schemas.first()) {
      is JsonObject -> JsonObject(buildMap {
        first.forEach { (key, value) ->
          val values = schemas.map { (it as JsonObject)[key] }
          when {
            ShapeFilter.isDocumentation(key, isNameMap) -> if (values.all { it == value }) put(key, value)
            else -> put(key, merge(values.map { it.checkNotNull { "Schemas of one shape disagree on [$key]" } }, isNameMap = ShapeFilter.opensNameMap(key, isNameMap)))
          }
        }
      })

      is JsonArray -> JsonArray(first.indices.map { index -> merge(schemas.map { (it as JsonArray)[index] }, isNameMap = false) })
      is JsonPrimitive -> first
    }
  }
}
