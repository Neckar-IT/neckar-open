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

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics
import io.micrometer.core.instrument.binder.jvm.JvmCompilationMetrics
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics
import io.micrometer.core.instrument.binder.jvm.JvmHeapPressureMetrics
import io.micrometer.core.instrument.binder.jvm.JvmInfoMetrics
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics
import io.micrometer.core.instrument.binder.logging.LogbackMetrics
import io.micrometer.core.instrument.binder.system.DiskSpaceMetrics
import io.micrometer.core.instrument.binder.system.ProcessorMetrics
import io.micrometer.core.instrument.binder.system.UptimeMetrics
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import java.io.File

/**
 * Creates an endpoint for the health state using micrometer
 */
class MicrometerSupport() {
  val prometheusRegistry: PrometheusMeterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)

  fun registerSystemMetrics() {
    UptimeMetrics().bindTo(prometheusRegistry)
    DiskSpaceMetrics(File("/")).bindTo(prometheusRegistry)
    ProcessorMetrics().bindTo(prometheusRegistry)
  }

  fun registerJvmMetrics() {
    ClassLoaderMetrics().bindTo(prometheusRegistry)
    JvmMemoryMetrics().bindTo(prometheusRegistry)
    JvmGcMetrics().bindTo(prometheusRegistry)
    JvmThreadMetrics().bindTo(prometheusRegistry)

    JvmCompilationMetrics().bindTo(prometheusRegistry)
    JvmHeapPressureMetrics().bindTo(prometheusRegistry)
    JvmInfoMetrics().bindTo(prometheusRegistry)

    LogbackMetrics().bindTo(prometheusRegistry)
  }

  fun registerDefaultMetrics() {
    registerSystemMetrics()
    registerJvmMetrics()
  }

  companion object {
    fun createDefault(): MicrometerSupport {
      return MicrometerSupport().also {
        it.registerDefaultMetrics()
      }
    }
  }
}

/**
 * Binds the Micrometer JVM/system binders that the OpenTelemetry Java Agent **does not**
 * cover out of the box, so they get bridged through to OTel via the global registry.
 *
 * The OTel Java Agent's `runtime-telemetry` instrumentation provides the JVM core set
 * (memory, GC, threads, classloader, processor/CPU) directly. What it omits — and what
 * this helper adds — is:
 *
 * - [LogbackMetrics] — logback event counts per level
 * - [UptimeMetrics] — process uptime and start time
 * - [DiskSpaceMetrics] — free/total bytes on the given path (defaults to `/`)
 * - [JvmHeapPressureMetrics] — GC overhead and long-pause indicators
 * - [JvmCompilationMetrics] — JIT compilation time
 * - [JvmInfoMetrics] — static info (JVM version, vendor, runtime)
 *
 * Call this on `Metrics.globalRegistry` from any service that runs with the OTel Java Agent
 * attached — without it those metrics are silently absent in Grafana even though they would
 * be cheap to collect.
 */
fun MeterRegistry.registerOtelAgentSupplementaryBinders(diskSpaceRoot: File = File("/")) {
  LogbackMetrics().bindTo(this)
  UptimeMetrics().bindTo(this)
  DiskSpaceMetrics(diskSpaceRoot).bindTo(this)
  JvmHeapPressureMetrics().bindTo(this)
  JvmCompilationMetrics().bindTo(this)
  JvmInfoMetrics().bindTo(this)
}
