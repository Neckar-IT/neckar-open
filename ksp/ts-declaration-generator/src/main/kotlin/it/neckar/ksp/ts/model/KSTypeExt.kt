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
package it.neckar.ksp.ts.model

import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSTypeAlias
import com.google.devtools.ksp.symbol.KSTypeParameter
import com.google.devtools.ksp.symbol.KSTypeReference
import com.google.devtools.ksp.symbol.Origin
import it.neckar.ksp.relevantSealedSubclasses
import it.neckar.ksp.ts.collectTypeScriptName

/**
 * Converts a KSType into a TypeScript type.
 *
 * If this is null, an exception is thrown.
 */
fun KSType?.toTypescriptType(): TsType {
  checkNotNull(this) { "KSType cannot be null" }

  // If this is a type alias, resolve to the underlying type
  val declaration = this.declaration
  if (declaration is KSTypeAlias) {
    // Resolve the type alias to its underlying type
    val resolvedType = declaration.type.resolve()
    return resolvedType.toTypescriptType().withNullable(isMarkedNullable)
  }

  val qualifiedNameAsString = declaration.qualifiedName?.asString().requireNotNull { "Qualified name cannot be null for [$this]" }

  val declarationName = declaration.collectTypeScriptName()
  val isNullable = isMarkedNullable
  val nameSpace = when (val parentDeclaration = declaration.parentDeclaration) {
    is KSFunctionDeclaration, is KSPropertyDeclaration -> null
    is KSClassDeclaration -> if (declaration is KSTypeParameter) null else TsNamespace(parentDeclaration.collectTypeScriptName())
    null -> null
    else -> throw IllegalStateException("Unexpected parent declaration type: ${parentDeclaration.javaClass}")
  }

  // Match Kotlin builtins by qualified name, not simple name: a domain type whose simple name
  // collides with a builtin (e.g. a type named `String`, `List`, `Array`) must route through `else`.
  return when (qualifiedNameAsString) {
    "kotlin.Any" -> TsType.Any.withNullable(isNullable)
    "kotlin.Unit" -> TsType.Void
    "kotlin.Nothing" -> TsType.Never
    "kotlin.String" -> TsType.String.withNullable(isNullable).withNamespace(nameSpace)
    "kotlin.Int", "kotlin.Long", "kotlin.Short", "kotlin.Byte", "kotlin.Double", "kotlin.Float" -> TsType.Number.withNullable(isNullable).withNamespace(nameSpace)
    "kotlin.Boolean" -> TsType.Boolean.withNullable(isNullable).withNamespace(nameSpace)

    "kotlin.collections.List" -> {
      check(arguments.size == 1) { "List must have exactly one type argument" }
      val type = this.arguments.first().type.toTypescriptType()
      type.asArray().orNullable(isNullable)
    }

    "kotlin.Array" -> {
      check(arguments.size == 1) { "Array must have exactly one type argument" }
      val type = this.arguments.first().type.toTypescriptType()
      type.asArray().orNullable(isNullable)
    }

    "kotlin.Function1" -> {
      check(arguments.size == 2) { "Function1 must have exactly two type arguments" }

      // When implemented:
      //   val parameterType = this.arguments.first().type.toTypescriptType()
      //   val returnType    = this.arguments.last().type.toTypescriptType()
      TODO("not implemented for function1")
    }

    else -> {
      //find the module name where the type is declared, and we want to import from
      val moduleReference = declaration.findModuleReference()

      val classKind = when (declaration) {
        is KSClassDeclaration -> {
          when (declaration.classKind) {
            ClassKind.INTERFACE -> TsType.ClassKind.Interface
            ClassKind.CLASS -> TsType.ClassKind.Class
            ClassKind.ENUM_CLASS -> TsType.ClassKind.Class
            ClassKind.ENUM_ENTRY, ClassKind.ANNOTATION_CLASS, ClassKind.OBJECT -> {
              throw IllegalStateException("Unexpected class kind: ${declaration.classKind} for $qualifiedNameAsString")
            }
          }
        }

        is KSTypeParameter -> TsType.ClassKind.TypeParameter

        else -> {
          throw IllegalStateException("Unexpected declaration type: ${declaration::class.qualifiedName}")
        }
      }

      val sealedSubTypes = if (declaration is KSClassDeclaration) {
        declaration.relevantSealedSubclasses().toList().map { it.asStarProjectedType().toTypescriptType() }
      } else {
        emptyList()
      }

      return TsType(
        name = declarationName,
        namespace = nameSpace,
        isNullable = isNullable,
        isArray = false,
        typeArguments = arguments.mapNotNull { it.type?.toTypescriptType() },
        classKind = classKind,
        sealedSubTypes = sealedSubTypes,
        importedFrom = moduleReference,
      )
    }
  }
}


/**
 * Finds the module reference for the given declaration.
 */
private fun KSDeclaration.findModuleReference(): TsModuleReference {
  if (this is KSTypeParameter) {
    return TsModuleReference.GenericType
  }

  if (origin != Origin.KOTLIN_LIB && origin != Origin.JAVA_LIB) {
    //Same project
    return TsModuleReference.SameFile
  }

  val qualifiedNameAsString = qualifiedName?.asString().requireNotNull { "Qualified name cannot be null for [${this}]" }

  // Check if the type is a predefined Kotlin type (e.g., Uuid)
  PredefinedTypesFromKotlinStdLib.find(qualifiedNameAsString)?.let { return it }

  // Guess the project path
  val guessedProjectPath = KotlinModulesGuesser.guessProjectPath(qualifiedNameAsString)

  if (guessedProjectPath == null) {
    throw IllegalStateException("Cannot guess project path for [$qualifiedNameAsString]. ${KotlinModulesGuesser.registeredClassesCount()} classes registered. Ensure Gradle plugin `generateTypesList` has been registered in the project providing the types.")
  }

  return TsModuleReference.ProjectModuleReference(guessedProjectPath)
}

/**
 * Has been copied to be able to be used in the KSP context.
 */
private inline fun String?.requireNotNull(messageProvider: () -> String): String {
  return this ?: throw IllegalStateException(messageProvider())
}

fun KSTypeReference?.toTypescriptType(): TsType {
  requireNotNull(this) { "KSTypeReference cannot be null" }
  val resolved = resolve()
  return resolved.toTypescriptType()
}
