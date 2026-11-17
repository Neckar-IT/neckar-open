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

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * Decides by its position in an OpenAPI document whether a key is documentation: below a [nameMapKeys] key
 * the keys are names, so a property called `description` counts; below a [verbatimKeys] key the value is data.
 */
internal class DocumentationKeyFilter(
  private val documentationKeys: Set<String>,
  private val nameMapKeys: Set<String>,
  private val verbatimKeys: Set<String>,
) {
  fun withoutDocumentation(element: JsonElement): JsonElement {
    return withoutDocumentation(element, isNameMap = false)
  }

  /**
   * Whether [key] is one of the documentation keys and stands outside a name map ([isNameMap]).
   */
  fun isDocumentation(key: String, isNameMap: Boolean): Boolean {
    return isNameMap.not() && key in documentationKeys
  }

  /**
   * Whether [key] is one of the name-map keys and stands outside a name map ([isNameMap]): its value is keyed by names.
   */
  fun opensNameMap(key: String, isNameMap: Boolean): Boolean {
    return isNameMap.not() && key in nameMapKeys
  }

  private fun isVerbatim(key: String, isNameMap: Boolean): Boolean {
    return isNameMap.not() && key in verbatimKeys
  }

  private fun withoutDocumentation(element: JsonElement, isNameMap: Boolean): JsonElement {
    return when (element) {
      is JsonObject -> JsonObject(
        element.filterKeys { isDocumentation(it, isNameMap).not() }.mapValues { (key, value) ->
          when {
            isVerbatim(key, isNameMap) -> value
            else -> withoutDocumentation(value, isNameMap = opensNameMap(key, isNameMap))
          }
        }
      )

      is JsonArray -> JsonArray(element.map { withoutDocumentation(it, isNameMap = false) })
      is JsonPrimitive -> element
    }
  }

  companion object {
    /**
     * The keys of a JSON schema whose object is keyed by property names, property name patterns or discriminator values.
     */
    val SchemaNameMapKeys: Set<String> = setOf("properties", "patternProperties", "mapping")

    /**
     * The keys of a JSON schema whose value is data: an example, a default, a constant, the enum values.
     */
    val SchemaVerbatimKeys: Set<String> = setOf("example", "default", "const", "enum")
  }
}
