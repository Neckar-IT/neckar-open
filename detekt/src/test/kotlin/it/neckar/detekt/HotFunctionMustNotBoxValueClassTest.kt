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

class HotFunctionMustNotBoxValueClassTest {

  private val rule = HotFunctionMustNotBoxValueClass(Config.empty)
  private val environment = createEnvironment()

  @Test
  fun `flags a value class read through a generic provider in a @Hot body`() {
    val findings = rule.lintWithContext(
      environment,
      """
      package it.neckar.example

      import it.neckar.open.annotations.Hot

      @JvmInline
      value class Meters(val value: Int)

      interface ValueProvider<T> {
        fun valueAt(index: Int): T
      }

      class Painter(private val provider: ValueProvider<Meters>) {
        @Hot
        fun paint() {
          val distance = provider.valueAt(0)
        }
      }
      """.trimIndent(),
      HOT_STUB,
    )

    assertThat(findings).hasSize(1)
    assertThat(findings[0].message).contains("Meters")
  }

  @Test
  fun `flags a value class read through indexed list access in a @Hot body`() {
    val findings = rule.lintWithContext(
      environment,
      """
      package it.neckar.example

      import it.neckar.open.annotations.Hot

      @JvmInline
      value class Meters(val value: Int)

      class Painter(private val distances: List<Meters>) {
        @Hot
        fun paint() {
          val distance = distances[0]
        }
      }
      """.trimIndent(),
      HOT_STUB,
    )

    assertThat(findings).hasSize(1)
    assertThat(findings[0].message).contains("Meters")
  }

  @Test
  fun `flags a value class boxed in an expression body`() {
    val findings = rule.lintWithContext(
      environment,
      """
      package it.neckar.example

      import it.neckar.open.annotations.Hot

      @JvmInline
      value class Meters(val value: Int)

      fun <T> provideValue(): T = throw UnsupportedOperationException()

      class Painter {
        @Hot
        fun distance(): Meters = provideValue()
      }
      """.trimIndent(),
      HOT_STUB,
    )

    assertThat(findings).hasSize(1)
    assertThat(findings[0].message).contains("Meters")
  }

  @Test
  fun `accepts a value class returned directly (no generic position)`() {
    val findings = rule.lintWithContext(
      environment,
      """
      package it.neckar.example

      import it.neckar.open.annotations.Hot

      @JvmInline
      value class Meters(val value: Int)

      class DirectSource {
        fun distance(): Meters = Meters(1)
      }

      class Painter(private val source: DirectSource) {
        @Hot
        fun paint() {
          val distance = source.distance()
        }
      }
      """.trimIndent(),
      HOT_STUB,
    )

    assertThat(findings).isEmpty()
  }

  @Test
  fun `accepts a non-value-class value read through a generic position`() {
    val findings = rule.lintWithContext(
      environment,
      """
      package it.neckar.example

      import it.neckar.open.annotations.Hot

      interface ValueProvider<T> {
        fun valueAt(index: Int): T
      }

      class Painter(private val provider: ValueProvider<String>) {
        @Hot
        fun paint() {
          val label = provider.valueAt(0)
        }
      }
      """.trimIndent(),
      HOT_STUB,
    )

    assertThat(findings).isEmpty()
  }

  @Test
  fun `accepts boxing acknowledged with @HotAllocation on the local variable`() {
    val findings = rule.lintWithContext(
      environment,
      """
      package it.neckar.example

      import it.neckar.open.annotations.Hot
      import it.neckar.open.annotations.HotAllocation

      @JvmInline
      value class Meters(val value: Int)

      interface ValueProvider<T> {
        fun valueAt(index: Int): T
      }

      class Painter(private val provider: ValueProvider<Meters>) {
        @Hot
        fun paint() {
          @HotAllocation("rare fallback path")
          val distance = provider.valueAt(0)
        }
      }
      """.trimIndent(),
      HOT_STUB,
      HOT_ALLOCATION_STUB,
    )

    assertThat(findings).isEmpty()
  }

  @Test
  fun `ignores value class boxing outside a @Hot body`() {
    val findings = rule.lintWithContext(
      environment,
      """
      package it.neckar.example

      @JvmInline
      value class Meters(val value: Int)

      interface ValueProvider<T> {
        fun valueAt(index: Int): T
      }

      class Painter(private val provider: ValueProvider<Meters>) {
        fun paint() {
          val distance = provider.valueAt(0)
        }
      }
      """.trimIndent(),
      HOT_STUB,
    )

    assertThat(findings).isEmpty()
  }

  companion object {
    val HOT_STUB: String = """
      package it.neckar.open.annotations

      @Target(AnnotationTarget.FUNCTION)
      annotation class Hot
    """.trimIndent()

    val HOT_ALLOCATION_STUB: String = """
      package it.neckar.open.annotations

      @Retention(AnnotationRetention.SOURCE)
      @Target(
        AnnotationTarget.EXPRESSION,
        AnnotationTarget.LOCAL_VARIABLE,
      )
      annotation class HotAllocation(val value: String)
    """.trimIndent()
  }
}
