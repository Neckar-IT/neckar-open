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

import io.micrometer.core.instrument.Gauge
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
import io.prometheus.metrics.model.snapshots.CounterSnapshot
import io.prometheus.metrics.model.snapshots.CounterSnapshot.CounterDataPointSnapshot
import io.prometheus.metrics.model.snapshots.DataPointSnapshot
import io.prometheus.metrics.model.snapshots.GaugeSnapshot
import io.prometheus.metrics.model.snapshots.GaugeSnapshot.GaugeDataPointSnapshot
import io.prometheus.metrics.model.snapshots.InfoSnapshot
import io.prometheus.metrics.model.snapshots.InfoSnapshot.InfoDataPointSnapshot
import io.prometheus.metrics.model.snapshots.MetricMetadata
import io.prometheus.metrics.model.snapshots.MetricSnapshots
import it.neckar.open.collections.fastForEach
import it.neckar.open.formatting.dateTimeFormat
import it.neckar.open.i18n.I18nConfiguration
import it.neckar.open.unit.si.ms
import java.io.File
import java.lang.management.BufferPoolMXBean
import java.lang.management.ManagementFactory
import java.lang.management.MemoryMXBean
import java.lang.management.MemoryUsage
import java.lang.management.RuntimeMXBean

fun main() {
  // Speicher-MXBean abrufen
  val memoryMXBean: MemoryMXBean = ManagementFactory.getMemoryMXBean()

  // Heap-Speicherstatistiken abrufen
  val heapMemoryUsage: MemoryUsage = memoryMXBean.heapMemoryUsage
  println("Heap-Speicher:")
  println("Maximaler Speicher: ${heapMemoryUsage.max / (1024 * 1024)} MB")
  println("Belegter Speicher: ${heapMemoryUsage.used / (1024 * 1024)} MB")
  println("Freier Speicher: ${(heapMemoryUsage.max - heapMemoryUsage.used) / (1024 * 1024)} MB")

  // Non-Heap-Speicherstatistiken abrufen
  val nonHeapMemoryUsage: MemoryUsage = memoryMXBean.nonHeapMemoryUsage
  println("\nNon-Heap-Speicher:")
  println("Maximaler Speicher: ${nonHeapMemoryUsage.max / (1024 * 1024)} MB")
  println("Belegter Speicher: ${nonHeapMemoryUsage.used / (1024 * 1024)} MB")

  // Runtime-MXBean abrufen
  val runtimeMXBean: RuntimeMXBean = ManagementFactory.getRuntimeMXBean()

  // JVM-Startparameter abrufen
  println("\nJVM-Startparameter:")
  runtimeMXBean.inputArguments.forEach { arg ->
    println(arg)
  }

  // JVM-Laufzeitinformationen abrufen
  println("\nJVM-Laufzeitinformationen:")
  println("Name: ${runtimeMXBean.name}")
  println("Uptime: ${runtimeMXBean.uptime} ms")
  println("VM-Name: ${runtimeMXBean.vmName}")
  println("VM-Version: ${runtimeMXBean.vmVersion}")
  println("VM-Vendor: ${runtimeMXBean.vmVendor}")

  if (true) {
    return
  }

  MicrometerDemo().runDemo()
}

/**
 *
 */
class MicrometerDemo {
  fun runDemo() {
    val prometheusRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)

    ClassLoaderMetrics().bindTo(prometheusRegistry)
    JvmMemoryMetrics().bindTo(prometheusRegistry)
    JvmGcMetrics().bindTo(prometheusRegistry)
    JvmThreadMetrics().bindTo(prometheusRegistry)
    ProcessorMetrics().bindTo(prometheusRegistry)

    // Register additional metrics
    JvmCompilationMetrics().bindTo(prometheusRegistry)
    JvmHeapPressureMetrics().bindTo(prometheusRegistry)
    JvmInfoMetrics().bindTo(prometheusRegistry)
    LogbackMetrics().bindTo(prometheusRegistry)
    UptimeMetrics().bindTo(prometheusRegistry)
    DiskSpaceMetrics(File("/")).bindTo(prometheusRegistry)
    ProcessorMetrics().bindTo(prometheusRegistry)

    Gauge.builder("jvm.memory.used") {
      Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
    }.description("Heap memory used")
      .baseUnit("bytes")
      .register(prometheusRegistry)

    val snapshot: MetricSnapshots = prometheusRegistry.prometheusRegistry.scrape()

    snapshot.forEach { metricSnapshot ->
      println("------------------- ${metricSnapshot::class}")

      when (metricSnapshot) {
        is GaugeSnapshot -> {
          println("Gauge")
        }

        is InfoSnapshot -> {
          println("Info")
        }

        is CounterSnapshot -> {
          println("Counter")
        }

        else -> {
          throw IllegalStateException("Unknown type: ${metricSnapshot::class}")
        }
      }

      val metadata: MetricMetadata = metricSnapshot.metadata
      println("${metadata.name} - ${metadata.unit} - [${metadata.prometheusName}]")

      metricSnapshot.dataPoints.forEach { dataPointSnapshot: DataPointSnapshot ->
        @ms val created: Double? = when {
          dataPointSnapshot.hasCreatedTimestamp() -> {
            dataPointSnapshot.createdTimestampMillis.toDouble()
          }

          dataPointSnapshot.hasScrapeTimestamp() -> {
            dataPointSnapshot.scrapeTimestampMillis.toDouble()
          }

          else -> {
            null
          }
        }

        println("\t${dateTimeFormat.formatNullable(created, I18nConfiguration.Germany, fallback = "")}: ${dataPointSnapshot.labels}")

        when (dataPointSnapshot) {
          is GaugeDataPointSnapshot -> {
            println("\t\tValue: ${dataPointSnapshot.value}")
          }

          is InfoDataPointSnapshot -> {
          }

          is CounterDataPointSnapshot -> {
            println("\t\tCount: ${dataPointSnapshot.value}")
          }
        }
      }
    }
  }
}
