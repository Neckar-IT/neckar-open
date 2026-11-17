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
import assertk.assertions.isEqualTo
import dev.detekt.api.Config
import dev.detekt.test.lint
import org.junit.jupiter.api.Test

class RealTimeDelayInTestTest {

  private val rule = RealTimeDelayInTest(Config.empty)

  @Test
  fun `flags delay in a suspend test`() {
    val findings = rule.lint(
      """
      import kotlinx.coroutines.delay
      import kotlin.time.Duration.Companion.milliseconds
      import org.junit.jupiter.api.Test

      class ProcessTest {
        @Test
        suspend fun `the process exits`() {
          delay(300.milliseconds)
        }
      }
      """.trimIndent(),
    )

    assertThat(findings).hasSize(1)
    assertThat(findings[0].message).isEqualTo("delay outside runTest waits in real time: poll with awaitCondition or use runTest")
  }

  @Test
  fun `flags delay in every lifecycle method`() {
    val findings = rule.lint(
      """
      import kotlinx.coroutines.delay
      import org.junit.jupiter.api.AfterAll
      import org.junit.jupiter.api.AfterEach
      import org.junit.jupiter.api.BeforeAll
      import org.junit.jupiter.api.BeforeEach
      import kotlin.test.AfterTest
      import kotlin.test.BeforeTest

      class ProcessTest {
        @BeforeEach
        suspend fun setUp() { delay(10) }

        @AfterEach
        suspend fun tearDown() { delay(10) }

        @BeforeTest
        suspend fun setUpKotlinTest() { delay(10) }

        @AfterTest
        suspend fun tearDownKotlinTest() { delay(10) }

        companion object {
          @BeforeAll
          @JvmStatic
          suspend fun setUpAll() { delay(10) }

          @AfterAll
          @JvmStatic
          suspend fun tearDownAll() { delay(10) }
        }
      }
      """.trimIndent(),
    )

    assertThat(findings).hasSize(6)
  }

  @Test
  fun `flags delay in a parameterized test`() {
    val findings = rule.lint(
      """
      import kotlinx.coroutines.delay
      import org.junit.jupiter.params.ParameterizedTest

      class ProcessTest {
        @ParameterizedTest
        suspend fun parameterized(value: Int) { delay(10) }
      }
      """.trimIndent(),
    )

    assertThat(findings).hasSize(1)
  }

  @Test
  fun `flags delay behind the star import while another class of the file declares a delay`() {
    val findings = rule.lint(
      """
      import kotlinx.coroutines.*
      import org.junit.jupiter.api.Test

      class FakeScheduler {
        suspend fun delay(millis: Long) = Unit
      }

      class ProcessTest {
        @Test
        suspend fun `the process exits`() {
          delay(10)
        }
      }
      """.trimIndent(),
    )

    assertThat(findings).hasSize(1)
  }

  @Test
  fun `flags delay in a nested lambda`() {
    val findings = rule.lint(
      """
      import kotlinx.coroutines.coroutineScope
      import kotlinx.coroutines.delay
      import kotlinx.coroutines.launch
      import org.junit.jupiter.api.Test

      class ProcessTest {
        @Test
        suspend fun `the process exits`(): Unit = coroutineScope {
          launch {
            delay(10)
          }
        }
      }
      """.trimIndent(),
    )

    assertThat(findings).hasSize(1)
  }

  @Test
  fun `flags delay behind the star import`() {
    val findings = rule.lint(
      """
      import kotlinx.coroutines.*
      import org.junit.jupiter.api.Test

      class ProcessTest {
        @Test
        suspend fun `the process exits`() {
          delay(10)
        }
      }
      """.trimIndent(),
    )

    assertThat(findings).hasSize(1)
  }

  @Test
  fun `flags delay behind an alias and fully qualified, and the hidden name behind the star import not`() {
    val findings = rule.lint(
      """
      import kotlinx.coroutines.*
      import kotlinx.coroutines.delay as pause
      import org.junit.jupiter.api.Test

      class ProcessTest {
        @Test
        suspend fun `the process exits`() {
          pause(10)
          kotlinx.coroutines.delay(10)
          delay(10)
        }
      }
      """.trimIndent(),
    )

    assertThat(findings).hasSize(2)
  }

  @Test
  fun `flags delay behind an explicit import over a top-level delay of the file`() {
    val findings = rule.lint(
      """
      import kotlinx.coroutines.delay
      import org.junit.jupiter.api.Test

      class ProcessTest {
        @Test
        suspend fun `the process exits`() {
          delay(10)
        }
      }

      fun delay(millis: Int) = Unit
      """.trimIndent(),
    )

    assertThat(findings).hasSize(1)
  }

  @Test
  fun `flags delay ahead of a local delay declared after the call`() {
    val findings = rule.lint(
      """
      import kotlinx.coroutines.delay
      import org.junit.jupiter.api.Test

      class ProcessTest {
        @Test
        suspend fun `the process exits`() {
          delay(10)
          fun delay(millis: Int) = Unit
        }
      }
      """.trimIndent(),
    )

    assertThat(findings).hasSize(1)
  }

  @Test
  fun `flags delay in a test with a qualified annotation`() {
    val findings = rule.lint(
      """
      import kotlinx.coroutines.delay

      class ProcessTest {
        @org.junit.jupiter.api.Test
        suspend fun `the process exits`() {
          delay(10)
        }
      }
      """.trimIndent(),
    )

    assertThat(findings).hasSize(1)
  }

  @Test
  fun `ignores delay under runTest`() {
    val findings = rule.lint(
      """
      import kotlinx.coroutines.delay
      import kotlinx.coroutines.launch
      import kotlinx.coroutines.test.runTest
      import org.junit.jupiter.api.Test

      class RetryTest {
        @Test
        fun `the retry waits`() = runTest {
          launch {
            delay(10)
          }
        }
      }
      """.trimIndent(),
    )

    assertThat(findings).isEmpty()
  }

  @Test
  fun `ignores delay under runTest on a receiver`() {
    val findings = rule.lint(
      """
      import kotlinx.coroutines.delay
      import kotlinx.coroutines.test.TestScope
      import kotlinx.coroutines.test.runTest
      import org.junit.jupiter.api.Test

      class RetryTest {
        private val testScope = TestScope()

        @Test
        fun `the retry waits`() = testScope.runTest {
          delay(10)
        }
      }
      """.trimIndent(),
    )

    assertThat(findings).isEmpty()
  }

  @Test
  fun `ignores delay outside test and lifecycle methods`() {
    val findings = rule.lint(
      """
      import kotlinx.coroutines.delay

      class ProcessTest {
        private suspend fun waitForProcess() {
          delay(10)
        }
      }
      """.trimIndent(),
    )

    assertThat(findings).isEmpty()
  }

  @Test
  fun `ignores another delay`() {
    val findings = rule.lint(
      """
      import kotlinx.coroutines.*
      import org.junit.jupiter.api.Test

      class ProcessTest {
        @Test
        fun `the scheduler delays`() {
          scheduler.delay(10)
          delay(10)
        }

        private fun delay(millis: Int) = Unit
      }
      """.trimIndent(),
    )

    assertThat(findings).isEmpty()
  }

  @Test
  fun `ignores delay on a nullable receiver`() {
    val findings = rule.lint(
      """
      import kotlinx.coroutines.*
      import org.junit.jupiter.api.Test

      class ProcessTest {
        @Test
        fun `the scheduler delays`() {
          scheduler?.delay(10)
        }
      }
      """.trimIndent(),
    )

    assertThat(findings).isEmpty()
  }

  @Test
  fun `ignores the delay of the companion over an explicit import`() {
    val findings = rule.lint(
      """
      import kotlinx.coroutines.delay
      import org.junit.jupiter.api.Test

      class ProcessTest {
        @Test
        fun `the scheduler delays`() {
          delay(10)
        }

        companion object {
          fun delay(millis: Int) = Unit
        }
      }
      """.trimIndent(),
    )

    assertThat(findings).isEmpty()
  }

  @Test
  fun `ignores a local delay declared before the call`() {
    val findings = rule.lint(
      """
      import kotlinx.coroutines.delay
      import org.junit.jupiter.api.Test

      class ProcessTest {
        @Test
        fun `the scheduler delays`() {
          fun delay(millis: Int) = Unit
          delay(10)
        }
      }
      """.trimIndent(),
    )

    assertThat(findings).isEmpty()
  }

  @Test
  fun `ignores the delay of the class over an explicit import`() {
    val findings = rule.lint(
      """
      import kotlinx.coroutines.delay
      import org.junit.jupiter.api.Test

      class ProcessTest {
        @Test
        fun `the scheduler delays`() {
          delay(10)
        }

        private fun delay(millis: Int) = Unit
      }
      """.trimIndent(),
    )

    assertThat(findings).isEmpty()
  }

  @Test
  fun `ignores a top-level delay of the file over the star import`() {
    val findings = rule.lint(
      """
      import kotlinx.coroutines.*
      import org.junit.jupiter.api.Test

      class ProcessTest {
        @Test
        fun `the scheduler delays`() {
          delay(10)
        }
      }

      fun delay(millis: Int) = Unit
      """.trimIndent(),
    )

    assertThat(findings).isEmpty()
  }

  @Test
  fun `ignores delay without an import of kotlinx coroutines`() {
    val findings = rule.lint(
      """
      import org.junit.jupiter.api.Test
      import it.neckar.open.time.delay

      class ProcessTest {
        @Test
        suspend fun `the process exits`() {
          delay(10)
        }
      }
      """.trimIndent(),
    )

    assertThat(findings).isEmpty()
  }

  @Test
  fun `ignores delay behind Suppress`() {
    val findings = rule.lint(
      """
      import kotlinx.coroutines.delay
      import org.junit.jupiter.api.Test

      class ProcessTest {
        @Test
        @Suppress("RealTimeDelayInTest") // the shell installs its trap before the SIGTERM arrives
        suspend fun `a process ignoring SIGTERM is killed after the grace`() {
          delay(300)
        }
      }
      """.trimIndent(),
    )

    assertThat(findings).isEmpty()
  }
}
