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

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents a value in a PATCH request with **explicit** serialization as a discriminated union.
 *
 * Unlike [PatchValue], this type serializes to an explicit JSON object with a "type" discriminator:
 * - [Update]: `{ "type": "Update", "value": ... }`
 * - [Keep]: `{ "type": "Keep" }`
 * - [Delete]: `{ "type": "Delete" }`
 *
 * This makes the semantics explicit in the JSON and provides better TypeScript type safety.
 *
 * Usage in data classes:
 * ```kotlin
 * @Serializable
 * data class UserPatch(
 *   val name: Patchable<String> = Patchable.Keep,
 *   val avatar: Patchable<String> = Patchable.Keep,
 * )
 * ```
 *
 * @see PatchValue for the implicit version (more compact JSON, but less explicit)
 * @see PatchableRequired for non-nullable fields (no Delete option)
 */
@Serializable
sealed interface Patchable<out T> {

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
    fun <T> keep(): Patchable<T> = Keep

    /**
     * Returns [Delete] - the field should be set to null/deleted.
     */
    fun <T> delete(): Patchable<T> = Delete

    /**
     * Creates a [Patchable] from a nullable value.
     * - `null` -> [Delete]
     * - non-null -> [Update]
     *
     * Note: This cannot represent [Keep], use the default value for that.
     */
    fun <T : Any> fromNullable(value: T?): Patchable<T> {
      return if (value == null) Delete else Update(value)
    }
  }

  /**
   * The field contains a new value that should replace the existing one.
   */
  @Serializable
  @SerialName("Update")
  data class Update<out T>(val value: T) : Patchable<T> {
    override fun toString(): String = "Update($value)"
  }

  /**
   * The field was not present or explicitly marked as "keep" - keep the existing value unchanged.
   */
  @Serializable
  @SerialName("Keep")
  data object Keep : Patchable<Nothing> {
    override fun toString(): String = "Keep"
  }

  /**
   * The field should be deleted/cleared (set to null).
   */
  @Serializable
  @SerialName("Delete")
  data object Delete : Patchable<Nothing> {
    override fun toString(): String = "Delete"
  }
}

/**
 * Returns the value for this patch:
 * - [Patchable.Update]: returns the new value
 * - [Patchable.Keep]: returns [currentValue]
 * - [Patchable.Delete]: returns null
 */
fun <T> Patchable<T>.getValueOr(currentValue: T?): T? {
  return when (this) {
    is Patchable.Update -> value
    is Patchable.Keep -> currentValue
    is Patchable.Delete -> null
  }
}

/**
 * Returns true if this patch changes the existing value.
 */
fun <T> Patchable<T>.wouldChange(existing: T?): Boolean {
  return when (this) {
    is Patchable.Update -> value != existing
    is Patchable.Keep -> false
    is Patchable.Delete -> existing != null
  }
}

/**
 * Converts this [Patchable] to an implicit [PatchValue].
 */
fun <T> Patchable<T>.toImplicit(): PatchValue<T> {
  return when (this) {
    is Patchable.Update -> PatchValue.Update(value)
    is Patchable.Keep -> PatchValue.Keep
    is Patchable.Delete -> PatchValue.Delete
  }
}

/**
 * Converts this implicit [PatchValue] to a [Patchable].
 */
fun <T> PatchValue<T>.toPatchable(): Patchable<T> {
  return when (this) {
    is PatchValue.Update -> Patchable.Update(value)
    is PatchValue.Keep -> Patchable.Keep
    is PatchValue.Delete -> Patchable.Delete
  }
}
