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

import assertk.*
import assertk.assertions.*
import it.neckar.gradle.ksp.options.KspPluginOptions
import it.neckar.ksp.test.KspTestUtils
import it.neckar.ksp.types.list.generator.sample.ExampleInnerClass
import it.neckar.ksp.types.list.generator.sample.ExampleInnerInterface
import it.neckar.open.collections.fastForEach
import it.neckar.open.test.utils.SlowTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File

/**
 *
 */
@SlowTest
class TypesListGeneratorProcessorTest {
  private val testClassWithInnerFile = File("src/test/kotlin/it/neckar/ksp/types/list/generator/sample/ExampleInnerClass.kt")
  private val testInterfaceWithInnerFile = File("src/test/kotlin/it/neckar/ksp/types/list/generator/sample/ExampleInnerInterface.kt")
  private val testTypeAliasesFile = File("src/test/kotlin/it/neckar/ksp/types/list/generator/sample/ExampleTypeAliases.kt")


  private val allSourceFiles = listOf(
    testClassWithInnerFile,
    testInterfaceWithInnerFile,
    testTypeAliasesFile,
  )

  private lateinit var unitTestOptions: Map<String, String>

  @BeforeEach
  fun setUp() {
    allSourceFiles.fastForEach { sourceFile ->
      assertThat(sourceFile).exists()
    }

    unitTestOptions = mapOf(
      KspPluginOptions.TypesListGenerator.PackageNamesListFileName to "the-PackageNamesListFileName.list",
      KspPluginOptions.TypesListGenerator.ClassNamesListFileName to "the-ClassNamesListFileName.list",
    )
  }

  @Test
  fun testGenerateTypeListsClassWithInner() {
    val context = executeCollect(listOf(testClassWithInnerFile))

    assertThat(context).isNotNull()
    assertThat(context.classNames!!).containsExactly(
      ExampleInnerClass::class.qualifiedName,
      ExampleInnerClass.InnerClass::class.qualifiedName,
      ExampleInnerClass.InnerClass.InnerInnerClass::class.qualifiedName,
      ExampleInnerClass.InnerEnum::class.qualifiedName,
      ExampleInnerClass.InnerInterface::class.qualifiedName,
      ExampleInnerClass.InnerObject::class.qualifiedName,
    )
  }

  @Test
  fun testGenerateTypeListsInterfaceWithInner() {
    val context = executeCollect(listOf(testInterfaceWithInnerFile))

    assertThat(context).isNotNull()
    assertThat(context.classNames!!).containsExactly(
      ExampleInnerInterface::class.qualifiedName,
      ExampleInnerInterface.InnerClass::class.qualifiedName,
      ExampleInnerInterface.InnerClass.InnerInnerClass::class.qualifiedName,
      ExampleInnerInterface.InnerEnum::class.qualifiedName,
      ExampleInnerInterface.InnerInterface::class.qualifiedName,
      ExampleInnerInterface.InnerObject::class.qualifiedName,
    )
  }

  @Test
  fun testGenerateTypeListsWithTypeAliases() {
    val context = executeCollect(listOf(testTypeAliasesFile))

    assertThat(context).isNotNull()
    assertThat(context.classNames!!).containsExactly(
      "it.neckar.ksp.types.list.generator.sample.IntList",
      "it.neckar.ksp.types.list.generator.sample.MyStringAlias",
      "it.neckar.ksp.types.list.generator.sample.StringIntMap",
    )
  }

  private fun executeCollect(sourceFiles: List<File>): TypesListGenerationContext {
    val symbolProcessorProvider = TypesListGeneratorProcessorProvider()

    KspTestUtils.compileSourceFiles(
      sourceFiles = sourceFiles,
      symbolProcessorProvider = symbolProcessorProvider,
      options = unitTestOptions,
    )

    return symbolProcessorProvider.typesListGenerationContext
  }

}
