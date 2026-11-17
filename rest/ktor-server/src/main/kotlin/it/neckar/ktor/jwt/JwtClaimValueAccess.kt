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

import com.auth0.jwt.interfaces.Claim
import com.auth0.jwt.interfaces.Payload

/**
 * The value of a claim the caller cannot work without — an identity a service is built on, not an
 * optional detail. Whitespace around the value belongs to the token, not to what it carries, so it
 * is trimmed.
 *
 * @throws IncompleteIdentityException in the case that applies, so the log line says whether to go
 * to the client's scopes, its protocol mappers or the user's profile
 */
fun Payload.getClaimValue(claimName: String): String {
  val claim: Claim? = getClaim(claimName)
  if (claim == null || claim.isMissing) {
    throw IncompleteIdentityException.ClaimAbsent(claimName, subject, claims.keys.toList())
  }
  if (claim.isNull) {
    throw IncompleteIdentityException.ClaimBlank(claimName, subject)
  }

  return claim.trimmedValue(claimName, subject)
    .ifBlank { throw IncompleteIdentityException.ClaimBlank(claimName, subject) }
}

/**
 * The value of a claim that may legitimately be absent — an optional detail, not an identity.
 *
 * Null covers the three ways of not being there: no claim, a JSON null, blank text. A claim that is
 * present but holds a number, a list or an object still throws
 * [IncompleteIdentityException.ClaimNotAString]: nobody configured that on purpose, and answering
 * null would file a broken protocol mapper under "the user did not fill this in".
 */
fun Payload.getClaimValueOrNull(claimName: String): String? {
  val claim: Claim = getClaim(claimName) ?: return null
  if (claim.isMissing || claim.isNull) return null

  return claim.trimmedValue(claimName, subject).takeIf { it.isNotBlank() }
}

/**
 * `asString()` answers null both for a claim that is not there and for one holding a non-textual
 * value, so it is only ever read once the first case has been ruled out.
 */
private fun Claim.trimmedValue(claimName: String, subject: String?): String {
  return asString()?.trim() ?: throw IncompleteIdentityException.ClaimNotAString(claimName, subject)
}
