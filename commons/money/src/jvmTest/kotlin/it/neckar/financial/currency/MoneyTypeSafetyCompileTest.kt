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
package it.neckar.financial.currency

import assertk.*
import assertk.assertions.*
import com.tschuchort.compiletesting.JvmCompilationResult
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.junit.jupiter.api.Test

/**
 * The tax basis of an amount is enforced by the compiler: among sums, differences, ratios, comparisons and the rate,
 * only `Net + Vat` and `Net * VatRate` cross a type.
 */
@OptIn(ExperimentalCompilerApi::class)
class MoneyTypeSafetyCompileTest {
  @Test
  fun `the operations within a type and across net, vat and gross compile`() {
    assertCompiles("Money.Net(1) + Money.Net(2)", resultType = "Money.Net")
    assertCompiles("Money.Gross(1) + Money.Gross(2)", resultType = "Money.Gross")
    assertCompiles("Money.Vat(1) + Money.Vat(2)", resultType = "Money.Vat")
    assertCompiles("Money(1) + Money(2)", resultType = "Money")
    assertCompiles("Money.Net(1) * VatRate.NineteenPercent", resultType = "Money.Vat")
    assertCompiles("Money.Net(1) + Money.Vat(2)", resultType = "Money.Gross")
  }

  @Test
  fun `every sum, difference, ratio and comparison across two types except net plus vat fails the compile`() {
    val crossTypePairs: List<Pair<String, String>> = AmountTypes.flatMap { left -> AmountTypes.filter { it != left }.map { right -> left to right } }
    assertEveryLineFailsTheCompile(
      crossTypePairs.filter { it != ("Money.Net" to "Money.Vat") }.map { (left, right) -> "$left(1) + $right(2)" } +
        listOf("-", "/", "<").flatMap { operator -> crossTypePairs.map { (left, right) -> "$left(1) $operator $right(2)" } },
    )
  }

  @Test
  fun `only a net amount takes a vat rate`() {
    assertEveryLineFailsTheCompile(listOf("Money(1) * VatRate.NineteenPercent", "Money.Gross(1) * VatRate.NineteenPercent", "Money.Vat(1) * VatRate.NineteenPercent"))
  }

  private fun assertCompiles(expression: String, resultType: String) {
    val result = compile("val sum: $resultType = $expression")
    assertThat(result.exitCode, expression).isEqualTo(KotlinCompilation.ExitCode.OK)
  }

  /**
   * One compilation for all expressions, each on its own line: the error on a line has to be the operator rejecting
   * its operand, never a type the snippet fails to resolve.
   */
  private fun assertEveryLineFailsTheCompile(expressions: List<String>) {
    val result = compile(expressions.mapIndexed { index, expression -> "val sum$index = $expression" }.joinToString("\n"))
    assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.COMPILATION_ERROR)
    val errors: List<String> = result.messages.lines().filter { it.startsWith("e: ") }
    expressions.forEachIndexed { index, expression ->
      val errorsOfLine: List<String> = errors.filter { it.contains("Sum.kt:${index + FirstExpressionLine}:") }
      assertThat(errorsOfLine, expression).isNotEmpty()
      assertThat(errorsOfLine.all { it.contains("Argument type mismatch") || it.contains("None of the following candidates") }, expression).isTrue()
    }
  }

  private fun compile(declarations: String): JvmCompilationResult {
    val source = SourceFile.kotlin(
      "Sum.kt",
      """
      import it.neckar.financial.currency.Money
      import it.neckar.financial.currency.VatRate

      $declarations
      """.trimIndent()
    )

    return KotlinCompilation().apply {
      sources = listOf(source)
      inheritClassPath = true
      // Classes built with -progressive or a pre-release Kotlin carry pre-release metadata the in-process compiler rejects.
      kotlincArguments += "-Xskip-prerelease-check"
      jvmTarget = Runtime.version().feature().toString()
      messageOutputStream = System.out
    }.compile()
  }

  companion object {
    private val AmountTypes: List<String> = listOf("Money", "Money.Net", "Money.Gross", "Money.Vat")

    /** The two imports and the blank line precede the first declaration of the compiled source. */
    private const val FirstExpressionLine: Int = 4
  }
}
