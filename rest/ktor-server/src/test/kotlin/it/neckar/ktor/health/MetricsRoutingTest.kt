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
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import it.neckar.rest.test.*
import it.neckar.ktor.metricsRouting
import it.neckar.open.http.Url
import org.junit.jupiter.api.Test
import it.neckar.open.test.utils.DisableLogging
import org.junit.jupiter.api.condition.DisabledOnOs
import org.junit.jupiter.api.condition.OS

/**
 *
 */
@DisableLogging
class MetricsRoutingTest {
  @DisabledOnOs(OS.WINDOWS, disabledReason = "somehow not all values are returned on windows")
  @Test
  fun testRouting() = testApplication {
    val micrometerSupport = MicrometerSupport()
    micrometerSupport.registerSystemMetrics()

    application {
      metricsRouting(
        metricsProvider = micrometerSupport.prometheusRegistry::scrape,
        baseUrl = Url.pattern("api/test-service")
      )
    }

    client.get("/api/test-service/metrics").let { response ->
      assertThat(response).isOk()
      assertThat(response.bodyAsText()).contains("disk_total_bytes")
      assertThat(response.bodyAsText()).contains("system_cpu_usage")
      assertThat(response.bodyAsText()).contains("system_load_average_1m")
    }
  }

  @DisabledOnOs(OS.WINDOWS, disabledReason = "somehow not all values are returned on windows")
  @Test
  fun testDefault() = testApplication {
    application {
      metricsRouting(
        baseUrl = Url.pattern("api/test-service")
      )
    }

    client.get("/api/test-service/metrics").let { response ->
      assertThat(response).isOk()
      assertThat(response.bodyAsText()).contains("disk_total_bytes")
      assertThat(response.bodyAsText()).contains("system_cpu_usage")
      assertThat(response.bodyAsText()).contains("system_load_average_1m")
    }
  }

  @Test
  fun testCustomMetrics() = testApplication {
    application {
      metricsRouting(
        baseUrl = Url.pattern("api/test-service"),
        metricsConfiguration = { micrometerSupport ->
          micrometerSupport.prometheusRegistry.gauge("hello-world-metric", 42.0)
        },
        addDefaultMetrics = false,
      )
    }

    client.get("/api/test-service/metrics").let { response ->
      assertThat(response).isOk()

      //Default has *not* been registered
      assertThat(response.bodyAsText()).doesNotContain("disk_total_bytes")
      assertThat(response.bodyAsText()).doesNotContain("system_cpu_usage")
      assertThat(response.bodyAsText()).doesNotContain("system_load_average_1m")

      assertThat(response.bodyAsText()).contains("hello_world_metric")
    }
  }

  /**
   * Verifies no double registration warning occurs: `registerDefaultMetrics()` must not be
   * called twice, which would trigger "This Gauge has been already registered" warnings.
   */
  @Test
  fun testNoDoubleRegistrationWarning() = testApplication {
    application {
      // This should NOT produce "This Gauge has been already registered" warnings
      metricsRouting(baseUrl = Url.pattern("api/test-service"))
    }

    client.get("/api/test-service/metrics").let { response ->
      assertThat(response).isOk()
      assertThat(response.bodyAsText()).contains("process_uptime_seconds")
    }
  }
}

