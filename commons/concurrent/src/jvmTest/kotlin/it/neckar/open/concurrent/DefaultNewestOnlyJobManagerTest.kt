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
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertTimeoutPreemptively
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.Duration
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 */
@ExtendWith(ThreadExtension::class)
class DefaultNewestOnlyJobManagerTest {
  private lateinit var executorService: ExecutorService

  @BeforeEach
  fun setUp() {
    executorService = Executors.newCachedThreadPool()
  }

  @AfterEach
  fun tearDown() {
    executorService.shutdownNow()
    assertThat(executorService.awaitTermination(1, TimeUnit.SECONDS)).isTrue()
  }

  @Test
  fun smoke() {
    val jobManager = DefaultNewestOnlyJobManager(executorService, 1)
    jobManager.startWorkers()
  }

  @Test
  fun onlyYoungestJobIsExecuted() {
    assertTimeoutPreemptively(Duration.ofSeconds(5)) {
      val jobManager = DefaultNewestOnlyJobManager(executorService, 1)
      for (i in 0..9) {
        jobManager.scheduleJob(object : NewestOnlyJobsManager.Job {
          override val key: Any
            get() {
              return "asdf"
            }

          override fun execute() {
            fail("Must not be executed")
          }
        })
      }
      val executed = AtomicBoolean()
      jobManager.scheduleJob(object : NewestOnlyJobsManager.Job {
        override val key: Any
          get() {
            return "asdf"
          }

        override fun execute() {
          executed.set(true)
        }
      })
      assertThat(executed.get()).isFalse()
      jobManager.startWorkers()
      Awaitility.await().atMost(30, TimeUnit.SECONDS).pollDelay(10, TimeUnit.MILLISECONDS).until { executed.get() }
    }
  }

  @Test
  fun reproduceDeadLock() {
    val executed = AtomicBoolean()
    val jobManager = DefaultNewestOnlyJobManager(executorService, 1)
    jobManager.startWorkers()
    jobManager.scheduleJob(object : NewestOnlyJobsManager.Job {
      override val key: Any
        get() = "asf"

      override fun execute() {
        executed.set(true)
      }
    })
    Awaitility.await().atMost(30, TimeUnit.SECONDS).pollDelay(10, TimeUnit.MILLISECONDS).until { executed.get() }
  }
}
