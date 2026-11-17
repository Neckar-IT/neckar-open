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
package it.neckar.ktor.mcp

import it.neckar.ktor.jwt.AuthProviderName
import it.neckar.open.authorization.Capability
import it.neckar.open.http.Url

/**
 * Whether [McpRouting.Path] requires an access token.
 */
sealed interface McpAuthentication {
  /**
   * Every client reaches the tools without a token.
   */
  data object None : McpAuthentication

  /**
   * `POST` [McpRouting.Path] requires a token that one of [providers] accepts; its `401` names the [ProtectedResourceMetadata],
   * which names the [authorizationServers] (RFC 9728).
   */
  data class OAuth(
    val providers: List<AuthProviderName>,
    /**
     * The issuers the client obtains its token from, e.g. `https://auth.neckar.it/realms/internal`.
     * Empty when the service issues its own tokens outside OAuth.
     */
    val authorizationServers: List<Url.Absolute>,
    /**
     * The scopes a client requests, announced as `scopes_supported` and in the `401` challenge; without them
     * a client such as Claude requests every scope of the authorization server, and Keycloak refuses those the client lacks.
     */
    val scopes: List<OAuthScope>,
    /**
     * Guards `POST` [McpRouting.Path]; the service installs [installCapabilityChecks][it.neckar.ktor.authorization.installCapabilityChecks].
     * `null` lets every token the [providers] accept reach the tools.
     */
    val capability: Capability?,
  ) : McpAuthentication
}
