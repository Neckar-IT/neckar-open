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

import assertk.*
import assertk.assertions.*
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.sdk.metrics.SdkMeterProvider
import io.opentelemetry.sdk.metrics.data.MetricData
import io.opentelemetry.sdk.testing.exporter.InMemoryMetricReader
import it.neckar.open.dispose.Disposable
import it.neckar.open.time.VirtualNowProvider
import it.neckar.open.test.utils.WithVirtualTime
import it.neckar.open.test.utils.advance
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.minutes

@WithVirtualTime
class BackgroundJobMetricsTest {
  private val jobName = BackgroundJobName("target-polling")

  private lateinit var reader: InMemoryMetricReader
  private lateinit var meterProvider: SdkMeterProvider
  private var registration: Disposable? = null

  @BeforeEach
  fun setUp() {
    BackgroundJobRegistry.clear()
    reader = InMemoryMetricReader.create()
    meterProvider = SdkMeterProvider.builder().registerMetricReader(reader).build()
  }

  @AfterEach
  fun tearDown() {
    registration?.dispose()
    registration = null
    meterProvider.shutdown()
    BackgroundJobRegistry.clear()
  }

  @Test
  fun `a job that just completed a cycle reports zero seconds and is not stale`() {
    BackgroundJobRegistry.register(jobName, stalenessThreshold = 5.minutes)
    BackgroundJobRegistry.recordCycleCompleted(jobName)
    registration = BackgroundJobRegistry.recordBackgroundJobsToOpenTelemetry(meterProvider.get("test"))

    val metrics = reader.collectAllMetrics()

    assertThat(metrics.longValueOf("background_job.seconds_since_last_cycle", jobName)).isEqualTo(0L)
    assertThat(metrics.longValueOf("background_job.stale", jobName)).isEqualTo(0L)
  }

  /**
   * The alert case: nothing completes any more. `stale` flips to 1 while the process, its HTTP server
   * and its container all keep looking perfectly healthy.
   */
  @Test
  fun `a silent job reports its silence and flips stale to one`(nowProvider: VirtualNowProvider) {
    BackgroundJobRegistry.register(jobName, stalenessThreshold = 5.minutes)
    BackgroundJobRegistry.recordCycleCompleted(jobName)
    registration = BackgroundJobRegistry.recordBackgroundJobsToOpenTelemetry(meterProvider.get("test"))

    nowProvider.advance(6.minutes)
    val metrics = reader.collectAllMetrics()

    assertThat(metrics.longValueOf("background_job.seconds_since_last_cycle", jobName)).isEqualTo(6.minutes.inWholeSeconds)
    assertThat(metrics.longValueOf("background_job.stale", jobName)).isEqualTo(1L)
  }

  /**
   * The gauges read the registry at collection time, so a job that registers after the metrics were
   * installed — a service that starts its jobs in stages — is reported without a second registration.
   */
  @Test
  fun `a job registered after the gauges are installed is reported`() {
    registration = BackgroundJobRegistry.recordBackgroundJobsToOpenTelemetry(meterProvider.get("test"))

    BackgroundJobRegistry.register(jobName, stalenessThreshold = 5.minutes)

    assertThat(reader.collectAllMetrics().longValueOf("background_job.stale", jobName)).isEqualTo(0L)
  }

  @Test
  fun `nothing is reported once the registration is disposed`() {
    BackgroundJobRegistry.register(jobName, stalenessThreshold = 5.minutes)
    registration = BackgroundJobRegistry.recordBackgroundJobsToOpenTelemetry(meterProvider.get("test"))
    registration?.dispose()
    registration = null

    assertThat(reader.collectAllMetrics()).isEmpty()
  }

  /**
   * The single measurement of [metricName] recorded for [jobName].
   */
  private fun Collection<MetricData>.longValueOf(metricName: String, jobName: BackgroundJobName): Long? {
    val metric = singleOrNull { it.name == metricName } ?: return null
    return metric.longGaugeData.points
      .singleOrNull { it.attributes.get(AttributeKey.stringKey("job.name")) == jobName.value }
      ?.value
  }
}
