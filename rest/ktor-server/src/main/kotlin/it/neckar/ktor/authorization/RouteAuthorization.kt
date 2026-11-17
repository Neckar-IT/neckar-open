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
package it.neckar.ktor.authorization

import io.ktor.server.application.createRouteScopedPlugin
import io.ktor.server.application.install
import io.ktor.server.application.isHandled
import io.ktor.server.auth.AuthenticationChecked
import io.ktor.server.routing.Route
import io.ktor.server.routing.RouteSelector
import io.ktor.server.routing.RouteSelectorEvaluation
import io.ktor.server.routing.RoutingResolveContext
import it.neckar.ktor.TransparentRouteSelector
import it.neckar.open.authorization.Capability

/**
 * Guards the routes defined in [build] with [capability]: a caller who lacks it gets a 403 naming
 * the missing capability, and no handler below runs.
 *
 * The requirement belongs to the route, not to the handler body — that way it shows up in the route
 * tree and a handler cannot silently omit it.
 *
 * ```
 * authorize(CoreCapability.DeleteData) {
 *   delete { … }
 * }
 * ```
 */
fun Route.authorize(capability: Capability, build: Route.() -> Unit): Route {
  val guarded = createChild(CapabilityRouteSelector(capability))
  guarded.install(AuthorizationPlugin) { this.capability = capability }
  guarded.build()
  return guarded
}

private class AuthorizationConfig {
  lateinit var capability: Capability
}

/**
 * Runs once the call is authenticated and before the handlers of the guarded route — the installed
 * [CallerCapabilities] resolves the caller from the token, which only exists after authentication.
 */
private val AuthorizationPlugin = createRouteScopedPlugin("Authorization", ::AuthorizationConfig) {
  val capability = pluginConfig.capability

  on(AuthenticationChecked) { call ->
    // A call the authentication already answered stays a 401 — missing credentials are not a
    // missing capability.
    if (call.isHandled) return@on

    call.requireCapability(capability)
  }
}

/**
 * Contributes no path segment — the guard nests inside the route it protects. Public so a test reads
 * the [capability] of a routing node from the route tree; [authorize] creates it.
 */
class CapabilityRouteSelector internal constructor(val capability: Capability) : RouteSelector(), TransparentRouteSelector {
  override suspend fun evaluate(context: RoutingResolveContext, segmentIndex: Int): RouteSelectorEvaluation =
    RouteSelectorEvaluation.Transparent

  override fun toString(): String = "(capability ${capability.key.value})"
}
