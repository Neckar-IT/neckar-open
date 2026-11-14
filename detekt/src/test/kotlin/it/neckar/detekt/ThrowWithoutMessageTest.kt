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
import dev.detekt.test.lint
import org.junit.jupiter.api.Test

class ThrowWithoutMessageTest {

  private val rule = ThrowWithoutMessage(Config.empty)

  @Test
  fun `flags bare throw IllegalStateException`() {
    val findings = rule.lint(
      """
      fun example() {
        throw IllegalStateException()
      }
      """.trimIndent(),
    )

    assertThat(findings).hasSize(1)
    assertThat(findings[0].message).contains("IllegalStateException")
  }

  @Test
  fun `flags bare throw IllegalArgumentException`() {
    val findings = rule.lint(
      """
      fun example() {
        throw IllegalArgumentException()
      }
      """.trimIndent(),
    )

    assertThat(findings).hasSize(1)
    assertThat(findings[0].message).contains("IllegalArgumentException")
  }

  @Test
  fun `flags bare throw UnsupportedOperationException`() {
    val findings = rule.lint(
      """
      fun example(): Nothing {
        throw UnsupportedOperationException()
      }
      """.trimIndent(),
    )

    assertThat(findings).hasSize(1)
    assertThat(findings[0].message).contains("UnsupportedOperationException")
  }

  @Test
  fun `accepts throw with message argument`() {
    val findings = rule.lint(
      """
      fun example() {
        throw IllegalStateException("computed value did not satisfy the invariant")
      }
      """.trimIndent(),
    )

    assertThat(findings).isEmpty()
  }

  @Test
  fun `accepts throw with cause and message`() {
    val findings = rule.lint(
      """
      fun example(cause: Throwable) {
        throw IllegalStateException("wrapped failure", cause)
      }
      """.trimIndent(),
    )

    assertThat(findings).isEmpty()
  }

  @Test
  fun `ignores throw of project-local exception type`() {
    val findings = rule.lint(
      """
      class MyDomainException : RuntimeException()

      fun example() {
        throw MyDomainException()
      }
      """.trimIndent(),
    )

    // The supertype call `: RuntimeException()` in the class header is not a throw expression
    // and the project-local `MyDomainException` is not in TARGET_EXCEPTIONS — neither is flagged.
    assertThat(findings).isEmpty()
  }

  @Test
  fun `ignores error() sugar`() {
    val findings = rule.lint(
      """
      fun example() {
        error("clear diagnostic message")
      }
      """.trimIndent(),
    )

    assertThat(findings).isEmpty()
  }

  @Test
  fun `flags multiple bare throws in one file`() {
    val findings = rule.lint(
      """
      fun a() {
        throw IllegalStateException()
      }
      fun b() {
        throw IllegalArgumentException()
      }
      """.trimIndent(),
    )

    assertThat(findings).hasSize(2)
  }
}
