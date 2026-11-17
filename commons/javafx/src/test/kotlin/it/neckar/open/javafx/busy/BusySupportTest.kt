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
package it.neckar.open.javafx.busy

import assertk.*
import assertk.assertions.*
import it.neckar.open.javafx.JavaFxTimer
import it.neckar.open.javafx.test.JavaFxTest
import javafx.animation.KeyFrame
import javafx.animation.Timeline
import javafx.beans.property.SimpleBooleanProperty
import javafx.util.Duration
import org.junit.jupiter.api.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

@JavaFxTest
class BusySupportTest {
  /**
   * The listener path, the manual check and the timeout all notify the same callback - only the first of them may do so.
   */
  @Test
  fun testNotifiedOnceWhenBusyTurnsFalseBeforeManualCheck() {
    val busyStatus = SimpleBooleanProperty(true)
    val callback = CountingFinishedCallback()

    val allTimersDone = registerAndRunTimers(busyStatus, callback) {
      //Still within the same pulse: both timers are pending, so the listener notifies first
      busyStatus.set(false)
    }

    assertThat(allTimersDone).isTrue()
    assertThat(callback.finishedCalls.get()).isEqualTo(1)
    assertThat(callback.timedOutCalls.get()).isEqualTo(0)
  }

  @Test
  fun testNotifiedOnceWhenBusyIsAlreadyFalse() {
    val busyStatus = SimpleBooleanProperty(false)
    val callback = CountingFinishedCallback()

    val allTimersDone = registerAndRunTimers(busyStatus, callback) {}

    assertThat(allTimersDone).isTrue()
    assertThat(callback.finishedCalls.get()).isEqualTo(1)
    assertThat(callback.timedOutCalls.get()).isEqualTo(0)
  }

  @Test
  fun testTimedOutOnceWhenBusyStaysTrue() {
    val busyStatus = SimpleBooleanProperty(true)
    val callback = CountingFinishedCallback()

    val allTimersDone = registerAndRunTimers(busyStatus, callback) {}

    assertThat(allTimersDone).isTrue()
    assertThat(callback.finishedCalls.get()).isEqualTo(0)
    assertThat(callback.timedOutCalls.get()).isEqualTo(1)
  }

  /**
   * Registers the callback on the FX thread, runs [afterRegistration] within the same pulse and waits until both the manual check and the timeout have fired.
   */
  private fun registerAndRunTimers(busyStatus: SimpleBooleanProperty, callback: FinishedCallback, afterRegistration: () -> Unit): Boolean {
    val timersDone = CountDownLatch(1)

    JavaFxTimer.runAndWait {
      BusySupport(busyStatus).registerCallback(callback, manualCheckDelay = manualCheckDelay, timeoutDelay = timeoutDelay)
      afterRegistration()
      Timeline(KeyFrame(timeoutDelay.add(Duration.millis(300.0)), { timersDone.countDown() })).play()
    }

    return timersDone.await(30, TimeUnit.SECONDS)
  }

  private class CountingFinishedCallback : FinishedCallback {
    val finishedCalls: AtomicInteger = AtomicInteger()
    val timedOutCalls: AtomicInteger = AtomicInteger()

    override fun finished() {
      finishedCalls.incrementAndGet()
    }

    override fun timedOut() {
      timedOutCalls.incrementAndGet()
    }
  }

  companion object {
    private val manualCheckDelay: Duration = Duration.millis(200.0)
    private val timeoutDelay: Duration = Duration.millis(600.0)
  }
}
