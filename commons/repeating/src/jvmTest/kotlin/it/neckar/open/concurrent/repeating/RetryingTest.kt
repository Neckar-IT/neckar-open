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
package it.neckar.open.concurrent.repeating

import assertk.*
import assertk.assertions.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.io.IOException
import kotlin.test.assertFailsWith
import kotlin.time.Duration.Companion.seconds

class RetryingTest {
  private val backoff = ErrorBackoff.exponential(initial = 2.seconds, maximum = 16.seconds)

  @Test
  fun `returns the result of the first successful attempt after the backoff pauses`() = runTest {
    val attempts = mutableListOf<Int>()
    val failed = mutableListOf<FailedAttempt>()

    val result = retrying(maxAttempts = 5, backoff = backoff, onFailedAttempt = { failed.add(it) }) { attempt ->
      attempts.add(attempt)
      if (attempt < 3) throw IOException("failure $attempt")
      "done"
    }

    assertThat(result).isEqualTo("done")
    assertThat(attempts).containsExactly(1, 2, 3)
    assertThat(failed.map { it.attempt to it.pause }).containsExactly(1 to 2.seconds, 2 to 4.seconds)
    assertThat(failed.map { it.exception.message }).containsExactly("failure 1", "failure 2")
    assertThat(failed.map { it.maxAttempts }.distinct()).containsExactly(5)
    assertThat(currentTime).isEqualTo(6.seconds.inWholeMilliseconds)
  }

  @Test
  fun `throws the exception of the last attempt without a pause after it`() = runTest {
    val failed = mutableListOf<FailedAttempt>()

    val thrown = assertFailsWith<IOException> {
      retrying<Unit>(maxAttempts = 3, backoff = backoff, onFailedAttempt = { failed.add(it) }) { attempt ->
        throw IOException("failure $attempt")
      }
    }

    assertThat(thrown.message).isEqualTo("failure 3")
    assertThat(failed.map { it.attempt }).containsExactly(1, 2)
    assertThat(currentTime).isEqualTo(6.seconds.inWholeMilliseconds)
  }

  @Test
  fun `throws an exception retryOn rejects at once`() = runTest {
    val failed = mutableListOf<FailedAttempt>()
    var attempts = 0

    assertFailsWith<IllegalStateException> {
      retrying<Unit>(maxAttempts = 5, backoff = backoff, retryOn = { it is IOException }, onFailedAttempt = { failed.add(it) }) {
        attempts++
        throw IllegalStateException("not transient")
      }
    }

    assertThat(attempts).isEqualTo(1)
    assertThat(failed).isEmpty()
    assertThat(currentTime).isEqualTo(0L)
  }

  @Test
  suspend fun `rethrows a CancellationException even when retryOn accepts every exception`() {
    var attempts = 0

    assertFailsWith<CancellationException> {
      retrying<Unit>(maxAttempts = 5, backoff = backoff) {
        attempts++
        throw CancellationException("cancelled")
      }
    }

    assertThat(attempts).isEqualTo(1)
  }

  @Test
  fun `a single attempt throws its exception without a pause`() = runTest {
    assertFailsWith<IOException> {
      retrying<Unit>(maxAttempts = 1, backoff = backoff) { throw IOException("only") }
    }

    assertThat(currentTime).isEqualTo(0L)
  }

  @Test
  suspend fun `rejects fewer than one attempt`() {
    assertFailsWith<IllegalArgumentException> {
      retrying(maxAttempts = 0, backoff = backoff) { "never" }
    }
  }
}
