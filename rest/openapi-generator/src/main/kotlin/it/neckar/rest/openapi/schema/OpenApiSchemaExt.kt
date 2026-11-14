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
import it.neckar.rest.RestResponse
import kotlin.contracts.contract
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.full.starProjectedType


fun SchemaDescriptor.isConstSchemaObject(): Boolean {
  contract { returns(true) implies (this@isConstSchemaObject is SchemaDescriptor.ConstSchemaObject) }
  return this is SchemaDescriptor.ConstSchemaObject
}

fun SchemaDescriptor.isClassSchemaObject(): Boolean {
  contract { returns(true) implies (this@isClassSchemaObject is SchemaDescriptor.ClassSchemaObject) }
  return this is SchemaDescriptor.ClassSchemaObject
}

fun SchemaDescriptor.isDiscriminatedClassSchema(): Boolean {
  contract { returns(true) implies (this@isDiscriminatedClassSchema is SchemaDescriptor.DiscriminatedClassSchemaWrapper) }
  return this is SchemaDescriptor.DiscriminatedClassSchemaWrapper
}

fun SchemaDescriptor.isClassAsInlinePrimitive(): Boolean {
  contract { returns(true) implies (this@isClassAsInlinePrimitive is SchemaDescriptor.SerializedAsPrimitiveSchema) }
  return this is SchemaDescriptor.SerializedAsPrimitiveSchema
}

fun SchemaDescriptor.isValueClass(): Boolean {
  contract { returns(true) implies (this@isValueClass is SchemaDescriptor.ValueClassSchema) }
  return this is SchemaDescriptor.ValueClassSchema
}

fun SchemaDescriptor.isPrimitive(): Boolean {
  contract { returns(true) implies (this@isPrimitive is SchemaDescriptor.NativePrimitiveSchema) }
  return this is SchemaDescriptor.NativePrimitiveSchema
}

fun SchemaDescriptor.isNothing(): Boolean {
  contract { returns(true) implies (this@isNothing is SchemaDescriptor.EmptySchema) }
  return this is SchemaDescriptor.EmptySchema
}

fun SchemaDescriptor.isEnumEntry(): Boolean {
  contract { returns(true) implies (this@isEnumEntry is SchemaDescriptor.EnumEntrySchema) }
  return this is SchemaDescriptor.EnumEntrySchema
}

fun SchemaDescriptor.isEnumSchema(): Boolean {
  contract { returns(true) implies (this@isEnumSchema is SchemaDescriptor.EnumSchema) }
  return this is SchemaDescriptor.EnumSchema
}

fun SchemaDescriptor.isComponentEnum(): Boolean {
  contract { returns(true) implies (this@isComponentEnum is SchemaDescriptor.ComponentEnumSchemaObject) }
  return this is SchemaDescriptor.ComponentEnumSchemaObject
}

fun SchemaDescriptor.isComponent(): Boolean {
  contract { returns(true) implies (this@isComponent is SchemaDescriptor.ComponentSchema) }
  return this is SchemaDescriptor.ComponentSchema
}

/**
 * Returns true if this schema is serialized inline in the OpenAPI specification (usually for primitive types or value classes of primitive classes).
 */
fun SchemaDescriptor.isInline(): Boolean {
  contract { returns(true) implies (this@isInline is SchemaDescriptor.InlineSchemaValue) }
  return this is SchemaDescriptor.InlineSchemaValue
}

/**
 * Returns true if this schema referenced in the OpenAPI specification.
 */
fun SchemaDescriptor.isReferenced(): Boolean {
  contract { returns(true) implies (this@isReferenced is SchemaDescriptor.ComponentSchema) }
  return this is SchemaDescriptor.ComponentSchema
}


/**
 * Returns true if this is a [RestResponse]
 */
fun SchemaDescriptor.isRestResponse(): Boolean {
  return when (this) {
    is SchemaDescriptor.ClassSchemaObject -> this.type.isSubtypeOf(RestResponse::class.starProjectedType)
    is SchemaDescriptor.DiscriminatedClassSchemaWrapper -> this.baseSchema.type.isSubtypeOf(RestResponse::class.starProjectedType)
    else -> false
  }
}
