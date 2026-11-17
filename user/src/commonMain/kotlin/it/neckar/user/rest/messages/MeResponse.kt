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
package it.neckar.user.rest.messages

import it.neckar.rest.RestResponse
import it.neckar.user.rest.views.UserDetails
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Response for a `me` request. Returns the user associated with the current authentication context.
 */
@Serializable
sealed interface MeResponse : RestResponse<UserDetails> {

  @Serializable
  @SerialName("good")
  data class Good(override val data: UserDetails) : MeResponse, RestResponse.GoodResponse<UserDetails>

  /**
   * The caller is authenticated but has no registered account in this service. A business-level
   * outcome, delivered with HTTP 200 — the frontend routes to the registration page.
   *
   * @param selfSignupAllowed whether registering without an invitation is possible; when false,
   * only an invitation link leads into the service
   */
  @Serializable
  @SerialName("bad/NotRegistered")
  data class NotRegistered(val selfSignupAllowed: Boolean) : MeResponse, RestResponse.BadResponse<UserDetails>

  companion object {
    /**
     * Helper method to create a success object - with the common type
     */
    fun good(data: UserDetails): MeResponse = Good(data)
  }
}
