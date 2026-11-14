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
package it.neckar.user

import it.neckar.open.annotations.TsExport
import it.neckar.open.kotlin.lang.ValueClassString
import kotlinx.serialization.Serializable

/**
 * A user login name.
 * Must not contain spaces or special chars
 *
 * Comparisons are done lower case
 *
 * A [ValueClassString]: the login name IS its text, and it is stored and sent as that text. Saying
 * so is what lets a query filter on it by property instead of by an untyped `PropertiesPath`.
 */
@Serializable(with = UserLoginNameSerializer::class)
@TsExport
data class UserLoginName(override val value: String) : ValueClassString {
  /**
   * Returns the value as lower case
   */
  fun lowerCase(): String {
    return value.lowercase()
  }

  override fun toString(): String {
    return value
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || this::class != other::class) return false

    other as UserLoginName

    return lowerCase() == other.lowerCase()
  }

  override fun hashCode(): Int {
    return lowerCase().hashCode()
  }

  companion object {
    val NeckarIT: UserLoginName = UserLoginName("neckarIT")
  }
}
