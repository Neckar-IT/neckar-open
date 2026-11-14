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
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import it.neckar.common.featureflags.FeatureFlag
import it.neckar.common.featureflags.FeatureFlags
import it.neckar.open.kotlin.lang.requireNotNull
import it.neckar.open.test.utils.SlowTest
import it.neckar.open.time.monotonicTimeSource
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.milliseconds

@SlowTest
class FeatureFlagsPluginTest {
  @Test
  fun `a default flag delays a route that never calls handle`() = testApplication {
    application {
      install(FeatureFlagsPlugin) {
        defaultFlags = FeatureFlags.empty + FeatureFlag.realisticLatency
      }
    }
    routing {
      get("/") {
        call.respondText(featureFlags().contains(FeatureFlag.realisticLatency).toString())
      }
    }

    val start = monotonicTimeSource.markNow()
    val response = client.get("/")
    val elapsed = start.elapsedNow()

    assertThat(response.bodyAsText()).isEqualTo("true")
    val delay = response.headers[SimulatedDelayHeader].requireNotNull().removeSuffix("ms").toLong().milliseconds
    assertThat(delay).isBetween(20.milliseconds, 60.milliseconds)
    assertThat(elapsed).isGreaterThanOrEqualTo(delay)
  }

  @Test
  fun `without a default flag the request flags decide`() = testApplication {
    application {
      install(FeatureFlagsPlugin)
    }
    routing {
      get("/") {
        call.respondText(featureFlags().encodeToString())
      }
    }

    val plain = client.get("/")
    assertThat(plain.bodyAsText()).isEqualTo("")
    assertThat(plain.headers[SimulatedDelayHeader]).isNull()

    val withHeader = client.get("/") { header(FeatureFlags.HeaderKey, "realisticLatency") }
    assertThat(withHeader.bodyAsText()).isEqualTo("realisticLatency")
    assertThat(withHeader.headers[SimulatedDelayHeader].requireNotNull().removeSuffix("ms").toLong()).isBetween(20L, 60L)
  }

  @Test
  fun `default and request flags combine, and handle adds no second latency`() = testApplication {
    application {
      install(FeatureFlagsPlugin) {
        defaultFlags = FeatureFlags.empty + FeatureFlag.realisticLatency
      }
    }
    routing {
      get("/") {
        featureFlags().handle()
        call.respondText(featureFlags().encodeToString())
      }
    }

    val response = client.get("/") { header(FeatureFlags.HeaderKey, "testing") }

    assertThat(response.bodyAsText().split(",").toSet()).isEqualTo(setOf("realisticLatency", "testing"))
    assertThat(response.headers.getAll(SimulatedDelayHeader).requireNotNull()).hasSize(1)
  }

  @Test
  fun `without the plugin handle applies the latency itself`() = testApplication {
    routing {
      get("/") {
        call.request.extractFeatureFlags().handle()
        call.respondText("ok")
      }
    }

    val response = client.get("/") { header(FeatureFlags.HeaderKey, "realisticLatency") }

    assertThat(response.headers.getAll(SimulatedDelayHeader).requireNotNull()).hasSize(1)
  }

  @Test
  fun `extractFeatureFlags carries the default flags of the plugin`() = testApplication {
    application {
      install(FeatureFlagsPlugin) {
        defaultFlags = FeatureFlags.empty + FeatureFlag.testing
      }
    }
    routing {
      get("/") {
        call.respondText(call.request.extractFeatureFlags().encodeToString())
      }
    }

    assertThat(client.get("/").bodyAsText()).isEqualTo("testing")
  }

  @Test
  fun `handle still delays slowServer with the plugin installed`() = testApplication {
    application {
      install(FeatureFlagsPlugin)
    }
    routing {
      get("/") {
        featureFlags().handle()
        call.respondText("ok")
      }
    }

    val response = client.get("/") { header(FeatureFlags.HeaderKey, "slowServer") }

    val delays = response.headers.getAll(SimulatedDelayHeader).requireNotNull().map { value -> value.removeSuffix("ms").toLong() }
    assertThat(delays).hasSize(1)
    assertThat(delays.single()).isBetween(1500L, 2500L)
  }

  @Test
  fun `a second installFeatureFlagSupport keeps the first default flags`() = testApplication {
    application {
      installFeatureFlagSupport(defaultFlags = FeatureFlags.empty + FeatureFlag.testing)
      installFeatureFlagSupport(defaultFlags = FeatureFlags.empty + FeatureFlag.slowUi)
    }
    routing {
      get("/") {
        call.respondText(featureFlags().encodeToString())
      }
    }

    assertThat(client.get("/").bodyAsText()).isEqualTo("testing")
  }
}
