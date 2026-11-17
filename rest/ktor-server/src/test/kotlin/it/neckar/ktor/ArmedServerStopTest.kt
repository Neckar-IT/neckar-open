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
package it.neckar.ktor

import assertk.*
import assertk.assertions.*
import io.ktor.server.application.*
import io.opentelemetry.api.OpenTelemetry
import it.neckar.ktor.health.RecordingProcessExit
import it.neckar.ktor.health.StallReaction
import it.neckar.ktor.health.WatchdogMode
import it.neckar.ktor.health.installBackgroundJobs
import it.neckar.open.concurrent.repeating.Repeating
import it.neckar.open.concurrent.repeating.RepeatingEndReason
import it.neckar.open.concurrent.repeating.stopRepeating
import it.neckar.open.health.BackgroundJobName
import it.neckar.open.health.BackgroundJobRegistry
import it.neckar.open.health.trackedAs
import it.neckar.open.test.utils.DisableLogging
import it.neckar.projects.common.Port
import it.neckar.projects.common.ServiceDescriptor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.net.ServerSocket
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes

/**
 * A server armed like a process start stops without ending the process: the end of its jobs belongs
 * to the shutdown.
 */
@DisableLogging
class ArmedServerStopTest {
  private val jobName: BackgroundJobName = BackgroundJobName("armed-stop-sweep")

  @BeforeEach
  @AfterEach
  fun clearRegistry() {
    BackgroundJobRegistry.clear()
  }

  @Test
  fun `a job of an armed server that ends by itself ends the process`() {
    val port = ServerSocket(0).use { Port(it.localPort) }
    val processExit = RecordingProcessExit()
    val stallReaction = StallReaction(WatchdogMode.Exit, processExit)
    val backgroundScope = CoroutineScope(SupervisorJob())
    val service = serviceRunningJobsIn(backgroundScope, port)

    val embeddedServer = server(service, port, stallReaction).watchEventLoop(port, stallReaction).start(wait = false)
    try {
      //The supervisor notices the end at its next pass
      Repeating.every(20.milliseconds) { stopRepeating() }
        .cycleTimeout(1.minutes)
        .trackedAs(jobName, stalenessThreshold = 5.minutes)
        .start(backgroundScope, supervisorJobDelay = 50.milliseconds)

      val deadline = System.nanoTime() + 5_000_000_000L
      while (processExit.statuses.isEmpty() && System.nanoTime() < deadline) {
        Thread.sleep(10)
      }
      assertThat(processExit.statuses).containsExactly(70)
    } finally {
      embeddedServer.stop(gracePeriodMillis = 100, timeoutMillis = 2_000)
    }
  }

  private fun serviceRunningJobsIn(backgroundScope: CoroutineScope, port: Port): KtorService = object : KtorService {
    override val serviceDescriptor: ServiceDescriptor = ServiceDescriptor("armed-stop-service", apiPrefix = "armed-stop", port)
    override val portOverride: Port = port

    override fun Application.configure() {
      installBackgroundJobs(OpenTelemetry.noop(), backgroundScope)
    }
  }

  @Test
  fun `stopping an armed server ends no process`() {
    val port = ServerSocket(0).use { Port(it.localPort) }
    val processExit = RecordingProcessExit()
    val stallReaction = StallReaction(WatchdogMode.Exit, processExit)
    val backgroundScope = CoroutineScope(SupervisorJob())

    val service = serviceRunningJobsIn(backgroundScope, port)

    val embeddedServer = server(service, port, stallReaction).watchEventLoop(port, stallReaction).start(wait = false)
    Repeating.every(20.milliseconds) {}
      .cycleTimeout(1.minutes)
      .trackedAs(jobName, stalenessThreshold = 5.minutes)
      .start(backgroundScope)
    Thread.sleep(200)

    embeddedServer.stop(gracePeriodMillis = 100, timeoutMillis = 2_000)
    Thread.sleep(200)

    assertThat(BackgroundJobRegistry.stateOf(jobName)?.endReason).isEqualTo(RepeatingEndReason.ScopeCancelled)
    assertThat(processExit.statuses).isEmpty()
  }
}
