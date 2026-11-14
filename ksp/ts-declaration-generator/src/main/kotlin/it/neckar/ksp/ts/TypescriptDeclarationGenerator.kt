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

import it.neckar.ksp.ts.model.CollectingContext
import it.neckar.ksp.ts.model.HasNamespace
import it.neckar.ksp.ts.model.Indentation
import it.neckar.ksp.ts.model.TsCodeFragmentProvider
import it.neckar.ksp.ts.model.TsCodeFragmentProviderWithNamespace
import it.neckar.ksp.ts.model.TsModuleReference
import it.neckar.ksp.ts.model.TsType
import java.io.Writer

/**
 * Generates the (default) TypeScript declaration for the given Kotlin class.
 */
class TypescriptDeclarationGenerator {
  fun generateDeclarationFile(collectingContext: CollectingContext, writer: Writer) {
    writer.writeImports(collectingContext.collectTypesToImport())
    writer.write("\n")

    //Paint all top level elements
    writer.writeTypescriptDefinition(collectingContext.collectTopLevelElements())

    //Generate elements within the namespaces
    writer.writeTypescriptDefinitionForElementsWithNamespace(collectingContext.collectElementsWithNamespace())
  }
}

/**
 * Writes TypeScript definitions for the provided code fragments
 */
fun Writer.writeTypescriptDefinition(codeFragments: Collection<TsCodeFragmentProvider>, indentation: Indentation = Indentation.None) {
  codeFragments.forEach { tsCodeFragment ->

    if (tsCodeFragment is HasNamespace) {
      //Ensure no namespace
      require(tsCodeFragment.namespace == null) {
        "Cannot write class with namespace here: ${tsCodeFragment.name}"
      }
    }

    appendLine(tsCodeFragment.toTypescriptString(indentation))
    appendLine()
  }
}

/**
 * Writes the TypeScript definition for the provided nested classes
 */
fun Writer.writeTypescriptDefinitionForElementsWithNamespace(nestedClasses: List<TsCodeFragmentProviderWithNamespace>, indentation: Indentation = Indentation.None) {
  val classesByNamespaces = nestedClasses.groupBy {
    requireNotNull(it.namespace) {
      "Namespace required when calling this class: ${it.name} - ${it::class.simpleName}"
    }
  }

  classesByNamespaces.forEach { namespace, nestedClasses ->
    appendLine("${indentation}export declare namespace ${namespace.name} {\n")

    nestedClasses.forEach { nestedClass ->
      appendLine(nestedClass.toTypescriptString(indentation.increased()))
    }

    appendLine("$indentation}")
  }
}

/**
 * Writes the imports for the provided references
 */
fun Writer.writeImports(references: Collection<TsType>) {
  //The basic types do not contain `isArray` or `isNullable` information anymore
  val basicTypes = references.map { it.basicType() }

  val groupedByImportedFrom = basicTypes
    .toSet()
    .groupBy { it.importedFrom }

  groupedByImportedFrom.forEach { (moduleReference, types) ->
    when (moduleReference) {
      is TsModuleReference.NoImportRequired -> {
        //no import required, ignore
      }

      is TsModuleReference.RequiresImport -> {
        val namespaces = types.mapNotNull { it.namespace }.toSortedSet()
        if (namespaces.isNotEmpty()) {
          val namespacesString = namespaces.joinToString(
            separator = ", ",
          ) { it.name }
          appendLine("import { $namespacesString } from \"./${moduleReference.moduleFileNameBase()}\";")
        }

        val typesToImport = types
          .filter { type -> type.namespace == null }
          .filter { type -> namespaces.none { it.name == type.name } } //remove classes that have already been imported as namespaces

        if (typesToImport.isNotEmpty()) {
          val typesString = typesToImport.joinToString(
            separator = ", ",
          ) { it.name }

          appendLine("import { $typesString } from \"./${moduleReference.moduleFileNameBase()}\";")
        }
      }
    }
  }
}
