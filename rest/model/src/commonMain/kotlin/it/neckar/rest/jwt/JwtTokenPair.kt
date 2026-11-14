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

import kotlinx.serialization.Serializable

/**
 * Represents a pair of JWT tokens
 */
@Serializable
data class JwtTokenPair(
  /**
   * The access token - that has a limited validity date.
   * Must be used to access the data
   */
  val accessToken: JwtToken,
  /**
   * The refresh token that must only be used once to fetch a new token pair
   */
  val refreshToken: JwtToken,
) {
  companion object {
    /**
     * Example object
     */
    val example: JwtTokenPair = JwtTokenPair(
      JwtToken("_the_content_of_the_accessToken"),
      JwtToken("_the_content_of_the_refreshToken")
    )
  }
}
