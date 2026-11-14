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

/**
 * Represents a const object
 */
data class TsConstObject(
  override val name: String,

  /**
   * The (optional) namespace for this class/interface.
   */
  override val namespace: TsNamespace? = null,

  /**
   * The documentation
   */
  override val doc: String?,
  /**
   * The name of the kotlin source class/interface
   */
  override val kotlinSource: String,
  /**
   * The supertypes
   */
  override val superTypes: List<TsType>,

  override val typeParameters: List<TsTypeParameter>,

  override val properties: List<TsProperty>,

  override val functions: List<TsFunctionSignature>,

  ) : TsClassTypeDeclaration {

  override val companionObject: TsCompanionObject?
    get() = null

  override fun toTypescriptString(indentation: Indentation): String {
    return buildString {
      appendDoc(doc, indentation)
      appendLine("${indentation}export declare const $name: {")

      appendLine("${indentation.increased()}getInstance(): {")

      buildPropertiesAsGetters(properties, indentation.increased(2))

      buildFunctionDeclarations(functions, indentation.increased(2))

      appendLine()
      append("${indentation.increased()}")
      append("}")
      if (superTypes.isNotEmpty()) {
        append(superTypes.joinToString(" & ", prefix = " & ") { it.toTypescriptTypeString() })
      }
      append(";")

      appendLine("\n$indentation};")
    }
  }

  fun toTypescriptModuleString(indentation: Indentation): String {
    return buildString {
      appendLine("${indentation}const $InstanceName = _$name.getInstance();")

      appendDoc(doc, indentation)
      appendLine("${indentation}export const $name: {")

      buildPropertiesAsTypeDeclarations(properties, indentation.increased())
      buildFunctionTypeDeclarations(functions, indentation.increased())

      appendLine("${indentation}} = {")

      buildPropertiesAsWrappers(properties, InstanceName, indentation.increased())

      buildFunctionWrappers(functions, InstanceName, indentation.increased())

      appendLine("$indentation};")
    }
  }

  override fun collectTypes(): Set<TsType> {
    return superTypes.flatMap { it.collectTypes() }.toSet() +
      typeParameters.flatMap { it.collectTypes() }.toSet() +
      properties.flatMap { it.collectTypes() }.toSet() +
      functions.flatMap { it.collectTypes() }.toSet()
  }

  companion object {
    const val InstanceName: String = "_instance"
  }
}
