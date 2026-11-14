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
package it.neckar.open.tenancy

import it.neckar.open.collections.fastForEach
import it.neckar.uuid.ValueClassUuid
import kotlin.uuid.Uuid

/*
 * The requireBelongsToTenant family is the last line of defence against tenant leaks: a query that
 * returns a foreign entity (mis-built filter, cache miss) fails loudly instead of reaching the
 * caller. The tenant-scoped collection of the mongodb library calls one of them on every entity it
 * hands back and on every entity it inserts or replaces.
 */

/**
 * Verifies that the given [BelongsToTenant] has the expected tenant ID.
 *
 * Throws [InvalidTenantException] if the tenant ID does not match the expected tenant.
 */
@IgnorableReturnValue
fun <T : BelongsToTenant> T.requireBelongsToTenant(expectedTenantId: Tenant.Id): T {
  tenantId.requireSameTenant(expectedTenantId)
  return this
}

/**
 * Verifies that the given nullable [BelongsToTenant] has the expected tenant ID.
 *
 * A null return means "there was nothing to check" — the lookup found no entity. It never means the
 * check failed; a mismatch always throws [InvalidTenantException]. Callers must not translate null
 * into a tenant violation.
 */
@IgnorableReturnValue
inline fun <T : BelongsToTenant> T?.requireBelongsToTenantNullable(expectedTenantId: Tenant.Id): T? {
  return this?.requireBelongsToTenant(expectedTenantId)
}

/**
 * Verifies that the given [BelongsToTenant] belongs to the expected [Tenant].
 */
@IgnorableReturnValue
inline fun <T : BelongsToTenant> T.requireBelongsToTenant(expectedTenant: Tenant): T {
  return requireBelongsToTenant(expectedTenant.id)
}

@IgnorableReturnValue
inline fun <T : BelongsToTenant> T?.requireBelongsToTenantNullable(expectedTenant: Tenant): T? {
  return this?.requireBelongsToTenant(expectedTenant.id)
}

/**
 * Verifies that the given [BelongsToTenant] belongs to the tenant the [TenantDescriptor] points at.
 *
 * Only [TenantDescriptor.id] is compared: a [BelongsToTenant] carries nothing but the tenant ID, so
 * a descriptor with a matching ID and a stale slug still passes.
 */
@IgnorableReturnValue
inline fun <T : BelongsToTenant> T.requireBelongsToTenant(expectedTenant: TenantDescriptor): T {
  return requireBelongsToTenant(expectedTenant.id)
}

@IgnorableReturnValue
inline fun <T : BelongsToTenant> T?.requireBelongsToTenantNullable(expectedTenant: TenantDescriptor): T? {
  return this?.requireBelongsToTenant(expectedTenant.id)
}

/**
 * Verifies that all elements in the list have the expected tenant ID.
 *
 * Throws [InvalidTenantException] if any element does not match the expected tenant.
 */
@IgnorableReturnValue
fun <T : BelongsToTenant> List<T>.requireBelongsToTenant(expectedTenantId: Tenant.Id): List<T> {
  fastForEach { element ->
    element.requireBelongsToTenant(expectedTenantId)
  }
  return this
}

/**
 * Verifies that all elements in the list belong to the given [Tenant].
 *
 * Throws [InvalidTenantException] if any element does not match the expected tenant.
 */
@IgnorableReturnValue
inline fun <T : BelongsToTenant> List<T>.requireBelongsToTenant(expectedTenant: Tenant): List<T> {
  return requireBelongsToTenant(expectedTenant.id)
}

@IgnorableReturnValue
inline fun <T : BelongsToTenant> List<T>.requireBelongsToTenant(expectedTenant: TenantDescriptor): List<T> {
  return requireBelongsToTenant(expectedTenant.id)
}

/**
 * Creates a new UUID based on a source ID and a tenant ID.
 *
 * This function combines the most significant bits of the source ID's UUID
 * with the least significant bits of the tenant ID's UUID to generate a new UUID.
 *
 * Used for creating unique IDs based on fixed IDs in Examples and the tenant ID.
 *
 * @param sourceId The source ID, which provides the most significant bits for the new UUID.
 * @param tenantId The tenant ID, which provides the least significant bits for the new UUID.
 * @return A new UUID created by combining the source ID and tenant ID.
 */
@Deprecated("Please do not use this! This is a bad idea for generating IDs in production code.")
fun calculateIdBasedOnTenant(sourceId: ValueClassUuid, tenantId: Tenant.Id): Uuid {
  fun Uuid.getMostSignificantBits(): Long {
    return toLongs { most, _ -> most }
  }

  fun Uuid.getLeastSignificantBits(): Long {
    return toLongs { _, least -> least }
  }

  // Extract the bits from the source ID
  val sourceIdMostSignificantBits = sourceId.uuid.getMostSignificantBits()

  // Extract the least significant bits from the tenant ID to use for the new UUID
  val tenantLeastSignificantBits = tenantId.uuid.getLeastSignificantBits()

  return Uuid.fromLongs(
    mostSignificantBits = sourceIdMostSignificantBits,
    leastSignificantBits = tenantLeastSignificantBits
  )
}

@Deprecated("Please do not use this! This is a bad idea for generating IDs in production code.")
fun calculateIdBasedOnTenant(sourceId: Uuid, tenantId: Uuid): Uuid {
  fun Uuid.getMostSignificantBits(): Long {
    return toLongs { most, _ -> most }
  }

  fun Uuid.getLeastSignificantBits(): Long {
    return toLongs { _, least -> least }
  }

  // Extract the bits from the source ID
  val sourceIdMostSignificantBits = sourceId.getMostSignificantBits()

  // Extract the least significant bits from the tenant ID to use for the new UUID
  val tenantLeastSignificantBits = tenantId.getLeastSignificantBits()

  return Uuid.fromLongs(
    mostSignificantBits = sourceIdMostSignificantBits,
    leastSignificantBits = tenantLeastSignificantBits
  )
}

/**
 * Throws [InvalidTenantException] unless this tenant ID is [expectedTenantId]; every `requireBelongsToTenant` overload compares through it.
 */
fun Tenant.Id.requireSameTenant(expectedTenantId: Tenant.Id) {
  if (this != expectedTenantId) {
    throw InvalidTenantException(
      expected = expectedTenantId,
      actual = this,
    )
  }
}
