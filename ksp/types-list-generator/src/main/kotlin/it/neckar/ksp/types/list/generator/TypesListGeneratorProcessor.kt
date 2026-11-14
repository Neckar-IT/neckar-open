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
package it.neckar.ksp.types.list.generator

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.KSTypeAlias
import it.neckar.gradle.ksp.options.KspPluginOptions
import it.neckar.ksp.getInnerClassLikesRecursively
import it.neckar.ksp.isClassLike
import it.neckar.ksp.isCompanionObject
import it.neckar.ksp.isEnumOption
import it.neckar.open.generated.GeneratedFileCommentSyntax
import it.neckar.open.generated.GeneratedFileMarker

/**
 * Creates lists containing all package names and class names within the module.
 *
 * ## Generated Files
 *
 * - `{packageNamesListFileName}.list`: All package names in the module
 * - `{classNamesListFileName}.list`: All class names (including type aliases) in the module
 *
 * ## Incremental Compilation
 *
 * This processor uses **aggregating** dependencies, meaning the generated files
 * depend on ALL source files in the module. When any source file changes,
 * the lists will be regenerated to ensure they remain complete and accurate.
 *
 * ## Multi-Round Processing
 *
 * This processor only runs once per KSP invocation (guarded by `passCount`).
 * Subsequent rounds are skipped to avoid duplicate file generation errors.
 *
 * @see TypesListGeneratorProcessorProvider for provider details
 */
class TypesListGeneratorProcessor(
  val codeGenerator: CodeGenerator,
  val logger: KSPLogger,
  val options: Map<String, String>,
  /**
   * The context that will contain the results of the processing.
   * Only populated during the first processing pass.
   */
  val context: TypesListGenerationContext,

  ) : SymbolProcessor {
  private var passCount = 0

  @OptIn(KspExperimental::class)
  override fun process(resolver: Resolver): List<KSAnnotated> {
    if (passCount > 0) {
      // Guard against multi-round processing: only run once per KSP invocation
      logger.logging("Already run $passCount times. Skipping.")
      passCount++
      return emptyList()
    }
    logger.logging("Processing symbols in [TypesListGeneratorProcessor] (pass $passCount)")
    passCount++

    val packageNamesListFileName = KspPluginOptions.TypesListGenerator.PackageNamesListFileName.get(options)
    val classNamesListFileName = KspPluginOptions.TypesListGenerator.ClassNamesListFileName.get(options)

    //Figure out which configuration (JVM, JS, Native) we are running on
    logger.logging("module name: ${resolver.getModuleName().asString()}")

    // Collect all source files for aggregating dependencies.
    // This ensures the generated lists are regenerated when ANY source file changes.
    val allSourceFiles: List<KSFile> = resolver.getAllFiles().toList()
    val aggregatingDependencies = Dependencies(
      aggregating = true,
      sources = allSourceFiles.toTypedArray()
    )

    val marker = GeneratedFileMarker(
      by = "types-list-generator",
      source = "the Kotlin sources of the module",
      regenerateHint = "Rebuild the module.",
    ).render(GeneratedFileCommentSyntax.NumberSign)

    val packageNames = allSourceFiles.map {
      logger.logging("File: ${it.fileName}")
      it.packageName.asString()
    }.toSet()
      .toList()
      .sorted()

    codeGenerator.createNewFile(
      dependencies = aggregatingDependencies,
      packageName = "it.neckar.types",
      fileName = packageNamesListFileName.removeSuffix(".list"),
      extensionName = "list"
    ).bufferedWriter()
      .use { writer ->
        context.packageNames = packageNames

        writer.write(marker)
        packageNames.forEach {
          writer.appendLine(it)
        }
      }


    //The class names
    val topLevelClassNames = allSourceFiles.flatMap { file ->
      file.declarations
        .filter { declaration ->
          declaration.isClassLike()
        }.flatMap { declaration ->
          declaration as KSClassDeclaration
          sequenceOf(declaration) + declaration.getInnerClassLikesRecursively()
        }.filter {
          it.isCompanionObject().not() //skip companion objects
            && it.isEnumOption().not() //skip enum options
        }.map { declaration ->
          declaration.qualifiedName?.asString()
        }.filterNotNull()
    }

    //Type aliases (e.g., NativeUuid, NativeInstant)
    val typeAliasNames = allSourceFiles.flatMap { file ->
      file.declarations
        .filterIsInstance<KSTypeAlias>()
        .mapNotNull { typeAlias ->
          typeAlias.qualifiedName?.asString()
        }
    }

    val allClassNames = buildClassNamesList(topLevelClassNames, typeAliasNames)


    codeGenerator.createNewFile(
      dependencies = aggregatingDependencies,
      packageName = "it.neckar.types",
      fileName = classNamesListFileName.removeSuffix(".list"),
      extensionName = "list"
    ).bufferedWriter()
      .use { writer ->
        context.classNames = allClassNames

        writer.write(marker)
        allClassNames.forEach {
          writer.appendLine(it)
        }
      }

    return emptyList()
  }
}

/**
 * Combines the class names and the type alias names of a module into the content of the class names list file.
 *
 * Duplicates are removed - just like for the package names. A Kotlin Multiplatform module hands the `expect`
 * declaration and its `actual` counterpart to the same KSP invocation as two source files, and both resolve to the
 * same qualified name (e.g. `it.neckar.uuid.UuidSerializer` from `commonMain` and `jvmMain`).
 */
internal fun buildClassNamesList(classNames: List<String>, typeAliasNames: List<String>): List<String> {
  return (classNames + typeAliasNames)
    .distinct()
    .sorted()
}

