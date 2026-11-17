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
package it.neckar.heartbeat

import assertk.*
import assertk.assertions.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.milliseconds

/**
 *
 */
class ServiceHeartbeatSupportTest {
  @Test
  fun `logger name contains the check description, not the object identity`() {
    val heartbeatSupport = ServiceHeartbeatSupport(
      heartbeatCheck = object : HeartbeatCheck {
        override val description: String = "https://example.org/health"
        override suspend fun check(): HeartbeatState = HeartbeatState.Alive(1.milliseconds)
      }
    )

    assertThat(heartbeatSupport.logger.name).isEqualTo("heartbeat-https://example.org/health")
  }

  @Test
  @Disabled
  fun testVerySlowIt() = runTest {
    val heartbeatSupport = ServiceHeartbeatSupport(
      delayBetweenChecks = 1000.milliseconds,
      timeout = 250.milliseconds,
      heartbeatFlowShareScope = TestScope(testScheduler),
      heartbeatFlowDispatcher = StandardTestDispatcher(testScheduler),
      heartbeatCheck = object : HeartbeatCheck {
        override val description: String = "the check"

        var count = 0

        override suspend fun check(): HeartbeatState {
          count++
          return HeartbeatState.Alive((10 + count).milliseconds)
        }
      }
    )

    assertThat(heartbeatSupport.heartbeat).isNotNull()
    assertThat(heartbeatSupport.heartbeatFlow.first()).isEqualTo(HeartbeatState.Alive(11.milliseconds))

    heartbeatSupport.heartbeatFlow.take(7).toList().let {
      assertThat(it).hasSize(7)
      assertThat((it[0] as HeartbeatState.Alive).responseTime).isEqualTo(12.milliseconds)
      assertThat((it[1] as HeartbeatState.Alive).responseTime).isEqualTo(13.milliseconds)
    }
  }

  @Test
  fun testMutableStateFlow() = runTest {
    val flow = MutableStateFlow(0)
    val stateFlow = flow.asStateFlow()

    val job = launch {
      var counter = 0
      while (isActive) {
        delay(500)
        counter++
        flow.value = counter
        yield()
      }
    }

    stateFlow
      .take(10)
      .collect { value -> println("collecting $value") }

    delay(1000)
    job.cancel()
  }

  @Test
  fun testMutableShareInFlow() = runTest {
    val flow = MutableStateFlow(0)
    val sharedFlow = flow
      .shareIn(TestScope(testScheduler), SharingStarted.Lazily)

    val job = launch {
      var counter = 0
      while (isActive) {
        delay(500)
        counter++
        flow.value = counter
      }
    }

    sharedFlow
      .take(10)
      .toList().let {
        assertThat(it).hasSize(10)
      }

    delay(1000)

    assertThat(job.isActive).isTrue()
    assertThat(job.isCancelled).isFalse()
    assertThat(job.isCompleted).isFalse()

    job.cancel()
    assertThat(job.isActive).isFalse()
    assertThat(job.isCancelled).isTrue()
    assertThat(job.isCompleted).isFalse()

    job.join()
    assertThat(job.isActive).isFalse()
    assertThat(job.isCancelled).isTrue()
    assertThat(job.isCompleted).isTrue()
  }
}
