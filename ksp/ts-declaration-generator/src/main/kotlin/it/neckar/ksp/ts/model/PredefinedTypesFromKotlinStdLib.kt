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


import it.neckar.ksp.ts.model.TsModuleReference.NativeType

/**
 * Contains predefined Kotlin types that are used in TypeScript generation.
 */
object PredefinedTypesFromKotlinStdLib {
  /**
   * Mapping from the qualified name to the corresponding TypeScript type.
   */
  private val predefinedTypes: Map<String, TsModuleReference> = mapOf(
    requireNotNull(kotlin.uuid.Uuid::class.qualifiedName) { "kotlin.uuid.Uuid must have a qualifiedName" } to TsModuleReference.KotlinNativeTypes,
  )

  fun find(fqName: String): TsModuleReference? {
    if (fqName.startsWith("org.w3c")) {
      return NativeType
    }

    if (fqName.startsWith("kotlin.js")) {
      return NativeType
    }

    if (fqName.startsWith("kotlin.")) {
      return KotlinNativeTypes
    }

    return predefinedTypes[fqName]
  }

  /**
   * Helper reference to the Kotlin Native types module.
   */
  val KotlinNativeTypes: TsModuleReference.KotlinNativeTypes = TsModuleReference.KotlinNativeTypes
}
