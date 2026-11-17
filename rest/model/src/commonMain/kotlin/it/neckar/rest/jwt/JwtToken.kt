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

import io.ktor.http.*
import io.ktor.util.*
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

/**
 * Represents a JWT token.
 * This class should only be used on the frontend or in common code.
 *
 * On the backend, use `io.ktor.server.auth.jwt.JWTPrincipal` instead.
 */
@Serializable
@JvmInline
value class JwtToken(val token: String) {
  init {
    require(token.isNotBlank()) { "Token must not be blank" }
    require(token.contains("Bearer").not()) {
      "Token must not contain the Bearer prefix. Probably adding the complete auth header?"
    }
  }

  override fun toString(): String {
    return token
  }

  /**
   * Parses a JWT token payload.
   * The Token consists of three parts: Header.Payload.Signature
   */
  fun extractJwtPayload(): String {
    val parts = token.split('.')
    require(parts.size == 3) { "Invalid JWT token format: expected 3 parts (header.payload.signature), got [${token}]" }

    return parts[1].decodeBase64String()
  }
}
