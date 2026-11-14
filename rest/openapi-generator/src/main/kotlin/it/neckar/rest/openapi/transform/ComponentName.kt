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
import it.neckar.rest.openapi.schema.ComponentSchemaReference

/**
 * The key of a schema under `components.schemas`, escaped exactly as the generator writes it and as every `$ref` names it.
 * A new name comes from [ofUnescaped]; the constructor takes a key that is already escaped.
 */
@JvmInline
value class ComponentName(val escaped: String) {
  /**
   * The `$ref` value pointing at this component.
   */
  val reference: String
    get() = ReferencePrefix + escaped

  override fun toString(): String {
    return escaped
  }

  companion object {
    private const val ReferencePrefix: String = "#/components/schemas/"

    /**
     * Escapes [name] the way the generator escapes every component key.
     */
    fun ofUnescaped(name: String): ComponentName {
      return ComponentName(ComponentSchemaReference.ofUnescaped(name).valueEscaped())
    }

    /**
     * Returns the component a string names, or null for any other string of the document.
     */
    fun fromReferenceOrNull(value: String): ComponentName? {
      return if (value.startsWith(ReferencePrefix)) ComponentName(value.removePrefix(ReferencePrefix)) else null
    }

    /**
     * Returns the component a reference names - a `$ref` or a `discriminator.mapping` value.
     */
    fun fromReference(value: String): ComponentName {
      return fromReferenceOrNull(value).checkNotNull { "Not a component reference: [$value]" }
    }
  }
}
