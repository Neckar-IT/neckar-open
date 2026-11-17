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
package it.neckar.ksp.ts

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSType
import it.neckar.ksp.format
import it.neckar.ksp.getValueType
import it.neckar.ksp.isPrimitiveArray
import it.neckar.ksp.isValueClass

/**
 * Converts kotlin types to TypeScript types.
 */
object TypeScriptTypeSupport {
  /**
   * Returns the TypeScript type name for the given type.
   */
  fun toTypescriptType(type: KSType, logger: KSPLogger): String {
    val typeDeclaration: KSDeclaration = type.declaration

    val qualifiedName: String = typeDeclaration.qualifiedName?.asString() ?: throw IllegalArgumentException("No qualified name for ${typeDeclaration.simpleName.asString()} @ ${typeDeclaration.location.format()}")

    if (qualifiedName == "kotlin.Unit") {
      return "void"
    }

    val nullableSuffix = when {
      type.isMarkedNullable -> " | null"
      else -> ""
    }

    return when {
      typeDeclaration.isPrimitiveArray() -> {
        typeDeclaration.simpleName.getShortName().toTypeScriptType() + "[]$nullableSuffix"
      }

      qualifiedName == "kotlin.collections.List" || qualifiedName == "kotlin.Array" -> {
        val typeArguments = type.arguments

        require(typeArguments.size == 1) {
          "Expected exactly one type argument for Array, but got $typeArguments"
        }

        val firstTypeArgument = typeArguments.first()
        val firstTypeArgumentResolved = firstTypeArgument.type?.resolve()
        requireNotNull(firstTypeArgumentResolved) {
          "Could not resolve type argument $firstTypeArgument"
        }

        val typescriptType = toTypescriptType(firstTypeArgumentResolved, logger)
        if (typescriptType.contains(" ")) {
          "($typescriptType)[]$nullableSuffix"
        } else {
          "$typescriptType[]$nullableSuffix"
        }
      }

      else -> {
        if (typeDeclaration.isValueClass()) {
          val valueType = typeDeclaration.getValueType().resolve()
          valueType.declaration.simpleName.getShortName().toTypeScriptType() + nullableSuffix
        } else {
          typeDeclaration.simpleName.getShortName().toTypeScriptType() + nullableSuffix
        }
      }
    }
  }


  private val kotlin2tsTypeMap = mapOf(
    "String" to "string",
    "Int" to "number",
    "Double" to "number",
    "Boolean" to "boolean",
    "IntArray" to "Int32Array",
    "DoubleArray" to "number[]",
  )

  /**
   * Maps the kotlin type to a typescript type
   */
  fun mapKotlinType2ts(kotlinType: String): String {
    return kotlin2tsTypeMap[kotlinType] ?: kotlinType
  }

  private fun String.toTypeScriptType(): String {
    return mapKotlinType2ts(this)
  }

  /**
   * Returns the optional marker for the provided type.
   * Returns a blank string if the type is not nullable.
   */
  fun getOptionalMarker(ksType: KSType): String {
    return if (ksType.isMarkedNullable) {
      "?"
    } else {
      ""
    }
  }
}
