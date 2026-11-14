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

import assertk.*
import assertk.assertions.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.awaitility.Awaitility
import org.awaitility.core.ConditionTimeoutException
import org.junit.jupiter.api.Test
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread
import kotlin.test.assertFailsWith
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds


/**
 */
class AwaitilityExtensionsKtTest {
  @Test
  fun testIt() {
    val atomicBoolean = AtomicBoolean(true)
    Awaitility.await().atMost(30, TimeUnit.SECONDS).untilAtomicIsTrue(atomicBoolean)
  }

  @Test
  fun testTimeout() {
    val atomicBoolean = AtomicBoolean(false)
    assertFailsWith<ConditionTimeoutException> {
      Awaitility.await().atMost(30, TimeUnit.SECONDS)
        .atMost(101, TimeUnit.MILLISECONDS)
        .untilAtomicIsTrue(atomicBoolean)
    }
  }

  @Test
  fun testWaitUntilTrue() = runTest {
    assertFailure {
      val atomicBoolean = AtomicBoolean(false)
      atomicBoolean.waitUntilTrue(1.milliseconds)
    }.isInstanceOf(IllegalStateException::class)
  }

  @Test
  fun testWaitUntilTrueMultipleThreads() = runTest {
    val atomicBoolean = AtomicBoolean(false)
    launch(testScheduler) {
      atomicBoolean.waitUntilTrue()
    }
    launch(testScheduler) {
      delay(3000)
      atomicBoolean.set(true)
    }
  }

  @Test
  fun testWaitUntilTrueWaitsInRealTimeUnderRunTest() = runTest {
    val atomicBoolean = AtomicBoolean(false)
    val writer = thread {
      Thread.sleep(50)
      atomicBoolean.set(true)
    }

    atomicBoolean.waitUntilTrue(5.seconds)
    writer.join()
  }

  @Test
  fun testWaitUntilFalse() = runTest {
    assertFailure {
      val atomicBoolean = AtomicBoolean(true)
      atomicBoolean.waitUntilFalse(1.milliseconds)
    }.isInstanceOf(IllegalStateException::class)
  }

  @Test
  fun testWaitUntil() = runTest {
    assertFailure {
      val atomicBoolean = AtomicBoolean(false)
      atomicBoolean.waitUntil(true, 100.milliseconds)
    }.isInstanceOf(IllegalStateException::class)
  }

  @Test
  suspend fun testWaitUntil1() {
    val atomicBoolean = AtomicBoolean(false)
    atomicBoolean.waitUntil(false, 100.milliseconds)
  }

  @Test
  suspend fun testWaitUntil2() {
    val atomicBoolean = AtomicBoolean(true)
    atomicBoolean.waitUntil(true, 100.milliseconds)
  }
}
