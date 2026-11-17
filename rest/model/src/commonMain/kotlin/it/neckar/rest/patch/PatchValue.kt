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
package it.neckar.rest.patch

import it.neckar.open.annotations.OpenApiNullable
import kotlinx.serialization.Serializable

/**
 * Represents a value in a PATCH request that can distinguish between:
 * - [Update]: A new value is provided
 * - [Keep]: The field was not present in the JSON (undefined) - keep existing value
 * - [Delete]: The field was explicitly set to null - delete/clear the value
 *
 * This is useful for PATCH operations where we need to distinguish between
 * "not sent" (keep current value) and "sent as null" (delete value).
 *
 * Usage in data classes:
 * ```kotlin
 * @Serializable
 * data class UserPatch(
 *   val name: PatchValue<String> = Keep,
 *   val email: PatchValue<String> = Keep,
 * )
 * ```
 */
@Deprecated("Use Patchable instead - more explicit serialization")
@OpenApiNullable
@Serializable(with = PatchValueSerializer::class)
sealed interface PatchValue<out T> {

  /**
   * Returns the value if this is an [Update], otherwise null.
   */
  fun valueOrNull(): T? {
    return when (this) {
      is Update -> value
      else -> null
    }
  }

  /**
   * Returns true if this is an [Update].
   */
  val isUpdate: Boolean
    get() = this is Update

  /**
   * Returns true if this is [Keep].
   */
  val isKeep: Boolean
    get() = this is Keep

  /**
   * Returns true if this is [Delete].
   */
  val isDelete: Boolean
    get() = this is Delete

  companion object {
    /**
     * Creates an [Update] with the given value.
     */
    fun <T> update(value: T): Update<T> = Update(value)

    /**
     * Returns [Keep] - the field should not be modified.
     */
    fun <T> keep(): PatchValue<T> = Keep

    /**
     * Returns [Delete] - the field should be set to null/deleted.
     */
    fun <T> delete(): PatchValue<T> = Delete

    /**
     * Creates a [PatchValue] from a nullable value.
     * - `null` -> [Delete]
     * - non-null -> [Update]
     *
     * Note: This cannot represent [Keep], use the default value for that.
     */
    fun <T : Any> fromNullable(value: T?): PatchValue<T> {
      return if (value == null) Delete else Update(value)
    }
  }

  /**
   * The field contains a new value that should replace the existing one.
   */
  data class Update<out T>(val value: T) : PatchValue<T> {
    override fun toString(): String = "Update($value)"
  }

  /**
   * The field was not present in the JSON - keep the existing value unchanged.
   * This is the default state.
   */
  data object Keep : PatchValue<Nothing> {
    override fun toString(): String = "Keep"
  }

  /**
   * The field was explicitly set to null - delete/clear the value.
   */
  data object Delete : PatchValue<Nothing> {
    override fun toString(): String = "Delete"
  }
}

/**
 * Returns the value for this patch:
 * - [PatchValue.Update]: returns the new value
 * - [PatchValue.Keep]: returns [currentValue]
 * - [PatchValue.Delete]: returns null
 */
fun <T> PatchValue<T>.getValueOr(currentValue: T?): T? {
  return when (this) {
    is PatchValue.Update -> value
    is PatchValue.Keep -> currentValue
    is PatchValue.Delete -> null
  }
}

/**
 * Returns true if this patch changes the existing value.
 */
fun <T> PatchValue<T>.wouldChange(existing: T?): Boolean {
  return when (this) {
    is PatchValue.Update -> value != existing
    is PatchValue.Keep -> false
    is PatchValue.Delete -> existing != null
  }
}

