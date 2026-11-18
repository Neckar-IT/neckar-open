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
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.plugins.statuspages.*
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
  fun `default and request flags combine into one latency`() = testApplication {
    application {
      install(FeatureFlagsPlugin) {
        defaultFlags = FeatureFlags.empty + FeatureFlag.realisticLatency
      }
    }
    routing {
      get("/") {
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
  fun `slowServer delays a route that never calls handle`() = testApplication {
    application {
      install(FeatureFlagsPlugin)
    }
    routing {
      get("/") {
        call.respondText("ok")
      }
    }

    val start = monotonicTimeSource.markNow()
    val response = client.get("/") { header(FeatureFlags.HeaderKey, "slowServer") }
    val elapsed = start.elapsedNow()

    val delays = response.headers.getAll(SimulatedDelayHeader).requireNotNull().map { value -> value.removeSuffix("ms").toLong().milliseconds }
    assertThat(delays).hasSize(1)
    assertThat(delays.single()).isBetween(1500.milliseconds, 2500.milliseconds)
    assertThat(elapsed).isGreaterThanOrEqualTo(delays.single())
  }

  @Test
  fun `forceServerError fails a route that never calls handle`() = testApplication {
    application {
      install(FeatureFlagsPlugin)
      installServerErrorStatusPage()
    }
    routing {
      get("/") {
        call.respondText("ok")
      }
    }

    assertThat(client.get("/").status).isEqualTo(HttpStatusCode.OK)
    assertThat(client.get("/") { header(FeatureFlags.HeaderKey, "forceServerError") }.status).isEqualTo(HttpStatusCode.InternalServerError)
  }

  @Test
  fun `an exempt route skips slowServer and forceServerError and keeps realisticLatency`() = testApplication {
    application {
      install(FeatureFlagsPlugin)
      installServerErrorStatusPage()
    }
    routing {
      get("/") {
        call.respondText("ok")
      }.exemptFromServerSimulation()
    }

    val response = client.get("/") { header(FeatureFlags.HeaderKey, "slowServer,forceServerError,realisticLatency") }

    assertThat(response.status).isEqualTo(HttpStatusCode.OK)
    val delays = response.headers.getAll(SimulatedDelayHeader).requireNotNull().map { value -> value.removeSuffix("ms").toLong() }
    assertThat(delays).hasSize(1)
    assertThat(delays.single()).isBetween(20L, 60L)
  }

  @Test
  fun `forceServerError fails an authenticated route only after the authentication`() = testApplication {
    application {
      install(FeatureFlagsPlugin)
      installServerErrorStatusPage()
      install(Authentication) {
        basic {
          validate { credentials -> UserIdPrincipal(credentials.name).takeIf { credentials.password == "secret" } }
        }
      }
    }
    routing {
      authenticate {
        get("/") {
          call.respondText("ok")
        }
      }
    }

    assertThat(client.get("/") { header(FeatureFlags.HeaderKey, "forceServerError") }.status).isEqualTo(HttpStatusCode.Unauthorized)
    assertThat(client.get("/") {
      header(FeatureFlags.HeaderKey, "forceServerError")
      basicAuth("user", "secret")
    }.status).isEqualTo(HttpStatusCode.InternalServerError)
  }

  @Test
  fun `a route below an exempt route keeps forceServerError`() = testApplication {
    application {
      install(FeatureFlagsPlugin)
      installServerErrorStatusPage()
    }
    routing {
      route("/meta") {
        get("child") {
          call.respondText("ok")
        }
      }.exemptFromServerSimulation()
    }

    assertThat(client.get("/meta/child") { header(FeatureFlags.HeaderKey, "forceServerError") }.status).isEqualTo(HttpStatusCode.InternalServerError)
  }

  @Test
  fun `realisticLatency delays a request that matches no route`() = testApplication {
    application {
      install(FeatureFlagsPlugin)
    }
    routing {
      get("/") {
        call.respondText("ok")
      }
    }

    val response = client.get("/missing") { header(FeatureFlags.HeaderKey, "realisticLatency") }

    assertThat(response.status).isEqualTo(HttpStatusCode.NotFound)
    assertThat(response.headers.getAll(SimulatedDelayHeader).requireNotNull()).hasSize(1)
  }

  @Test
  fun `handle adds no second slowServer delay with the plugin installed`() = testApplication {
    application {
      install(FeatureFlagsPlugin)
    }
    routing {
      get("/") {
        call.request.extractFeatureFlags().handle()
        call.respondText("ok")
      }
    }

    val response = client.get("/") { header(FeatureFlags.HeaderKey, "slowServer") }

    assertThat(response.headers.getAll(SimulatedDelayHeader).requireNotNull()).hasSize(1)
  }

  @Test
  fun `without the plugin handle applies slowServer itself`() = testApplication {
    routing {
      get("/") {
        call.request.extractFeatureFlags().handle()
        call.respondText("ok")
      }
    }

    val response = client.get("/") { header(FeatureFlags.HeaderKey, "slowServer") }

    assertThat(response.headers[SimulatedDelayHeader].requireNotNull().removeSuffix("ms").toLong()).isBetween(1500L, 2500L)
  }

  @Test
  fun `a CORS preflight ignores forceServerError`() = testApplication {
    application {
      install(FeatureFlagsPlugin)
      installServerErrorStatusPage()
      installProductCors(TestProductService, TestProductDomain)
    }
    routing {
      get("/") {
        call.respondText("ok")
      }
    }

    val response = client.options("/?${FeatureFlags.QueryParamName}=forceServerError") {
      header(HttpHeaders.Origin, "https://test-product.neckar.it")
      header(HttpHeaders.AccessControlRequestMethod, "GET")
    }

    assertThat(response.status).isEqualTo(HttpStatusCode.OK)
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

/**
 * Answers an exception with 500, like the `exception<Throwable>` handler of `installStatusPages`.
 */
private fun Application.installServerErrorStatusPage() {
  install(StatusPages) {
    exception<Throwable> { call, _ ->
      call.respond(HttpStatusCode.InternalServerError)
    }
  }
}
