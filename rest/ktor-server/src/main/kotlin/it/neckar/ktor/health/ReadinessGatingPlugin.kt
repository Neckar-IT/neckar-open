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

import io.ktor.server.application.RouteScopedPlugin
import io.ktor.server.application.createRouteScopedPlugin
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.RoutingPipelineCall
import io.ktor.util.AttributeKey
import it.neckar.open.health.DependencyState
import it.neckar.open.health.HealthHttpStatus
import it.neckar.open.health.HealthProjection
import it.neckar.open.health.ServiceState
import it.neckar.open.health.ServiceStateHolder
import it.neckar.open.kotlin.lang.requireNotNull
import it.neckar.rest.health.ReadinessResponse

/**
 * Configuration for [ReadinessGatingPlugin].
 */
class ReadinessGatingConfig {
  /**
   * The single source of truth for the service state. Required.
   */
  var stateHolder: ServiceStateHolder? = null
}

private val ReadinessGatingExemptAttribute: AttributeKey<Unit> = AttributeKey("ReadinessGatingExempt")

/**
 * Keeps exactly this route reachable in every service state; reserved for diagnosis. Routes below it stay gated.
 */
@IgnorableReturnValue
fun Route.exemptFromReadinessGating(): Route {
  attributes.put(ReadinessGatingExemptAttribute, Unit)
  return this
}

/**
 * App-side readiness gating: docs/infrastructure/health-monitoring-self-healing-concept.md, section
 * Gating, Option A. Installed inside `routing { }`, because only there the resolved route is known.
 */
val ReadinessGatingPlugin: RouteScopedPlugin<ReadinessGatingConfig> =
  createRouteScopedPlugin("ReadinessGating", ::ReadinessGatingConfig) {
    val stateHolder: ServiceStateHolder = pluginConfig.stateHolder.requireNotNull { "ReadinessGatingPlugin requires a stateHolder" }
    route.requireNotNull { "ReadinessGatingPlugin decides on the resolved route and must be installed inside routing { }" }

    onCall { call ->
      val resolvedRoute: Route = (call as RoutingPipelineCall).route
      if (resolvedRoute.attributes.contains(ReadinessGatingExemptAttribute)) {
        return@onCall
      }

      // One snapshot, so the 503 body shows the state the decision was made on.
      val state: ServiceState = stateHolder.state
      val dependencies: List<DependencyState> = stateHolder.dependencies
      when (HealthProjection.readinessStatus(state, dependencies)) {
        HealthHttpStatus.Ok -> Unit
        HealthHttpStatus.ServiceUnavailable -> call.respond<ReadinessResponse>(
          HealthHttpStatus.ServiceUnavailable.toHttpStatusCode(),
          HealthProjection.readinessResponse(state, dependencies),
        )
      }
    }
  }
