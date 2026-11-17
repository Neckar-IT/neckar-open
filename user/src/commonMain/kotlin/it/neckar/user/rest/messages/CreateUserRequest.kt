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
package it.neckar.user.rest.messages

import it.neckar.contact.EmailAddress
import it.neckar.open.http.Url
import it.neckar.open.serialization.TrimmedString
import it.neckar.user.User
import it.neckar.user.UserEnvironment
import it.neckar.user.UserLoginName
import it.neckar.user.UserPassword
import kotlinx.serialization.Serializable

/**
 * REST Request DTO for `POST /auth/users` — create a new user.
 *
 * Carries the user-editable fields, the password and the [User.Id] the caller generates, so the response names the same id.
 *
 * Free-text fields are typed as [TrimmedString] so trimming happens transparently on REST decode.
 */
@Serializable
data class CreateUserRequest(
  val id: User.Id,
  val loginName: UserLoginName,
  val firstName: TrimmedString,
  val lastName: TrimmedString,
  val email: EmailAddress?,
  val avatarUrl: Url? = null,
  val environment: Set<UserEnvironment>,
  val password: UserPassword,
) {
  /**
   * Returns the [User] domain object for the new user.
   * Identity (id, loginName) and editable fields are taken from the request;
   * the [password] is consumed by the auth backend separately.
   */
  fun toDomain(): User = User(
    id = id,
    loginName = loginName,
    firstName = firstName,
    lastName = lastName,
    email = email,
    avatarUrl = avatarUrl,
    environment = environment,
  )
}
