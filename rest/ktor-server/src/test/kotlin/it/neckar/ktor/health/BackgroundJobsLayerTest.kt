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
import io.ktor.client.request.*
import io.ktor.server.application.*
import io.ktor.server.testing.*
import io.opentelemetry.api.OpenTelemetry
import it.neckar.open.concurrent.repeating.Repeating
import it.neckar.open.concurrent.repeating.RepeatingEndReason
import it.neckar.open.health.BackgroundJobName
import it.neckar.open.health.BackgroundJobRegistry
import it.neckar.open.health.trackedAs
import it.neckar.open.test.utils.DisableLogging
import it.neckar.open.test.utils.awaitCondition
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.job
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * A background job that keeps running after its service stopped goes on working against GitLab,
 * Mattermost or the database with nobody left who wanted it to — the reason this is one call and not
 * two things every service has to remember.
 */
@DisableLogging
class BackgroundJobsLayerTest {
  private val jobName = BackgroundJobName("test-sweep")

  @BeforeEach
  @AfterEach
  fun clearRegistry() {
    BackgroundJobRegistry.clear()
  }

  @Test
  fun `stopping the application cancels the background scope`() = testApplication {
    val backgroundScope = CoroutineScope(SupervisorJob())
    val cycles = AtomicInteger()

    lateinit var startedApplication: Application
    application {
      startedApplication = this
      installBackgroundJobs(OpenTelemetry.noop(), backgroundScope)
    }
    Repeating.every(20.milliseconds) { cycles.incrementAndGet() }
      .cycleTimeout(1.minutes)
      .trackedAs(jobName, stalenessThreshold = 5.minutes)
      .start(backgroundScope)

    //Force the application to start before it is stopped again
    client.get("/")
    awaitCondition(5.seconds, { "a first cycle" }) { cycles.get() > 0 }

    startedApplication.monitor.raise(ApplicationStopping, startedApplication)
    awaitCondition(5.seconds, { "the end of every job in the background scope" }) { backgroundScope.coroutineContext.job.isCompleted }
  }

  /**
   * The normal stop of an armed server: its jobs end with their cancelled scope, and nobody exits.
   */
  @Test
  fun `the jobs ending with a stopping application end no process`() = testApplication {
    val backgroundScope = CoroutineScope(SupervisorJob())
    val processExit = RecordingProcessExit()

    lateinit var startedApplication: Application
    application {
      startedApplication = this
      installStallReaction(StallReaction(WatchdogMode.Exit, processExit))
      installBackgroundJobs(OpenTelemetry.noop(), backgroundScope)
    }
    Repeating.every(20.milliseconds) {}
      .cycleTimeout(1.minutes)
      .trackedAs(jobName, stalenessThreshold = 5.minutes)
      .start(backgroundScope)
    client.get("/")

    startedApplication.monitor.raise(ApplicationStopPreparing, startedApplication.environment)
    startedApplication.monitor.raise(ApplicationStopping, startedApplication)
    awaitCondition(5.seconds, { "the end of $jobName" }) { BackgroundJobRegistry.stateOf(jobName)?.endReason != null }

    assertThat(BackgroundJobRegistry.stateOf(jobName)?.endReason).isEqualTo(RepeatingEndReason.ScopeCancelled)
    assertThat(processExit.statuses).isEmpty()
  }

  /**
   * Most tests start no background job at all, so the scope is optional — and leaving it out must not
   * cost the metrics.
   */
  @Test
  fun `the scope is optional`() = testApplication {
    lateinit var startedApplication: Application
    application {
      startedApplication = this
      installBackgroundJobs(OpenTelemetry.noop())
    }

    client.get("/")
    startedApplication.monitor.raise(ApplicationStopping, startedApplication)
  }
}
