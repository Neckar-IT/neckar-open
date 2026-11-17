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
package it.neckar.open.concurrent

import assertk.*
import assertk.assertions.*
import it.neckar.open.test.utils.untilAtomicIsTrue
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.awaitility.Awaitility
import org.hamcrest.CoreMatchers
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 */
@ExperimentalCoroutinesApi
class CoAsyncTest {
  @Test
  fun testSimple() = runTest {
    val async = CoAsync()
    async.start(CoroutineScope(testScheduler))

    val lastRun = AtomicBoolean(false)
    val firstExecuted = AtomicBoolean(false)

    val otherJobsScheduled = AtomicBoolean(false)

    async.last {
      firstExecuted.set(true)
      delay(1000)
      Awaitility.await().atMost(30, TimeUnit.SECONDS).untilTrue(otherJobsScheduled)
    }

    Awaitility.await().atMost(30, TimeUnit.SECONDS).untilTrue(firstExecuted)

    async.last {
      delay(1000)
      fail("Must not be executed. Should be overwritten by next async")
    }
    async.last {
      delay(1000)
      fail("Must not be executed. Should be overwritten by next async")
    }

    async.last {
      delay(1000)
      lastRun.set(true)
    }

    otherJobsScheduled.set(true)

    Awaitility.await().atMost(30, TimeUnit.SECONDS).untilTrue(lastRun)
  }

  @Test
  fun testExceptionInJob(): TestResult = runTest {
    val caughtException = mutableListOf<Throwable>()
    val exceptionHandler = CoroutineExceptionHandler { _, exception ->
      caughtException.add(exception)
    }

    val async = CoAsync()
    async.start(CoroutineScope(testScheduler + exceptionHandler))

    try {
      var counter = 0

      async.last {
        assertThat(counter).isEqualTo(0)
        counter++
        throw UnsupportedOperationException()
      }

      withTimeout(10_000) {
        while (counter != 1) {
          delay(10)
        }
      }

      async.last {
        assertThat(counter).isEqualTo(1)
        counter++
        throw UnsupportedOperationException()
      }

      withTimeout(10_000) {
        while (counter != 2) {
          delay(10)
        }
      }

      assertThat(counter).isEqualTo(2)
    } finally {
      async.dispose()
    }

    assertThat(caughtException).hasSize(2)
  }

  @ExperimentalCoroutinesApi
  @Test
  fun testIt() {
    runTest {
      val async = CoAsync()
      async.start(CoroutineScope(testScheduler))

      try {
        val calledFor = AtomicInteger()
        val runCount = AtomicInteger()

        val finished = AtomicBoolean()

        //create threads that schedule updates
        val job = async(testScheduler) {
          for (i in 1..70_000) {
            async.last {
              delay(5)
              calledFor.set(i)
              runCount.incrementAndGet()
            }
          }

          finished.set(true)
        }

        job.join()
        Awaitility.await().atMost(30, TimeUnit.SECONDS).untilAtomic(calledFor, CoreMatchers.`is`(70_000))
        assertThat(finished.get()).isTrue()
        assertThat(calledFor.get()).isEqualTo(70000)
        assertThat(runCount.get()).isLessThan(70000 - 1)
      } finally {
        async.dispose()
      }
    }
  }

  @Test
  fun testDelay() = runTest {
    val async = CoAsync()
    async.start(CoroutineScope(testScheduler))

    try {
      val calledFor = AtomicInteger()
      val runCount = AtomicInteger()

      val finishedAdding = AtomicBoolean()
      val finishedRun = AtomicBoolean()

      //create threads that schedule updates
      val job = async(testScheduler) {
        for (i in 1..30) {
          async.last {
            calledFor.set(i)
            runCount.incrementAndGet()
            delay(100)

            if (finishedAdding.get()) {
              finishedRun.set(true)
            }
          }
        }

        finishedAdding.set(true)
      }

      job.join()
      Awaitility.await().atMost(30, TimeUnit.SECONDS).untilAtomicIsTrue(finishedAdding)
      Awaitility.await().atMost(30, TimeUnit.SECONDS).untilAtomicIsTrue(finishedRun)

      assertThat(calledFor.get()).isEqualTo(30)
      assertThat(runCount.get()).isLessThan(3)
    } finally {
      async.dispose()
    }
  }


  private fun println(message: String) {
    logger.info(message)
  }

  companion object {
    val logger: org.slf4j.Logger = LoggerFactory.getLogger(CoAsyncTest::class.java)
  }
}
