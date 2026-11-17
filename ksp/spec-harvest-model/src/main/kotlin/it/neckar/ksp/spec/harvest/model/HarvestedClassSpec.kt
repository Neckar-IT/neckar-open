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
package it.neckar.ksp.spec.harvest.model

import kotlinx.serialization.Serializable
import kotlin.reflect.KClass

/**
 * Contains information about a single class/interface/enum/etc.
 */
@Serializable
data class HarvestedClassSpec(
  /**
   * The package name. Harvested specs use the empty string for the default package - never null.
   */
  val packageName: String?,

  /**
   * The qualified name (including the package name). Falls back to the simple name when KSP reports no qualified
   * name for the declaration, so the value is not guaranteed to be resolvable.
   */
  val qualifiedName: String,

  /**
   * The simple name (without the package name)
   */
  val simpleName: String,

  /**
   * The kind of the declaration as the KSP `ClassKind` enum name - upper snake case, not the Kotlin keyword:
   * `CLASS`, `INTERFACE`, `ENUM_CLASS`, `ENUM_ENTRY`, `OBJECT`, `ANNOTATION_CLASS`.
   */
  val type: String,

  /**
   * Kotlin modifiers as KSP `Modifier` enum names, sorted alphabetically: `DATA`, `INTERNAL`, `SEALED`.
   */
  val modifiers: List<String>,

  /**
   * Rendered direct supertypes (including interfaces and their type arguments, e.g. `kotlin.collections.List<kotlin.String>`),
   * sorted alphabetically. Contains `kotlin.Any` when the declaration names no supertype of its own.
   */
  val superTypes: List<String>,

  /**
   * The documentation
   */
  val documentation: String? = null,

  /**
   * All annotations directly present on the declaration; includes meta-annotation info.
   */
  val annotations: List<HarvestedAnnotationSpec>,

  /**
   * Declared type parameter names in declaration order.
   */
  val typeParameters: List<String>,

  /**
   * Properties declared directly on the class (no inherited members), sorted by name - not in declaration order.
   */
  val properties: List<HarvestedPropertySpec>,

  /**
   * For enum classes: one entry per enum constant in declaration order. Empty for non-enums.
   */
  val enumEntries: List<HarvestedEnumEntrySpec> = emptyList(),
) {

  /**
   * Finds the value of the given annotation on this class, or null if the annotation is not present.
   *
   * Throws [IllegalArgumentException] if [klass] has no qualified name - there would be nothing to match against.
   */
  inline fun <reified T : Annotation> findAnnotationValue(klass: KClass<T> = T::class): String? {
    val annotationQualifiedName = klass.qualifiedName ?: throw IllegalArgumentException("The annotation class ${klass.simpleName} must have a qualified name")
    return findAnnotationValue(annotationQualifiedName)
  }

  /**
   * Returns the "value" argument of the given annotation, or null if the annotation is absent or has no `value` argument.
   *
   * Matches the qualified name exactly, so an annotation whose qualified name the harvester could not resolve - it is
   * then stored under its simple name - is not found here. Use [findValueArgument] on [annotations] for those.
   */
  fun findAnnotationValue(annotationQualifiedName: String): String? {
    val annotation = annotations.find { it.name == annotationQualifiedName } ?: return null
    return annotation.arguments["value"]
  }

  /**
   * Finds the property spec for the given property name
   */
  fun findPropertySpec(propertyName: String): HarvestedPropertySpec? {
    return properties.find { it.name == propertyName }
  }

  /**
   * Finds the enum entry spec for the given constant name, or null if this class has no such constant.
   */
  fun findEnumEntrySpec(entryName: String): HarvestedEnumEntrySpec? {
    return enumEntries.find { it.name == entryName }
  }
}
