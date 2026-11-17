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
 * Contains information about an interface to create
 */
data class TsAbstractClass(
  /**
   * The name of the interface/class
   */
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

  val constructors: List<TsConstructor>,

  /**
   * The functions to create for this interface
   */
  override val functions: List<TsFunctionSignature>,

  override val companionObject: TsCompanionObject?,

  ) : TsClassTypeDeclaration {

  init {
    require(superTypes.none { it.isNullable }) { "Nullable types are not allowed in superTypes ($superTypes)" }
    require(superTypes.size <= 1) { "Only one super type is allowed for classes" }
  }

  override fun toTypescriptString(indentation: Indentation): String {
    return buildString {
      buildTypeDeclaration(indentation)
      appendLine()

      buildProperties(indentation.increased())
      // buildConstructorDeclarations(this, increasedIndentation)
      buildFunctionDeclarations(indentation.increased())
      buildCompanionObjectDeclaration(indentation.increased())

      append("\n$indentation}")
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

  fun StringBuilder.buildConstructorDeclarations(indentation: Indentation) {
    constructors.forEach {
      appendLine("${it.toTypescriptString(indentation)};")
    }
  }

  /**
   * Returns all types that are used in this class
   */
  override fun collectTypes(): Set<TsType> {
    return buildSet {
      addAll(superTypes.flatMap { it.collectTypes() })
      addAll(typeParameters.flatMap { it.collectTypes() })
      addAll(properties.flatMap { it.collectTypes() })
      addAll(constructors.flatMap { it.collectTypes() })
      addAll(functions.flatMap { it.collectTypes() })

      companionObject?.collectTypes()?.let {
        addAll(it)
      }
    }
  }
}
