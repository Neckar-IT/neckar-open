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
package it.neckar.ksp.spec.harvest

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.Origin
import com.google.devtools.ksp.validate
import it.neckar.ksp.spec.harvest.model.HarvestedClassSpec
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToStream

/**
 * KSP processor that collects metadata for `@Serializable` types.
 *
 * ## How it Works
 *
 * For each class annotated with `@Serializable`, this processor:
 * 1. Extracts metadata (properties, annotations, documentation, etc.)
 * 2. Writes a JSON file to `harvested-specs/{qualifiedName}.json`
 * 3. Adds the result to [context] for immediate access (testing only)
 *
 * ## Incremental Compilation
 *
 * This processor uses **isolating** dependencies (`Dependencies(false, source)`),
 * meaning each generated JSON file depends only on its source file. This enables
 * efficient incremental builds where only changed classes are reprocessed.
 *
 * The individual JSON files are later merged by the Gradle [MergeSpecHarvestResultTask]
 * into a single aggregated result file.
 *
 * ## Multi-Round Processing
 *
 * KSP may invoke this processor multiple times (rounds) if:
 * - Other processors generate new `@Serializable` classes
 * - Some symbols couldn't be validated in earlier rounds
 *
 * The processor handles this by deferring unvalidated symbols to later rounds.
 *
 * @see SpecHarvestProcessorProvider for provider and context details
 */
class SpecHarvestProcessor(
  val codeGenerator: CodeGenerator,

  private val logger: KSPLogger,
  private val options: Map<String, String>,

  private val harvester: SpecHarvester = SpecHarvester(logger),

  private val json: Json = Json {
    prettyPrint = true
    encodeDefaults = true
    explicitNulls = false
  },
  /**
   * The context for sharing results with the provider.
   * Note: Only contains results from the current KSP run; incomplete during incremental builds.
   */
  val context: SpecHarvestProcessorProvider.Context,
) : SymbolProcessor {

  private var passCount = 0

  override fun process(resolver: Resolver): List<KSAnnotated> {
    logger.logging("Processing symbols in [SpecHarvestProcessor] (pass $passCount)")
    passCount++

    val serializableClassDeclarations = resolver
      .getSymbolsWithAnnotation(SerializableAnnotationFqName)
      .also {
        logger.logging("Found ${it.count()} symbols with @$SerializableAnnotationFqName annotation")
      }
      .filterIsInstance<KSClassDeclaration>()
      .also {
        logger.logging("Filtered to ${it.count()} class declarations")
      }

    val deferredSymbols = mutableListOf<KSAnnotated>()

    serializableClassDeclarations.forEach { classDeclaration ->
      if (classDeclaration.origin == Origin.JAVA || classDeclaration.origin == Origin.JAVA_LIB) {
        logger.warn("Skipping Java declaration ${classDeclaration.qualifiedName?.asString()} - only Kotlin sources are supported")
        return@forEach
      }

      if (classDeclaration.validate().not()) {
        deferredSymbols += classDeclaration
        return@forEach
      }

      val harvestedResult: HarvestedClassSpec = harvester.harvest(classDeclaration)
      writeResult(harvestedResult, classDeclaration)

      context.result.add(harvestedResult)
    }

    if (deferredSymbols.isNotEmpty()) {
      // Names every class, because a deferral surviving the last round is the only trace of a type that does not resolve.
      val deferredNames = deferredSymbols.joinToString(", ") { (it as KSClassDeclaration).qualifiedName?.asString() ?: it.toString() }
      logger.error(
        "Deferring ${deferredSymbols.size} @Serializable class(es) for later processing: $deferredNames. " +
          "A class stays deferred when a type it names (property, supertype, annotation) does not resolve - " +
          "check its imports and supertypes, then run compileKotlin for the unresolved reference.",
      )
      return deferredSymbols
    }

    return emptyList()
  }

  /**
   * Writes the harvested result to a JSON file.
   *
   * Uses isolating dependencies (`aggregating = false`) so that:
   * - Each JSON file is only regenerated when its source file changes
   * - Incremental builds remain efficient
   * - KSP correctly tracks the dependency between source and generated file
   */
  fun writeResult(harvestedResult: HarvestedClassSpec, classDeclaration: KSClassDeclaration) {
    val fileNameBase = harvestedResult.qualifiedName

    logger.logging("Writing spec harvest to file: $fileNameBase")

    val source = classDeclaration.containingFile
    requireNotNull(source) {
      "Containing file not found for class declaration: ${classDeclaration.qualifiedName?.asString()}"
    }

    codeGenerator.createNewFileByPath(
      // Isolating dependency: this JSON file depends only on this source file.
      // This enables efficient incremental compilation.
      dependencies = Dependencies(aggregating = false, source),
      path = "harvested-specs/$fileNameBase",
      extensionName = "json"
    ).buffered().use { outputStream ->
      json.encodeToStream(harvestedResult, outputStream)
    }

    logger.logging("Spec harvest written")
  }

  companion object {
    private val SerializableAnnotationFqName: String = requireNotNull(Serializable::class.qualifiedName) { "Serializable must have a qualified name" }
  }
}
