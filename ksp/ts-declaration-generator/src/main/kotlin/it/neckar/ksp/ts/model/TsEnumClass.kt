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
 * Represents a Kotlin enum as TypeScript (abstract) class
 */
data class TsEnumClass(
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

  val constructors: List<TsConstructor>,

  val values: List<TsEnumValue>,

  override val properties: List<TsProperty>,

  override val functions: List<TsFunctionSignature>,

  override val superTypes: List<TsType>,

  override val typeParameters: List<TsTypeParameter>,

  override val companionObject: TsCompanionObject?,

  ) : TsClassTypeDeclaration {

  override fun collectTypes(): Set<TsType> {
    return buildSet {
      addAll(constructors.flatMap { it.collectTypes() }.toSet())
      addAll(properties.flatMap { it.collectTypes() }.toSet())
      addAll(functions.flatMap { it.collectTypes() }.toSet())
      addAll(superTypes.flatMap { it.collectTypes() }.toSet())
      addAll(typeParameters.flatMap { it.collectTypes() }.toSet())

      if (companionObject != null) {
        addAll(companionObject.collectTypes())
      }
    }
  }

  override fun toTypescriptString(indentation: Indentation): String {
    return buildString {
      buildTypeDeclaration(indentation)

      addEnumValues(indentation.increased())
      buildProperties(indentation.increased())
      buildFunctionDeclarations(indentation.increased())
      buildEnumBoilerPlate(indentation.increased())
      buildCompanionObjectDeclaration(indentation.increased())

      appendLine("$indentation}")
    }
  }

  fun StringBuilder.buildTypeDeclaration(indentation: Indentation) {
    appendDoc(doc, indentation)
    append("$indentation")

    append("export declare abstract class ")
    append(name)
    appendTypeParameters(typeParameters)

    val interfaceSuperTypes = superTypes.filter { it.isInterface }
    val classSuperTypes = superTypes.filter { it.isClass } //TODO remove dirty null check
    if (classSuperTypes.size > 1) {
      error("Only one class super type is allowed but was [$classSuperTypes]")
    }

    if (classSuperTypes.isNotEmpty()) {
      append(" extends ")
      append(classSuperTypes.joinToString(", ") { it.toTypescriptTypeString(Indentation.None) })
    }

    if (interfaceSuperTypes.isNotEmpty()) {
      append(" implements ")
      append(interfaceSuperTypes.joinToString(", ") { it.toTypescriptTypeString(Indentation.None) })
    }

    append(" {")
    appendLine()
  }


  /**
   * Returns the union type of the name function
   */
  private fun createNameFunctionReturnType(): String {
    return values.joinToString(" | ") {
      """"${it.value}""""
    }
  }

  /**
   * Returns the ordinal function return type. E.g. "0 | 1 | 2"
   */
  private fun createOrdinalFunctionReturnType(): String {
    return values.indices.joinToString(" | ")
  }

  private fun StringBuilder.addEnumValues(indentation: Indentation) {
    values.forEachIndexed { index, enumValue ->
      appendDoc(enumValue.doc, indentation)
      appendLine("${indentation}static get ${enumValue.value}(): $name & {")
      appendLine()
      appendLine("${indentation.increased()}get name(): \"${enumValue.value}\";")
      appendLine()
      appendLine("${indentation.increased()}get ordinal(): ${index};")
      appendLine()
      appendLine("${indentation}};")
    }
  }

  private fun StringBuilder.buildEnumBoilerPlate(indentation: Indentation) {
    appendLine("${indentation}get name(): ${createNameFunctionReturnType()};")
    appendLine("${indentation}get ordinal(): ${createOrdinalFunctionReturnType()};")

    appendLine("${indentation}static values(): $name[];")
    appendLine("${indentation}static valueOf(name: string): $name;")
  }

  /**
   * Represents an enum value
   */
  data class TsEnumValue(
    val value: String,
    val doc: String? = null,
  )
}
