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

class HotFunctionMustOnlyCallHotFunctionsTest {

  private val rule = HotFunctionMustOnlyCallHotFunctions(Config.empty)
  private val environment = createEnvironment()

  @Test
  fun `flags a non-hot callee invoked from a @Hot method`() {
    val findings = rule.lintWithContext(
      environment,
      """
      package it.neckar.example

      import it.neckar.open.annotations.Hot

      class Renderer {
        @Hot
        fun paint() {
          helper()
        }

        fun helper() {}
      }
      """.trimIndent(),
      HOT_STUB,
    )

    assertThat(findings).hasSize(1)
    assertThat(findings[0].message).contains("helper")
  }

  @Test
  fun `flags a non-hot callee in an expression body`() {
    val findings = rule.lintWithContext(
      environment,
      """
      package it.neckar.example

      import it.neckar.open.annotations.Hot

      class Renderer {
        @Hot
        fun paint() = helper()

        fun helper() {}
      }
      """.trimIndent(),
      HOT_STUB,
    )

    assertThat(findings).hasSize(1)
    assertThat(findings[0].message).contains("helper")
  }

  @Test
  fun `accepts a @Hot callee invoked from a @Hot method`() {
    val findings = rule.lintWithContext(
      environment,
      """
      package it.neckar.example

      import it.neckar.open.annotations.Hot

      class Renderer {
        @Hot
        fun paint() {
          helper()
        }

        @Hot
        fun helper() {}
      }
      """.trimIndent(),
      HOT_STUB,
    )

    assertThat(findings).isEmpty()
  }

  @Test
  fun `accepts a stdlib callee invoked from a @Hot method`() {
    val findings = rule.lintWithContext(
      environment,
      """
      package it.neckar.example

      import it.neckar.open.annotations.Hot

      class Renderer {
        @Hot
        fun paint() {
          val values = listOf(1, 2, 3)
        }
      }
      """.trimIndent(),
      HOT_STUB,
    )

    assertThat(findings).isEmpty()
  }

  @Test
  fun `accepts an inline callee invoked from a @Hot method`() {
    val findings = rule.lintWithContext(
      environment,
      """
      package it.neckar.example

      import it.neckar.open.annotations.Hot

      class Renderer {
        @Hot
        fun paint() {
          runHelper { }
        }

        inline fun runHelper(block: () -> Unit) {
          block()
        }
      }
      """.trimIndent(),
      HOT_STUB,
    )

    assertThat(findings).isEmpty()
  }

  @Test
  fun `ignores a constructor call in a @Hot method`() {
    val findings = rule.lintWithContext(
      environment,
      """
      package it.neckar.example

      import it.neckar.open.annotations.Hot

      class Thing

      class Renderer {
        @Hot
        fun paint() {
          val thing = Thing()
        }
      }
      """.trimIndent(),
      HOT_STUB,
    )

    assertThat(findings).isEmpty()
  }

  @Test
  fun `flags a @Slow callee invoked from a @Hot method`() {
    val findings = rule.lintWithContext(
      environment,
      """
      package it.neckar.example

      import it.neckar.open.annotations.Hot
      import it.neckar.open.annotations.Slow

      class Renderer {
        @Hot
        fun paint() {
          recalculate()
        }

        @Slow
        fun recalculate() {}
      }
      """.trimIndent(),
      HOT_STUB,
      SLOW_STUB,
    )

    assertThat(findings).hasSize(1)
    assertThat(findings[0].message).contains("Slow")
  }

  @Test
  fun `flags an @Allocates inline callee - deny beats the inline exemption`() {
    val findings = rule.lintWithContext(
      environment,
      """
      package it.neckar.example

      import it.neckar.open.annotations.AllocationCost
      import it.neckar.open.annotations.Allocates
      import it.neckar.open.annotations.Hot

      class Renderer {
        @Hot
        fun paint() {
          buildSnapshot()
        }

        @Allocates(AllocationCost.Linear)
        inline fun buildSnapshot(): List<Int> = listOf(1, 2, 3)
      }
      """.trimIndent(),
      HOT_STUB,
      ALLOCATES_STUB,
    )

    assertThat(findings).hasSize(1)
    assertThat(findings[0].message).contains("Allocates")
  }

  @Test
  fun `flags reading an @Allocates property from a @Hot method`() {
    val findings = rule.lintWithContext(
      environment,
      """
      package it.neckar.example

      import it.neckar.open.annotations.AllocationCost
      import it.neckar.open.annotations.Allocates
      import it.neckar.open.annotations.Hot

      class Renderer {
        @Allocates(AllocationCost.Linear)
        val snapshot: List<Int>
          get() = listOf(1, 2, 3)

        @Hot
        fun paint() {
          val current = snapshot
        }
      }
      """.trimIndent(),
      HOT_STUB,
      ALLOCATES_STUB,
    )

    assertThat(findings).hasSize(1)
    assertThat(findings[0].message).contains("Allocates")
  }

  @Test
  fun `flags reading a property whose getter is @Slow from a @Hot method`() {
    val findings = rule.lintWithContext(
      environment,
      """
      package it.neckar.example

      import it.neckar.open.annotations.Hot
      import it.neckar.open.annotations.Slow

      class Renderer {
        val expensive: Int
          @Slow
          get() = (0..1000).sum()

        @Hot
        fun paint() {
          val value = expensive
        }
      }
      """.trimIndent(),
      HOT_STUB,
      SLOW_STUB,
    )

    assertThat(findings).hasSize(1)
    assertThat(findings[0].message).contains("Slow")
  }

  @Test
  fun `accepts a plain property read from a @Hot method`() {
    val findings = rule.lintWithContext(
      environment,
      """
      package it.neckar.example

      import it.neckar.open.annotations.Hot

      class Renderer {
        val width: Int = 800

        @Hot
        fun paint() {
          val current = width
        }
      }
      """.trimIndent(),
      HOT_STUB,
    )

    assertThat(findings).isEmpty()
  }

  @Test
  fun `accepts a @Slow call acknowledged with @HotAllocation on the expression`() {
    val findings = rule.lintWithContext(
      environment,
      """
      package it.neckar.example

      import it.neckar.open.annotations.Hot
      import it.neckar.open.annotations.HotAllocation
      import it.neckar.open.annotations.Slow

      class Renderer {
        @Hot
        fun paint() {
          @HotAllocation("once per frame, not per data point")
          recalculate()
        }

        @Slow
        fun recalculate() {}
      }
      """.trimIndent(),
      HOT_STUB,
      SLOW_STUB,
      HOT_ALLOCATION_STUB,
    )

    assertThat(findings).isEmpty()
  }

  @Test
  fun `accepts an @Allocates property read acknowledged with @HotAllocation on the local variable`() {
    val findings = rule.lintWithContext(
      environment,
      """
      package it.neckar.example

      import it.neckar.open.annotations.AllocationCost
      import it.neckar.open.annotations.Allocates
      import it.neckar.open.annotations.Hot
      import it.neckar.open.annotations.HotAllocation

      class Renderer {
        @Allocates(AllocationCost.Linear)
        val snapshot: List<Int>
          get() = listOf(1, 2, 3)

        @Hot
        fun paint() {
          @HotAllocation("snapshot taken once per frame")
          val current = snapshot
        }
      }
      """.trimIndent(),
      HOT_STUB,
      ALLOCATES_STUB,
      HOT_ALLOCATION_STUB,
    )

    assertThat(findings).isEmpty()
  }

  @Test
  fun `accepts calls acknowledged with @HotAllocation on the local variable - including inside a lambda argument`() {
    val findings = rule.lintWithContext(
      environment,
      """
      package it.neckar.example

      import it.neckar.open.annotations.Hot
      import it.neckar.open.annotations.HotAllocation
      import it.neckar.open.annotations.Slow

      class ColorCache {
        @Hot
        fun getPaint(): String {
          @HotAllocation("cache miss only")
          val created = convert().also {
            store(it)
          }
          return created
        }

        @Slow
        fun convert(): String = "paint"

        @Slow
        fun store(value: String) {}
      }
      """.trimIndent(),
      HOT_STUB,
      SLOW_STUB,
      HOT_ALLOCATION_STUB,
    )

    assertThat(findings).isEmpty()
  }

  @Test
  fun `accepts a polymorphic delegation acknowledged with @HotBoundary`() {
    val findings = rule.lintWithContext(
      environment,
      """
      package it.neckar.example

      import it.neckar.open.annotations.Hot
      import it.neckar.open.annotations.HotBoundary

      interface Layer {
        fun paint()
      }

      class WrapperLayer(val delegate: Layer) {
        @Hot
        fun paint() {
          @HotBoundary("polymorphic layer delegation - delegate is colored where implemented")
          delegate.paint()
        }
      }
      """.trimIndent(),
      HOT_STUB,
      HOT_BOUNDARY_STUB,
    )

    assertThat(findings).isEmpty()
  }

  @Test
  fun `@HotBoundary does not suppress the @Slow deny`() {
    val findings = rule.lintWithContext(
      environment,
      """
      package it.neckar.example

      import it.neckar.open.annotations.Hot
      import it.neckar.open.annotations.HotBoundary
      import it.neckar.open.annotations.Slow

      class Renderer {
        @Hot
        fun paint() {
          @HotBoundary("boundary must not silence a known-slow callee")
          recalculate()
        }

        @Slow
        fun recalculate() {}
      }
      """.trimIndent(),
      HOT_STUB,
      SLOW_STUB,
      HOT_BOUNDARY_STUB,
    )

    assertThat(findings).hasSize(1)
    assertThat(findings[0].message).contains("Slow")
  }

  @Test
  fun `acknowledgement does not leak beyond its statement`() {
    val findings = rule.lintWithContext(
      environment,
      """
      package it.neckar.example

      import it.neckar.open.annotations.Hot
      import it.neckar.open.annotations.HotAllocation
      import it.neckar.open.annotations.Slow

      class Renderer {
        @Hot
        fun paint() {
          @HotAllocation("acknowledged")
          recalculate()
          recalculate()
        }

        @Slow
        fun recalculate() {}
      }
      """.trimIndent(),
      HOT_STUB,
      SLOW_STUB,
      HOT_ALLOCATION_STUB,
    )

    assertThat(findings).hasSize(1)
  }

  @Test
  fun `ignores non-hot callers`() {
    val findings = rule.lintWithContext(
      environment,
      """
      package it.neckar.example

      class Renderer {
        fun paint() {
          helper()
        }

        fun helper() {}
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

    val SLOW_STUB: String = """
      package it.neckar.open.annotations

      @Target(
        AnnotationTarget.FUNCTION,
        AnnotationTarget.PROPERTY,
        AnnotationTarget.PROPERTY_GETTER,
      )
      annotation class Slow
    """.trimIndent()

    val ALLOCATES_STUB: String = """
      package it.neckar.open.annotations

      enum class AllocationCost { Constant, Linear }

      @Target(
        AnnotationTarget.FUNCTION,
        AnnotationTarget.PROPERTY,
        AnnotationTarget.PROPERTY_GETTER,
      )
      annotation class Allocates(val value: AllocationCost)
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

    val HOT_BOUNDARY_STUB: String = """
      package it.neckar.open.annotations

      @Retention(AnnotationRetention.SOURCE)
      @Target(
        AnnotationTarget.EXPRESSION,
        AnnotationTarget.LOCAL_VARIABLE,
      )
      annotation class HotBoundary(val value: String)
    """.trimIndent()
  }
}
