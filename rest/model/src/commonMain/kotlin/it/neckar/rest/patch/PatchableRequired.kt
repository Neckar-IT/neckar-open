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
 * Represents a value in a PATCH request for **non-nullable** fields with **explicit** serialization.
 *
 * Unlike [RequiredPatchValue], this type serializes to an explicit JSON object with a "type" discriminator:
 * - [Update]: `{ "type": "Update", "value": ... }`
 * - [Keep]: `{ "type": "Keep" }`
 *
 * Unlike [Patchable], this type does NOT support Delete because
 * deleting (setting to null) is not valid for required/non-nullable fields.
 *
 * This makes the semantics explicit in the JSON and provides better TypeScript type safety.
 *
 * Usage in data classes:
 * ```kotlin
 * @Serializable
 * data class UserPatch(
 *   // Non-nullable field: Delete not allowed
 *   val username: PatchableRequired<String> = PatchableRequired.Keep,
 *
 *   // Nullable field: Delete allowed
 *   val avatar: Patchable<String> = Patchable.Keep,
 * )
 * ```
 *
 * @see RequiredPatchValue for the implicit version (more compact JSON, but less explicit)
 * @see Patchable for nullable fields where Delete is valid
 */
@Serializable
sealed interface PatchableRequired<out T> {

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
   * Returns true if this is an [Update].
   */
  val isUpdate: Boolean
    get() = this is Update

  /**
   * Returns true if this is [Keep].
   */
  val isKeep: Boolean
    get() = this is Keep

  companion object {
    /**
     * Creates an [Update] with the given value.
     */
    fun <T> update(value: T): Update<T> = Update(value)

    /**
     * Returns [Keep] - the field should not be modified.
     */
    fun <T> keep(): PatchableRequired<T> = Keep
  }

  /**
   * The field contains a new value that should replace the existing one.
   */
  @Serializable
  @SerialName("Update")
  data class Update<out T>(val value: T) : PatchableRequired<T> {
    override fun toString(): String = "Update($value)"
  }

  /**
   * The field was not present or explicitly marked as "keep" - keep the existing value unchanged.
   */
  @Serializable
  @SerialName("Keep")
  data object Keep : PatchableRequired<Nothing> {
    override fun toString(): String = "Keep"
  }
}

/**
 * Returns the value for this patch:
 * - [PatchableRequired.Update]: returns the new value
 * - [PatchableRequired.Keep]: returns [currentValue]
 *
 * Always returns a non-null value.
 */
fun <T : Any> PatchableRequired<T>.getValueOr(currentValue: T): T {
  return when (this) {
    is PatchableRequired.Update -> value
    is PatchableRequired.Keep -> currentValue
  }
}

/**
 * Returns the value for this patch:
 * - [PatchableRequired.Update]: returns the new value
 * - [PatchableRequired.Keep]: returns [currentValue]
 *
 * May return null if [currentValue] is null.
 */
fun <T : Any> PatchableRequired<T>.getValueOrNull(currentValue: T?): T? {
  return when (this) {
    is PatchableRequired.Update -> value
    is PatchableRequired.Keep -> currentValue
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
 * @param currentValue The current value to use for [PatchableRequired.Keep]
 * @param onNull Called when the result would be null; should return or throw
 */
inline fun <T : Any> PatchableRequired<T>.getValueOrElse(currentValue: T?, onNull: () -> Nothing): T {
  return when (this) {
    is PatchableRequired.Update -> value
    is PatchableRequired.Keep -> currentValue ?: onNull()
  }
}

/**
 * Returns the value if this is an [PatchableRequired.Update], or throws an exception.
 *
 * @param lazyMessage The message to include in the exception if this is [PatchableRequired.Keep]
 * @throws IllegalStateException if this is [PatchableRequired.Keep]
 */
@IgnorableReturnValue
inline fun <T> PatchableRequired<T>.valueOrThrow(lazyMessage: () -> String): T {
  return when (this) {
    is PatchableRequired.Update -> value
    is PatchableRequired.Keep -> throw IllegalStateException(lazyMessage())
  }
}

/**
 * Returns true if this patch changes the existing value.
 */
fun <T> PatchableRequired<T>.wouldChange(existing: T): Boolean {
  return when (this) {
    is PatchableRequired.Update -> value != existing
    is PatchableRequired.Keep -> false
  }
}

/**
 * Converts this [PatchableRequired] to a [Patchable].
 * [Update] becomes [Patchable.Update], [Keep] becomes [Patchable.Keep].
 */
fun <T> PatchableRequired<T>.toPatchable(): Patchable<T> {
  return when (this) {
    is PatchableRequired.Update -> Patchable.Update(value)
    is PatchableRequired.Keep -> Patchable.Keep
  }
}

/**
 * Converts this [PatchableRequired] to an implicit [RequiredPatchValue].
 */
fun <T> PatchableRequired<T>.toImplicit(): RequiredPatchValue<T> {
  return when (this) {
    is PatchableRequired.Update -> RequiredPatchValue.Update(value)
    is PatchableRequired.Keep -> RequiredPatchValue.Keep
  }
}

/**
 * Converts this implicit [RequiredPatchValue] to a [PatchableRequired].
 */
fun <T> RequiredPatchValue<T>.toPatchableRequired(): PatchableRequired<T> {
  return when (this) {
    is RequiredPatchValue.Update -> PatchableRequired.Update(value)
    is RequiredPatchValue.Keep -> PatchableRequired.Keep
  }
}

/**
 * Converts this [Patchable] to a [PatchableRequired].
 *
 * @throws IllegalStateException if this is [Patchable.Delete]
 */
fun <T> Patchable<T>.toPatchableRequiredOrThrow(): PatchableRequired<T> {
  return when (this) {
    is Patchable.Update -> PatchableRequired.Update(value)
    is Patchable.Keep -> PatchableRequired.Keep
    is Patchable.Delete -> throw IllegalStateException("Cannot convert Delete to PatchableRequired")
  }
}

/**
 * Converts this [Patchable] to a [PatchableRequired],
 * treating [Patchable.Delete] as [PatchableRequired.Keep].
 */
fun <T> Patchable<T>.toPatchableRequiredOrKeep(): PatchableRequired<T> {
  return when (this) {
    is Patchable.Update -> PatchableRequired.Update(value)
    is Patchable.Keep -> PatchableRequired.Keep
    is Patchable.Delete -> PatchableRequired.Keep
  }
}
