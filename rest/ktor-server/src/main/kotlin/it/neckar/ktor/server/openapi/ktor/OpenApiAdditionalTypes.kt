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

import it.neckar.rest.openapi.OpenApiMetaData
import it.neckar.rest.openapi.schema.SchemaDescriptor
import kotlin.reflect.KClass

/**
 * Contains additional types that are not part of the OpenAPI specification but are used in the application.
 *
 * These types are exported to the OpenAPI documentation - without the endpoints.
 */
data class OpenApiAdditionalTypes(
  /**
   * The additional types that are used in the application - by category
   */
  val additionalTypes: Map<OpenApiMetaData.Category, List<KClass<*>>>,
  /**
   * The additional schemas that carry more than the class, such as schema-level examples - by category
   */
  val additionalSchemas: Map<OpenApiMetaData.Category, List<SchemaDescriptor>> = emptyMap(),
) {
  /**
   * Returns the categories that have additional types or additional schemas.
   */
  fun categories(): Set<OpenApiMetaData.Category> {
    return additionalTypes.keys + additionalSchemas.keys
  }

  /**
   * Returns the union of both: per category, the types and the schemas of this and of [other].
   */
  operator fun plus(other: OpenApiAdditionalTypes): OpenApiAdditionalTypes {
    return OpenApiAdditionalTypes(
      additionalTypes = additionalTypes.mergeLists(other.additionalTypes),
      additionalSchemas = additionalSchemas.mergeLists(other.additionalSchemas),
    )
  }

  /**
   * Returns the schemas of the additional types and additional schemas for the given category - returns an empty list if nothing is registered for the category.
   */
  operator fun get(
    category: OpenApiMetaData.Category
  ): List<SchemaDescriptor> {
    return additionalTypes[category].orEmpty().map { SchemaDescriptor.of(it) } + additionalSchemas[category].orEmpty()
  }

  companion object {
    /**
     * Creates an empty OpenApiAdditionalTypes instance
     */
    val empty: OpenApiAdditionalTypes = OpenApiAdditionalTypes(
      additionalTypes = emptyMap(),
    )

    /**
     * Creates an OpenApiAdditionalTypes instance containing the provided types in the "environment" category.
     */
    fun environment(
      types: List<KClass<*>>
    ): OpenApiAdditionalTypes {
      return OpenApiAdditionalTypes(
        additionalTypes = mapOf(OpenApiMetaData.Category.Environment to types),
      )
    }
  }
}

private fun <V> Map<OpenApiMetaData.Category, List<V>>.mergeLists(other: Map<OpenApiMetaData.Category, List<V>>): Map<OpenApiMetaData.Category, List<V>> {
  return (keys + other.keys).associateWith { category -> this[category].orEmpty() + other[category].orEmpty() }
}
