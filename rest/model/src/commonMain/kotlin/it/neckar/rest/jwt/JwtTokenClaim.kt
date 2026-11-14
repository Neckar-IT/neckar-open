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
package it.neckar.rest.jwt

import it.neckar.open.tenancy.TenantDescriptor
import kotlin.uuid.Uuid


/**
 * Represents a type safe Jwt token claim
 */
class JwtTokenClaim<T>(
  val key: JwtClaimKey,

  val serializer: ClaimSerializer<T>,
) {

  constructor(
    key: JwtClaimKey,
    serialize: (T) -> String,
    deserialize: (String) -> T,
  ) : this(key, ClaimSerializer<T>(serialize, deserialize))

  /**
   * Serializes the given value to a string that is added to the token payload
   */
  fun serialize(value: T): String {
    return serializer.serialize(value)
  }

  /**
   * Deserializes the claim
   */
  fun deserialize(claimValue: String): T {
    return serializer.deserialize(claimValue)
  }

  companion object {
    /**
     * Key for the usage type of the token
     */
    val TokenUsageType: JwtTokenClaim<JwtTokenUsageType> = JwtTokenClaim(
      key = JwtClaimKey("tu"),
      serialize = { it.name },
      deserialize = { JwtTokenUsageType.valueOf(it) }
    )

    /**
     * Key for the refresh token chain ID
     */
    val RefreshTokenChainId: JwtTokenClaim<RefreshTokenChainId> = JwtTokenClaim(
      key = JwtClaimKey("rtc"),
      serialize = { it.uuid.toString() },
      deserialize = { RefreshTokenChainId(Uuid.parse(it)) })

    /**
     * Key for the tenant descriptor
     */
    val TenantDescriptor: JwtTokenClaim<TenantDescriptor> = JwtTokenClaim(
      key = JwtClaimKey("td"),
      serialize = { it.toString() },
      deserialize = { string -> it.neckar.open.tenancy.TenantDescriptor.parse(string) }
    )
  }
}

