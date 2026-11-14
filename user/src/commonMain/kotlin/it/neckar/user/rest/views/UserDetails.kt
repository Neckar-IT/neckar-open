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
package it.neckar.user.rest.views

import it.neckar.contact.EmailAddress
import it.neckar.open.annotations.DataView
import it.neckar.open.authorization.RoleLabel
import it.neckar.open.authorization.UserCapabilities
import it.neckar.open.http.Url
import it.neckar.open.tenancy.Tenant
import it.neckar.user.User
import it.neckar.user.UserEnvironment
import it.neckar.user.UserLoginName
import kotlinx.serialization.Serializable

/**
 * REST projection of a [User].
 *
 * Carries identity, the editable fields, and what the user is allowed to do. The [User] domain
 * class never crosses the REST boundary — this projection is the wire-format representation.
 *
 * No property carries a default: the services serialize with `encodeDefaults = false`, so a default
 * would drop its field from the JSON for exactly the user whose value equals it — the one without
 * environments, without capabilities, without a tenant. A frontend would then read `undefined`
 * where the type promises a list. The defaults belong on [toDetails] instead, where a product that
 * has none of this simply omits the argument.
 */
@DataView("Full user details for detail/edit views and 'me' responses")
@Serializable
data class UserDetails(
  val id: User.Id,
  val loginName: UserLoginName,
  val firstName: String,
  val lastName: String,
  val email: EmailAddress?,
  val avatarUrl: Url?,
  /**
   * The parts of the product the user works in. Empty where a product has no such split.
   */
  val environment: Set<UserEnvironment>,

  /**
   * The tenant the user acts for, or null in a product without tenants.
   *
   * Sent by the server so a frontend never derives it from the token or compares it against a
   * hardcoded id.
   */
  val tenantId: Tenant.Id?,

  /**
   * What the user may do — the same set the endpoints check, so a hidden button and a rejected
   * request cannot disagree. Empty in a product that grants no capabilities; a frontend gating on
   * a capability then simply finds none.
   */
  val capabilities: UserCapabilities,

  /**
   * Human-readable label of the user's role within their tenant ("owner", "Buchhalter Pro"), for
   * display only — every authorization decision reads [capabilities]. Null in a product without
   * named roles.
   */
  val roleLabel: RoleLabel?,
)

/**
 * Converts a [User] domain object to a [UserDetails] for REST output.
 *
 * [tenantId], [capabilities] and [roleLabel] live outside the [User] domain class — in the user
 * document of the product — and are therefore passed in. A product that has none of them leaves
 * them at their defaults.
 */
fun User.toDetails(
  tenantId: Tenant.Id? = null,
  capabilities: UserCapabilities = UserCapabilities.Empty,
  roleLabel: RoleLabel? = null,
): UserDetails = UserDetails(
  id = id,
  loginName = loginName,
  firstName = firstName,
  lastName = lastName,
  email = email,
  avatarUrl = avatarUrl,
  environment = environment,
  tenantId = tenantId,
  capabilities = capabilities,
  roleLabel = roleLabel,
)
