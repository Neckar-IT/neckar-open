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

/**
 * A token that cannot answer for its bearer, thrown by [getClaimValue] and [getClaimValueOrNull].
 *
 * Each case names where the repair belongs, because they are three different places: the client's
 * scopes, the client's protocol mappers, and the user's own profile. Telling them apart is the
 * whole point — a single "claim missing" leaves whoever reads the log to guess between them.
 *
 * Never carries a claim value, only claim names: the message ends up in logs.
 *
 * @param subject whose token it is, as the token itself states it — `preferred_username` behind a
 * [KeycloakPayloadAdapter], the raw `sub` otherwise
 */
sealed class IncompleteIdentityException(
  val claimName: String,
  val subject: String?,
  message: String,
) : Exception(message) {

  /**
   * The token does not carry the claim at all: the client lacks the scope or the protocol mapper
   * that would add it. Everyone signing in through that client is affected.
   *
   * @param claimsInToken the claim names that did arrive, so the answer to "which scope is
   * missing" is in the log line itself
   */
  class ClaimAbsent(
    claimName: String,
    subject: String?,
    val claimsInToken: List<String>,
  ) : IncompleteIdentityException(
    claimName = claimName,
    subject = subject,
    message = "Token of [$subject] carries no claim [$claimName]. Its claims are ${claimsInToken.sorted()} — " +
      "the client is missing the scope or protocol mapper that adds [$claimName].",
  )

  /**
   * The claim arrived empty — blank text or a JSON null: this one account has nothing in that
   * field of its profile. Everyone else signing in through the same client is fine.
   */
  class ClaimBlank(
    claimName: String,
    subject: String?,
  ) : IncompleteIdentityException(
    claimName = claimName,
    subject = subject,
    message = "Token of [$subject] carries claim [$claimName] empty — that field of their profile has no value.",
  )

  /**
   * The claim carries something other than text — a number, a list, an object. The protocol mapper
   * behind it produces the wrong shape, which no edit to a profile changes.
   */
  class ClaimNotAString(
    claimName: String,
    subject: String?,
  ) : IncompleteIdentityException(
    claimName = claimName,
    subject = subject,
    message = "Token of [$subject] carries claim [$claimName] as something other than a string — " +
      "the protocol mapper behind it produces the wrong shape.",
  )
}
