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
 * Appends a list of properties
 */
fun StringBuilder.buildProperties(properties: List<TsProperty>, indentation: Indentation) {
  properties.forEach { property ->
    appendLine(property.toTypescriptString(indentation))
  }
}

fun StringBuilder.buildPropertiesAsTypeDeclarations(properties: List<TsProperty>, indentation: Indentation) {
  properties.forEach { property ->
    appendLine(property.toTypescriptTypeDeclarationString(indentation))
  }
}

fun StringBuilder.buildPropertiesAsWrappers(properties: List<TsProperty>, instanceSource: String, indentation: Indentation) {
  properties.forEach { property ->
    appendLine(property.toTypescriptWrapperString(instanceSource, indentation))
  }
}

fun StringBuilder.buildPropertiesAsGetters(properties: List<TsProperty>, indentation: Indentation) {
  properties.forEach { property ->
    appendLine(property.toTypescriptGetterString(indentation))
  }
}

fun StringBuilder.buildFunctionDeclarations(functions: List<TsFunctionSignature>, indentation: Indentation) {
  functions.forEach {
    appendLine(it.toTypescriptString(indentation))
  }
}

fun StringBuilder.buildFunctionTypeDeclarations(functions: List<TsFunctionSignature>, indentation: Indentation) {
  functions.forEach {
    appendLine(it.toTypescriptTypeDeclarationString(indentation))
  }
}

fun StringBuilder.buildFunctionWrappers(functions: List<TsFunctionSignature>, instanceSource: String, indentation: Indentation) {
  functions.forEach {
    appendLine(it.toTypescriptWrapperString(instanceSource, indentation))
  }
}

fun StringBuilder.buildTopLevelFunctionDeclarations(functions: List<TsTopLevelFunction>, indentation: Indentation) {
  functions.forEach {
    appendLine(it.toTypescriptString(indentation))
  }
}

/**
 * Generates the type parameters for a class
 */
fun StringBuilder.appendTypeParameters(typeParameters: List<TsTypeParameter>) {
  if (typeParameters.isNotEmpty()) {
    append("<")
    append(typeParameters.joinToString(", ") { it.toTypescriptString(Indentation.None) })
    append(">")
  }
}
