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
package it.neckar.ktor.health

import assertk.*
import assertk.assertions.*
import it.neckar.open.test.utils.DisableLogging
import org.junit.jupiter.api.Test
import java.net.ServerSocket
import java.net.URI
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@DisableLogging
class EventLoopWatchdogTest {
  private val processExit = RecordingProcessExit()
  private val url: URI = URI("http://127.0.0.1:1/")

  /**
   * A watchdog whose probes answer as listed in [answers], one per [EventLoopWatchdog.check].
   */
  private fun watchdogAnswering(answers: List<Boolean>, mode: WatchdogMode = WatchdogMode.Exit): EventLoopWatchdog {
    val remaining = ArrayDeque(answers)
    return EventLoopWatchdog(url, StallReaction(mode, processExit), probe = { remaining.removeFirst() })
  }

  private fun EventLoopWatchdog.probe(times: Int) {
    repeat(times) { check() }
  }

  @Test
  fun `three silent probes in a row end an armed process`() {
    val watchdog = watchdogAnswering(listOf(true) + List(3) { false })

    watchdog.probe(3)
    assertThat(processExit.statuses).isEmpty()

    watchdog.probe(1)
    assertThat(processExit.statuses).containsExactly(70)
  }

  @Test
  fun `a server that never answered is still starting`() {
    watchdogAnswering(List(20) { false }).probe(20)

    assertThat(processExit.statuses).isEmpty()
  }

  /**
   * A freeze costs at most the probe in flight, and a server under load misses a probe now and then.
   */
  @Test
  fun `two silent probes between answers are no stall`() {
    watchdogAnswering(List(10) { listOf(true, false, false) }.flatten()).probe(30)

    assertThat(processExit.statuses).isEmpty()
  }

  @Test
  fun `an unarmed watchdog only logs`() {
    watchdogAnswering(listOf(true) + List(10) { false }, mode = WatchdogMode.LogOnly).probe(11)

    assertThat(processExit.statuses).isEmpty()
  }

  @Test
  fun `a single silent probe is not enough to stall`() {
    assertFailure { EventLoopWatchdog(url, StallReaction(WatchdogMode.Exit, processExit), silentProbesToStall = 1, probe = { true }) }
      .isInstanceOf(IllegalArgumentException::class)
  }

  @Test
  fun `the http probe counts a stalled body as no answer`() {
    ServerSocket(0).use { stallingServer ->
      val connections = mutableListOf<java.net.Socket>()
      val acceptor = Thread {
        runCatching {
          val socket = stallingServer.accept()
          connections.add(socket)
          socket.getOutputStream().write("HTTP/1.1 200 OK\r\nContent-Length: 100\r\n\r\n".toByteArray())
          socket.getOutputStream().flush()
        }
      }.apply { isDaemon = true; start() }

      HttpServerProbe(500.milliseconds).use { probe ->
        assertThat(probe.answers(URI("http://127.0.0.1:${stallingServer.localPort}/"))).isFalse()
      }
      acceptor.join(1_000)
      connections.forEach { it.close() }
    }
  }

  @Test
  fun `the http probe counts a silent server as no answer`() {
    ServerSocket(0).use { silentServer ->
      //Accepts into the backlog and never answers
      HttpServerProbe(1.seconds).use { probe ->
        assertThat(probe.answers(URI("http://127.0.0.1:${silentServer.localPort}/"))).isFalse()
      }
    }
  }

}
