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
package it.neckar.open.exceptions

import it.neckar.open.annotations.JavaFriendly
import it.neckar.open.exceptions.io.ErrorCodeSerializer
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline
import kotlin.jvm.JvmStatic


/**
 * Identifies an error.
 * Usually an enumeration should be created that contains one set of error codes.
 */
@Serializable(with = ErrorCodeSerializer::class)
data class ErrorCode(
  /**
   * The prefix for the error code - must be unique over all components
   */
  /**
   * Returns the prefix of the error code
   */
  val prefix: Prefix,
  /**
   * The id for this error code - must be unique for one prefix
   */
  val id: Int
) {

  init {
    require(prefix.asString().contains(Separator).not()) {
      "Prefix <${prefix}> must not contain separator <$Separator>"
    }
  }

  /**
   * Returns the error code formatted as string
   */
  fun format(): String {
    return prefix.asString() + Separator + id
  }

  override fun toString(): String {
    return format()
  }

  /**
   * Represents a prefix for error codes.
   * The prefix is used to structure the error codes.
   * The ids are unique within a prefix.
   */
  @Serializable
  @JvmInline
  value class Prefix(private val prefix: String) {

    /**
     * Returns the string representation of this prefix
     */
    fun asString(): String {
      return prefix
    }

    override fun toString(): String {
      return prefix
    }
  }

  companion object {
    private const val Separator = '-'

    @JavaFriendly
    @JvmStatic
    fun create(prefix: String, id: Int): ErrorCode {
      return ErrorCode(Prefix(prefix), id)
    }

    fun parse(errorCodeAsString: String): ErrorCode {
      val parts = errorCodeAsString.split(Separator, limit = 2)
      require(parts.size == 2) {
        "Invalid error code as string <$errorCodeAsString>"
      }

      return ErrorCode.create(parts[0], parts[1].toInt())
    }
  }
}
