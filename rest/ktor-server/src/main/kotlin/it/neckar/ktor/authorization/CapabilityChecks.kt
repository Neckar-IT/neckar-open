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

import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.routing.RoutingContext
import io.ktor.util.AttributeKey
import io.ktor.util.reflect.typeInfo
import it.neckar.open.authorization.Capability
import it.neckar.open.authorization.MissingCapabilityException
import it.neckar.open.authorization.UserCapabilities

/**
 * Answers what the caller of the current request may do. Every product resolves that differently —
 * one asks its user store for the login name in the token, another reads the account it has already
 * loaded for the call — so each installs its own.
 *
 * A caller the product does not know grants [UserCapabilities.Empty] rather than failing: the
 * request is authenticated, only not entitled, and every gate below then answers 403.
 */
fun interface CallerCapabilities {
  suspend fun resolve(call: ApplicationCall): UserCapabilities
}

private val CallerCapabilitiesAttribute: AttributeKey<CallerCapabilities> =
  AttributeKey("caller-capabilities", typeInfo<Any>())

/**
 * Registers [resolver] as this service's source of capabilities. Call once during `configure()`;
 * a route below then names the capability it demands and nothing else.
 */
fun Application.installCapabilityChecks(resolver: CallerCapabilities) {
  attributes.put(CallerCapabilitiesAttribute, resolver)
}

/**
 * Verifies that the caller holds [capability]. [installStatusPages][it.neckar.ktor.status.installStatusPages]
 * turns the failure into a 403 whose body names the missing capability.
 */
suspend fun RoutingContext.requireCapability(capability: Capability) {
  call.requireCapability(capability)
}

/**
 * The [RoutingContext.requireCapability] of a call outside a routing handler — [authorize] guards a
 * route from a plugin, where only the [ApplicationCall] is at hand.
 */
suspend fun ApplicationCall.requireCapability(capability: Capability) {
  if (capability !in callerCapabilities()) {
    throw MissingCapabilityException(capability.key)
  }
}

/**
 * The capabilities of the caller, as the installed [CallerCapabilities] resolves them. Public for
 * the checks a product formulates itself — one capability out of several, or a set that depends on
 * the request; [requireCapability] covers the plain case.
 *
 * Fails when nothing was installed: a route that checks a capability against a service that never
 * registered a resolver would otherwise let every caller through.
 */
suspend fun RoutingContext.callerCapabilities(): UserCapabilities =
  call.callerCapabilities()

/**
 * The [RoutingContext.callerCapabilities] of a call outside a routing handler.
 */
suspend fun ApplicationCall.callerCapabilities(): UserCapabilities {
  val resolver = application.attributes.getOrNull(CallerCapabilitiesAttribute)
    ?: error("No CallerCapabilities installed — call installCapabilityChecks(…) in the service's configure()")

  return resolver.resolve(this)
}
