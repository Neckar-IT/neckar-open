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
package it.neckar.ksp.spec.harvest

import com.google.devtools.ksp.getDeclaredProperties
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSName
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSTypeArgument
import com.google.devtools.ksp.symbol.KSTypeReference
import com.google.devtools.ksp.symbol.KSValueParameter
import com.google.devtools.ksp.symbol.Modifier
import com.google.devtools.ksp.symbol.Nullability
import com.google.devtools.ksp.symbol.Origin
import com.google.devtools.ksp.symbol.Variance
import it.neckar.ksp.spec.harvest.model.HarvestedAnnotationSpec
import it.neckar.ksp.spec.harvest.model.HarvestedClassSpec
import it.neckar.ksp.spec.harvest.model.HarvestedEnumEntrySpec
import it.neckar.ksp.spec.harvest.model.HarvestedPropertySpec

/**
 * Harvests the information about serializable classes and converts them into serializable model objects.
 */
class SpecHarvester(private val logger: KSPLogger) {
  /**
   * Collects the information for this single class declaration
   */
  fun harvest(classDeclaration: KSClassDeclaration): HarvestedClassSpec {
    logger.logging("Collecting spec for class: ${classDeclaration.qualifiedName?.asString()}")
    return classDeclaration.harvestedClassSpec()
  }

  /**
   * Builds the serializable descriptor for a class declaration, including metadata, properties, and annotations.
   */
  private fun KSClassDeclaration.harvestedClassSpec(): HarvestedClassSpec {
    val primaryConstructorParametersByName: Map<String, KSValueParameter> = primaryConstructor?.parameters
      ?.associateBy { it.name?.asString().orEmpty() }
      .orEmpty()

    return HarvestedClassSpec(
      packageName = packageName.asString(),
      qualifiedName = qualifiedName?.asString() ?: simpleName.asString(),
      simpleName = simpleName.asString(),
      type = classKind.name,
      modifiers = modifiers.map(Modifier::name).sorted(),
      superTypes = superTypes.map { it.renderTypeReference() }.toList().sorted(),
      documentation = docString?.trim(),
      annotations = annotations.map { it.toHarvestedAnnotationSpec(includeMetaAnnotations = true) }.toList(),
      typeParameters = typeParameters.map { it.name.asString() },
      properties = getDeclaredProperties()
        //Compiler-synthesized properties (e.g. an enum class' `entries`) are not part of the declared shape
        .filter { it.origin != Origin.SYNTHETIC }
        .map { property -> property.toHarvestedPropertySpec(primaryConstructorParametersByName) }
        .toList()
        .sortedBy { it.name },
      enumEntries = harvestEnumEntries(),
    )
  }

  /**
   * For an enum class, collects one [HarvestedEnumEntrySpec] per constant in declaration order.
   * The constants' KDoc is not available via runtime reflection, so it is harvested here together with their annotations.
   */
  private fun KSClassDeclaration.harvestEnumEntries(): List<HarvestedEnumEntrySpec> {
    if (classKind != ClassKind.ENUM_CLASS) {
      return emptyList()
    }

    return declarations
      .filterIsInstance<KSClassDeclaration>()
      .filter { it.classKind == ClassKind.ENUM_ENTRY }
      .map { entry ->
        HarvestedEnumEntrySpec(
          name = entry.simpleName.asString(),
          documentation = entry.docString?.trim()?.takeIf { it.isNotEmpty() },
          annotations = entry.annotations.map { it.toHarvestedAnnotationSpec(includeMetaAnnotations = true) }.toList(),
        )
      }
      .toList()
  }

  /**
   * Converts a property into the flattened, serializable representation that the processor emits.
   *
   * Matches constructor parameters to detect `hasDefault`, because KSP exposes that flag only there.
   */
  private fun KSPropertyDeclaration.toHarvestedPropertySpec(
    constructorParameters: Map<String, KSValueParameter>
  ): HarvestedPropertySpec {
    val resolvedType: KSType = type.resolve()
    val constructorParameter: KSValueParameter? = constructorParameters[simpleName.asString()]

    val hasDefault = constructorParameter?.hasDefault == true

    return HarvestedPropertySpec(
      name = simpleName.asString(),
      type = resolvedType.render(),
      isNullable = resolvedType.nullability == Nullability.NULLABLE,
      isMutable = isMutable,
      hasDefault = hasDefault,
      documentation = docString?.trim(),
      annotations = annotations.map { it.toHarvestedAnnotationSpec(includeMetaAnnotations = true) }.toList(),
      typeAnnotations = type.annotations.map { it.toHarvestedAnnotationSpec(includeMetaAnnotations = true) }.toList(),
    )
  }

  /**
   * Captures the annotation's fully-qualified name, arguments, and optional meta-annotations.
   *
   * A small visited set prevents cycles when meta-annotations reference each other.
   */
  private fun KSAnnotation.toHarvestedAnnotationSpec(
    includeMetaAnnotations: Boolean = false,
    visitedDeclarations: MutableSet<KSDeclaration> = mutableSetOf(),
  ): HarvestedAnnotationSpec {
    val resolvedAnnotationType = annotationType.resolve()
    val declaration = resolvedAnnotationType.declaration
    val name = declaration.qualifiedName?.asString() ?: shortName.asString()

    val argumentMap = linkedMapOf<String, String?>()
    arguments.forEachIndexed { index, argument ->
      val baseName = argument.name?.asString() ?: "value"
      val key = uniqueArgumentName(argumentMap, baseName, index)
      argumentMap[key] = argument.value.renderAnnotationValue()
    }

    val metaAnnotations = if (includeMetaAnnotations && declaration.isKotlinStandardAnnotation().not()) {
      collectMetaAnnotations(declaration, visitedDeclarations)
    } else {
      emptyList()
    }

    return HarvestedAnnotationSpec(
      name = name,
      arguments = argumentMap,
      metaAnnotations = metaAnnotations,
    )
  }

  /**
   * Walks meta-annotations recursively while guarding against infinite recursion via `visitedDeclarations`.
   */
  private fun collectMetaAnnotations(
    declaration: KSDeclaration,
    visitedDeclarations: MutableSet<KSDeclaration>,
  ): List<HarvestedAnnotationSpec> {
    if (!visitedDeclarations.add(declaration)) {
      return emptyList()
    }

    return try {
      declaration.annotations
        .map { nested ->
          nested.toHarvestedAnnotationSpec(
            includeMetaAnnotations = true,
            visitedDeclarations = visitedDeclarations,
          )
        }
        .toList()
    } finally {
      visitedDeclarations.remove(declaration)
    }
  }

  /**
   * Normalizes annotation argument values to stable strings so that they can be serialized easily later on.
   */
  private fun Any?.renderAnnotationValue(): String? {
    return when (this) {
      null -> null
      is String -> this
      is Boolean, is Int, is Long, is Short, is Byte, is Float, is Double, is Char -> toString()
      is Enum<*> -> "${this::class.qualifiedName}.$this"
      is KSAnnotation -> "@${this.shortName.asString()}"
      is KSDeclaration -> this.qualifiedName?.asString() ?: this.simpleName.asString()
      is KSType -> renderKSType(this)
      is KSTypeReference -> resolve().render()
      is KSName -> asString()
      is List<*> -> joinToString(prefix = "[", postfix = "]") { element ->
        element.renderAnnotationValue() ?: "null"
      }

      else -> toString()
    }
  }

  /**
   * Provides a uniform `String` representation for the resolved type, including generics and nullability.
   */
  private fun KSType.render(): String = renderKSType(this)

  /**
   * Shared rendering logic for any `KSType`, reused by both annotations and property harvesting.
   */
  private fun renderKSType(type: KSType): String {
    val declarationName = type.declaration.qualifiedName?.asString()
      ?: type.declaration.simpleName.asString()

    val typeArguments = type.arguments
    val base = if (typeArguments.isEmpty()) {
      declarationName
    } else {
      val argumentsRendered = typeArguments.joinToString(", ") { it.renderTypeArgument() }
      "$declarationName<$argumentsRendered>"
    }

    return if (type.nullability == Nullability.NULLABLE) {
      "$base?"
    } else {
      base
    }
  }

  /**
   * Renders a type argument including variance (`in`/`out`) and nested type information or wildcards.
   */
  private fun KSTypeArgument.renderTypeArgument(): String {
    val variancePrefix = when (variance) {
      Variance.COVARIANT -> "out "
      Variance.CONTRAVARIANT -> "in "
      Variance.STAR -> ""
      Variance.INVARIANT -> ""
    }

    val type = type?.resolve()
    val typeText = when {
      variance == Variance.STAR -> "*"
      type != null -> renderKSType(type)
      else -> "*"
    }

    return "$variancePrefix$typeText".trim()
  }

  /**
   * Ensures that duplicate annotation argument names (e.g., repeated `value`) remain unique in the output map.
   */
  private fun uniqueArgumentName(existing: Map<String, String?>, baseName: String, index: Int): String {
    if (existing.containsKey(baseName).not()) {
      return baseName
    }

    var counter = 1
    var candidate = "$baseName#$counter"
    while (existing.containsKey(candidate)) {
      counter += 1
      candidate = "$baseName#$counter"
    }

    return candidate
  }

  /**
   * Delegates to `renderKSType` so that both property and super-type handling stay consistent.
   */
  private fun KSTypeReference.renderTypeReference(): String {
    return renderKSType(resolve())
  }

  private fun KSDeclaration.isKotlinStandardAnnotation(): Boolean {
    return qualifiedName?.asString()?.startsWith("kotlin.annotation.") == true
  }
}
