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
package it.neckar.integrations.mattermost.rest

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MattermostUserResponse(
  val id: UserId,
  val username: String,
  @SerialName("first_name") val firstName: String = "",
  @SerialName("last_name") val lastName: String = "",
  @SerialName("nickname") val nickname: String = "",
  @SerialName("is_bot") val isBot: Boolean = false,
  @SerialName("delete_at") val deleteAt: Long = 0,
  val roles: String = "",
) {
  /** A member without a first and last name is a system account, not a colleague. */
  val isActiveHuman: Boolean
    get() = isBot.not() && deleteAt == 0L && firstName.isNotBlank() && lastName.isNotBlank()

  fun displayName(): String {
    if (nickname.isNotBlank()) return nickname
    val full = "$firstName $lastName".trim()
    if (full.isNotBlank()) return full
    return username
  }
}
