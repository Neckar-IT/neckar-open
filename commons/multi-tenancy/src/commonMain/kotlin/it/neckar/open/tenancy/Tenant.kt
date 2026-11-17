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
@file:UseSerializers(UuidSerializer::class)
package it.neckar.open.tenancy

import it.neckar.open.kotlin.serializers.NativeInstant
import it.neckar.uuid.UuidSerializer
import it.neckar.uuid.NativeUuid
import it.neckar.uuid.ValueClassUuid
import it.neckar.uuid.randomUuid4
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import kotlin.jvm.JvmInline
import kotlin.time.Instant
import kotlin.uuid.Uuid

/**
 * Represents a tenant in a multi-tenancy system.
 */
interface Tenant {
  /**
   * The unique identifier of the tenant.
   */
  val id: Id

  /**
   * The name of the tenant.
   */
  val name: String

  /**
   * The slug of the tenant.
   * A slug is a URL-friendly identifier for the tenant.
   *
   * The slug must be unique across all tenants and should not contain spaces or special characters.
   */
  val slug: Slug

  /**
   * When the tenant was created
   */
  val createdAt: NativeInstant

  /**
   * Returns a descriptor for the tenant.
   */
  fun descriptor(): TenantDescriptor {
    return TenantDescriptor(
      id = id,
      slug = slug,
    )
  }

  companion object {
    /**
     * Neckar IT tenant. Can be used, if the software does not have any tenants configured.
     */
    val NeckarIT: Tenant = DefaultTenant(
      id = Id(Uuid.parse("c23e03d9-3712-4365-a34e-641719270e16")),
      name = "Neckar IT GmbH",
      slug = Slug("Neckar-IT"),
      createdAt = Instant.fromEpochMilliseconds(1_567_987_200_000) // 2019-09-09T00:00:00Z
    )

    /**
     * Neckar IT E2e tenant. Is used for e2e screenshot tests, do not add or remove data to keep screenshot consistency.
     */
    val NeckarITe2e: Tenant = DefaultTenant(
      id = Id(Uuid.parse("3d11dd42-ae46-4a8b-afb3-8d0c51777d3e")),
      name = "Neckar IT GmbH E2E",
      slug = Slug("Neckar-IT-E2E"),
      createdAt = Instant.fromEpochMilliseconds(1_567_987_200_000) // 2019-09-09T00:00:00Z
    )
  }

  /**
   * The ID of the tenant.
   */
  @Serializable
  @JvmInline
  value class Id(
    override val uuid: NativeUuid,
  ) : ValueClassUuid {
    override fun toString(): String {
      return uuid.toHexDashString()
    }

    companion object {
      fun random(): Id = Id(randomUuid4())
    }
  }
}
