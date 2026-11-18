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

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.routing
import it.neckar.ktor.KtorRoutes
import it.neckar.ktor.exemptFromServerSimulation
import it.neckar.ktor.get
import it.neckar.ktor.server.openapi.ktor.doc
import it.neckar.open.health.BackgroundJobRegistry
import it.neckar.open.health.DependencyMonitor
import it.neckar.open.health.HealthHttpStatus
import it.neckar.open.health.HealthProjection
import it.neckar.open.health.ServiceStateHolder
import it.neckar.open.http.UrlPattern
import it.neckar.rest.health.HealthExamples
import it.neckar.rest.health.HealthResponse
import it.neckar.rest.health.ReadinessResponse
import it.neckar.rest.openapi.OpenApiMetaData
import it.neckar.rest.openapi.OpenApiTag
import it.neckar.rest.openapi.dsl.OperationId

private val HealthTag: OpenApiTag = OpenApiTag(name = "health", displayName = "Health")

/**
 * The three health probes plus the dependency diagnosis endpoint, projecting the [stateHolder].
 *
 * `live` never touches a dependency; `ready` triggers an on-demand [DependencyMonitor.checkAll] so
 * readiness is live; `health` is always `200` and carries the aggregated state for alerting.
 *
 * `health` additionally reports the process-wide [BackgroundJobRegistry]. The background jobs are not
 * held by the [stateHolder]: a job records its cycles where it runs, without a holder threaded through
 * every constructor, and a stale job deliberately changes neither the service state nor readiness.
 */
class HealthRouting(
  /**
   * The service base URL the probes are bound to (e.g. `api/widget`).
   */
  val baseUrl: UrlPattern.Relative0,
  /**
   * The single source of truth the probes project.
   */
  val stateHolder: ServiceStateHolder,
  /**
   * The monitor invoked on-demand by `/health/ready`.
   */
  val dependencyMonitor: DependencyMonitor,
) : KtorRoutes {

  override fun Application.configure() {
    val endpoints = HealthRoutingEndpoints.forBaseUrl(baseUrl)

    routing {
      get(endpoints.Health_Live) {
        when (HealthProjection.liveStatus(stateHolder.state)) {
          HealthHttpStatus.Ok -> call.respondText("live")
          HealthHttpStatus.ServiceUnavailable -> call.respondText("starting", status = HttpStatusCode.ServiceUnavailable)
        }
      }.exemptFromServerSimulation().exemptFromReadinessGating().doc {
        category = OpenApiMetaData.Category.Meta
        operationId = OperationId("GetHealthLive")
        tag = HealthTag

        summary = "Liveness probe"
        description = "Returns 200 as soon as the HTTP server is up. Never touches a dependency. Drives the container restart."
        response("The server is live") {
          statusCode = HttpStatusCode.OK
          body<String> { noExamples() }
        }
        response("The server is still starting") {
          statusCode = HttpStatusCode.ServiceUnavailable
          body<String> { noExamples() }
        }
      }

      get(endpoints.Health_Ready) {
        dependencyMonitor.checkAll()
        val state = stateHolder.state
        val dependencies = stateHolder.dependencies
        call.respond<ReadinessResponse>(
          HealthProjection.readinessStatus(state, dependencies).toHttpStatusCode(),
          HealthProjection.readinessResponse(state, dependencies),
        )
      }.exemptFromServerSimulation().exemptFromReadinessGating().doc {
        category = OpenApiMetaData.Category.Meta
        operationId = OperationId("GetHealthReady")
        tag = HealthTag

        summary = "Readiness probe"
        description = "Returns 200 only when hard dependencies are reachable and no migration is active; otherwise 503 with the state in the body. Checked live (on-demand dependency probe)."
        response("The service is ready") {
          statusCode = HttpStatusCode.OK
          body<ReadinessResponse> { example("ready", HealthExamples.readyResponse) }
        }
        response("The service is not ready") {
          statusCode = HttpStatusCode.ServiceUnavailable
          body<ReadinessResponse> { example("dependency-unavailable", HealthExamples.dependencyUnavailableResponse) }
        }
      }

      get(endpoints.Health) {
        call.respond<HealthResponse>(
          HttpStatusCode.OK,
          HealthProjection.healthResponse(
            stateHolder.state,
            stateHolder.dependencies,
            stateHolder.uncaughtExceptions,
            BackgroundJobRegistry.states,
          ),
        )
      }.exemptFromServerSimulation().exemptFromReadinessGating().doc {
        category = OpenApiMetaData.Category.Meta
        operationId = OperationId("GetHealth")
        tag = HealthTag

        summary = "Aggregated health"
        description = "Always returns 200; the aggregated state, the last error and the liveness of the background jobs are in the body. Source for alerting."
        response("The aggregated health") {
          statusCode = HttpStatusCode.OK
          body<HealthResponse> { example("degraded", HealthExamples.healthResponse) }
        }
      }

      get(endpoints.Health_Dependencies) {
        call.respond<ReadinessResponse>(
          HttpStatusCode.OK,
          HealthProjection.readinessResponse(stateHolder.state, stateHolder.dependencies),
        )
      }.exemptFromServerSimulation().exemptFromReadinessGating().doc {
        category = OpenApiMetaData.Category.Meta
        operationId = OperationId("GetHealthDependencies")
        tag = HealthTag

        summary = "Dependency diagnosis"
        description = "Lists the status of every declared dependency. Always reachable (bound to liveness, never gated)."
        response("The dependency statuses") {
          statusCode = HttpStatusCode.OK
          body<ReadinessResponse> { example("ready", HealthExamples.readyResponse) }
        }
      }
    }
  }
}
