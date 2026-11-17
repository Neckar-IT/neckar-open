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
package it.neckar.rest.openapi.documentation
import it.neckar.open.kotlin.lang.primaryConstructorParameterNames
import it.neckar.open.kotlin.serializers.isElementNullable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlin.reflect.KClass
import it.neckar.rest.openapi.schema.SchemaPropertyConstraints

/**
 * Contains information about a Kotlin property from the serialization descriptor.
 *
 * This is the "source" information extracted from Kotlin code,
 * before it is converted to OpenAPI schema constraints.
 */
data class KotlinPropertyInfo(
  /**
   * Whether the property has a default value in Kotlin.
   * (corresponds to `isElementOptional` in the SerialDescriptor)
   */
  val hasDefaultValue: Boolean,

  /**
   * Whether the property type is nullable in Kotlin.
   */
  val isNullable: Boolean,

  /**
   * Whether the property is a computed property (defined in class body, not constructor).
   *
   * Computed properties are always serialized by kotlinx.serialization, so they should
   * be marked as "required" in OpenAPI response schemas, even though [hasDefaultValue]
   * is true (because they're optional during deserialization - they get computed).
   */
  val isComputedProperty: Boolean = false,
) {

  /**
   * Converts this Kotlin property info to OpenAPI schema constraints.
   *
   * @param forceNullableInSchema If true, null is always shown as valid value in the schema,
   *   regardless of [hasDefaultValue]. Used for types annotated with @OpenApiNullable
   *   (e.g., Patchable where null means "delete").
   */
  fun toSchemaConstraints(forceNullableInSchema: Boolean = false): SchemaPropertyConstraints {
    return SchemaPropertyConstraints(
      // Computed properties are always serialized, so they're required in OpenAPI
      required = hasDefaultValue.not() || isComputedProperty,
      // Nullability is independent of the default value — a Kotlin `String? = null` field
      // can both be omitted (default applies) AND carry an explicit `null` on the wire.
      showNullInType = forceNullableInSchema || isNullable,
    )
  }

  companion object {
    val Required: KotlinPropertyInfo = KotlinPropertyInfo(hasDefaultValue = false, isNullable = false)
    val Optional: KotlinPropertyInfo = KotlinPropertyInfo(hasDefaultValue = true, isNullable = false)
    val NullableRequired: KotlinPropertyInfo = KotlinPropertyInfo(hasDefaultValue = false, isNullable = true)
    val NullableOptional: KotlinPropertyInfo = KotlinPropertyInfo(hasDefaultValue = true, isNullable = true)

    fun of(hasDefaultValue: Boolean, isNullable: Boolean): KotlinPropertyInfo {
      return when {
        hasDefaultValue && isNullable -> NullableOptional
        hasDefaultValue -> Optional
        isNullable -> NullableRequired
        else -> Required
      }
    }
  }
}

/**
 * Extracts the Kotlin property info from the descriptor for the indexed child element.
 *
 * Note: This method cannot detect computed properties. Use the overload with [KClass]
 * parameter for accurate detection of computed properties.
 *
 * This method is still used for list/collection element types where:
 * - No KClass is available for the element type
 * - Computed property detection is not applicable (list elements are values, not class properties)
 */
@Deprecated(
  message = "Use kotlinPropertyInfo(elementIndex, ownerClass) for class properties to detect computed properties",
  replaceWith = ReplaceWith("kotlinPropertyInfo(elementIndex, ownerClass)"),
  level = DeprecationLevel.WARNING,
)
fun SerialDescriptor.kotlinPropertyInfo(elementIndex: Int): KotlinPropertyInfo {
  val elementNullable = isElementNullable(elementIndex)
  val optional = isElementOptional(elementIndex)

  return KotlinPropertyInfo.of(
    hasDefaultValue = optional,
    isNullable = elementNullable
  )
}

/**
 * Extracts the Kotlin property info from the descriptor for the indexed child element,
 * using the [ownerClass] to detect computed properties.
 *
 * Computed properties (defined in class body, not constructor) have [SerialDescriptor.isElementOptional]
 * return `true` because they're optional during deserialization. However, they are always serialized,
 * so they should be marked as "required" in OpenAPI response schemas.
 *
 * This method uses reflection to check if the property is defined in the primary constructor.
 * If not, and it's marked as optional, it's considered a computed property.
 */
fun SerialDescriptor.kotlinPropertyInfo(elementIndex: Int, ownerClass: KClass<*>): KotlinPropertyInfo {
  val elementNullable = isElementNullable(elementIndex)
  val optional = isElementOptional(elementIndex)
  val propertyName = getElementName(elementIndex)

  // Check if property is in the primary constructor
  val constructorParamNames = ownerClass.primaryConstructorParameterNames()

  // A property is computed if it's optional (has default) but NOT in the constructor.
  // This means it's defined in the class body and gets computed/initialized there.
  val isComputedProperty = optional && propertyName !in constructorParamNames

  return KotlinPropertyInfo(
    hasDefaultValue = optional,
    isNullable = elementNullable,
    isComputedProperty = isComputedProperty,
  )
}

/**
 * Returns all element names that are required for OpenAPI schema generation,
 * considering computed properties.
 *
 * Unlike [it.neckar.open.kotlin.serializers.requiredElementNames], this method
 * also marks computed properties (defined in class body, not constructor) as required,
 * because they are always present in serialized output.
 *
 * @param ownerClass The KClass of the owner to check for constructor parameters
 */
fun SerialDescriptor.requiredElementNamesForOpenApi(ownerClass: KClass<*>): List<String> {
  val constructorParamNames = ownerClass.primaryConstructorParameterNames()

  return (0 until elementsCount).mapNotNull { index ->
    val propertyName = getElementName(index)
    val isOptional = isElementOptional(index)

    // A property is required if:
    // 1. It's not optional (no default value, must be provided) OR
    // 2. It's optional BUT not in constructor (computed property, always serialized)
    val isRequired = isOptional.not() || (isOptional && propertyName !in constructorParamNames)

    if (isRequired) propertyName else null
  }
}
