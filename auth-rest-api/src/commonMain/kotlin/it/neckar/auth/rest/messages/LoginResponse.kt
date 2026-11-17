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
package it.neckar.auth.rest.messages

import it.neckar.rest.RestResponse
import it.neckar.rest.jwt.JwtTokenPair
import it.neckar.user.UserLoginName
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Response for a login request
 */
@Serializable
sealed interface LoginResponse : RestResponse<LoginResponseData> {

  @Serializable
  @SerialName("good")
  data class Good(override val data: LoginResponseData) : LoginResponse, RestResponse.GoodResponse<LoginResponseData>

  @Serializable
  @SerialName("bad/InvalidCredentials")
  data object InvalidCredentials : LoginResponse, RestResponse.BadResponse<LoginResponseData>

  /**
   * The user exists but has been disabled. Sent independent of the password.
   */
  @Serializable
  @SerialName("bad/UserDisabled")
  data object UserDisabled : LoginResponse, RestResponse.BadResponse<LoginResponseData>

  companion object {
    /**
     * Helper method to create a success object - with the correct type
     */
    fun good(data: LoginResponseData): LoginResponse = Good(data)

    /**
     * Helper method to create a failure object - with the correct type
     */
    fun invalidCredentials(): LoginResponse = InvalidCredentials

    /**
     * Helper method to create a failure object for a disabled user - with the correct type
     */
    fun userDisabled(): LoginResponse = UserDisabled
  }
}

/**
 * The data for a login response
 */
@Serializable
data class LoginResponseData(
  /**
   * The login name of the user that has been logged in.
   * The login name is required for requests that do not contain the login name (e.g., when using some kind of token)
   */
  val loginName: UserLoginName,
  /**
   * The JWS token pair
   */
  val jwsTokens: JwtTokenPair,
)
