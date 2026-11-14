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
package it.neckar.rest.openapi.schema
import it.neckar.open.unit.other.Unescaped
import java.net.URLEncoder

/**
 * A reference to a component schema
 */
data class ComponentSchemaReference(
  /**
   * The unescaped value of the reference (without the "#/components/schemas/" prefix)
   */
  val unescapedValue: @Unescaped String
) {

  /**
   * Returns the escaped value of the reference (without the "#/components/schemas/" prefix)
   */
  fun valueEscaped(): String {
    return encodeSchemaRef(unescapedValue)
  }

  /**
   * The reference string (e.g. "#/components/schemas/YourSchemaName")
   */
  fun completeReference(): String {
    return "#/components/schemas/${valueEscaped()}"
  }

  /**
   * Appends the given suffix to the unescaped value and returns a new [ComponentSchemaReference]
   */
  operator fun plus(suffix: String): ComponentSchemaReference {
    return ofUnescaped(unescapedValue + suffix)
  }

  companion object {
    /**
     * Creates a [ComponentSchemaReference] from an unescaped string
     */
    fun ofUnescaped(unescaped: @Unescaped String): ComponentSchemaReference {
      return ComponentSchemaReference(unescaped)
    }

    /**
     *  Encodes the schema reference name for usage in OpenAPI specification.
     *  Does *not* replace "." - which is allowed in references.
     */
    private fun encodeSchemaRef(ref: String): String {
      val replaced = ref
        .replace("<", "__")
        .replace(">", "__")
        .replace(", ", "_")
      //TODO: is this necessary?
      return URLEncoder.encode(replaced, Charsets.UTF_8)
    }
  }
}
