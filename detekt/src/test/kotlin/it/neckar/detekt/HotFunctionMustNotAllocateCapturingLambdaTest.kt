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

class HotFunctionMustNotAllocateCapturingLambdaTest {

  private val rule = HotFunctionMustNotAllocateCapturingLambda(Config.empty)
  private val environment = createEnvironment()

  @Test
  fun `flags a capturing lambda passed to a non-inline function`() {
    val findings = rule.lintWithContext(
      environment,
      """
      package it.neckar.example

      import it.neckar.open.annotations.Hot

      class Renderer {
        fun registerValidator(validator: (Double) -> Boolean) {}

        @Hot
        fun paint(threshold: Double) {
          registerValidator { value -> value >= threshold }
        }
      }
      """.trimIndent(),
      HOT_STUB,
    )

    assertThat(findings).hasSize(1)
    assertThat(findings[0].message).contains("threshold")
  }

  @Test
  fun `accepts a non-capturing lambda passed to a non-inline function`() {
    val findings = rule.lintWithContext(
      environment,
      """
      package it.neckar.example

      import it.neckar.open.annotations.Hot

      class Renderer {
        fun registerValidator(validator: (Double) -> Boolean) {}

        @Hot
        fun paint() {
          registerValidator { value -> value >= 0.0 }
        }
      }
      """.trimIndent(),
      HOT_STUB,
    )

    assertThat(findings).isEmpty()
  }

  @Test
  fun `accepts a capturing lambda passed to an inline function-type parameter`() {
    val findings = rule.lintWithContext(
      environment,
      """
      package it.neckar.example

      import it.neckar.open.annotations.Hot

      inline fun repeatTimes(times: Int, block: (Int) -> Unit) {
        for (i in 0 until times) block(i)
      }

      class Renderer {
        @Hot
        fun paint(offset: Double) {
          var sum = 0.0
          repeatTimes(3) { index -> sum += offset + index }
        }
      }
      """.trimIndent(),
      HOT_STUB,
    )

    assertThat(findings).isEmpty()
  }

  @Test
  fun `flags a capturing lambda SAM-converted onto a fun interface parameter of an inline function`() {
    val findings = rule.lintWithContext(
      environment,
      """
      package it.neckar.example

      import it.neckar.open.annotations.Hot

      fun interface DoublePredicate {
        fun test(value: Double): Boolean
      }

      inline fun anyMatch(predicate: DoublePredicate): Boolean = predicate.test(1.0)

      class Renderer {
        @Hot
        fun paint(start: Double) {
          anyMatch { value -> value >= start }
        }
      }
      """.trimIndent(),
      HOT_STUB,
    )

    assertThat(findings).hasSize(1)
    assertThat(findings[0].message).contains("start")
  }

  @Test
  fun `flags a capturing lambda passed to a noinline parameter`() {
    val findings = rule.lintWithContext(
      environment,
      """
      package it.neckar.example

      import it.neckar.open.annotations.Hot

      inline fun schedule(crossinline direct: () -> Unit, noinline deferred: () -> Unit) {
        direct()
        keep(deferred)
      }

      fun keep(action: () -> Unit) {}

      class Renderer {
        @Hot
        fun paint(frame: Int) {
          schedule({ }) { consume(frame) }
        }

        fun consume(frame: Int) {}
      }
      """.trimIndent(),
      HOT_STUB,
    )

    assertThat(findings).hasSize(1)
    assertThat(findings[0].message).contains("frame")
  }

  @Test
  fun `flags a capturing lambda assigned without a call context`() {
    val findings = rule.lintWithContext(
      environment,
      """
      package it.neckar.example

      import it.neckar.open.annotations.Hot

      class Renderer {
        var callback: (() -> Double)? = null

        @Hot
        fun paint(scale: Double) {
          callback = { scale * 2.0 }
        }
      }
      """.trimIndent(),
      HOT_STUB,
    )

    assertThat(findings).hasSize(1)
    assertThat(findings[0].message).contains("scale")
  }

  @Test
  fun `flags an explicit this capture`() {
    val findings = rule.lintWithContext(
      environment,
      """
      package it.neckar.example

      import it.neckar.open.annotations.Hot

      class Renderer {
        fun registerAction(action: () -> Renderer) {}

        @Hot
        fun paint() {
          registerAction { this }
        }
      }
      """.trimIndent(),
      HOT_STUB,
    )

    assertThat(findings).hasSize(1)
    assertThat(findings[0].message).contains("this")
  }

  @Test
  fun `accepts a labeled capturing lambda passed to an inline function-type parameter`() {
    val findings = rule.lintWithContext(
      environment,
      """
      package it.neckar.example

      import it.neckar.open.annotations.Hot

      inline fun <T> Iterable<T>.eachFast(block: (T) -> Unit) {
        for (element in this) block(element)
      }

      class Renderer {
        @Hot
        fun paint(offset: Double, buckets: List<Double>) {
          var sum = 0.0
          buckets.eachFast bucketLoop@{ bucket ->
            if (bucket < 0.0) {
              return@bucketLoop
            }
            sum += bucket + offset
          }
        }
      }
      """.trimIndent(),
      HOT_STUB,
    )

    assertThat(findings).isEmpty()
  }

  @Test
  fun `accepts an acknowledged capturing lambda`() {
    val findings = rule.lintWithContext(
      environment,
      """
      package it.neckar.example

      import it.neckar.open.annotations.Hot
      import it.neckar.open.annotations.HotAllocation

      class Renderer {
        fun registerValidator(validator: (Double) -> Boolean) {}

        @Hot
        fun paint(threshold: Double) {
          @HotAllocation("registered once per configuration change, not per frame")
          registerValidator { value -> value >= threshold }
        }
      }
      """.trimIndent(),
      HOT_STUB,
      HOT_ALLOCATION_STUB,
    )

    assertThat(findings).isEmpty()
  }

  @Test
  fun `ignores lambdas in non-hot functions`() {
    val findings = rule.lintWithContext(
      environment,
      """
      package it.neckar.example

      class Renderer {
        fun registerValidator(validator: (Double) -> Boolean) {}

        fun configure(threshold: Double) {
          registerValidator { value -> value >= threshold }
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
