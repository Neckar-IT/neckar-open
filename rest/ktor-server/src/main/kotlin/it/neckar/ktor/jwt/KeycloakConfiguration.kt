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
package it.neckar.ktor.jwt

import it.neckar.open.http.Url
import it.neckar.rest.jwt.JwtAudience

/**
 * A client of a Keycloak realm: the issuer and JWKS a service verifies tokens against, and the endpoints of the authorization code
 * flow.
 */
data class KeycloakConfiguration(
  /**
   * The Keycloak realm URL (e.g., `https://auth.neckar.it/realms/internal`).
   * [issuer] and every endpoint derive from it.
   */
  val realmUrl: Url.Absolute,

  /**
   * The Keycloak client ID for this service.
   * Must match the `aud` claim in the Keycloak JWT.
   */
  val clientId: JwtAudience,
) {
  /** [realmUrl] without a trailing slash: issuer, endpoints and realm name derive from it, so a configured slash changes none of them. */
  private val realmUrlWithoutTrailingSlash: String = realmUrl.value.trimEnd('/')

  init {
    // The value comes from an option or a configuration file, and `Url.Absolute` checks only the scheme. Anything
    // past the realm path would become part of the realm name, which names the seed resource.
    require(realmUrlWithoutTrailingSlash.matches(RealmUrlPattern)) {
      "Keycloak realm URL must read <scheme>://<host>/…/realms/<realm> but was '${realmUrl.value}'"
    }
  }

  /**
   * The issuer URL — Keycloak sets `iss` to the realm URL.
   */
  val issuer: String = realmUrlWithoutTrailingSlash

  /**
   * The JWKS endpoint URL — used to fetch Keycloak's public signing keys.
   */
  val jwksUrl: Url.Absolute = Url.Absolute("$realmUrlWithoutTrailingSlash/protocol/openid-connect/certs")

  /** Where the browser signs in: the start of the authorization code flow. */
  val authorizationEndpoint: Url.Absolute = Url.Absolute("$realmUrlWithoutTrailingSlash/protocol/openid-connect/auth")

  /** Redeems an authorization code or a refresh token. */
  val tokenEndpoint: Url.Absolute = Url.Absolute("$realmUrlWithoutTrailingSlash/protocol/openid-connect/token")

  /** Ends the Keycloak session of a refresh token. */
  val logoutEndpoint: Url.Absolute = Url.Absolute("$realmUrlWithoutTrailingSlash/protocol/openid-connect/logout")

  /**
   * The last segment of [realmUrl], e.g. `internal` or `customers`. A service names its seed resource and background job by it.
   */
  val realmName: String = realmUrlWithoutTrailingSlash.substringAfterLast('/')

  companion object {
    /**
     * What a Keycloak realm URL looks like: a scheme, a host, and a path whose last segment is the
     * realm. No query, no fragment — both would end up inside [realmName]. Case-insensitive, because
     * a scheme is (RFC 3986) and `Url.Absolute` accepts one written either way.
     */
    private val RealmUrlPattern: Regex =
      Regex("""https?://[^/?#]+(/[^/?#]+)*/realms/[^/?#]+""", RegexOption.IGNORE_CASE)
  }
}
