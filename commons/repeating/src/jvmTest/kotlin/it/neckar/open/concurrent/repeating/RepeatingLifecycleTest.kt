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
import it.neckar.open.test.utils.DisableLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.milliseconds

/**
 * The lifecycle of a repeating job: start, cancel, restart, and what happens when the scope it runs
 * in goes away. A background job that cannot be restarted, or that keeps thrashing after its scope
 * died, is worse than one that simply stops.
 */
@DisableLogging
class RepeatingLifecycleTest {
  @Test
  fun `a cancelled job can be started again`() = runTest {
    var cycles = 0
    val repeating = Repeating.every(50.milliseconds) { cycles++ }.start(this, this)

    delay(60)
    repeating.cancel()
    val cyclesAtCancel = cycles

    repeating.start(this, this)
    delay(120)
    repeating.cancel()

    assertThat(cycles).isGreaterThan(cyclesAtCancel)
  }

  @Test
  suspend fun `cancelling twice is a no-op`(): Unit = coroutineScope {
    val repeating = Repeating.every(50.milliseconds) {}.start(this, this)

    repeating.cancel()
    repeating.cancel()

    assertThat(repeating.currentJob).isNull()
  }

  /**
   * The supervisor exists to revive a worker that died unexpectedly. Once the scope the worker runs
   * in is gone, reviving is impossible: every restart produces an already-cancelled job. The
   * supervisor must stop instead of thrashing against a dead scope forever.
   */
  @Test
  fun `the supervisor stops once the job scope is cancelled`() = runTest {
    val jobScope = CoroutineScope(coroutineContext + Job())
    val repeating = Repeating.every(50.milliseconds) {}.start(coroutineScope = jobScope, supervisorScope = this)

    jobScope.cancel("the service shut its job scope down")
    delay(200)

    assertThat(repeating.supervisorJob?.isActive ?: false).isFalse()
  }

  /**
   * A job that declared itself finished must stay finished — the supervisor exists to revive a worker
   * that died unexpectedly, and an intentional stop is the opposite of that.
   */
  @Test
  fun `a job that stops itself is not revived by the supervisor`() = runTest {
    var cycles = 0
    val repeating = Repeating.every(20.milliseconds) {
      cycles++
      stopRepeating()
    }.start(coroutineScope = this, supervisorScope = this, supervisorJobDelay = 30.milliseconds)

    delay(300)
    repeating.cancel()

    assertThat(cycles).isEqualTo(1)
  }

  @Test
  fun `a job that stops itself reports the cycle as completed, not failed`() = runTest {
    val events = mutableListOf<String>()
    val listener = object : RepeatingCycleListener {
      override fun onCycleStarted() {
        events.add("started")
      }

      override fun onCycleCompleted() {
        events.add("completed")
      }

      override fun onCycleFailed(error: Throwable) {
        events.add("failed")
      }
    }
    var errorsReported = 0

    val repeating = Repeating.every(20.milliseconds) { stopRepeating() }
      .cycleListener(listener)
      .onError { errorsReported++ }
      .start(this, this)

    delay(100)
    repeating.cancel()

    assertThat(events).isEqualTo(listOf("started", "completed"))
    assertThat(errorsReported).isEqualTo(0)
  }

  /**
   * The error handler runs inside the job loop. A handler that throws — a metrics backend that is
   * down, a notifier that fails — must not end the very job it reports about.
   */
  @Test
  fun `a throwing error handler does not stop the job`() = runTest {
    var cycles = 0
    val repeating = Repeating.every(50.milliseconds) {
      cycles++
      throw IllegalStateException("cycle failed")
    }.onError {
      throw IllegalStateException("the error handler is broken too")
    }.start(this, this)

    delay(160)
    repeating.cancel()

    assertThat(cycles).isGreaterThan(1)
  }
}
