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

import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.KSAnnotated
import it.neckar.ksp.isRunningForJsPlatform
import it.neckar.ksp.ts.model.CollectingContext

/**
 * Provider for [TypeScriptDefinitionGenerationProcessor]
 */
class TypeScriptDefinitionGenerationProcessorProvider : SymbolProcessorProvider {
  /**
   * The collecting context that will be used by the symbol processors
   */
  val collectingContext: CollectingContext = CollectingContext()

  override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
    environment.logger.logging("Creating ${TypeScriptDefinitionGenerationProcessor::class.simpleName} for ${environment.platforms}")

    if (environment.platforms.size != 1) {
      error("Only one platform is supported but was ${environment.platforms}")
    }

    if (isRunningInJUnit().not()) {
      //Unit Tests run with JVM environment, therefore, skip the platform check

      if (environment.isRunningForJsPlatform().not()) {
        environment.logger.logging("Not running on JS platform. Skipping")

        return object : SymbolProcessor {
          override fun process(resolver: Resolver): List<KSAnnotated> {
            return emptyList()
          }
        }
      }
    }

    return TypeScriptDefinitionGenerationProcessor(environment.codeGenerator, environment.logger, environment.options, collectingContext = collectingContext)
  }
}

private fun isRunningInJUnit(): Boolean {
  return Thread.currentThread().stackTrace.any { it.className.startsWith("org.junit.") }
}

