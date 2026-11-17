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

import assertk.*
import assertk.assertions.*
import it.neckar.gradle.ksp.generating.types.list.model.TypesListForProject
import it.neckar.gradle.ksp.generating.types.list.model.TypesListForProjectCollection
import it.neckar.gradle.ksp.options.KspPluginOptions
import it.neckar.ksp.test.KspTestUtils
import it.neckar.ksp.ts.TypeScriptDefinitionGenerationProcessorProvider
import it.neckar.ksp.ts.TypescriptDeclarationGenerator
import it.neckar.open.test.utils.isEqualIgnoringWhitespacesEmptyLines
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.io.StringWriter

/**
 *
 */
class TsSealedInterfaceTest {
  private val testSealedInterfaceFile = File("src/test/kotlin/it/neckar/ksp/ts/generation/sample/ExampleSealedInterface.kt")


  @BeforeEach
  fun setUp(@TempDir directory: File) {
    val typesListFile = directory.resolve("types-list-for-project-collection.json")
    KotlinModulesGuesser.clear()

    assertThat(testSealedInterfaceFile).isFile()

    val projectCollection = TypesListForProjectCollection(
      listOf(
        TypesListForProject(
          projectPath = "neckarIT-internal-open-commons-kotlin-lang",
          packageNames = listOf("it.neckar.ksp.ts.generation.sample"),
          classNames = listOf(
            "it.neckar.open.http.UrlPattern.Relative0"
          )
        )
      )
    )

    typesListFile.writeText(Json.encodeToString(projectCollection))

    unitTestOptions = mapOf(
      KspPluginOptions.TypeScriptDefinitionGeneration.TypeScriptDefinitionFileName to "index.kspunittest.d.ts",
      KspPluginOptions.TypeScriptDefinitionGeneration.TypesListForProjectCollectionFilePath to typesListFile.absolutePath,
    )
  }

  @AfterEach
  fun tearDown() {
    KotlinModulesGuesser.clear()
  }

  private lateinit var unitTestOptions: Map<String, String>


  private fun executeCollect(sourceFiles: List<File>): CollectingContext {
    val symbolProcessorProvider = TypeScriptDefinitionGenerationProcessorProvider()

    KspTestUtils.compileSourceFiles(
      sourceFiles = sourceFiles,
      symbolProcessorProvider = symbolProcessorProvider,
      options = unitTestOptions,
    )

    return symbolProcessorProvider.collectingContext
  }

  @Test
  fun testGeneration() {
    val collectingContext = executeCollect(listOf(testSealedInterfaceFile))

    assertThat(collectingContext.interfaces()).hasSize(1)
    assertThat(collectingContext.classes()).hasSize(2)

    val declarationGenerator = TypescriptDeclarationGenerator()

    val writer = StringWriter()
    declarationGenerator.generateDeclarationFile(collectingContext, writer)

    assertThat(writer.toString()).isEqualIgnoringWhitespacesEmptyLines(
      //language=TypeScript
      """
        /**
         * This is a sealed interface
         */
        export declare interface ExampleSealedInterface {
          bar: string;
        }

        export declare namespace ExampleSealedInterface {

          export declare class SealedClass1 implements ExampleSealedInterface {
            value: string;
            constructor(value: string);
          }

          export declare class SealedClass2 implements ExampleSealedInterface {
            value: string;
            constructor(value: string);
          }

        }
      """.trimIndent()
    )
  }

}
