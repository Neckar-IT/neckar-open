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

import it.neckar.open.test.utils.ThreadExtension
import assertk.*
import assertk.assertions.*

import org.awaitility.Awaitility
import org.hamcrest.core.IsNot
import org.hamcrest.core.IsNull
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/**
 */
@ExtendWith(ThreadExtension::class)
class AsyncTest {
  private var executor: ExecutorService? = null

  @BeforeEach
  fun setUp() {
    executor = Executors.newSingleThreadExecutor()
  }

  @AfterEach
  fun tearDown() {
    executor!!.shutdownNow()
    executor!!.awaitTermination(100, TimeUnit.MILLISECONDS)
  }

  @Disabled
  @Test
  fun testException() {
    val defaultUncaughtExceptionHandler = Thread.getDefaultUncaughtExceptionHandler()
    try {
      val caught = AtomicReference<Throwable>()
      Thread.setDefaultUncaughtExceptionHandler { t: Thread?, e: Throwable -> caught.set(e) }
      val async = Async(executor!!)
      async.last { throw IllegalArgumentException("Uups") }
      Awaitility.await().timeout(30, TimeUnit.SECONDS).untilAtomic(caught, IsNot(IsNull()))
    } finally {
      Thread.setDefaultUncaughtExceptionHandler(defaultUncaughtExceptionHandler)
    }
  }

  @Test
  fun basic() {
    val async = Async(executor!!)
    val otherJobsAdded = AtomicBoolean()
    val firstJobStarted = AtomicBoolean()
    async.last("asdf") {
      firstJobStarted.set(true)
      Awaitility.await().atMost(30, TimeUnit.SECONDS)
        .pollDelay(10, TimeUnit.MILLISECONDS)
        .until { otherJobsAdded.get() }
    }

    //Ensure the first stop has been started
    Awaitility.await().atMost(30, TimeUnit.SECONDS)
      .pollDelay(10, TimeUnit.MILLISECONDS)
      .until { firstJobStarted.get() }


    //Schedule other jobs that should be skipped, since there will be another job added last
    for (i in 0..9) {
      async.last("asdf") { fail("Must not be called") }
    }


    //Start the final job that is executed
    val executed = AtomicBoolean()
    async.last("asdf") { executed.set(true) }

    //finish first job
    otherJobsAdded.set(true)
    Awaitility.await().atMost(30, TimeUnit.SECONDS).pollDelay(10, TimeUnit.MILLISECONDS).until { executed.get() }
  }
}
