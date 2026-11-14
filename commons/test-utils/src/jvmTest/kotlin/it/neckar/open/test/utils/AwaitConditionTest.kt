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
package it.neckar.open.test.utils

import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.hasMessage
import assertk.assertions.isGreaterThanOrEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isLessThanOrEqualTo
import it.neckar.open.time.VirtualNowProvider
import it.neckar.open.time.monotonicTimeSource
import it.neckar.open.time.resetMonotonicTimeSource
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

class AwaitConditionTest {

  @AfterEach
  fun resetTimeSource() {
    resetMonotonicTimeSource()
  }

  @Test
  suspend fun `returns once another thread brings the condition about`() {
    val done = AtomicBoolean(false)
    val writer = thread {
      Thread.sleep(50)
      done.set(true)
    }

    awaitCondition(5.seconds, { "the other thread" }) { done.get() }
    writer.join()
  }

  @Test
  suspend fun `throws naming the description after the timeout`() {
    assertFailure {
      awaitCondition(50.milliseconds, { "the file being written" }) { false }
    }.isInstanceOf(IllegalStateException::class).hasMessage("Timed out after 50ms waiting for the file being written")
  }

  @Test
  fun `waits in real time under runTest`() = runTest {
    @Suppress("MonotonicTimeSourceViaProvider")
    val start = TimeSource.Monotonic.markNow()

    var checks = 0
    assertFailure {
      awaitCondition(200.milliseconds, { "nothing" }) { checks++; false }
    }.isInstanceOf(IllegalStateException::class)

    assertThat(start.elapsedNow()).isGreaterThanOrEqualTo(200.milliseconds)
    // A poll interval of 10 ms allows about 20 checks; a delay on virtual time would check thousands of times.
    assertThat(checks).isLessThanOrEqualTo(25)
  }

  @Test
  @Timeout(5)
  suspend fun `waits in real time under a virtual monotonic time source`() {
    // The virtual source stands still, so only a real-time deadline ends the wait.
    monotonicTimeSource = VirtualNowProvider(1_700_000_000_000.0)

    assertFailure {
      awaitCondition(50.milliseconds, { "nothing" }) { false }
    }.isInstanceOf(IllegalStateException::class)
  }
}
