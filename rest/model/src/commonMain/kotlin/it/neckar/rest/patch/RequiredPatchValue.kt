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

import kotlinx.serialization.Serializable

/**
 * Represents a value in a PATCH request for **non-nullable** fields.
 *
 * Unlike [PatchValue], this type does NOT support [PatchValue.Delete] because
 * deleting (setting to null) is not valid for required/non-nullable fields.
 *
 * States:
 * - [Update]: A new value is provided
 * - [Keep]: The field was not present in the JSON (undefined) - keep existing value
 *
 * Usage in data classes:
 * ```kotlin
 * @Serializable
 * data class UserPatch(
 *   // Non-nullable field: Delete not allowed
 *   val username: RequiredPatchValue<String> = RequiredPatchValue.Keep,
 *
 *   // Nullable field: Delete allowed
 *   val email: PatchValue<String> = PatchValue.Keep,
 * )
 * ```
 *
 * @see PatchValue for nullable fields where [PatchValue.Delete] is valid
 */
@Deprecated("Use PatchableRequired instead - more explicit serialization")
@Serializable(with = RequiredPatchValueSerializer::class)
sealed interface RequiredPatchValue<out T> {

  /**
   * Returns the value if this is an [Update], otherwise null.
   */
  fun valueOrNull(): T? {
    return when (this) {
      is Update -> value
      is Keep -> null
    }
  }

  /**
   * Returns the value if this is an [Update], otherwise throws.
   * Useful for CREATE operations where the field is required and there's no existing value to fall back to.
   *
   * @param lazyMessage Message for the exception if the value is not present
   * @throws IllegalStateException if this is [Keep]
   */
  @IgnorableReturnValue
  fun valueOrThrow(lazyMessage: () -> String): T {
    return when (this) {
      is Update -> value
      is Keep -> throw IllegalStateException(lazyMessage())
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
   * The field contains a new value that should replace the existing one.
   */
  data class Update<out T>(val value: T) : RequiredPatchValue<T> {
    override fun toString(): String = "Update($value)"
  }

  /**
   * The field was not present in the JSON - keep the existing value unchanged.
   * This is the default state.
   */
  data object Keep : RequiredPatchValue<Nothing> {
    override fun toString(): String = "Keep"
  }

  companion object {
    /**
     * Creates an [Update] with the given value.
     */
    fun <T> update(value: T): Update<T> = Update(value)

    /**
     * Returns [Keep] - the field should not be modified.
     */
    fun <T> keep(): RequiredPatchValue<T> = Keep
  }
}

/**
 * Returns the value for this patch:
 * - [RequiredPatchValue.Update]: returns the new value
 * - [RequiredPatchValue.Keep]: returns [currentValue]
 *
 * Always returns a non-null value.
 */
fun <T : Any> RequiredPatchValue<T>.getValueOr(currentValue: T): T {
  return when (this) {
    is RequiredPatchValue.Update -> value
    is RequiredPatchValue.Keep -> currentValue
  }
}

/**
 * Returns the value for this patch:
 * - [RequiredPatchValue.Update]: returns the new value
 * - [RequiredPatchValue.Keep]: returns [currentValue]
 *
 * May return null if [currentValue] is null.
 */
fun <T : Any> RequiredPatchValue<T>.getValueOrNull(currentValue: T?): T? {
  return when (this) {
    is RequiredPatchValue.Update -> value
    is RequiredPatchValue.Keep -> currentValue
  }
}

/**
 * Returns the value for this patch, or invokes [onNull] if the result would be null.
 *
 * This is useful for validation where a null result should trigger an early return:
 * ```kotlin
 * val imageUrl = request.imageUrl.getValueOrElse(existing?.imageUrl) {
 *   return ApplyResult.Invalid(ValidationError.MissingImage)
 * }
 * ```
 *
 * @param currentValue The current value to use for [RequiredPatchValue.Keep]
 * @param onNull Called when the result would be null; should return or throw
 */
inline fun <T : Any> RequiredPatchValue<T>.getValueOrElse(currentValue: T?, onNull: () -> Nothing): T {
  return when (this) {
    is RequiredPatchValue.Update -> value
    is RequiredPatchValue.Keep -> currentValue ?: onNull()
  }
}

/**
 * Returns true if this patch changes the existing value.
 */
fun <T> RequiredPatchValue<T>.wouldChange(existing: T): Boolean {
  return when (this) {
    is RequiredPatchValue.Update -> value != existing
    is RequiredPatchValue.Keep -> false
  }
}

/**
 * Converts this [RequiredPatchValue] to a [PatchValue].
 * [PatchValue.Update] becomes [PatchValue.Update], [PatchValue.Keep] becomes [PatchValue.Keep].
 */
fun <T> RequiredPatchValue<T>.toPatchValue(): PatchValue<T> {
  return when (this) {
    is RequiredPatchValue.Update -> PatchValue.Update(value)
    is RequiredPatchValue.Keep -> PatchValue.Keep
  }
}

/**
 * Converts this [PatchValue] to a [RequiredPatchValue].
 *
 * @throws IllegalStateException if this is [PatchValue.Delete]
 */
fun <T> PatchValue<T>.toRequiredOrThrow(): RequiredPatchValue<T> {
  return when (this) {
    is PatchValue.Update -> RequiredPatchValue.Update(value)
    is PatchValue.Keep -> RequiredPatchValue.Keep
    is PatchValue.Delete -> throw IllegalStateException("Cannot convert Delete to RequiredPatchValue")
  }
}

/**
 * Converts this [PatchValue] to a [RequiredPatchValue], treating [PatchValue.Delete] as [RequiredPatchValue.Keep].
 */
fun <T> PatchValue<T>.toRequiredOrKeep(): RequiredPatchValue<T> {
  return when (this) {
    is PatchValue.Update -> RequiredPatchValue.Update(value)
    is PatchValue.Keep -> RequiredPatchValue.Keep
    is PatchValue.Delete -> RequiredPatchValue.Keep
  }
}

