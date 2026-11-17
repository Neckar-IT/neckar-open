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

class OverrideMustRepeatContractAnnotationTest {

  private val rule = OverrideMustRepeatContractAnnotation(Config.empty)
  private val environment = createEnvironment()

  @Test
  fun `flags an override that drops the contract annotation`() {
    val findings = rule.lintWithContext(
      environment,
      """
      package it.neckar.example

      import it.neckar.open.annotations.UiThread

      interface Renderer {
        @UiThread
        fun render()
      }

      class DefaultRenderer : Renderer {
        override fun render() {}
      }
      """.trimIndent(),
      THREAD_DESCRIBING_STUB,
      UI_THREAD_STUB,
    )

    assertThat(findings).hasSize(1)
    assertThat(findings[0].message).contains("UiThread")
  }

  @Test
  fun `accepts an override that repeats the contract annotation`() {
    val findings = rule.lintWithContext(
      environment,
      """
      package it.neckar.example

      import it.neckar.open.annotations.UiThread

      interface Renderer {
        @UiThread
        fun render()
      }

      class DefaultRenderer : Renderer {
        @UiThread
        override fun render() {}
      }
      """.trimIndent(),
      THREAD_DESCRIBING_STUB,
      UI_THREAD_STUB,
    )

    assertThat(findings).isEmpty()
  }

  @Test
  fun `flags an override that drops the @Hot allocation contract`() {
    val findings = rule.lintWithContext(
      environment,
      """
      package it.neckar.example

      import it.neckar.open.annotations.Hot

      interface Renderer {
        @Hot
        fun render()
      }

      class DefaultRenderer : Renderer {
        override fun render() {}
      }
      """.trimIndent(),
      ALLOCATION_DESCRIBING_STUB,
      HOT_STUB,
    )

    assertThat(findings).hasSize(1)
    assertThat(findings[0].message).contains("Hot")
  }

  @Test
  fun `ignores a non-contract annotation on the overridden declaration`() {
    val findings = rule.lintWithContext(
      environment,
      """
      package it.neckar.example

      @Target(AnnotationTarget.FUNCTION)
      annotation class Marker

      interface Renderer {
        @Marker
        fun render()
      }

      class DefaultRenderer : Renderer {
        override fun render() {}
      }
      """.trimIndent(),
    )

    assertThat(findings).isEmpty()
  }

  @Test
  fun `ignores a non-override method`() {
    val findings = rule.lintWithContext(
      environment,
      """
      package it.neckar.example

      import it.neckar.open.annotations.UiThread

      class DefaultRenderer {
        @UiThread
        fun render() {}
      }
      """.trimIndent(),
      THREAD_DESCRIBING_STUB,
      UI_THREAD_STUB,
    )

    assertThat(findings).isEmpty()
  }

  companion object {
    val THREAD_DESCRIBING_STUB: String = """
      package it.neckar.open.annotations.meta

      @Target(AnnotationTarget.CLASS)
      annotation class ThreadDescribingAnnotation(val descriptor: String)
    """.trimIndent()

    val UI_THREAD_STUB: String = """
      package it.neckar.open.annotations

      import it.neckar.open.annotations.meta.ThreadDescribingAnnotation

      @ThreadDescribingAnnotation("ui-thread")
      @Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
      annotation class UiThread
    """.trimIndent()

    val ALLOCATION_DESCRIBING_STUB: String = """
      package it.neckar.open.annotations.meta

      @Target(AnnotationTarget.CLASS)
      annotation class AllocationDescribingAnnotation(val value: String)
    """.trimIndent()

    val HOT_STUB: String = """
      package it.neckar.open.annotations

      import it.neckar.open.annotations.meta.AllocationDescribingAnnotation

      @AllocationDescribingAnnotation("hot-path")
      @Target(AnnotationTarget.FUNCTION)
      annotation class Hot
    """.trimIndent()
  }
}
