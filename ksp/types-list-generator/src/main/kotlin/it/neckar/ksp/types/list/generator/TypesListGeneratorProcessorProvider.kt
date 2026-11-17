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

import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider

/**
 * Provider for [TypesListGeneratorProcessor].
 * Entry point for KSP.
 *
 * ## Incremental Compilation Behavior
 *
 * This processor generates lists of all package names and class names in the module.
 * It uses **aggregating** dependencies with all source files, which means:
 *
 * - The generated files depend on ALL source files in the module
 * - When ANY source file changes, the lists are regenerated
 * - This ensures the type lists always reflect the complete module state
 * - A `passCount` guard ensures the processor only runs once per KSP invocation
 *
 * @see TypesListGenerationContext for accessing results programmatically
 */
class TypesListGeneratorProcessorProvider : SymbolProcessorProvider {
  val typesListGenerationContext: TypesListGenerationContext = TypesListGenerationContext()

  override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
    environment.logger.logging("Creating ${TypesListGeneratorProcessor::class.simpleName} for ${environment.platforms}")

    return TypesListGeneratorProcessor(codeGenerator = environment.codeGenerator, logger = environment.logger, options = environment.options, context = typesListGenerationContext)
  }
}
