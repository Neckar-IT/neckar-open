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
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration.Companion.milliseconds

/**
 * The start and the end of a job as a [RepeatingCycleListener] sees them — what a watchdog needs to
 * tell a job its owner stopped from one that stopped by itself.
 */
@DisableLogging
class RepeatingJobLifecycleListenerTest {
  private class RecordingLifecycleListener : RepeatingCycleListener {
    val events: MutableList<String> = mutableListOf()
    val endReasons: MutableList<RepeatingEndReason> = mutableListOf()

    override fun onJobStarted(jobDescriptor: Repeating.RepeatingJobDescriptor) {
      events.add("job started")
    }

    override fun onJobEnded(reason: RepeatingEndReason) {
      events.add("job ended")
      endReasons.add(reason)
    }

    override fun onCycleStarted() {
      events.add("started")
    }

    override fun onCycleCompleted() {
      events.add("completed")
    }

    override fun onCycleFailed(error: Throwable) {
      events.add("failed")
    }

    override fun onCycleCancelled() {
      events.add("cancelled")
    }
  }

  @Test
  fun `cancel reports Cancelled once`() = runTest {
    val listener = RecordingLifecycleListener()
    val repeating = Repeating.every(100.milliseconds) {}.cycleListener(listener).start(this, this)

    delay(50)
    repeating.cancel()
    delay(50)

    assertThat(listener.endReasons).containsExactly(RepeatingEndReason.Cancelled)
    assertThat(listener.events.first()).isEqualTo("job started")
  }

  @Test
  fun `a cancelled scope reports ScopeCancelled once`() = runTest {
    val listener = RecordingLifecycleListener()
    val jobScope = CoroutineScope(coroutineContext + SupervisorJob(coroutineContext[kotlinx.coroutines.Job]))
    Repeating.every(100.milliseconds) {}.cycleListener(listener).start(jobScope)

    delay(50)
    jobScope.cancel()
    delay(50)

    assertThat(listener.endReasons).containsExactly(RepeatingEndReason.ScopeCancelled)
  }

  @Test
  fun `a start into a cancelled scope reports ScopeCancelled once`() = runTest {
    val listener = RecordingLifecycleListener()
    val cancelledScope = CoroutineScope(SupervisorJob() + StandardTestDispatcher(testScheduler)).apply { cancel() }

    Repeating.every(100.milliseconds) {}.cycleListener(listener).start(cancelledScope)
    advanceUntilIdle()

    assertThat(listener.endReasons).containsExactly(RepeatingEndReason.ScopeCancelled)
  }

  @Test
  fun `stopRepeating reports StoppedItself once`() = runTest {
    val listener = RecordingLifecycleListener()
    val repeating = Repeating.every(100.milliseconds) { stopRepeating() }.cycleListener(listener).start(this, this, supervisorJobDelay = 10.milliseconds)

    delay(50)
    repeating.cancel()

    assertThat(listener.endReasons).containsExactly(RepeatingEndReason.StoppedItself)
  }

  @Test
  fun `a throwing supervisor reports SupervisorFailed once`() = runTest {
    val listener = RecordingLifecycleListener()
    val testContext = coroutineContext
    //The supervisor reads the worker scope twice before launching the worker; the second read throws
    val breakingWorkerScope = object : CoroutineScope {
      var reads = 0
      override val coroutineContext: CoroutineContext
        get() {
          reads++
          check(reads < 2) { "worker scope broke" }
          return testContext
        }
    }
    Repeating(Repeating.every(100.milliseconds) {}.cycleListener(listener).build()).start(coroutineScope = breakingWorkerScope, supervisorScope = this)

    delay(10)

    assertThat(listener.endReasons).hasSize(1)
    assertThat(listener.endReasons.single()).isInstanceOf(RepeatingEndReason.SupervisorFailed::class)
      .transform { it.error }.hasMessage("worker scope broke")
  }

  @Test
  fun `a cycle cancelled with its job reports the cancellation`() = runTest {
    val listener = RecordingLifecycleListener()
    val repeating = Repeating.every(100.milliseconds) { delay(10_000) }.cycleListener(listener).start(this, this)

    delay(50)
    repeating.cancel()
    delay(10)

    //Supervisor and worker end as two coroutines, in either order
    assertThat(listener.events.take(2)).isEqualTo(listOf("job started", "started"))
    assertThat(listener.events.drop(2)).containsExactlyInAnyOrder("cancelled", "job ended")
  }

  @Test
  suspend fun `a listener that rejects the job fails the start`(): Unit = coroutineScope {
    val rejecting = object : RepeatingCycleListener {
      override fun onJobStarted(jobDescriptor: Repeating.RepeatingJobDescriptor) {
        throw IllegalArgumentException("no cycle timeout")
      }

      override fun onCycleStarted() {}
      override fun onCycleCompleted() {}
      override fun onCycleFailed(error: Throwable) {}
    }

    assertFailure {
      Repeating.every(100.milliseconds) {}.cycleListener(rejecting).start(this, this)
    }.isInstanceOf(IllegalArgumentException::class).hasMessage("no cycle timeout")
  }
}
