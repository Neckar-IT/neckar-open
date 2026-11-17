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

import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.metrics.Meter
import it.neckar.open.dispose.Disposable
import it.neckar.open.time.monotonicTimeSource

private const val MeterName: String = "it.neckar.open.health"

/**
 * The job the measurement belongs to (`target-polling`, `auto-merge-cycle`).
 */
private val JobNameAttribute: AttributeKey<String> = AttributeKey.stringKey("job.name")

/**
 * Publishes the [BackgroundJobRegistry] as two observable gauges, so a dead or wedged background job
 * becomes alertable instead of only visible in `GET /health`:
 *
 * - `background_job.seconds_since_last_cycle` — how long the job has not completed a cycle. The
 *   diagnostic value: it shows *how far* a job is off, and it rises while a cycle hangs.
 * - `background_job.stale` — `1` when the job passed its staleness threshold, `0` otherwise. The
 *   value the alert fires on: the threshold belongs to the service that knows the job's interval, not
 *   to the alert rule.
 *
 * Call once at service start, after the jobs were registered — the gauges report whatever is
 * registered at collection time, so a job registered later is picked up automatically.
 *
 * A Ktor service does not call this directly: `installBackgroundJobs` does, and also ties the jobs'
 * scope to the application lifecycle.
 *
 * @param openTelemetry the instance the service resolved itself (`resolveServiceOpenTelemetry`).
 * Without a configured SDK the meter is a no-op, so the call is safe in tests and non-instrumented
 * runs.
 */
fun BackgroundJobRegistry.recordBackgroundJobsToOpenTelemetry(openTelemetry: OpenTelemetry): Disposable =
  recordBackgroundJobsToOpenTelemetry(openTelemetry.getMeter(MeterName))

/**
 * [recordBackgroundJobsToOpenTelemetry] with an explicit [meter]. Visible for tests so they can assert
 * the emitted measurements against an in-memory SDK without touching the global OTel instance.
 */
internal fun BackgroundJobRegistry.recordBackgroundJobsToOpenTelemetry(meter: Meter): Disposable {
  val secondsSinceLastCycle = meter.gaugeBuilder("background_job.seconds_since_last_cycle")
    .ofLongs()
    //No setUnit: the Prometheus exporter appends the unit to the metric name, and the name already
    //carries it - background_job_seconds_since_last_cycle_seconds would be the result.
    .setDescription("Seconds since the background job last completed a cycle (measured from its registration while no cycle completed yet)")
    .buildWithCallback { measurement ->
      val now = monotonicTimeSource.markNow()
      states.forEach { state ->
        measurement.record(state.sinceLastCompletedCycle(now).inWholeSeconds, state.attributes())
      }
    }

  val stale = meter.gaugeBuilder("background_job.stale")
    .ofLongs()
    .setDescription("1 when the background job has not completed a cycle within its staleness threshold, 0 otherwise")
    .buildWithCallback { measurement ->
      val now = monotonicTimeSource.markNow()
      states.forEach { state ->
        measurement.record(if (state.isStale(now)) 1L else 0L, state.attributes())
      }
    }

  return Disposable {
    secondsSinceLastCycle.close()
    stale.close()
  }
}

private fun BackgroundJobState.attributes(): Attributes = Attributes.of(JobNameAttribute, name.value)
