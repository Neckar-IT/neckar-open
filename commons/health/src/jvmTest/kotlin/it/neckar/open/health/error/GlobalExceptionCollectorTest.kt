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
package it.neckar.open.health.error

import assertk.*
import assertk.assertions.*
import it.neckar.open.dispose.Disposable
import it.neckar.open.dispose.use
import it.neckar.open.test.utils.DisableLogging
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@DisableLogging(mute = true)
class GlobalExceptionCollectorTest {
  @BeforeEach
  fun setUp() {
    GlobalExceptionCollector.clearErrors()
  }

  @AfterEach
  fun tearDown() {
    GlobalExceptionCollector.clearErrors()
  }

  @Test
  fun testCoroutines() = runTest {
    assertThat(GlobalExceptionCollector.uncaughtExceptions).isNull()

    val job = CoroutineScope(testScheduler + GlobalExceptionCollector.coroutineContext).launch {
      throw RuntimeException("Test exception")
    }

    job.join()

    assertThat(job.isActive).isFalse()
    assertThat(job.isCompleted).isTrue()
    assertThat(job.isCancelled).isTrue()

    assertThat(job.children).isEmpty()

    val recorded = GlobalExceptionCollector.uncaughtExceptions
    assertThat(recorded?.count).isEqualTo(1)
    assertThat(recorded?.latest).isNotNull().isInstanceOf(RuntimeException::class)
    assertThat(recorded?.latest?.message).isEqualTo("Test exception")
  }

  @Test
  fun testDefaultException() {
    GlobalExceptionCollector.registerGlobalExceptionHandler().use {
      assertThat(GlobalExceptionCollector.uncaughtExceptions).isNull()

      Thread {
        throw RuntimeException("Test exception in thread")
      }.also {
        it.start()
        it.join()
      }

      val recorded = GlobalExceptionCollector.uncaughtExceptions
      assertThat(recorded?.count).isEqualTo(1)
      assertThat(recorded?.latest).isNotNull().isInstanceOf(RuntimeException::class)
      assertThat(recorded?.latest?.message).isEqualTo("Test exception in thread")
    }
  }

  @Test
  fun `a throwing onError callback is isolated and does not break the other sinks`() {
    var secondSinkRan = false
    val throwingSink = GlobalExceptionCollector.onError { throw RuntimeException("sink boom") }
    val recordingSink = GlobalExceptionCollector.onError { secondSinkRan = true }

    throwingSink.use {
      recordingSink.use {
        // Must not throw even though the first sink does.
        GlobalExceptionCollector.addError(RuntimeException("boom"))

        assertThat(secondSinkRan).isTrue()
        assertThat(GlobalExceptionCollector.uncaughtExceptions?.count).isEqualTo(1)
      }
    }
  }

  /**
   * `installHealthLayer` subscribes and disposes while another thread may be recording an exception.
   * [GlobalExceptionCollector.addError] therefore notifies a snapshot, not the live callback list.
   */
  @Test
  fun `a callback registered while an exception is recorded does not break the notification`() {
    var lateSink: Disposable? = null

    GlobalExceptionCollector.onError { lateSink = GlobalExceptionCollector.onError { } }.use {
      // Would throw ConcurrentModificationException if the live callback list were iterated.
      GlobalExceptionCollector.addError(RuntimeException("boom"))
      assertThat(GlobalExceptionCollector.uncaughtExceptions?.count).isEqualTo(1)
    }

    lateSink?.dispose()
  }

  /**
   * The lock is not held while the callbacks run, so a callback may read the collector.
   */
  @Test
  fun `a callback may read the collector`() {
    var observed = 0

    GlobalExceptionCollector.onError { observed = GlobalExceptionCollector.uncaughtExceptions?.count ?: 0 }.use {
      GlobalExceptionCollector.addError(RuntimeException("boom"))
    }

    assertThat(observed).isEqualTo(1)
  }

  /**
   * The collector keeps the latest exception plus the count, not every throwable — see
   * [GlobalExceptionCollector].
   */
  @Test
  fun `recording counts up and keeps the first timestamp`() {
    GlobalExceptionCollector.addError(RuntimeException("first"))
    val first = GlobalExceptionCollector.uncaughtExceptions

    GlobalExceptionCollector.addError(NullPointerException("second"))
    val second = GlobalExceptionCollector.uncaughtExceptions

    assertThat(second?.count).isEqualTo(2)
    assertThat(second?.latest?.message).isEqualTo("second")
    assertThat(second?.since).isEqualTo(first?.since)
  }
}
