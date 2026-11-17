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
@file:Suppress("unused", "RETURN_VALUE_NOT_USED")

package it.neckar.ksp.ts.generation

import assertk.*
import assertk.assertions.*
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import com.tschuchort.compiletesting.configureKsp
import com.tschuchort.compiletesting.kspSourcesDir
import it.neckar.gradle.ksp.generating.types.list.model.TypesListForProject
import it.neckar.gradle.ksp.generating.types.list.model.TypesListForProjectCollection
import it.neckar.ksp.ts.TypeScriptDefinitionGenerationProcessorProvider
import it.neckar.ksp.ts.model.TsConstObject
import it.neckar.open.file.file
import it.neckar.open.resources.getResourceSafe
import kotlinx.serialization.json.Json
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.io.FileFilter

/**
 * Tests collecting of the model to create
 */
@OptIn(ExperimentalCompilerApi::class)
@Deprecated("extract to own methods")
class InlineCollectingTest {

  @BeforeEach
  fun setUp(@TempDir directory: File) {
    val typesListFile = directory.resolve("types-list-for-project-collection.json")
    val projectCollection = TypesListForProjectCollection(
      listOf(
        TypesListForProject(
          projectPath = ":internal:open:test:foobar",
          packageNames = listOf("it.neckar.ksp.ts.generation.sample"),
          classNames = listOf(
            "ExampleTestClass", "ExampleTestObject", "ExampleTestInterface", "ExampleTestEnum",
          )
        )
      )
    )

    typesListFile.writeText(Json.encodeToString(projectCollection))

    unitTestOptions = mapOf(
      "typeScriptDefinitionFileName" to "index.kspunittest.d.ts",
      "typesListForProjectCollectionFilePath" to typesListFile.absolutePath,
    )
  }

  private lateinit var unitTestOptions: Map<String, String>

  @Test
  fun `test collect enum`() {
    val processorProvider = TypeScriptDefinitionGenerationProcessorProvider()

    val kotlinSource = SourceFile.kotlin(
      "KClass.kt",
      //language=kotlin
      """
        @it.neckar.open.annotations.TsExport
        enum class KEnum {
            FOO,
            BAR,
            /**
            * This is a baz
            */
            BAZ,
            ;

          fun foo() {
            println("do nothing...")
          }
        }
    """
    )

    val kotlinCompilation = KotlinCompilation().apply {
      sources = listOf(kotlinSource)

      configureKsp {
        symbolProcessorProviders.add(processorProvider)

        processorOptions.putAll(unitTestOptions)
      }

      inheritClassPath = true
      messageOutputStream = System.out // see diagnostics in real time
      kotlincArguments += "-Xskip-prerelease-check"
    }

    kotlinCompilation.compile()

    val collectingContext = processorProvider.collectingContext

    assertThat(collectingContext.classes()).hasSize(0)
    assertThat(collectingContext.interfaces()).hasSize(0)
    assertThat(collectingContext.topLevelFunctions()).hasSize(0)
    assertThat(collectingContext.constObjects()).hasSize(0)
    assertThat(collectingContext.enums()).hasSize(1)

    collectingContext.enums()[0].let { enum ->
      assertThat(enum.name).isEqualTo("KEnum")

      assertThat(enum.properties).isEmpty()
      assertThat(enum.constructors).isEmpty()
      assertThat(enum.superTypes).isEmpty()
      assertThat(enum.typeParameters).isEmpty()

      val values = enum.values
      val functions = enum.functions

      assertThat(values).hasSize(3)
      assertThat(functions).hasSize(1)

      values[0].let { value ->
        assertThat(value.value).isEqualTo("FOO")
        assertThat(value.doc).isNull()
      }

      values[1].let { value ->
        assertThat(value.value).isEqualTo("BAR")
        assertThat(value.doc).isNull()
      }

      values[2].let { value ->
        assertThat(value.value).isEqualTo("BAZ")
        assertThat(value.doc).isEqualTo("\n This is a baz\n")
      }

      val functionSignature = functions[0]
      assertThat(functionSignature.name).isEqualTo("foo")
      assertThat(functionSignature.parameters).isEmpty()
    }

  }

  @Test
  fun `test collect object`() {
    val processorProvider = TypeScriptDefinitionGenerationProcessorProvider()

    val kotlinSource = SourceFile.kotlin(
      "KClass.kt",
      //language=kotlin
      """
        @it.neckar.open.annotations.TsExport
        object KObject {
            const val bar: Int = 42
            fun foo() {
              println("do nothing...")
            }
        }
    """
    )

    val kotlinCompilation = KotlinCompilation().apply {
      sources = listOf(kotlinSource)

      configureKsp {
        symbolProcessorProviders.add(processorProvider)

        processorOptions.putAll(unitTestOptions)
      }

      inheritClassPath = true
      messageOutputStream = System.out // see diagnostics in real time
      kotlincArguments += "-Xskip-prerelease-check"
    }

    kotlinCompilation.compile()

    val collectingContext = processorProvider.collectingContext

    val constObjects = collectingContext.constObjects()

    assertThat(collectingContext.classes()).hasSize(0)
    assertThat(collectingContext.interfaces()).hasSize(0)
    assertThat(collectingContext.topLevelFunctions()).hasSize(0)
    assertThat(constObjects).hasSize(1)

    constObjects[0].let { constObject: TsConstObject ->
      assertThat(constObject.name).isEqualTo("KObject")

      val properties = constObject.properties
      val functions = constObject.functions

      assertThat(properties).hasSize(1)
      assertThat(functions).hasSize(1)

      val firstFunction = functions[0]
      assertThat(firstFunction.name).isEqualTo("foo")
      assertThat(firstFunction.parameters).isEmpty()
    }
  }

  @Test
  fun `test collect top level function`() {
    val processorProvider = TypeScriptDefinitionGenerationProcessorProvider()

    val kotlinSource = SourceFile.kotlin(
      "KClass.kt",
      //language=kotlin
      """
        @file:it.neckar.open.annotations.TsExport

        fun foo(bar: Int): Int {
          return bar + 1
        }

        @Deprecated("Use foo instead")
        fun fooDeprecated(bar: Int): Int {
          return bar + 1
        }

        internal fun foo2(bar: Int): Int {
          return bar + 1
        }

        private fun foo3(bar: Int): Int {
          return bar + 1
        }
    """
    )

    val kotlinCompilation = KotlinCompilation().apply {
      sources = listOf(kotlinSource)

      configureKsp {
        symbolProcessorProviders.add(processorProvider)

        processorOptions.putAll(unitTestOptions)
      }

      inheritClassPath = true
      messageOutputStream = System.out // see diagnostics in real time
      kotlincArguments += "-Xskip-prerelease-check"
    }

    kotlinCompilation.compile()

    val collectingContext = processorProvider.collectingContext

    assertThat(collectingContext.classes()).isEmpty()
    assertThat(collectingContext.interfaces()).isEmpty()
    assertThat(collectingContext.topLevelFunctions()).hasSize(1)
  }

  @Test
  fun `test simple data class`() {
    val processorProvider = TypeScriptDefinitionGenerationProcessorProvider()

    val kotlinSource = SourceFile.kotlin(
      "KClass.kt",
      //language=kotlin
      """
        @it.neckar.open.annotations.TsExport
        data class KClass(val bar: Int)
    """
    )

    val kotlinCompilation = KotlinCompilation().apply {
      sources = listOf(kotlinSource)

      configureKsp {
        symbolProcessorProviders.add(processorProvider)

        processorOptions.putAll(unitTestOptions)
      }

      inheritClassPath = true
      messageOutputStream = System.out // see diagnostics in real time
      kotlincArguments += "-Xskip-prerelease-check"
    }

    kotlinCompilation.compile()
    val collectingContext = processorProvider.collectingContext

    assertThat(collectingContext.classes()).hasSize(1)
    assertThat(collectingContext.interfaces()).hasSize(0)

    collectingContext.classes().let { classes ->
      val tsClass = classes[0]

      assertThat(tsClass.name).isEqualTo("KClass")
      assertThat(tsClass.properties).hasSize(1)
      tsClass.properties[0].let { property ->
        assertThat(property.name).isEqualTo("bar")
        assertThat(property.type.name).isEqualTo("number")
      }
    }
  }

  @Test
  fun `nullable list property keeps the outer nullability`() {
    val processorProvider = TypeScriptDefinitionGenerationProcessorProvider()

    val kotlinSource = SourceFile.kotlin(
      "KClass.kt",
      //language=kotlin
      """
        @it.neckar.open.annotations.TsExport
        data class KClass(
          val nullableList: List<String>?,
          val nonNullList: List<String>,
        )
    """
    )

    val kotlinCompilation = KotlinCompilation().apply {
      sources = listOf(kotlinSource)

      configureKsp {
        symbolProcessorProviders.add(processorProvider)

        processorOptions.putAll(unitTestOptions)
      }

      inheritClassPath = true
      messageOutputStream = System.out // see diagnostics in real time
      kotlincArguments += "-Xskip-prerelease-check"
    }

    kotlinCompilation.compile()
    val collectingContext = processorProvider.collectingContext

    assertThat(collectingContext.classes()).hasSize(1)

    collectingContext.classes()[0].let { tsClass ->
      assertThat(tsClass.name).isEqualTo("KClass")
      assertThat(tsClass.properties).hasSize(2)

      tsClass.properties[0].let { property ->
        assertThat(property.name).isEqualTo("nullableList")
        assertThat(property.type.isArray).isTrue()
        //The outer List<String>? is nullable - it must not render as a non-null string[].
        assertThat(property.type.isNullable).isTrue()
      }

      tsClass.properties[1].let { property ->
        assertThat(property.name).isEqualTo("nonNullList")
        assertThat(property.type.isArray).isTrue()
        assertThat(property.type.isNullable).isFalse()
      }
    }
  }

  @Test
  fun `test simple interface`() {
    val processorProvider = TypeScriptDefinitionGenerationProcessorProvider()

    val kotlinSource = SourceFile.kotlin(
      "KClass.kt",
      //language=kotlin
      """
        @it.neckar.open.annotations.TsExport
        interface KClass {
            val bar: Int

            fun foo()
        }
    """
    )

    val kotlinCompilation = KotlinCompilation().apply {
      sources = listOf(kotlinSource)

      configureKsp {
        symbolProcessorProviders.add(processorProvider)

        processorOptions.putAll(unitTestOptions)
      }

      inheritClassPath = true
      messageOutputStream = System.out // see diagnostics in real time
      kotlincArguments += "-Xskip-prerelease-check"
    }

    val compilationResult = kotlinCompilation.compile()


    val collectingContext = processorProvider.collectingContext

    println("collectingContext: $collectingContext")

    assertThat(collectingContext.classes()).hasSize(0)
    assertThat(collectingContext.interfaces()).hasSize(1)

    collectingContext.interfaces().let { classes ->
      val tsInterface = classes[0]
      assertThat(tsInterface.name).isEqualTo("KClass")
      assertThat(tsInterface.properties).hasSize(1)

      tsInterface.properties[0].let { property ->
        assertThat(property.name).isEqualTo("bar")
        assertThat(property.type.name).isEqualTo("number")
      }

      assertThat(tsInterface.functions).hasSize(1)
      tsInterface.functions[0].let { function ->
        assertThat(function.name).isEqualTo("foo")
        assertThat(function.parameters).isEmpty()
      }
    }

    assertThat(compilationResult.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
  }

  @Test
  fun `collect default class`() {
    this::class.getResourceSafe("/sample/FooBar.kt")

    val processorProvider = TypeScriptDefinitionGenerationProcessorProvider()


    val kotlinSource = SourceFile.kotlin(
      "KClass.kt",
      //language=kotlin
      """
        @it.neckar.open.annotations.TsExport
        class KClass {
            private val foo: String = "asdf"
            val bar: Int = 42

            fun foo() {
              println("do nothing...")
            }
        }
    """
    )

    val kotlinCompilation = KotlinCompilation().apply {
      sources = listOf(kotlinSource)

      configureKsp {
        symbolProcessorProviders.add(processorProvider)

        processorOptions.putAll(unitTestOptions)
      }

      inheritClassPath = true
      messageOutputStream = System.out // see diagnostics in real time
      kotlincArguments += "-Xskip-prerelease-check"
    }

    assertThat(kotlinCompilation.workingDir).isDirectory()
    assertThat(kotlinCompilation.kaptSourceDir.absolutePath).endsWith("/kapt/sources")
    assertThat(kotlinCompilation.classesDir.absolutePath).endsWith("/classes")
    assertThat(kotlinCompilation.kaptStubsDir.absolutePath).endsWith("/kapt/stubs")
    assertThat(kotlinCompilation.kaptKotlinGeneratedDir.absolutePath).endsWith("/kapt/kotlinGenerated")

    assertThat(kotlinCompilation.kspSourcesDir.absolutePath).endsWith("/ksp/sources")

    val kspSourcesResourcesDir = kotlinCompilation.kspSourcesDir.file("resources")


    val compilationResult = kotlinCompilation.compile()


    val collectingContext = processorProvider.collectingContext

    assertThat(collectingContext.classes()).hasSize(1)
    assertThat(collectingContext.interfaces()).hasSize(0)

    collectingContext.classes().let { classes ->
      val tsClass = classes[0]
      assertThat(tsClass.name).isEqualTo("KClass")
      assertThat(tsClass.properties).hasSize(1)

      tsClass.properties[0].let { property ->
        assertThat(property.name).isEqualTo("bar")
        assertThat(property.type.name).isEqualTo("number")
      }

      assertThat(tsClass.functions).hasSize(1)
      tsClass.functions[0].let { function ->
        assertThat(function.name).isEqualTo("foo")
        assertThat(function.parameters).isEmpty()
      }
    }


    assertThat(compilationResult.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
    assertThat(compilationResult.outputDirectory.absolutePath).endsWith("/classes")


    kspSourcesResourcesDir.listFiles(FileFilter { it.isFile }).sorted().let { generatedFiles ->
      assertThat(generatedFiles).hasSize(1)
      assertThat(generatedFiles[0].name).isEqualTo("index.kspunittest.d.ts")
    }

    assertThat(compilationResult.sourcesGeneratedByAnnotationProcessor).isEmpty()
    assertThat(compilationResult.generatedStubFiles).isEmpty()
  }

}
