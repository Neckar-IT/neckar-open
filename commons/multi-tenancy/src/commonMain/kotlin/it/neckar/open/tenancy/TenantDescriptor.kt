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

import it.neckar.uuid.UuidSerializer
import it.neckar.uuid.decodeFromUrl
import it.neckar.uuid.encodeForUrl
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import kotlin.uuid.Uuid

/**
 * Represents a minimal descriptor of a tenant.
 * Consisting only of the tenant's ID and the slug.
 */
@Serializable
data class TenantDescriptor(
  /**
   * The unique identifier of the tenant.
   */
  val id: Tenant.Id,

  /**
   * The slug of the tenant.
   */
  val slug: Slug,
) {
  /**
   * Returns a string representation of the tenant descriptor that [parse] reads back.
   * Is used for serialization in JWT tokens.
   *
   * Format: `<slug><SeparatorCharacter><id as URL-safe Base64>`, e.g. `the-tenant-slug@obRWeeDTQr6ft_1AzBK6Dg`.
   * The ID is URL-safe Base64 (22 characters), not the 36-character hex-dash form [Tenant.Id.toString] returns.
   */
  fun asString(): String {
    return "${slug.value}$SeparatorCharacter${id.uuid.encodeForUrl()}"
  }

  override fun toString(): String {
    return asString()
  }

  companion object {
    /**
     * The character separating the slug from the ID in the string representation of a tenant descriptor.
     *
     * `@` splits unambiguously because neither side can contain it: [Slug] permits only letters, digits
     * and hyphens, and URL-safe Base64 only adds `_`. Any replacement must keep that property.
     */
    const val SeparatorCharacter: Char = '@'

    /**
     * Parses a string representation of a tenant descriptor as produced by [asString].
     *
     * Throws [IllegalArgumentException] for anything else — a missing or repeated
     * [SeparatorCharacter], or a slug violating the [Slug] constraints.
     */
    fun parse(stringRepresentation: String): TenantDescriptor {
      val parts = stringRepresentation.split(SeparatorCharacter)
      require(parts.size == 2) { "Invalid tenant descriptor format: [$stringRepresentation]" }

      val slug = Slug(parts[0])
      val id = Tenant.Id(Uuid.decodeFromUrl(parts[1]))

      return TenantDescriptor(id, slug)
    }

    /**
     * The default tenant descriptor for Neckar IT.
     * Is used, if there are no tenants configured.
     */
    val NeckarIT: TenantDescriptor = Tenant.NeckarIT.descriptor()
  }
}
