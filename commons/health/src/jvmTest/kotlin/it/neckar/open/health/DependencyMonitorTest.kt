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
package it.neckar.open.health

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isNull
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import it.neckar.open.time.VirtualNowProvider
import it.neckar.open.time.now
import it.neckar.open.time.nowProvider
import it.neckar.open.time.resetNowProvider
import it.neckar.rest.health.DependencyName
import it.neckar.rest.health.ServiceStateDto
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class DependencyMonitorTest {
  private val monitorLogger: Logger = LoggerFactory.getLogger("it.neckar.open.health.DependencyMonitor") as Logger
  private val logEvents: ListAppender<ILoggingEvent> = ListAppender()
  private val clock = VirtualNowProvider(1_000_000_000.0)

  @BeforeEach
  fun setUp() {
    nowProvider = clock
    logEvents.start()
    monitorLogger.addAppender(logEvents)
  }

  @AfterEach
  fun tearDown() {
    monitorLogger.detachAppender(logEvents)
    logEvents.stop()
    resetNowProvider()
  }

  private fun loggedLines(): List<String> = logEvents.list.map { "${it.level} ${it.formattedMessage}" }

  private fun readyHolder(): ServiceStateHolder = ServiceStateHolder().apply {
    transitionTo(ServiceState.Verifying)
    transitionTo(ServiceState.Ready)
  }

  private fun statusOf(holder: ServiceStateHolder, name: DependencyName): DependencyStatus =
    holder.dependencies.first { it.name == name }.status

  @Test
  suspend fun `required dependency down projects to dependency-unavailable`() {
    val holder = readyHolder()
    val monitor = DependencyMonitor(holder, listOf(downProbe(DependencyName.MongoDb, DependencyClassification.Required)))

    monitor.checkAll()

    assertThat(HealthProjection.wireState(holder.state, holder.dependencies)).isEqualTo(ServiceStateDto.DependencyUnavailable)
  }

  @Test
  suspend fun `optional dependency down transitions to degraded`() {
    val holder = readyHolder()
    val monitor = DependencyMonitor(holder, listOf(downProbe(DependencyName.S3, DependencyClassification.Optional)))

    monitor.checkAll()

    assertThat(holder.state).isEqualTo(ServiceState.Degraded)
    assertThat(HealthProjection.wireState(holder.state, holder.dependencies)).isEqualTo(ServiceStateDto.Degraded)
  }

  @Test
  suspend fun `degraded recovers to ready when optional dependency returns`() {
    val holder = readyHolder()
    val probe = SwitchableProbe(DependencyName.S3, DependencyClassification.Optional)
    val monitor = DependencyMonitor(holder, listOf(probe))

    probe.down = true
    monitor.checkAll()
    assertThat(holder.state).isEqualTo(ServiceState.Degraded)

    probe.down = false
    monitor.checkAll()
    assertThat(holder.state).isEqualTo(ServiceState.Ready)
  }

  @Test
  fun `probe exceeding its timeout counts as down and does not block`() = runTest {
    val holder = readyHolder()
    val hangingProbe = object : DependencyProbe {
      override val name = DependencyName.MongoDb
      override val classification = DependencyClassification.Required
      override val timeout: Duration = 2.seconds
      override suspend fun check(): DependencyStatus {
        delay(10.seconds)
        return DependencyStatus.Up()
      }
    }
    val monitor = DependencyMonitor(holder, listOf(hangingProbe))

    monitor.checkAll()

    val status = statusOf(holder, DependencyName.MongoDb)
    assertThat(status).isInstanceOf(DependencyStatus.Down::class)
    assertThat((status as DependencyStatus.Down).error).isInstanceOf(DependencyTimeoutException::class)
  }

  @Test
  suspend fun `recovery is error-scoped - only the recovered dependency is cleared`() {
    val holder = readyHolder()
    val mongo = SwitchableProbe(DependencyName.MongoDb, DependencyClassification.Required)
    val s3 = SwitchableProbe(DependencyName.S3, DependencyClassification.Optional)
    val monitor = DependencyMonitor(holder, listOf(mongo, s3))

    mongo.down = true
    s3.down = true
    monitor.checkAll()
    assertThat(statusOf(holder, DependencyName.MongoDb)).isInstanceOf(DependencyStatus.Down::class)
    assertThat(statusOf(holder, DependencyName.S3)).isInstanceOf(DependencyStatus.Down::class)

    mongo.down = false
    monitor.checkAll()

    assertThat(statusOf(holder, DependencyName.MongoDb)).isEqualTo(DependencyStatus.Up())
    assertThat(statusOf(holder, DependencyName.S3)).isInstanceOf(DependencyStatus.Down::class)
  }

  @Test
  fun `every status change writes exactly one line with its cause`() = runTest {
    val holder = readyHolder()
    val probe = SequenceProbe(DependencyName.MongoDb, listOf(Answer.Up, Answer.Hang, Answer.Hang, Answer.Up))
    val monitor = DependencyMonitor(holder, listOf(probe))

    monitor.checkAll()
    assertThat(loggedLines()).isEmpty()

    clock.advanceBy(23_000.0)
    monitor.checkAll()
    val outageStart = (statusOf(holder, DependencyName.MongoDb) as DependencyStatus.Down).since
    clock.advanceBy(23_000.0)
    monitor.checkAll()
    clock.advanceBy(23_000.0)
    monitor.checkAll()

    assertThat(loggedLines()).containsExactly(
      "WARN Dependency 'mongodb' (Required) is down since $outageStart: DependencyTimeoutException: probe 'mongodb' exceeded 5s",
      "INFO Dependency 'mongodb' (Required) is up again after 46s",
    )
    assertThat(logEvents.list.first().throwableProxy).isNull()
  }

  @Test
  fun `concurrent checks log a change once`() = runTest {
    val holder = readyHolder()
    val slowProbe = object : DependencyProbe {
      override val name = DependencyName.MongoDb
      override val classification = DependencyClassification.Required
      override suspend fun check(): DependencyStatus {
        delay(1.seconds)
        throw IllegalStateException("connection refused")
      }
    }
    val monitor = DependencyMonitor(holder, listOf(slowProbe))

    launch { monitor.checkAll() }
    launch { monitor.checkAll() }
    testScheduler.advanceUntilIdle()

    assertThat(logEvents.list.map { it.level.toString() }).containsExactly("WARN")
  }

  @Test
  suspend fun `cancellation raised inside a probe counts as down`() {
    val holder = readyHolder()
    val probe = object : DependencyProbe {
      override val name = DependencyName.S3
      override val classification = DependencyClassification.Optional
      override suspend fun check(): DependencyStatus = throw CancellationException("probe-internal timeout")
    }
    val monitor = DependencyMonitor(holder, listOf(probe))

    monitor.checkAll()

    assertThat(statusOf(holder, DependencyName.S3)).isInstanceOf(DependencyStatus.Down::class)
    assertThat(holder.state).isEqualTo(ServiceState.Degraded)
  }

  @Test
  suspend fun `repeated down writes no further line`() {
    val holder = readyHolder()
    val monitor = DependencyMonitor(holder, listOf(throwingProbe(DependencyName.S3, DependencyClassification.Optional)))

    monitor.checkAll()
    monitor.checkAll()
    monitor.checkAll()

    assertThat(loggedLines()).containsExactly(
      "WARN Dependency 's3' (Optional) is down since ${(statusOf(holder, DependencyName.S3) as DependencyStatus.Down).since}: IllegalStateException: connection refused",
    )
    assertThat(logEvents.list.single().throwableProxy.className).isEqualTo(IllegalStateException::class.qualifiedName)
  }

  @Test
  suspend fun `degradation writes one line with its detail`() {
    val holder = readyHolder()
    val probe = object : DependencyProbe {
      override val name = DependencyName.Jwks
      override val classification = DependencyClassification.Required
      override suspend fun check(): DependencyStatus = DependencyStatus.Degraded("held key set never confirmed", now())
    }
    val monitor = DependencyMonitor(holder, listOf(probe))

    monitor.checkAll()
    monitor.checkAll()

    assertThat(loggedLines()).containsExactly(
      "WARN Dependency 'jwks' (Required) is degraded: held key set never confirmed",
    )
  }

  private enum class Answer {
    Up,
    Hang,
  }

  /**
   * Answers each [check] with the next [Answer]; an [Answer.Hang] outlasts the 5 s timeout.
   */
  private class SequenceProbe(
    override val name: DependencyName,
    answers: List<Answer>,
  ) : DependencyProbe {
    private val remaining = ArrayDeque(answers)
    override val classification = DependencyClassification.Required
    override val timeout: Duration = 5.seconds
    override suspend fun check(): DependencyStatus {
      return when (remaining.removeFirst()) {
        Answer.Up -> DependencyStatus.Up()

        Answer.Hang -> {
          delay(1000.seconds)
          DependencyStatus.Up()
        }
      }
    }
  }

  private fun throwingProbe(name: DependencyName, classification: DependencyClassification): DependencyProbe =
    object : DependencyProbe {
      override val name = name
      override val classification = classification
      override suspend fun check(): DependencyStatus = throw IllegalStateException("connection refused")
    }

  private fun downProbe(name: DependencyName, classification: DependencyClassification): DependencyProbe =
    object : DependencyProbe {
      override val name = name
      override val classification = classification
      override suspend fun check(): DependencyStatus = DependencyStatus.Down(RuntimeException("down"), now())
    }

  private class SwitchableProbe(
    override val name: DependencyName,
    override val classification: DependencyClassification,
  ) : DependencyProbe {
    var down: Boolean = false
    override suspend fun check(): DependencyStatus =
      if (down) DependencyStatus.Down(RuntimeException("down"), now()) else DependencyStatus.Up()
  }
}
