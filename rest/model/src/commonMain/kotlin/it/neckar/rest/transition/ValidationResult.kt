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
package it.neckar.rest.transition

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Result of a read-only validation check on an object that gates a lifecycle transition.
 *
 * @param E The validation error type (e.g., `Author.ValidationError`, `Book.ValidationError`,
 *   `BookOrder.ValidationError`)
 */
@Serializable
sealed interface ValidationResult<out E : ValidationError> {
  /**
   * Validation was successful — the object is in a state where the transition is currently allowed.
   */
  @Serializable
  @SerialName("Valid")
  data object Valid : ValidationResult<Nothing>

  /**
   * Validation failed — contains all collected errors.
   */
  @Serializable
  @SerialName("Invalid")
  data class Invalid<out E : ValidationError>(val errors: List<E>) : ValidationResult<E> {
    init {
      require(errors.isNotEmpty()) { "Invalid ValidationResult must contain at least one error." }
    }
  }

  companion object {
    /**
     * [Valid] for an empty [errors], else [Invalid] with [errors].
     */
    fun <E : ValidationError> of(errors: List<E>): ValidationResult<E> {
      return when {
        errors.isEmpty() -> Valid
        else -> Invalid(errors)
      }
    }
  }
}

/**
 * The errors of this result; empty for [ValidationResult.Valid].
 */
val <E : ValidationError> ValidationResult<E>.errors: List<E>
  get() = when (this) {
    is ValidationResult.Valid -> emptyList()
    is ValidationResult.Invalid -> errors
  }
