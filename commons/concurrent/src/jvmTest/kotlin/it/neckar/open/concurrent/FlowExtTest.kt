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
import com.hoc081098.flowext.chunked
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.milliseconds

/**
 *
 */
class FlowExtTest {
  @Test
  suspend fun testValuesAreLostIfNoSubscribers() {
    val flow = MutableSharedFlow<String>()
    assertThat(flow.replayCache).isEmpty()
    assertThat(flow.subscriptionCount.value).isEqualTo(0)

    flow.emit("foobar1")
    flow.emit("foobar2")

    assertThat(flow.replayCache).isEmpty()
    assertThat(flow.subscriptionCount.value).isEqualTo(0)
  }

  @Test
  suspend fun testReplay() {
    val flow = MutableSharedFlow<String>(7)
    assertThat(flow.replayCache).isEmpty()
    assertThat(flow.subscriptionCount.value).isEqualTo(0)

    flow.emit("foobar1")
    flow.emit("foobar2")

    assertThat(flow.replayCache).hasSize(2)
    assertThat(flow.subscriptionCount.value).isEqualTo(0)
  }

  @Test
  fun testTimeoutConsume() = runTest {
    val flow = MutableSharedFlow<String>(7)

    val job = launch(coroutineContext) {
      flow.firstWithTimeout(100.milliseconds)
    }

    withTimeout(100 + 1) {
      job.join()
    }

    assertThat(job.isCancelled).isTrue()
    assertThat(job.isCompleted).isTrue()
  }

  @Test
  suspend fun testnoMoreThanOneFailure() {
    val flow = listOf("a", "b", "c", "d", "e").asFlow()

    assertFailure {
      flow.noMoreThanOne()
    }.messageContains("Flow contains an element already!")
  }

  @Test
  suspend fun testnoMoreThanOneOne() {
    assertThat(listOf("a").asFlow().noMoreThanOne()).isEqualTo("a")
    assertThat(emptyList<String>().asFlow().noMoreThanOne()).isNull()
  }

  @Test
  suspend fun testLimitList() {
    val exampleFlow = flow {
      repeat(150) {
        emit(it)
      }
    }

    assertFailure {
      exampleFlow.toListLimited(100)
    }.all {
      isInstanceOf(IllegalStateException::class)
      hasMessage("Flow contains more than 100 elements!")
    }
  }

  @Test
  suspend fun `chunked should split flow into pages of given size`() {
    val flow = flowOf(1, 2, 3, 4, 5, 6, 7, 8, 9)
    val result = flow.chunked(3).toList()

    assertThat(result).isEqualTo(
      listOf(
        listOf(1, 2, 3),
        listOf(4, 5, 6),
        listOf(7, 8, 9)
      )
    )
  }

  @Test
  suspend fun `chunked should handle flow with remaining elements`() {
    val flow = flowOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
    val result = flow.chunked(3).toList()

    assertThat(result).isEqualTo(
      listOf(
        listOf(1, 2, 3),
        listOf(4, 5, 6),
        listOf(7, 8, 9),
        listOf(10),
      )
    )
  }

  @Test
  suspend fun `chunked should handle empty flow`() {
    val flow = flowOf<Int>()
    assertThat(flow.chunked(3).toList()).isEqualTo(emptyList())
  }

  @Test
  suspend fun `chunked with take`() {
    val flow = flowOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
    val result = flow.chunked(3).take(2).toList()

    assertThat(result).isEqualTo(
      listOf(
        listOf(1, 2, 3),
        listOf(4, 5, 6),
      )
    )
  }
}

