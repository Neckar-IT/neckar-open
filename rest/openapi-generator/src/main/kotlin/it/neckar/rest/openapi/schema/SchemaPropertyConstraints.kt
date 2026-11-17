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
/**
 * Defines the constraints for a property in an OpenAPI schema.
 *
 * This is the "target" representation - what goes into the OpenAPI JSON schema,
 * independent of how this information was derived.
 *
 * Important: This does *not* match the Kotlin property constraints directly.
 *
 * !! Most of the time Kotlin Nullability is mapped to OpenAPI optionality !!
 */
data class SchemaPropertyConstraints(
  /**
   * Whether the property is required in the OpenAPI schema.
   * If true, the property must be present in the JSON.
   */
  val required: Boolean,

  /**
   * Whether null should be shown as a valid type in the OpenAPI schema.
   * If true, the type will be `["string", "null"]` instead of just `"string"`.
   */
  val showNullInType: Boolean,
) {
  companion object {
    /**
     * Required property that does not allow null values.
     */
    val Required: SchemaPropertyConstraints = SchemaPropertyConstraints(required = true, showNullInType = false)

    /**
     * Optional property that does not allow null values.
     */
    val Optional: SchemaPropertyConstraints = SchemaPropertyConstraints(required = false, showNullInType = false)

    /**
     * Required property that allows explicit null values.
     */
    val RequiredNullable: SchemaPropertyConstraints = SchemaPropertyConstraints(required = true, showNullInType = true)

    /**
     * Optional property that allows explicit null values.
     */
    val OptionalNullable: SchemaPropertyConstraints = SchemaPropertyConstraints(required = false, showNullInType = true)
  }
}
