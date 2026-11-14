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

import io.ktor.server.auth.authenticate
import io.ktor.server.routing.Route
import it.neckar.ktor.jwt.AuthProviderName

/**
 * Whether a group of routes requires authentication, and if so through which provider.
 *
 * Scope-neutral: applies to any [Route] subtree, not just a particular feature's endpoints. Deny by
 * default is expressed by handing [Required] with the appropriate provider; [None] leaves the routes
 * public.
 */
sealed interface AuthenticationRequirement {
  /**
   * No authentication — the routes are public.
   */
  data object None : AuthenticationRequirement

  /**
   * The routes are wrapped in `authenticate([provider])` — a request authenticated by that provider
   * is required.
   */
  data class Required(val provider: AuthProviderName) : AuthenticationRequirement
}

/**
 * Applies the [requirement] to the routes defined in [build].
 *
 * [AuthenticationRequirement.Required] wraps them in `authenticate(provider)`;
 * [AuthenticationRequirement.None] registers them without any authentication.
 */
fun Route.authenticateIfRequired(
  requirement: AuthenticationRequirement,
  build: Route.() -> Unit,
) {
  when (requirement) {
    AuthenticationRequirement.None -> build()
    is AuthenticationRequirement.Required -> authenticate(requirement.provider.value) { build() }
  }
}
