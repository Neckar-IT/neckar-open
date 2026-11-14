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
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import it.neckar.ktor.installJson
import it.neckar.open.health.DependencyClassification
import it.neckar.open.health.DependencyProbe
import it.neckar.open.health.DependencyStatus
import it.neckar.open.health.FatalReason
import it.neckar.open.health.ServiceState
import it.neckar.open.health.ServiceStateHolder
import it.neckar.open.http.Url
import it.neckar.open.test.utils.DisableLogging
import it.neckar.rest.health.DependencyName
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.seconds

@DisableLogging
class ReadinessGatingTest {
  private val baseUrl = Url.pattern("api/widget")

  private fun Application.setupHealth(holder: ServiceStateHolder, probes: List<DependencyProbe> = emptyList()) {
    installJson()
    installHealthLayer(baseUrl, holder, probes, DependencyPolling.OnDemand)
    routing {
      get("/api/widget/books") { call.respondText("books") }
      get("/api/tenants/health") { call.respondText("tenant health") }
      get("/api/documents/{id}/schema") { call.respondText("document schema") }
      get("/api/widget/diagnosis") { call.respondText("diagnosis") }.exemptFromReadinessGating()
      route("/api/widget/reports") {
        exemptFromReadinessGating()
        get("monthly") { call.respondText("monthly report") }
      }
    }
  }

  @Test
  fun `not-ready blocks business routes but keeps diagnosis reachable`() = testApplication {
    val probe = CountingProbe()
    application { setupHealth(ServiceStateHolder(), listOf(probe)) }

    assertThat(client.get("/api/widget/books").status).isEqualTo(HttpStatusCode.ServiceUnavailable)

    //live and ready answer 503 while starting themselves: the plain-text body and the probe call prove their handlers ran
    val live = client.get("/api/widget/health/live")
    assertThat(live.status).isEqualTo(HttpStatusCode.ServiceUnavailable)
    assertThat(live.bodyAsText()).isEqualTo("starting")
    assertThat(client.get("/api/widget/health/ready").status).isEqualTo(HttpStatusCode.ServiceUnavailable)
    assertThat(probe.checkCount).isEqualTo(1)

    assertThat(client.get("/api/widget/health").status).isEqualTo(HttpStatusCode.OK)
    assertThat(client.get("/api/widget/health/dependencies").status).isEqualTo(HttpStatusCode.OK)
  }

  @Test
  fun `not-ready keeps a marked route reachable`() = testApplication {
    application { setupHealth(ServiceStateHolder()) }

    assertThat(client.get("/api/widget/diagnosis").status).isEqualTo(HttpStatusCode.OK)
  }

  @Test
  fun `not-ready gates a route below a marked parent`() = testApplication {
    application { setupHealth(ServiceStateHolder()) }

    assertThat(client.get("/api/widget/reports/monthly").status).isEqualTo(HttpStatusCode.ServiceUnavailable)
  }

  @Test
  fun `not-ready gates a business route carrying a health or schema segment`() = testApplication {
    application { setupHealth(ServiceStateHolder()) }

    val tenantHealth = client.get("/api/tenants/health")
    assertThat(tenantHealth.status).isEqualTo(HttpStatusCode.ServiceUnavailable)
    assertThat(tenantHealth.bodyAsText()).contains("\"starting\"")

    assertThat(client.get("/api/documents/4711/schema").status).isEqualTo(HttpStatusCode.ServiceUnavailable)
  }

  @Test
  fun `ready allows business routes`() = testApplication {
    val holder = ServiceStateHolder().apply {
      transitionTo(ServiceState.Verifying)
      transitionTo(ServiceState.Ready)
    }
    application { setupHealth(holder) }

    val response = client.get("/api/widget/books")
    assertThat(response.status).isEqualTo(HttpStatusCode.OK)
    assertThat(response.bodyAsText()).isEqualTo("books")
  }

  /**
   * The state a running migration puts the service in. It is the reason the server binds before the
   * migration at all: the business routes have to be shut while the schema changes underneath them,
   * and the probes have to say why.
   */
  @Test
  fun `a migration gates the business routes and answers migrating`() = testApplication {
    val holder = ServiceStateHolder().apply {
      transitionTo(ServiceState.Verifying)
      transitionTo(ServiceState.Migrating)
    }
    application { setupHealth(holder) }

    assertThat(client.get("/api/widget/books").status).isEqualTo(HttpStatusCode.ServiceUnavailable)

    val readiness = client.get("/api/widget/health/ready")
    assertThat(readiness.status).isEqualTo(HttpStatusCode.ServiceUnavailable)
    assertThat(readiness.bodyAsText()).contains("\"migrating\"")

    //Liveness stays up: a migration must not look like a dead process and trigger a restart
    assertThat(client.get("/api/widget/health/live").status).isEqualTo(HttpStatusCode.OK)
    assertThat(client.get("/api/widget/health").status).isEqualTo(HttpStatusCode.OK)
  }

  /**
   * What a failed migration looks like from outside: reachable, refusing business traffic, and
   * saying which of the not-ready reasons applies.
   */
  @Test
  fun `a failed migration answers migration-failed and stays live`() = testApplication {
    val holder = ServiceStateHolder().apply {
      transitionTo(ServiceState.Verifying)
      transitionTo(ServiceState.Fatal(FatalReason.MigrationFailed, "migration 1.0.0 → 1.1.0 failed"))
    }
    application { setupHealth(holder) }

    assertThat(client.get("/api/widget/books").status).isEqualTo(HttpStatusCode.ServiceUnavailable)

    val readiness = client.get("/api/widget/health/ready")
    assertThat(readiness.status).isEqualTo(HttpStatusCode.ServiceUnavailable)
    assertThat(readiness.bodyAsText()).contains("\"migration-failed\"")

    assertThat(client.get("/api/widget/health/live").status).isEqualTo(HttpStatusCode.OK)
  }

  @Test
  fun `live never invokes a probe while ready triggers an on-demand check`() = testApplication {
    val holder = ServiceStateHolder().apply {
      transitionTo(ServiceState.Verifying)
      transitionTo(ServiceState.Ready)
    }
    val probe = CountingProbe()
    application { setupHealth(holder, listOf(probe)) }

    client.get("/api/widget/health/live")
    assertThat(probe.checkCount).isEqualTo(0)

    client.get("/api/widget/health/ready")
    assertThat(probe.checkCount).isEqualTo(1)
  }

  @Test
  fun `the periodic mode checks the probes at startup without any request`() = testApplication {
    val probe = CountingProbe()
    application {
      installJson()
      installHealthLayer(baseUrl, ServiceStateHolder(), listOf(probe), DependencyPolling.Periodic)
    }

    startApplication()

    // Real time: the poll runs on the application's dispatcher, which the virtual clock of the test does not drive.
    withContext(Dispatchers.Default) { withTimeout(10.seconds) { probe.firstCheck.await() } }
  }

  private class CountingProbe : DependencyProbe {
    var checkCount: Int = 0
    val firstCheck: CompletableDeferred<Unit> = CompletableDeferred()
    override val name: DependencyName = DependencyName.MongoDb
    override val classification: DependencyClassification = DependencyClassification.Required
    override suspend fun check(): DependencyStatus {
      checkCount++
      firstCheck.complete(Unit)
      return DependencyStatus.Up()
    }
  }
}
