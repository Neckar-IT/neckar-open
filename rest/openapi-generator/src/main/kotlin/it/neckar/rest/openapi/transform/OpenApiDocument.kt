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

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject

/**
 * A generated OpenAPI document - the only type an [OpenApiDocumentTransformer] reads and writes.
 */
@JvmInline
value class OpenApiDocument(val json: JsonObject) {
  /**
   * The entries of `components.schemas`, in document order.
   */
  val schemas: Map<ComponentName, JsonObject>
    get() = components.getValue("schemas").jsonObject.entries.associate { (name, schema) -> ComponentName(name) to schema.jsonObject }

  private val components: JsonObject
    get() = json.getValue("components").jsonObject

  /**
   * Replaces `components.schemas` - in the order of [schemas].
   */
  fun withSchemas(schemas: Map<ComponentName, JsonObject>): OpenApiDocument {
    val replacedComponents = JsonObject(components + ("schemas" to JsonObject(schemas.mapKeys { (name, _) -> name.escaped })))
    return OpenApiDocument(JsonObject(json + ("components" to replacedComponents)))
  }

  /**
   * Points every string naming a renamed component - `$ref`, `discriminator.mapping`, `x-type-ref` - at its new name.
   * The keys under `components.schemas` stay; the caller decides what lives under the new names.
   */
  fun rewriteReferences(renames: Map<ComponentName, ComponentName>): OpenApiDocument {
    return OpenApiDocument(json.rewriteReferences(renames).jsonObject)
  }

  companion object {
    /**
     * Replaces every string naming a component in [renames]; [inline] may answer a schema to put in place of a whole `{"$ref": …}` object,
     * which is taken as it is, without [renames].
     */
    fun JsonElement.rewriteReferences(renames: Map<ComponentName, ComponentName>, inline: (ComponentName) -> JsonElement? = { null }): JsonElement {
      return when (this) {
        is JsonObject -> {
          // A single-key object whose `$ref` holds an object is a `properties` map with a property named `$ref`.
          @Suppress("UntypedValueSafeCast")
          val referenceValue = if (size == 1) this[$$"$ref"] as? JsonPrimitive else null
          val reference = referenceValue?.takeIf { it.isString }?.let { ComponentName.fromReferenceOrNull(it.content) }
          reference?.let(inline) ?: JsonObject(mapValues { (_, value) -> value.rewriteReferences(renames, inline) })
        }

        is JsonArray -> JsonArray(map { it.rewriteReferences(renames, inline) })

        is JsonPrimitive -> {
          val renamed = if (isString) ComponentName.fromReferenceOrNull(content)?.let { renames[it] } else null
          if (renamed == null) this else JsonPrimitive(renamed.reference)
        }
      }
    }
  }
}
