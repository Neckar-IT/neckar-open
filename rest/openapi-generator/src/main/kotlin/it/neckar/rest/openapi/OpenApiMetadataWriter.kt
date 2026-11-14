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
@file:Suppress("IdentifierGrammar")
@file:OptIn(InternalSerializationApi::class, ExperimentalSerializationApi::class)

package it.neckar.rest.openapi

import it.neckar.open.kotlin.lang.asKClass
import it.neckar.open.kotlin.lang.getTypeName
import it.neckar.open.kotlin.lang.requireNotNull
import it.neckar.open.kotlin.serializers.findSerialName
import it.neckar.open.kotlin.serializers.putNotNull
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.reflect.KClass
import kotlin.reflect.KType
import it.neckar.rest.openapi.documentation.DocumentationProvider
import it.neckar.rest.openapi.schema.NativeSchemaType
import it.neckar.rest.openapi.schema.SchemaDescriptor
import it.neckar.rest.openapi.schema.SchemaPropertyConstraints
import it.neckar.rest.openapi.schema.TypeDiscriminator
import it.neckar.rest.openapi.schema.wrapNullable

/**
 * Helper class for adding metadata and documentation to OpenAPI schema objects.
 *
 * This class handles:
 * - Kotlin source references
 * - Documentation/descriptions
 * - Unit symbols and names
 * - German terminology
 * - Value class metadata
 * - Type discriminators
 * - Native type formatting
 */
internal class OpenApiMetadataWriter(
  /**
   * The documentation provider that is used to resolve the documentation that is added to the generated openapi file.
   */
  private val documentationProvider: DocumentationProvider,
) {

  /**
   * Puts the kotlin source reference for the provided class - if there is one
   */
  context(jsonObjectBuilder: JsonObjectBuilder)
  internal fun putTypeDiscriminatorIfExists(clazz: KClass<*>): Boolean {
    return putTypeDiscriminator(TypeDiscriminator.ofNullable(clazz.findSerialName()))
  }

  /**
   * Puts the type discriminator
   *
   * This is (usually) used for sealed classes and enums.
   *
   * Returns true if the type discriminator was put, false otherwise — the Boolean is
   * primarily for the nullable [putTypeDiscriminatorIfExists] variant; callers that
   * pass a known non-null [typeDiscriminator] may ignore the return.
   */
  @IgnorableReturnValue
  context(jsonObjectBuilder: JsonObjectBuilder)
  internal fun putTypeDiscriminator(typeDiscriminator: TypeDiscriminator?): Boolean {
    if (typeDiscriminator == null) {
      return false
    }

    jsonObjectBuilder.put("type", buildJsonObject {
      putNativeType(SchemaDescriptor.String.toNativeType().requireNotNull { }, SchemaPropertyConstraints.Required)
      put("enum", buildJsonArray { add(typeDiscriminator.value) })
    })

    return true
  }

  context(jsonObjectBuilder: JsonObjectBuilder)
  internal fun putNativeType(nativeType: NativeSchemaType, schemaConstraints: SchemaPropertyConstraints) {
    jsonObjectBuilder.put("type", nativeType.wrapNullable(schemaConstraints))

    nativeType.format?.let {
      jsonObjectBuilder.put("format", it)
    }
  }

  /**
   * Adds the kotlin source ref to the schema type
   */
  context(jsonObjectBuilder: JsonObjectBuilder)
  internal fun putKotlinSourceRef(schemaType: SchemaDescriptor) {
    when (schemaType) {
      SchemaDescriptor.EmptySchema -> {
        //Do nothing
      }

      is SchemaDescriptor.NativePrimitiveSchema -> {
        putKotlinSourceRef(schemaType.type.getTypeName())
      }

      is SchemaDescriptor.FormattedStringSchema -> {
        putKotlinSourceRef(schemaType.sourceType)
      }

      is SchemaDescriptor.InlineEnumSchema -> {
        putKotlinSourceRef(String::class.qualifiedName)
      }

      is SchemaDescriptor.SerializedAsPrimitiveSchema -> {
        putKotlinSourceRef(schemaType.asClass())
      }

      is SchemaDescriptor.ValueClassSchema -> {
        putKotlinSourceRef(schemaType.asClass())
        putValueClassMetadata(schemaType.asClass())
      }

      is SchemaDescriptor.ConstSchemaObject -> {
        putKotlinSourceRef(schemaType.asClass())
      }

      is SchemaDescriptor.ClassSchemaObject -> {
        putKotlinSourceRef(schemaType.asClass())
      }

      is SchemaDescriptor.DiscriminatedClassSchemaWrapper -> {
        putKotlinSourceRef(schemaType.asClass())
      }

      is SchemaDescriptor.EnumSchema -> {
        putKotlinSourceRef(schemaType.asClass())
      }

      is SchemaDescriptor.ComponentEnumSchemaObject -> {
        putKotlinSourceRef(schemaType.asClass())
      }

      is SchemaDescriptor.EnumEntrySchema -> {
        putKotlinSourceRef(schemaType.enumType.asKClass().qualifiedName + "." + schemaType.enumEntry.name)
      }

      is SchemaDescriptor.ClassSchemaWithDiscriminatorObject -> {
        //TODO()
      }

      is SchemaDescriptor.InterfaceBaseSchema -> {
        putKotlinSourceRef(schemaType.asClass())
      }
    }
  }

  context(jsonObjectBuilder: JsonObjectBuilder)
  internal fun putKotlinSourceRef(clazz: KClass<*>) {
    putKotlinSourceRef(clazz.qualifiedName)
  }

  context(jsonObjectBuilder: JsonObjectBuilder)
  internal fun putKotlinSourceRefIfNotNull(clazz: KClass<*>?) {
    if (clazz != null) {
      putKotlinSourceRef(clazz)
    }
  }

  context(jsonObjectBuilder: JsonObjectBuilder)
  internal fun putDocumentation(documentation: String?) {
    jsonObjectBuilder.putNotNull("description", documentation)
  }

  /**
   * Adds the unit symbol to the schema property (e.g., "mo" for @Months, "%" for @pct)
   */
  context(jsonObjectBuilder: JsonObjectBuilder)
  internal fun putUnit(unit: String?) {
    if (unit != null) {
      jsonObjectBuilder.put(AdditionalOpenApiKeys.SourceRef.Unit, unit)
    }
  }

  /**
   * Adds the German term to the schema (for classes or properties)
   */
  context(jsonObjectBuilder: JsonObjectBuilder)
  internal fun putGermanTerm(germanTerm: String?) {
    if (germanTerm != null) {
      jsonObjectBuilder.put(AdditionalOpenApiKeys.ValueClassMeta.GermanTerm, germanTerm)
    }
  }

  context(jsonObjectBuilder: JsonObjectBuilder)
  internal fun putKotlinSourceRef(kType: KType) {
    putKotlinSourceRef(kType.toString())
  }

  /**
   * Adds the kotlin source ref to the schema type
   */
  context(jsonObjectBuilder: JsonObjectBuilder)
  internal fun putKotlinSourceRef(qualifiedName: String?) {
    jsonObjectBuilder.put(AdditionalOpenApiKeys.SourceRef.KotlinType, qualifiedName)
  }

  /**
   * Adds value class metadata to the schema if available: unit symbol/name, German term, and the
   * JSON-Schema constraints (minLength/maxLength/pattern/minimum/maximum/multipleOf).
   */
  context(jsonObjectBuilder: JsonObjectBuilder)
  internal fun putValueClassMetadata(clazz: KClass<*>) {
    val metadata = documentationProvider.getValueClassMetadata(clazz) ?: return

    metadata.unitSymbol?.let {
      jsonObjectBuilder.put(AdditionalOpenApiKeys.ValueClassMeta.UnitSymbol, it)
    }
    metadata.unitName?.let {
      jsonObjectBuilder.put(AdditionalOpenApiKeys.ValueClassMeta.UnitName, it)
    }
    metadata.germanTerm?.let {
      jsonObjectBuilder.put(AdditionalOpenApiKeys.ValueClassMeta.GermanTerm, it)
    }

    //JSON-Schema constraints derived from the value class' invariant (@MinLength, @Pattern, @Minimum, ...)
    metadata.minLength?.let { jsonObjectBuilder.put("minLength", it) }
    metadata.maxLength?.let { jsonObjectBuilder.put("maxLength", it) }
    metadata.pattern?.let { jsonObjectBuilder.put("pattern", it) }
    metadata.minimum?.let { jsonObjectBuilder.put("minimum", it) }
    metadata.maximum?.let { jsonObjectBuilder.put("maximum", it) }
    metadata.multipleOf?.let { jsonObjectBuilder.put("multipleOf", it) }
  }
}
