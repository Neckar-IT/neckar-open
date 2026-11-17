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
package it.neckar.ksp.serialization

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSTypeParameter
import it.neckar.ksp.fqName
import it.neckar.ksp.implementsSealed
import it.neckar.ksp.isClass
import it.neckar.ksp.isObject
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


/**
 * Fully qualified name of HasTypeDiscriminator interface
 */
private const val HasTypeDiscriminatorFqName = "it.neckar.domain.HasTypeDiscriminator"

/**
 * Verifies serialization
 */
class SerializationVerifierProcessor(val codeGenerator: CodeGenerator, val logger: KSPLogger) : SymbolProcessor {
  override fun process(resolver: Resolver): List<KSAnnotated> {
    val qualifiedName = requireNotNull(Serializable::class.qualifiedName) { "qualifiedName must be provided" }

    resolver.getSymbolsWithAnnotation(qualifiedName)
      .filterIsInstance<KSClassDeclaration>()
      .filter {
        //only classes, not interfaces
        it.classKind.isClass || it.classKind.isObject
      }.forEach {
        val errors = resolver.findSerializationErrors(it, logger)
        errors.forEach { error ->
          logger.error(error.format(), it)
        }
      }

    return emptyList()
  }
}

private fun Resolver.findSerializationErrors(classDeclaration: KSClassDeclaration, logger: KSPLogger): List<SerializationError> {
  val errors = mutableListOf<SerializationError>()

  val serialNameAnnotation = classDeclaration.annotations.find { it.shortName.asString() == SerialName::class.simpleName }

  //Ensure that all sealed classes are annotated with @SerialName
  if (classDeclaration.implementsSealed()) {
    if (serialNameAnnotation == null) {
      errors.add(
        SerializationError(
          location = classDeclaration.location,
          className = classDeclaration.fqName(),
          additionalMessage = "Missing @SerialName annotation"
        )
      )
    }

    //Verify HasTypeDiscriminator: @SerialName must match an enum entry name
    findHasTypeDiscriminatorErrors(classDeclaration, serialNameAnnotation, logger)?.let { errors.add(it) }
  }

  return errors
}

/**
 * Verifies that for classes implementing a sealed class with HasTypeDiscriminator,
 * the @SerialName value matches an enum entry name.
 */
private fun findHasTypeDiscriminatorErrors(
  classDeclaration: KSClassDeclaration,
  serialNameAnnotation: com.google.devtools.ksp.symbol.KSAnnotation?,
  logger: KSPLogger,
): SerializationError? {
  if (serialNameAnnotation == null) return null

  val enumDeclaration = classDeclaration.findTypeDiscriminatorEnum()
    ?: return null // HasTypeDiscriminator not implemented or the enum type cannot be resolved

  val enumEntryNames = enumDeclaration.declarations
    .filterIsInstance<KSClassDeclaration>()
    .filter { it.classKind == ClassKind.ENUM_ENTRY }
    .map { it.simpleName.asString() }
    .toSet()

  val serialNameArgument = serialNameAnnotation.arguments.firstOrNull() ?: return null
  val serialNameValue: String = serialNameArgument.value as? String
    ?: error("@SerialName on ${classDeclaration.fqName()} holds ${serialNameArgument.value?.let { it::class.simpleName }} instead of a String")

  // Verify the @SerialName value matches an enum entry
  if (serialNameValue !in enumEntryNames) {
    return SerializationError(
      location = classDeclaration.location,
      className = classDeclaration.fqName(),
      additionalMessage = "@SerialName(\"$serialNameValue\") does not match any enum entry in ${enumDeclaration.simpleName.asString()}. " +
          "Available entries: ${enumEntryNames.sorted().joinToString(", ")}"
    )
  }

  logger.logging("Success: @SerialName(\"$serialNameValue\") matches enum entry in ${enumDeclaration.simpleName.asString()}")
  return null
}

/**
 * Returns the enum this declaration binds to the type parameter of HasTypeDiscriminator - or null if the hierarchy does
 * not reach HasTypeDiscriminator or the enum cannot be resolved.
 *
 * The supertype hierarchy is walked by hand because the type arguments must be substituted layer by layer:
 * `getAllSuperTypes()` reports the supertypes as they are declared, so a hierarchy that binds the enum in a *generic*
 * intermediate interface (`interface Carrier<T : Enum<T>> : HasTypeDiscriminator<T>`, as
 * `it.neckar.domain.HasTypeDiscriminatorWithDomain` does) would only yield the unresolved type parameter `T`.
 *
 * [substitutions] maps the type parameter names of this declaration to the already resolved types the subtype bound
 * them to.
 */
private fun KSClassDeclaration.findTypeDiscriminatorEnum(substitutions: Map<String, KSType> = emptyMap()): KSClassDeclaration? {
  superTypes.forEach { superTypeReference ->
    val superType = superTypeReference.resolve()
    val superDeclaration = superType.declaration as? KSClassDeclaration ?: return@forEach

    //The arguments handed to the super type - a type parameter of *this* declaration is replaced by what the subtype bound it to
    val arguments = superType.arguments.map { argument ->
      val argumentType = argument.type?.resolve()
      when (val argumentDeclaration = argumentType?.declaration) {
        is KSTypeParameter -> substitutions[argumentDeclaration.name.asString()]
        else -> argumentType
      }
    }

    if (superDeclaration.qualifiedName?.asString() == HasTypeDiscriminatorFqName) {
      return arguments.firstOrNull()?.declaration as? KSClassDeclaration
    }

    val superSubstitutions = superDeclaration.typeParameters.withIndex().mapNotNull { (index, typeParameter) ->
      arguments.getOrNull(index)?.let { typeParameter.name.asString() to it }
    }.toMap()

    superDeclaration.findTypeDiscriminatorEnum(superSubstitutions)?.let { return it }
  }

  return null
}

