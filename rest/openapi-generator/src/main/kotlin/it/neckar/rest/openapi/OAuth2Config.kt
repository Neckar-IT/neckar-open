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
package it.neckar.rest.openapi

import it.neckar.open.http.Url

/**
 * OAuth2 Authorization Code configuration for OpenAPI specs and Scalar UI.
 *
 * When provided to [OpenApiMetaData], the OpenAPI spec includes an `oauth2` security
 * scheme with an `authorizationCode` flow, and the Scalar UI can use it for
 * interactive authentication.
 */
data class OAuth2Config(
  /**
   * The Keycloak (or other IdP) authorization endpoint.
   * For Keycloak: `{realmUrl}/protocol/openid-connect/auth`
   */
  val authorizationUrl: Url.Absolute,

  /**
   * The token endpoint.
   * For Keycloak: `{realmUrl}/protocol/openid-connect/token`
   */
  val tokenUrl: Url.Absolute,

  /**
   * The OAuth2 client ID. Must be a public client with Authorization Code + PKCE enabled.
   */
  val clientId: String,

  /**
   * Requested scopes.
   */
  val scopes: Map<String, String> = defaultScopes,
) {
  companion object {
    val defaultScopes: Map<String, String> = mapOf(
      "openid" to "OpenID Connect",
      "profile" to "User profile",
      "email" to "Email address",
    )

    /**
     * Creates an [OAuth2Config] from a Keycloak realm URL and client ID.
     */
    fun keycloak(realmUrl: Url.Absolute, clientId: String): OAuth2Config {
      return OAuth2Config(
        authorizationUrl = realmUrl + "protocol/openid-connect/auth",
        tokenUrl = realmUrl + "protocol/openid-connect/token",
        clientId = clientId,
      )
    }
  }
}
