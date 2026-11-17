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

class MonotonicTimeSourceViaProviderTest {

  private val rule = MonotonicTimeSourceViaProvider(Config.empty)

  @Test
  fun `flags TimeSource Monotonic`() {
    val findings = rule.lint(
      """
      import kotlin.time.TimeSource

      fun measure() {
        val start = TimeSource.Monotonic.markNow()
      }
      """.trimIndent(),
    )

    assertThat(findings).hasSize(1)
    assertThat(findings[0].message).contains("monotonicTimeSource")
  }

  @Test
  fun `flags the fully qualified reference`() {
    val findings = rule.lint(
      """
      fun measure() {
        val start = kotlin.time.TimeSource.Monotonic.markNow()
      }
      """.trimIndent(),
    )

    assertThat(findings).hasSize(1)
  }

  @Test
  fun `flags a reference behind an import alias`() {
    val findings = rule.lint(
      """
      import kotlin.time.TimeSource as Clock

      val source = Clock.Monotonic
      """.trimIndent(),
    )

    assertThat(findings).hasSize(1)
  }

  @Test
  fun `flags the import of Monotonic`() {
    val findings = rule.lint(
      """
      import kotlin.time.TimeSource.Monotonic

      fun measure() {
        val start = Monotonic.markNow()
      }
      """.trimIndent(),
    )

    assertThat(findings).hasSize(1)
  }

  @Test
  fun `stays silent in the file declaring monotonicTimeSource`() {
    val findings = rule.lint(
      """
      package it.neckar.open.time

      import kotlin.time.TimeSource

      var monotonicTimeSource: TimeSource.WithComparableMarks = TimeSource.Monotonic

      fun resetMonotonicTimeSource() {
        monotonicTimeSource = TimeSource.Monotonic
      }
      """.trimIndent(),
    )

    assertThat(findings).isEmpty()
  }

  @Test
  fun `flags a file declaring a property of the same name in another package`() {
    val findings = rule.lint(
      """
      package example

      import kotlin.time.TimeSource

      var monotonicTimeSource: TimeSource.WithComparableMarks = TimeSource.Monotonic
      """.trimIndent(),
    )

    assertThat(findings).hasSize(1)
  }

  @Test
  fun `ignores monotonicTimeSource and a TimeSource parameter`() {
    val findings = rule.lint(
      """
      import it.neckar.open.time.monotonicTimeSource
      import kotlin.time.TimeSource

      fun measure(source: TimeSource.WithComparableMarks) {
        val start = monotonicTimeSource.markNow()
        val other = source.markNow()
      }
      """.trimIndent(),
    )

    assertThat(findings).isEmpty()
  }

  @Test
  fun `flags a callable reference on TimeSource Monotonic`() {
    val findings = rule.lint(
      """
      import kotlin.time.TimeSource

      val mark = TimeSource.Monotonic::markNow
      """.trimIndent(),
    )

    assertThat(findings).hasSize(1)
  }

  @Test
  fun `ignores Monotonic of another class named TimeSource`() {
    val findings = rule.lint(
      """
      import example.clock.TimeSource

      val source = TimeSource.Monotonic
      """.trimIndent(),
    )

    assertThat(findings).isEmpty()
  }

  @Test
  fun `flags the fully qualified reference next to another imported TimeSource`() {
    val findings = rule.lint(
      """
      import example.clock.TimeSource

      val source = kotlin.time.TimeSource.Monotonic
      """.trimIndent(),
    )

    assertThat(findings).hasSize(1)
  }

  @Test
  fun `ignores Monotonic of another fully qualified TimeSource`() {
    val findings = rule.lint(
      """
      val source = example.clock.TimeSource.Monotonic
      """.trimIndent(),
    )

    assertThat(findings).isEmpty()
  }
}
