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
package it.neckar.ktor.jwt.jwks

import com.auth0.jwt.interfaces.DecodedJWT
import it.neckar.open.time.now
import it.neckar.rest.jwt.JwtAudience

/**
 * Decides whether a token with an unknown `kid` is worth asking the realm about.
 *
 * **This grants nothing.** Every claim it reads comes from an unverified token, and the token is
 * rejected either way. All it decides is whether a fetch would plausibly pay off, so that a random
 * `kid` from a stranger does not reach the realm at all.
 *
 * The verifier of the caller checks the signature afterwards, against the fetched key.
 */
class JwksRefreshPrefilter(
  /**
   * The `iss` a token of this realm carries — the realm URL.
   */
  val issuer: String,
  /**
   * The `aud` values of the clients this service serves, one of which a token carries.
   */
  val audiences: Set<JwtAudience>,
  /**
   * The signature algorithm the realm signs with. Keycloak's `rsa-generated` provider signs `RS256`.
   */
  val algorithm: String = DefaultAlgorithm,
) {
  init {
    require(audiences.isNotEmpty()) { "A prefilter without audience refreshes for no token of realm [$issuer]" }
  }

  constructor(issuer: String, audience: String, algorithm: String = DefaultAlgorithm) : this(issuer, setOf(JwtAudience(audience)), algorithm)

  /**
   * Whether [token] looks like one of ours: our realm, one of our clients, our algorithm, and not expired.
   */
  fun worthARefresh(token: DecodedJWT): Boolean {
    if (token.issuer != issuer) {
      return false
    }
    if (token.audience.orEmpty().none { JwtAudience(it) in audiences }) {
      return false
    }
    if (token.algorithm != algorithm) {
      return false
    }

    val expiresAt = token.expiresAtAsInstant ?: return false
    return expiresAt.toEpochMilli() > now().toEpochMilliseconds()
  }

  companion object {
    const val DefaultAlgorithm: String = "RS256"
  }
}
