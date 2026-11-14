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
package it.neckar.ktor.client

import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.request.*
import io.ktor.http.*
import it.neckar.rest.jwt.JwtToken
import it.neckar.rest.jwt.JwtTokenPair
import it.neckar.rest.jwt.appendJwtTokenBearer


/**
 * Applies the bearer token from the provided JwsToken
 */
fun HttpMessageBuilder.bearerAuth(token: JwtToken) {
  bearerAuth(token.token)
}

/**
 * Converts a JWS token pair to Ktor BearerTokens
 */
fun JwtTokenPair?.toBearerTokens(): BearerTokens? {
  if (this == null) {
    return null
  }
  return BearerTokens(
    accessToken = this.accessToken.token,
    refreshToken = this.refreshToken.token,
  )
}

/**
 * Appends the JWT token as a header.
 *
 * Attention: Use the `io.ktor.server.auth.Authentication` plugin if possible!
 */
fun HttpRequestBuilder.appendJwtTokenBearer(jwtToken: JwtToken) {
  headers {
    appendJwtTokenBearer(jwtToken)
  }
}

/**
 * Appends the JWT access token from the provided pair as a header.
 */
fun HttpRequestBuilder.appendJwtTokenBearer(jwtTokenPair: JwtTokenPair) {
  appendJwtTokenBearer(jwtTokenPair.accessToken)
}
