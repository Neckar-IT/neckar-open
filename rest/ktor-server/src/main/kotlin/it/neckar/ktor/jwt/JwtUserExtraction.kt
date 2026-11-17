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

import com.auth0.jwt.interfaces.Payload
import io.ktor.server.auth.jwt.JWTPrincipal
import it.neckar.contact.EmailAddress
import it.neckar.user.User
import java.util.UUID
import kotlin.uuid.Uuid

/**
 * The [User] a token stands for, for a service that keeps no user record of its own.
 *
 * Both names and the mail address are mandatory: these services put a person's name in front of
 * other people and have no second source to fall back on. A service with a user record answers a
 * missing name with the empty string instead (`UserEntity.toDetails`).
 *
 * @throws IncompleteIdentityException for a token that cannot answer for its bearer
 */
fun JWTPrincipal.extractUser(): User {
  val payload: Payload = payload
  val loginName = loginName()

  val userId: User.Id = when (payload) {
    is KeycloakPayloadAdapter -> {
      val keycloakUserId = payload.keycloakUserId
        ?: throw IncompleteIdentityException.ClaimAbsent(KeycloakClaim.Subject, payload.subject, payload.claims.keys.toList())
      User.Id(Uuid.parse(keycloakUserId))
    }

    // An HMAC test token carries the login name as its subject, so the id is derived from it and
    // stays the same across runs.
    else -> User.Id(Uuid.parse(UUID.nameUUIDFromBytes(loginName.value.toByteArray()).toString()))
  }

  return User(
    id = userId,
    loginName = loginName,
    firstName = payload.getClaimValue(KeycloakClaim.GivenName),
    lastName = payload.getClaimValue(KeycloakClaim.FamilyName),
    email = EmailAddress(payload.getClaimValue(KeycloakClaim.Email)),
    environment = emptySet(),
  )
}
