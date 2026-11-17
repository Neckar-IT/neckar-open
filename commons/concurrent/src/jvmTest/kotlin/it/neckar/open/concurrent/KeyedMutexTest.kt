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

import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import org.junit.jupiter.api.Test

class KeyedMutexTest {
  private val keyedMutex = KeyedMutex<String>()

  @Test
  fun `callers with the same key run one after the other`() = runTest {
    var running = 0
    var maxRunning = 0

    (1..10).map {
      async {
        keyedMutex.withLock("a") {
          running++
          maxRunning = maxOf(maxRunning, running)
          delay(10)
          running--
        }
      }
    }.awaitAll()

    assertThat(maxRunning).isEqualTo(1)
  }

  @Test
  suspend fun `callers with different keys run at the same time`(): Unit = coroutineScope {
    val bEntered = CompletableDeferred<Unit>()

    val a = async {
      keyedMutex.withLock("a") {
        bEntered.await()
        "a"
      }
    }
    val b = async {
      keyedMutex.withLock("b") {
        bEntered.complete(Unit)
        "b"
      }
    }

    assertThat(listOf(a.await(), b.await())).isEqualTo(listOf("a", "b"))
  }

  @Test
  suspend fun `a key holds no entry once its last caller is done`(): Unit = coroutineScope {
    val release = CompletableDeferred<Unit>()
    val holder = launch { keyedMutex.withLock("a") { release.await() } }
    val waiter = launch { keyedMutex.withLock("a") {} }
    yield()
    assertThat(keyedMutex.activeKeyCount()).isEqualTo(1)

    release.complete(Unit)
    holder.join()
    waiter.join()

    assertThat(keyedMutex.activeKeyCount()).isEqualTo(0)
  }

  @Test
  suspend fun `a failing block releases the lock`() {
    assertFailure { keyedMutex.withLock("a") { error("boom") } }.isInstanceOf<IllegalStateException>()

    assertThat(keyedMutex.withLock("a") { "next" }).isEqualTo("next")
    assertThat(keyedMutex.activeKeyCount()).isEqualTo(0)
  }
}
