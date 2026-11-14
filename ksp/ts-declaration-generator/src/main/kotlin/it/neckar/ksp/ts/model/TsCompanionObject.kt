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
data class TsCompanionObject(
  /**
   * The documentation
   */
  override val doc: String?,
  /**
   * The name of the kotlin source class/interface
   */
  override val kotlinSource: String,

  override val properties: List<TsProperty>,

  /**
   * The functions to create for this interface
   */
  override val functions: List<TsFunctionSignature>,

  ) : TsTypeDeclaration {

  override fun toTypescriptString(indentation: Indentation): String {
    if (properties.isEmpty() && functions.isEmpty()) return ""

    return buildString {
      appendLine("${indentation}static get Companion(): {\n")

      buildPropertiesAsGetters(indentation.increased())
      buildFunctionDeclarations(indentation.increased())

      appendLine("\n$indentation};")
    }
  }

  fun StringBuilder.buildPropertiesAsGetters(indentation: Indentation) {
    buildPropertiesAsGetters(properties, indentation)
  }

  override fun collectTypes(): Set<TsType> {
    return properties.flatMap { it.collectTypes() }.toSet() +
      functions.flatMap { it.collectTypes() }.toSet()
  }
}
