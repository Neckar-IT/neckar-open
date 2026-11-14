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
package it.neckar.ksp.serialization

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.doesNotContain
import assertk.assertions.isEqualTo
import com.tschuchort.compiletesting.JvmCompilationResult
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import com.tschuchort.compiletesting.configureKsp
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.junit.jupiter.api.Test

/**
 * Verifies that [SerializationVerifierProcessor] checks the `@SerialName`-vs-enum-entry constraint even when a class
 * reaches [it.neckar.domain.HasTypeDiscriminator] through intermediate layers - sealed ones as well as generic ones.
 */
@OptIn(ExperimentalCompilerApi::class)
class SerializationVerifierProcessorTest {
  @Test
  fun testMismatchThroughIntermediateSealedLayer() {
    val result = compileIntermediateSealedLayer(
      leafSerialName = "NotAnEnumEntry",
    )

    assertThat(result.messages).contains("does not match any enum entry in MyEnum")
    assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.COMPILATION_ERROR)
  }

  @Test
  fun testMatchThroughIntermediateSealedLayer() {
    val result = compileIntermediateSealedLayer(
      leafSerialName = "Alpha",
    )

    assertThat(result.messages).doesNotContain("does not match any enum entry")
    assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
  }

  @Test
  fun testMismatchThroughGenericIntermediateInterface() {
    val result = compileGenericIntermediateInterface(
      leafSerialName = "NotAnEnumEntry",
    )

    assertThat(result.messages).contains("does not match any enum entry in MyEnum")
    assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.COMPILATION_ERROR)
  }

  @Test
  fun testMatchThroughGenericIntermediateInterface() {
    val result = compileGenericIntermediateInterface(
      leafSerialName = "Alpha",
    )

    assertThat(result.messages).doesNotContain("does not match any enum entry")
    assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
  }

  /**
   * Compiles a `Leaf -> Mid(sealed) -> Root(sealed) : HasTypeDiscriminator<MyEnum>` hierarchy where [leafSerialName]
   * is the `@SerialName` value on the concrete leaf class.
   */
  private fun compileIntermediateSealedLayer(leafSerialName: String): JvmCompilationResult {
    return compile(
      SourceFile.kotlin(
        name = "IntermediateSealedLayer.kt",
        contents = """
          package it.neckar.ksp.serialization.sample

          import it.neckar.domain.HasTypeDiscriminator
          import kotlinx.serialization.SerialName
          import kotlinx.serialization.Serializable

          enum class MyEnum { Alpha, Beta }

          sealed interface Root : HasTypeDiscriminator<MyEnum>

          sealed interface Mid : Root

          @Serializable
          @SerialName("$leafSerialName")
          class Leaf : Mid {
            override val typeDiscriminator: MyEnum get() = MyEnum.Alpha
          }
        """.trimIndent(),
      )
    )
  }

  /**
   * Compiles a `Leaf -> Root(sealed) : Carrier<MyEnum> : HasTypeDiscriminator<T>` hierarchy where the enum is bound by
   * a *generic* intermediate interface - the shape of [it.neckar.domain.HasTypeDiscriminatorWithDomain].
   */
  private fun compileGenericIntermediateInterface(leafSerialName: String): JvmCompilationResult {
    return compile(
      SourceFile.kotlin(
        name = "GenericIntermediateInterface.kt",
        contents = """
          package it.neckar.ksp.serialization.sample

          import it.neckar.domain.HasTypeDiscriminator
          import kotlinx.serialization.SerialName
          import kotlinx.serialization.Serializable

          enum class MyEnum { Alpha, Beta }

          interface Carrier<T : Enum<T>> : HasTypeDiscriminator<T>

          sealed interface Root : Carrier<MyEnum>

          @Serializable
          @SerialName("$leafSerialName")
          class Leaf : Root {
            override val typeDiscriminator: MyEnum get() = MyEnum.Alpha
          }
        """.trimIndent(),
      )
    )
  }

  private fun compile(source: SourceFile): JvmCompilationResult {
    return KotlinCompilation().apply {
      sources = listOf(source)

      configureKsp {
        symbolProcessorProviders.add(SerializationVerifierProcessorProvider())
      }

      inheritClassPath = true
      messageOutputStream = System.out
      kotlincArguments += "-Xskip-prerelease-check"
    }.compile()
  }
}
