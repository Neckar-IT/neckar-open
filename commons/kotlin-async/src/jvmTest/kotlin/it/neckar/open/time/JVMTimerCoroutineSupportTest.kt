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
package it.neckar.open.time

import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import assertk.*
import assertk.assertions.*

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.milliseconds

class JVMTimerCoroutineSupportTest {
  @Test
  fun `test delay function`() = runTest {
    val context = StandardTestDispatcher(testScheduler)

    var callbackCounter = 0
    val callback: () -> Unit = {
      callbackCounter++
    }

    val timer = JVMTimerCoroutineImplementation(CoroutineScope(context), CoroutineScope(context))

    timer.delay(500.milliseconds, callback)
    advanceTimeBy(5000)

    assertThat(callbackCounter).isEqualTo(1)
  }

  @Test
  @Timeout(1000, unit = TimeUnit.MILLISECONDS)
  fun `test repeat function`() = runTest(timeout = 999.milliseconds) {
    val context = StandardTestDispatcher(testScheduler)

    var callbackCounter = 0
    val callback: () -> Unit = {
      callbackCounter++
    }

    val timer = JVMTimerCoroutineImplementation(CoroutineScope(context), CoroutineScope(context))
    val disposable = timer.repeat(50.milliseconds, callback)

    advanceTimeBy(1001)
    disposable.dispose()

    assertThat(callbackCounter).isEqualTo(20)
  }

  @Test
  @Timeout(1000, unit = TimeUnit.MILLISECONDS)
  fun `repeat must not fire before the first delay has elapsed`() = runTest(timeout = 999.milliseconds) {
    val context = StandardTestDispatcher(testScheduler)

    var callbackCounter = 0
    val callback: () -> Unit = {
      callbackCounter++
    }

    val timer = JVMTimerCoroutineImplementation(CoroutineScope(context), CoroutineScope(context))
    val disposable = timer.repeat(50.milliseconds, callback)

    //Collect the counts before asserting: a failing assertion would leave the timer running and the scheduler never idle
    advanceTimeBy(49)
    val beforeFirstTick = callbackCounter
    advanceTimeBy(2)
    val afterFirstTick = callbackCounter
    advanceTimeBy(100)
    val afterTwoMoreTicks = callbackCounter
    disposable.dispose()

    assertThat(beforeFirstTick).isEqualTo(0)
    assertThat(afterFirstTick).isEqualTo(1)
    assertThat(afterTwoMoreTicks).isEqualTo(3)
  }

  @Test
  fun testMultipleDelay() = runTest {
    val context = StandardTestDispatcher(testScheduler)

    var callbackCounter = 0
    val callback: () -> Unit = {
      callbackCounter++
    }

    val timer = JVMTimerCoroutineImplementation(CoroutineScope(context), CoroutineScope(context))

    timer.delay(500.milliseconds, callback)
    timer.delay(600.milliseconds, callback)
    timer.delay(700.milliseconds, callback)
    timer.delay(800.milliseconds, callback)
    advanceTimeBy(501)
    assertThat(callbackCounter).isEqualTo(1)

    advanceTimeBy(501) //total 10002
    assertThat(callbackCounter).isEqualTo(4)
  }

  @Test
  fun testMultipleDelayWithCancel() = runTest {
    val context = StandardTestDispatcher(testScheduler)

    var callbackCounter = 0
    val callback: () -> Unit = {
      callbackCounter++
    }

    val timer = JVMTimerCoroutineImplementation(CoroutineScope(context), CoroutineScope(context))

    timer.delay(500.milliseconds, callback)
    timer.delay(600.milliseconds, callback).dispose()
    val job3 = timer.delay(700.milliseconds, callback)
    timer.delay(800.milliseconds, callback)
    advanceTimeBy(501)
    assertThat(callbackCounter).isEqualTo(1)

    job3.dispose()

    advanceTimeBy(501) //total 10002
    assertThat(callbackCounter).isEqualTo(2) //2 jobs have been disposed

    job3.dispose() //does nothing

    advanceTimeBy(501)
    assertThat(callbackCounter).isEqualTo(2) //same
  }
}
