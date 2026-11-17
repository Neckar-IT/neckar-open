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

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import it.neckar.gradle.ksp.generating.types.list.model.Class2ProjectMapping
import it.neckar.gradle.ksp.generating.types.list.model.TypesListForProjectCollection
import it.neckar.gradle.ksp.options.KspPluginOptions
import it.neckar.ksp.ts.model.CollectingContext
import it.neckar.ksp.ts.model.KotlinModulesGuesser
import it.neckar.open.annotations.TsExport
import it.neckar.open.generated.GeneratedFileCommentSyntax
import it.neckar.open.generated.GeneratedFileMarker
import java.io.File
import java.io.Writer

/**
 * Generates the TypeScript definition file
 * Collects all symbols annotated with the @JsExport and @TsExport annotation and generates the TypeScript definition file
 */
class TypeScriptDefinitionGenerationProcessor(
  val codeGenerator: CodeGenerator,
  val logger: KSPLogger,
  val options: Map<String, String>,

  /**
   * The collecting context that is used to collect all types that are annotated with the annotation to export.
   */
  val collectingContext: CollectingContext = CollectingContext(),
) : SymbolProcessor {

  override fun process(resolver: Resolver): List<KSAnnotated> {
    //The file that is created
    val typeScriptDefinitionFileName = KspPluginOptions.TypeScriptDefinitionGeneration.TypeScriptDefinitionFileName.get(options)
    require(typeScriptDefinitionFileName.endsWith(".d.ts")) {
      "The TypeScript definition file name must end with `.d.ts` but was `$typeScriptDefinitionFileName`"
    }

    //the types-list file (must have been generated beforehand)
    val typesListForProjectCollectionFilePath = KspPluginOptions.TypeScriptDefinitionGeneration.TypesListForProjectCollectionFilePath.get(options)
    logger.logging("Types list for project collection file path: $typesListForProjectCollectionFilePath")


    val typesListForProjectCollectionFile = File(typesListForProjectCollectionFilePath)
    if (typesListForProjectCollectionFile.exists().not()) {
      logger.error("The types list for project collection file does not exist: $typesListForProjectCollectionFilePath")
      return emptyList()
    }

    val typesListForProjectCollection: TypesListForProjectCollection = typesListForProjectCollectionFile.inputStream().use { input ->
      TypesListForProjectCollection.load(input)
    }
    val class2ProjectMapping = Class2ProjectMapping.loadFromTypesListCollection(
      typesListForProjectCollection
    )

    if (false) {
      logger.logging("Loaded class2ProjectMapping: $class2ProjectMapping")
      class2ProjectMapping.mapping.forEach {
        logger.logging("  ${it.key} -> ${it.value}")
      }
    }

    KotlinModulesGuesser.registerClass2ProjectMapping(class2ProjectMapping)

    //Collect the relevant classes/objects first
    val visitor = TypeScriptDefinitionCollectorVisitor(
      logger = logger,
      options = options,
    )

    /**
     * Get all symbols that are annotated with the annotation @JsExport or @TsExport
     */
    val symbolsWithAnnotation = exportAnnotationNames.flatMap {
      resolver.getSymbolsWithAnnotation(it)
    }.toSet()

    /**
     * When executing this code after the first time, all symbols will have been marked as processed and we can skip the processing
     * If we or another processor create new files with new annotated symbols, the following code will try to run again, resulting in a [FileAlreadyExistsException]
     * In most cases, this should cleanly check if the file exists and skip the processing
     */
    if (symbolsWithAnnotation.isEmpty()) {
      logger.logging("No symbols found with annotations [$exportAnnotationNames]")
      logger.logging("Skipping")
      return emptyList()
    } else {
      logger.logging("Found ${symbolsWithAnnotation.size} symbols with annotations [$exportAnnotationNames]")
    }

    symbolsWithAnnotation.forEach { annotated ->
      logger.logging("Found annotated symbol: ${annotated.location}")

      annotated.accept(
        visitor = visitor,
        data = collectingContext,
      )
    }

    val baseFileName = typeScriptDefinitionFileName.removeSuffix(".d.ts")

    //
    // Generate the TypeScript definition file
    //
    writeMarkedFile(path = baseFileName, extensionName = "d.ts") { writer ->
      TypescriptDeclarationGenerator().generateDeclarationFile(
        collectingContext = collectingContext,
        writer = writer,
      )
    }

    logger.logging("Creating TypeScript definition file: $baseFileName")

    //
    //Export the objects - in separate files!
    //
    val objectTsGenerator = ConstObjectTsGenerator()

    collectingContext.constObjects()
      .filter { it.properties.isNotEmpty() || it.functions.isNotEmpty() }
      .forEach { constObject ->
        writeMarkedFile(path = "$baseFileName.${constObject.name}", extensionName = "ts") { writer ->
          objectTsGenerator.generateTsFile(
            constObject = constObject,
            writer = writer,
            ownModuleBaseName = baseFileName,
          )
        }
      }

    return emptyList()
  }

  /**
   * The processor's single write path: it puts the marker in before [writeContent] gets the writer,
   * so the next output file cannot be written without one.
   */
  private fun writeMarkedFile(path: String, extensionName: String, writeContent: (Writer) -> Unit) {
    codeGenerator.createNewFileByPath(
      dependencies = Dependencies.ALL_FILES,
      path = path,
      extensionName = extensionName,
    ).bufferedWriter().use { writer ->
      writer.write(
        GeneratedFileMarker(
          by = GeneratedBy,
          source = "the Kotlin declarations annotated with @JsExport or @TsExport",
          regenerateHint = "Change the Kotlin declaration and rebuild.",
        ).render(GeneratedFileCommentSyntax.DoubleSlash)
      )
      writeContent(writer)
    }
  }


  companion object {
    /** What the marker names as author — the processor, so all its output agrees on one name. */
    const val GeneratedBy: String = "ts-declaration-generator"

    /**
     * The annotation names for the export annotations
     */
    val exportAnnotationNames: Set<String> = setOf(
      "kotlin.js.JsExport",
      requireNotNull(TsExport::class.qualifiedName) { "TsExport annotation class must have a qualifiedName" },
    )
  }
}
