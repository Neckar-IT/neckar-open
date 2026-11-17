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
import it.neckar.open.tenancy.Tenant
import it.neckar.open.tenancy.TenantName
import it.neckar.user.User
import kotlinx.serialization.Serializable

@DataView("A user the caller may impersonate, for the user picker")
@Serializable
data class ImpersonationCandidate(
  /** The value the frontend sends in the `NeckarIT-Impersonate` header. */
  val userId: User.Id,
  val email: EmailAddress,
  val firstName: String,
  val lastName: String,
  val tenantId: Tenant.Id,
  /**
   * Null for a user whose tenant no longer exists or is archived: a product that deletes tenants, as
   * the Pattern Service does, leaves their users behind.
   */
  val tenantName: TenantName?,
)
