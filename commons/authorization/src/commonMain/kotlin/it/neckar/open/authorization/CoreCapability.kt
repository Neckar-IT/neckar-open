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
package it.neckar.open.authorization

/**
 * Capabilities shared by every product. Products may reference these in their
 * own [Role] declarations and endpoint checks.
 */
object CoreCapability {
  /**
   * Read access to the tenant's business data.
   */
  data object ReadData : Capability {
    override val key: CapabilityKey = CapabilityKey("core:read-data")
  }

  /**
   * Write access (create/update) to the tenant's business data.
   */
  data object WriteData : Capability {
    override val key: CapabilityKey = CapabilityKey("core:write-data")
  }

  /**
   * Delete the tenant's business data irrecoverably.
   *
   * Separate from [WriteData] because deleting is not one more edit: an edit leaves a record that
   * can be corrected, a deletion leaves nothing. Archiving therefore stays with [WriteData] — it
   * hides a record and keeps it.
   */
  data object DeleteData : Capability {
    override val key: CapabilityKey = CapabilityKey("core:delete-data")
  }

  /**
   * Invite, remove, or modify members of the tenant.
   */
  data object ManageMembers : Capability {
    override val key: CapabilityKey = CapabilityKey("core:manage-members")
  }

  /**
   * Lifecycle operations on the tenant itself (e.g. rename, delete).
   */
  data object ManageTenant : Capability {
    override val key: CapabilityKey = CapabilityKey("core:manage-tenant")
  }

  /**
   * Operating the product across all tenants — listing, creating and deleting tenants, seeding
   * and clearing their data.
   *
   * Crosses the tenant boundary like [ImpersonateUsers]: the four above answer "what may this user
   * do inside their own tenant", this one answers "may this user act on every tenant".
   * It therefore belongs in no [Role] that a user can reach on their own — neither self-signup
   * (which grants the product's owner role) nor an invitation nor a role change may hand it out.
   *
   * Deliberately not named "admin": inside a tenant that word already denotes a role one step
   * below the owner. Operating the product is not one more step of privilege inside a tenant.
   */
  data object OperateProduct : Capability {
    override val key: CapabilityKey = CapabilityKey("core:operate-product")
  }

  /**
   * Act as any registered user of any tenant, one request at a time, by naming them in the
   * `NeckarIT-Impersonate` header.
   *
   * Separate from [OperateProduct]: seeing the product through a customer's account is a support
   * task that must not bring the right to delete tenants along. Like [OperateProduct] it belongs in
   * no [Role] a user can reach on their own.
   */
  data object ImpersonateUsers : Capability {
    override val key: CapabilityKey = CapabilityKey("core:impersonate-users")
  }
}
