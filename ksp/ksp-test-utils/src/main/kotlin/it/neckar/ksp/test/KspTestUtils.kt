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
package it.neckar.ksp.test

import assertk.*
import assertk.assertions.*
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import com.tschuchort.compiletesting.configureKsp
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import java.io.File

/**
 * Runs a symbol processor against real sample sources in a unit test.
 *
 * A KSP processor produces no return value a test could inspect, so the pattern is: hand in a provider that keeps its
 * processor's collected state, compile, then assert on that state.
 *
 * ```kotlin
 * val symbolProcessorProvider = SpecHarvestProcessorProvider()
 * KspTestUtils.compileSourceFiles(
 *   sourceFiles = listOf(File("src/test/kotlin/it/neckar/ksp/spec/harvest/sample/SerializableSamples.kt")),
 *   symbolProcessorProvider = symbolProcessorProvider,
 * )
 * assertThat(symbolProcessorProvider.context.result).isNotEmpty()
 * ```
 */
@OptIn(ExperimentalCompilerApi::class)
object KspTestUtils {
  /**
   * Compiles the given source files with [symbolProcessorProvider] attached and fails the calling test unless the
   * compilation ends in [KotlinCompilation.ExitCode.OK].
   *
   * Generated files and processor results are not returned — read them off [symbolProcessorProvider] after this call.
   */
  fun compileSourceFiles(
    /**
     * Existing files on disk, read eagerly. Their simple names must be distinct: the compilation writes every source
     * into one flat directory, so two files named `Samples.kt` in different directories would overwrite each other and
     * only one of them would reach the processor.
     */
    sourceFiles: List<File>,
    /**
     * The provider the test later reads its expectations from. It is the only channel out of the compilation, so the
     * test must hold on to the instance it passes here and assert on that same one.
     */
    symbolProcessorProvider: SymbolProcessorProvider,

    /**
     * KSP processor options, keyed as the processor reads them (`KspPluginOptions.Common.ProjectDir` and friends).
     */
    options: Map<String, String> = emptyMap(),
  ) {
    require(sourceFiles.isNotEmpty()) { "No source files provided" }

    //Copy the contents instead of pointing at the files: SourceFile.fromPath leaves the sources outside the
    //compilation's own directory, where KSP does not reliably pick them up.
    val kotlinSourceFiles = sourceFiles.map { sourceFile ->
      val kotlinSource = SourceFile.kotlin(
        name = sourceFile.name,
        contents = sourceFile.readText(),
      )

      kotlinSource
    }

    val kotlinCompilation = KotlinCompilation().apply {
      sources = kotlinSourceFiles

      configureKsp {
        symbolProcessorProviders.add(symbolProcessorProvider)

        processorOptions.putAll(
          options
        )
      }

      // The sample sources reference types and annotations of the calling test module; only the inherited classpath
      // makes them resolvable to the in-process compiler.
      inheritClassPath = true
      messageOutputStream = System.out // see diagnostics in real time

      // When the project uses -progressive or pre-release Kotlin features, compiled classes
      // contain pre-release metadata. The kotlin-compile-testing compiler (from Gradle's
      // embedded Kotlin) may be an older minor version and would reject these classes without
      // this flag.
      kotlincArguments += "-Xskip-prerelease-check"
    }

    val compilationResult = kotlinCompilation.compile()
    assertThat(compilationResult).isNotNull()
    assertThat(compilationResult.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
  }

}
