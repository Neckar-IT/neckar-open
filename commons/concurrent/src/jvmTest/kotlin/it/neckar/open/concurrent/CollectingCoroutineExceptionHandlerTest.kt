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
import kotlinx.coroutines.*
import org.junit.jupiter.api.Test

/**
 *
 */
class CollectingCoroutineExceptionHandlerTest {
  /**
   * handleException runs on whichever thread the failing coroutine ran on, so a thousand children
   * failing on Dispatchers.Default write the list concurrently. Without a lock an add can be lost
   * and the snapshot can fail outright, which makes the count the assertion to pin.
   */
  @Test
  suspend fun `every exception of concurrently failing children is collected`() {
    val handler = CollectingCoroutineExceptionHandler()
    val scope = CoroutineScope(Dispatchers.Default + SupervisorJob() + handler)

    val jobs = (1..1000).map { scope.launch { throw IllegalStateException("failure $it") } }
    jobs.forEach { it.join() }

    assertThat(handler.getExceptions()).hasSize(1000)
  }

  @Test
  suspend fun testCollectingSingleException() {
    val collectingCoroutineExceptionHandler = CollectingCoroutineExceptionHandler()
    assertThat(collectingCoroutineExceptionHandler.getExceptions()).isEmpty()

    val coroutineScope = CoroutineScope(Dispatchers.Default + collectingCoroutineExceptionHandler)
    assertThat(coroutineScope.coroutineContext[CoroutineExceptionHandler.Key]).isSameInstanceAs(collectingCoroutineExceptionHandler)

    val job = coroutineScope.launch {
      throw IllegalStateException("test")
    }
    job.join()

    assertThat(collectingCoroutineExceptionHandler.getExceptions()).hasSize(1)
    assertThat(collectingCoroutineExceptionHandler.getExceptions().first()).isInstanceOf(IllegalStateException::class)
  }

  @Test
  suspend fun testCollectingMultipleExceptions() {
    val collectingCoroutineExceptionHandler = CollectingCoroutineExceptionHandler()
    assertThat(collectingCoroutineExceptionHandler.getExceptions()).isEmpty()

    val coroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob() + collectingCoroutineExceptionHandler)
    assertThat(coroutineScope.coroutineContext[CoroutineExceptionHandler.Key]).isSameInstanceAs(collectingCoroutineExceptionHandler)

    val job1 = coroutineScope.launch {
      throw IllegalStateException("test1")
    }
    job1.join()

    val job2 = coroutineScope.launch {
      throw IllegalArgumentException("test2")
    }
    job2.join()

    assertThat(collectingCoroutineExceptionHandler.getExceptions()).hasSize(2)

    assertThat(collectingCoroutineExceptionHandler.getExceptions()[0]).isInstanceOf(IllegalStateException::class)
    assertThat(collectingCoroutineExceptionHandler.getExceptions()[1]).isInstanceOf(IllegalArgumentException::class)
  }

  @Test
  suspend fun `getExceptions returns a snapshot, not the live list`() {
    val handler = CollectingCoroutineExceptionHandler()
    val coroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob() + handler)

    coroutineScope.launch { throw IllegalStateException("first") }.join()

    val snapshot = handler.getExceptions()
    assertThat(snapshot).hasSize(1)

    // Collecting another exception must not retroactively change the previously returned list.
    coroutineScope.launch { throw IllegalArgumentException("second") }.join()

    assertThat(snapshot).hasSize(1)
    assertThat(handler.getExceptions()).hasSize(2)
  }
}
