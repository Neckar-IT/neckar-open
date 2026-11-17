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
package it.neckar.detekt

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.hasSize
import assertk.assertions.isEmpty
import dev.detekt.api.Config
import dev.detekt.test.lintWithContext
import dev.detekt.test.utils.createEnvironment
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class WhenInitializerRequiresExplicitTypeTest {

  private val rule = WhenInitializerRequiresExplicitType(Config.empty)
  private val environment = createEnvironment()

  @Test
  fun `flags val with when initializer when inferred type is a sealed hierarchy`() {
    val findings = rule.lintWithContext(
      environment,
      """
      package it.neckar.example.book

      sealed interface UpsertResponse {
        data object Good : UpsertResponse
        data object NotFound : UpsertResponse
      }

      fun handle(flag: Int) {
        val response = when (flag) {
          0 -> UpsertResponse.Good
          else -> UpsertResponse.NotFound
        }
        println(response)
      }
      """.trimIndent(),
    )

    assertThat(findings).hasSize(1)
    assertThat(findings[0].message).contains("response")
    assertThat(findings[0].message).contains("UpsertResponse")
  }

  @ParameterizedTest
  @ValueSource(
    strings = [
      "\"a\"|\"b\"", // String
      "1|2", // Int
      "1L|2L", // Long
      "true|false", // Boolean
      "1.0|2.0", // Double
      "1.0f|2.0f", // Float
      "1.toShort()|2.toShort()", // Short
      "1.toByte()|2.toByte()", // Byte
      "'a'|'b'", // Char
    ],
  )
  fun `does not flag val with when initializer when inferred type is a Kotlin primitive`(branches: String) {
    val (goodBranch, elseBranch) = branches.split("|")
    val findings = rule.lintWithContext(
      environment,
      """
      fun handle(flag: Int) {
        val value = when (flag) {
          0 -> $goodBranch
          else -> $elseBranch
        }
        println(value)
      }
      """.trimIndent(),
    )

    assertThat(findings).isEmpty()
  }

  @Test
  fun `does not flag val with when initializer and explicit type annotation`() {
    val findings = rule.lintWithContext(
      environment,
      """
      package it.neckar.example.book

      sealed interface UpsertResponse {
        data object Good : UpsertResponse
        data object NotFound : UpsertResponse
      }

      fun handle(flag: Int) {
        val response: UpsertResponse = when (flag) {
          0 -> UpsertResponse.Good
          else -> UpsertResponse.NotFound
        }
        println(response)
      }
      """.trimIndent(),
    )

    assertThat(findings).isEmpty()
  }

  @Test
  fun `does not flag val without when initializer`() {
    val findings = rule.lintWithContext(
      environment,
      """
      package it.neckar.example.book

      sealed interface UpsertResponse {
        data object Good : UpsertResponse
        data object NotFound : UpsertResponse
      }

      fun handle(flag: Int) {
        val literal = 42
        val viaIf = if (flag == 0) UpsertResponse.Good else UpsertResponse.NotFound
        val viaTry = try { UpsertResponse.Good } catch (e: Throwable) { UpsertResponse.NotFound }
        val viaCall = UpsertResponse.Good
        println(literal)
        println(viaIf)
        println(viaTry)
        println(viaCall)
      }
      """.trimIndent(),
    )

    assertThat(findings).isEmpty()
  }

  @Test
  fun `does not flag single-expression function body with when`() {
    val findings = rule.lintWithContext(
      environment,
      """
      package it.neckar.example.book

      sealed interface UpsertResponse {
        data object Good : UpsertResponse
        data object NotFound : UpsertResponse
      }

      fun choose(flag: Int) = when (flag) {
        0 -> UpsertResponse.Good
        else -> UpsertResponse.NotFound
      }
      """.trimIndent(),
    )

    assertThat(findings).isEmpty()
  }

  @Test
  fun `does not flag last-expression when in block without val binding`() {
    val findings = rule.lintWithContext(
      environment,
      """
      package it.neckar.example.book

      sealed interface UpsertResponse {
        data object Good : UpsertResponse
        data object NotFound : UpsertResponse
      }

      fun choose(flag: Int): UpsertResponse {
        return when (flag) {
          0 -> UpsertResponse.Good
          else -> UpsertResponse.NotFound
        }
      }
      """.trimIndent(),
    )

    assertThat(findings).isEmpty()
  }

  @Test
  fun `does not flag val with when initializer when every branch throws (inferred Nothing)`() {
    val findings = rule.lintWithContext(
      environment,
      """
      fun handle(flag: Int) {
        val never = when (flag) {
          0 -> throw IllegalStateException("zero")
          else -> throw IllegalStateException("other")
        }
        println(never)
      }
      """.trimIndent(),
    )

    assertThat(findings).isEmpty()
  }

  @Test
  fun `does not flag val with when initializer when inferred type is a type parameter`() {
    val findings = rule.lintWithContext(
      environment,
      """
      fun <T> pick(flag: Int, a: T, b: T): T {
        val chosen = when (flag) {
          0 -> a
          else -> b
        }
        return chosen
      }
      """.trimIndent(),
    )

    assertThat(findings).isEmpty()
  }
}
