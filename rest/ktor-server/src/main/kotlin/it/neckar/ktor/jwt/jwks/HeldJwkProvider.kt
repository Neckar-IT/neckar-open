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

import com.auth0.jwk.Jwk
import com.auth0.jwk.JwkProvider
import com.auth0.jwk.SigningKeyNotFoundException

/**
 * The [JwkProvider] the Ktor verifier is handed: a lookup in the set [jwksKeyManager] holds, and
 * nothing else.
 *
 * It replaces `JwkProviderBuilder(url).cached(…).rateLimited(…)`, which fetched from inside the
 * request — the first request after a restart waited on Keycloak, and a token with an unknown `kid`
 * turned foreign traffic into outgoing network calls. Fetching lives in the manager's background job
 * now; what is left here cannot block and cannot fail for any reason but a missing key.
 */
class HeldJwkProvider(
  private val jwksKeyManager: JwksKeyManager,
) : JwkProvider {

  override fun get(keyId: String?): Jwk {
    val requested = keyId?.takeIf { it.isNotBlank() }
      ?: throw SigningKeyNotFoundException("Token carries no 'kid'; ${jwksKeyManager.describeHeldSet()}", null)

    return jwksKeyManager.keyOf(JwkKeyId(requested))
      ?: throw SigningKeyNotFoundException("No signing key '$requested' in ${jwksKeyManager.describeHeldSet()}", null)
  }
}
