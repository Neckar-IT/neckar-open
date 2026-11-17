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
import it.neckar.open.dispose.Disposable
import it.neckar.open.kotlin.lang.requireNotNull
import it.neckar.open.test.utils.DisableLogging
import it.neckar.open.test.utils.awaitCondition
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import kotlinx.coroutines.awaitCancellation
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 *
 */
@DisableLogging
class CoroutinesRepeatingTest {

  @Test
  fun testRepeating() = runTest {
    var callCount = 0

    val repeating = Repeating.every(100.milliseconds) {
      callCount++
    }.onError { error ->
      throw UnsupportedOperationException("Error in repeating job", error)
    }.start(this, this)


    assertThat(callCount).isEqualTo(0)
    delay(401)
    assertThat(callCount).isEqualTo(5)
    repeating.cancel()

    assertThat(repeating.currentJob).isNull()
  }

  @Test
  fun testDispose() = runTest {
    var callCount = 0

    val repeating: Repeating = Repeating.every(100.milliseconds) {
      callCount++
    }.onError { error ->
      throw UnsupportedOperationException("Error in repeating job", error)
    }.start(this, this)

    assertThat(callCount).isEqualTo(0)
    delay(401)
    assertThat(callCount).isEqualTo(5)

    //dispose() should stop the jobs just like cancel()
    repeating.dispose()

    assertThat(repeating.currentJob).isNull()
  }

  @Test
  suspend fun testDisposableTyping(): Unit = coroutineScope {
    val repeating: Repeating = Repeating.every(100.milliseconds) {
      //noop
    }.start(this, this)

    //Repeating is assignable to Disposable
    val disposable: Disposable = repeating
    assertThat(disposable).isInstanceOf(Disposable::class)

    disposable.dispose()
    assertThat(repeating.currentJob).isNull()
  }

  @Test
  fun testDisposableAll() = runTest {
    var callCount1 = 0
    var callCount2 = 0

    val repeating1: Repeating = Repeating.every(100.milliseconds) {
      callCount1++
    }.start(this, this)

    val repeating2: Repeating = Repeating.every(100.milliseconds) {
      callCount2++
    }.start(this, this)

    delay(201)
    assertThat(callCount1).isGreaterThan(0)
    assertThat(callCount2).isGreaterThan(0)

    //Dispose both via Disposable.all()
    Disposable.all(repeating1, repeating2).dispose()

    assertThat(repeating1.currentJob).isNull()
    assertThat(repeating2.currentJob).isNull()
  }

  @Test
  suspend fun testRepeatingContinuesAfterFailingCycles(): Unit = coroutineScope { //to *not* use runTest here, since we try to test exception handling
    val callCount = AtomicInteger()
    var caughtException: Throwable? = null

    val repeating = Repeating.every(1.milliseconds) {
      val call = callCount.incrementAndGet()
      if (call > 3) {
        throw UnsupportedOperationException("Error in repeating job at call $call")
      }
    }.onError { error ->
      caughtException = error
    }.start(this, supervisorJobDelay = 1.milliseconds)

    assertThat(callCount.get()).isEqualTo(0)

    awaitCondition(5.seconds, { "a fourth call" }) { callCount.get() > 3 }
    assertThat(caughtException).isNotNull()

    //Check that the job is executed further
    awaitCondition(5.seconds, { "a fifth call" }) { callCount.get() >= 5 }

    repeating.cancel()

    assertThat(repeating.currentJob).isNull()
  }

  // Cycle timeout: block() that exceeds cycleTimeout is cancelled cooperatively,
  // onError sees the TimeoutCancellationException, and the loop continues with the next cycle.

  @Test
  fun testCycleTimeoutCancelsHungCycleAndContinues() = runTest {
    var callCount = 0
    val errors = mutableListOf<Throwable>()

    val repeating = Repeating.every(100.milliseconds) {
      callCount++
      if (callCount == 1) {
        // First cycle hangs forever — must be aborted by cycleTimeout.
        awaitCancellation()
      }
      // Subsequent cycles return immediately.
    }
      .cycleTimeout(50.milliseconds)
      .onError { errors += it }
      .start(this, this)

    // Drive virtual time forward enough for: first-cycle timeout + delay + several normal cycles.
    delay(600)

    repeating.cancel()

    // The first cycle was aborted by withTimeout — onError got the TimeoutCancellationException.
    assertThat(errors).hasSize(1)
    assertThat(errors.single()).isInstanceOf(TimeoutCancellationException::class)

    // After the timeout, the supervisor loop kept running cycles. Anything > 1 proves the
    // outer loop survived the cancellation of a single cycle.
    assertThat(callCount).isGreaterThan(1)
  }

  @Test
  suspend fun testRepeatingCancelRestart(): Unit = coroutineScope { //to *not* use runTest here, since we try to test exception handling
    val callCount = AtomicInteger()
    var caughtException: Throwable? = null

    val repeating = Repeating.every(1.milliseconds) {
      callCount.incrementAndGet()
    }.onError { error ->
      caughtException = error
    }.start(this, supervisorJobDelay = 1.milliseconds)

    assertThat(callCount.get()).isEqualTo(0)

    awaitCondition(5.seconds, { "a fourth call" }) { callCount.get() > 3 }
    assertThat(caughtException).isNull()

    repeating.currentJob.requireNotNull { "The repeating job is not running" }.cancel()
    val callsBeforeRestart = callCount.get()

    //Should restart automatically
    awaitCondition(5.seconds, { "a call after the restart, last count ${callCount.get()}" }) { callCount.get() > callsBeforeRestart }
    assertThat(caughtException).isNull()

    repeating.cancel()
    assertThat(repeating.currentJob).isNull()
  }
}
