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
import assertk.assertions.hasSize
import assertk.assertions.isEmpty
import assertk.assertions.contains
import dev.detekt.api.Config
import dev.detekt.test.lint
import org.junit.jupiter.api.Test

class RequireCheckWithoutMessageTest {

  private val rule = RequireCheckWithoutMessage(Config.empty)

  @Test
  fun `flags bare require call`() {
    val findings = rule.lint(
      """
      fun example(value: Int) {
        require(value > 0)
      }
      """.trimIndent(),
    )

    assertThat(findings).hasSize(1)
    assertThat(findings[0].message).contains("require")
  }

  @Test
  fun `flags bare check call`() {
    val findings = rule.lint(
      """
      fun example(value: Int) {
        check(value > 0)
      }
      """.trimIndent(),
    )

    assertThat(findings).hasSize(1)
    assertThat(findings[0].message).contains("check")
  }

  @Test
  fun `accepts require with trailing message lambda`() {
    val findings = rule.lint(
      """
      fun example(value: Int) {
        require(value > 0) { "value must be positive but was ${'$'}value" }
      }
      """.trimIndent(),
    )

    assertThat(findings).isEmpty()
  }

  @Test
  fun `accepts check with trailing message lambda`() {
    val findings = rule.lint(
      """
      fun example(value: Int) {
        check(value > 0) { "value must be positive but was ${'$'}value" }
      }
      """.trimIndent(),
    )

    assertThat(findings).isEmpty()
  }

  @Test
  fun `accepts require with non-trailing lambda message argument`() {
    val findings = rule.lint(
      """
      fun example(value: Int) {
        val lazyMessage: () -> Any = { "value must be positive" }
        require(value > 0, lazyMessage)
      }
      """.trimIndent(),
    )

    assertThat(findings).isEmpty()
  }

  @Test
  fun `accepts named-argument message form`() {
    val findings = rule.lint(
      """
      fun example(value: Int) {
        require(value = value > 0, lazyMessage = { "must be positive" })
      }
      """.trimIndent(),
    )

    assertThat(findings).isEmpty()
  }

  @Test
  fun `ignores unrelated callee starting with require`() {
    val findings = rule.lint(
      """
      fun requireFoo(value: Int): Boolean = value > 0

      fun example(value: Int) {
        requireFoo(value)
      }
      """.trimIndent(),
    )

    assertThat(findings).isEmpty()
  }

  @Test
  fun `ignores qualified require call on a receiver`() {
    val findings = rule.lint(
      """
      class Container {
        fun require(value: Int) {}
      }

      fun example(container: Container, value: Int) {
        container.require(value)
      }
      """.trimIndent(),
    )

    assertThat(findings).isEmpty()
  }

  @Test
  fun `flags multiple bare calls in one file`() {
    val findings = rule.lint(
      """
      fun example(a: Int, b: Int) {
        require(a > 0)
        check(b > 0)
        require(a + b > 0)
      }
      """.trimIndent(),
    )

    assertThat(findings).hasSize(3)
  }

  @Test
  fun `flags bare requireNotNull call`() {
    val findings = rule.lint(
      """
      fun example(value: String?): String {
        return requireNotNull(value)
      }
      """.trimIndent(),
    )

    assertThat(findings).hasSize(1)
    assertThat(findings[0].message).contains("requireNotNull")
  }

  @Test
  fun `flags bare checkNotNull call`() {
    val findings = rule.lint(
      """
      fun example(value: String?): String {
        return checkNotNull(value)
      }
      """.trimIndent(),
    )

    assertThat(findings).hasSize(1)
    assertThat(findings[0].message).contains("checkNotNull")
  }

  @Test
  fun `accepts requireNotNull with trailing message lambda`() {
    val findings = rule.lint(
      """
      fun example(value: String?): String {
        return requireNotNull(value) { "value must not be null" }
      }
      """.trimIndent(),
    )

    assertThat(findings).isEmpty()
  }

  @Test
  fun `accepts qualified requireNotNull extension form`() {
    val findings = rule.lint(
      """
      fun <T> T?.requireNotNull(lazyMessage: () -> Any): T = this!!

      fun example(value: String?): String {
        return value.requireNotNull { "value must not be null" }
      }
      """.trimIndent(),
    )

    assertThat(findings).isEmpty()
  }
}
