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
package it.neckar.ksp

import assertk.*
import assertk.assertions.*
import com.google.devtools.ksp.getDeclaredFunctions
import com.google.devtools.ksp.isConstructor
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import com.tschuchort.compiletesting.configureKsp
import it.neckar.ksp.sample.ExportedInTest
import it.neckar.ksp.sample.TestKClass
import it.neckar.open.test.utils.SlowTest
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.junit.jupiter.api.Test
import java.io.File

/**
 *
 */
@OptIn(ExperimentalCompilerApi::class)
@SlowTest
class KspExtKtTest {
  @Test
  fun testGetKotlinSource() {
    //load source file "it.neckar.ksp.sample.TestKClass" using resources
    val sourceFile = File("src/test/kotlin/it/neckar/ksp/sample/TestKClass.kt")
    assertThat(sourceFile).exists()

    val kotlinSource = SourceFile.kotlin(
      TestKClass::class.simpleName + ".kt",
      sourceFile.readText(),
    )

    val processorProvider = SymbolProcessorProvider { _ ->
      object : SymbolProcessor {
        override fun process(resolver: Resolver): List<KSAnnotated> {

          val symbolsWithAnnotation = resolver.getSymbolsWithAnnotation(ExportedInTest::class.qualifiedName!!).toList()

          assertThat(symbolsWithAnnotation).hasSize(1)
          val annotated = symbolsWithAnnotation.first()

          assertThat(annotated).isInstanceOf(KSClassDeclaration::class)

          val classDeclaration = annotated as KSClassDeclaration

          assertThat(classDeclaration.simpleName.asString()).isEqualTo("TestKClass")
          assertThat(classDeclaration.fqName()).isEqualTo("it.neckar.ksp.sample.TestKClass")
          assertThat(classDeclaration.location.format()).endsWith("sources/TestKClass.kt:7")
          assertThat(classDeclaration.hasAnnotation(ExportedInTest::class)).isTrue()
          assertThat(classDeclaration.hasAnnotation(String::class)).isFalse()

          //A local class has no qualified name at all - the lookup must fail loudly instead of silently reporting "not annotated"
          class LocalClassWithoutQualifiedName

          assertThat(LocalClassWithoutQualifiedName::class.qualifiedName).isNull()

          val missingQualifiedNameFailure = runCatching { classDeclaration.hasAnnotation(LocalClassWithoutQualifiedName::class) }.exceptionOrNull()
          assertThat(missingQualifiedNameFailure).isNotNull().isInstanceOf(IllegalArgumentException::class)

          val functions = classDeclaration.getDeclaredFunctions().toList()
          assertThat(functions).hasSize(2) //1 constructor, 1 function

          val constructor = functions.first { declaration -> declaration.isConstructor() }
          val function = functions.first { declaration -> declaration.isConstructor().not() }

          assertThat(constructor.isSynthetic).isTrue()
          assertThat(function.isSynthetic).isFalse()

          assertThat(constructor.qualifiedName?.asString()).isNull()
          assertThat(constructor.simpleName.asString()).isEqualTo("<init>")
          assertThat(function.qualifiedName?.asString()).isEqualTo("it.neckar.ksp.sample.TestKClass.foo")

          assertThat(function.typeParameters).hasSize(1)

          function.typeParameters.first().let { typeParameter ->
            assertThat(typeParameter.name.asString()).isEqualTo("T")
            assertThat(typeParameter.simpleName.asString()).isEqualTo("T")
            assertThat(typeParameter.isReified).isFalse()

            typeParameter.bounds.toList().let { bounds ->
              assertThat(bounds).hasSize(1)
              val bound = bounds.first()

              assertThat(bound.isSynthetic).isFalse()
              assertThat(bound.location.format()).endsWith("sources/TestKClass.kt:11")
              assertThat(bound.resolve().isMarkedNullable).isTrue()
            }
          }

          assertThat(function.typeParameters).hasSize(1)
          assertThat(function.parameters).hasSize(2)

          assertThat(function.findContainingClass()).isEqualTo(classDeclaration)

          return emptyList()
        }
      }
    }

    val kotlinCompilation = KotlinCompilation().apply {
      sources = listOf(kotlinSource)

      configureKsp {
        symbolProcessorProviders.add(processorProvider)
      }

      inheritClassPath = true
      messageOutputStream = System.out // see diagnostics in real time
      kotlincArguments += "-Xskip-prerelease-check"
    }

    val compilationResult = kotlinCompilation.compile()
    assertThat(compilationResult).isNotNull()
    assertThat(compilationResult.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
  }

  @Test
  fun testGetSuperParameterBoundsGuards() {
    val kotlinSource = SourceFile.kotlin(
      name = "BoundsGuardSample.kt",
      contents = """
        package it.neckar.ksp.sample

        fun functionWithTwoParams(a: String, b: Int) {}

        fun functionWithOneParam(x: String) {}
      """.trimIndent(),
    )

    var missingParameterFailure: Throwable? = null
    var indexOutOfRangeFailure: Throwable? = null

    val processorProvider = SymbolProcessorProvider { _ ->
      object : SymbolProcessor {
        override fun process(resolver: Resolver): List<KSAnnotated> {
          val functions = resolver.getAllFiles()
            .flatMap { it.declarations }
            .filterIsInstance<KSFunctionDeclaration>()
            .toList()

          val functionWithTwoParams = functions.first { it.simpleName.asString() == "functionWithTwoParams" }
          val functionWithOneParam = functions.first { it.simpleName.asString() == "functionWithOneParam" }

          //The parameter belongs to a different function -> indexOf returns -1
          missingParameterFailure = runCatching {
            getSuperParameter(functionWithOneParam, functionWithTwoParams, functionWithTwoParams.parameters[0])
          }.exceptionOrNull()

          //The parameter index exceeds the super function's parameter count
          indexOutOfRangeFailure = runCatching {
            getSuperParameter(functionWithTwoParams, functionWithOneParam, functionWithTwoParams.parameters[1])
          }.exceptionOrNull()

          return emptyList()
        }
      }
    }

    val kotlinCompilation = KotlinCompilation().apply {
      sources = listOf(kotlinSource)

      configureKsp {
        symbolProcessorProviders.add(processorProvider)
      }

      inheritClassPath = true
      messageOutputStream = System.out
      kotlincArguments += "-Xskip-prerelease-check"
    }

    assertThat(kotlinCompilation.compile().exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)

    assertThat(missingParameterFailure).isNotNull().isInstanceOf(IllegalArgumentException::class)
    assertThat(missingParameterFailure?.message).isNotNull().contains("not found in function")

    assertThat(indexOutOfRangeFailure).isNotNull().isInstanceOf(IllegalArgumentException::class)
    assertThat(indexOutOfRangeFailure?.message).isNotNull().contains("No matching parameter")
  }
}

