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

import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * Result of a guarded lifecycle transition.
 *
 * Used by patterns where a transition is gated by validation:
 *  - `confirmable-object`: Draft.confirm() returns `TransitionResult<Confirmed, ValidationError>`
 *  - `freeze-on-finalize`: InPreparation.finalize() returns `TransitionResult<Finalized, ValidationError>`
 *
 * @param T The post-transition type (e.g., `Book.Confirmed`, `BookOrder.Finalized`)
 * @param E The validation error type (e.g., `Book.ValidationError`, `BookOrder.ValidationError`)
 */
sealed interface TransitionResult<out T, out E : ValidationError> {
  /**
   * Transition succeeded — contains the produced object.
   */
  data class Success<out T>(val value: T) : TransitionResult<T, Nothing>

  /**
   * Validation failed — contains all collected errors. The source object is unchanged.
   */
  data class ValidationFailed<out E : ValidationError>(val errors: List<E>) : TransitionResult<Nothing, E>
}

/**
 * Executes [block] if this is a [TransitionResult.Success], passing the produced value.
 */
inline fun <T, E : ValidationError> TransitionResult<T, E>.ifSuccess(
  block: (T) -> Unit,
): TransitionResult<T, E> {
  contract { callsInPlace(block, InvocationKind.AT_MOST_ONCE) }

  if (this is TransitionResult.Success) {
    block(value)
  }

  return this
}

/**
 * Executes [block] if this is a [TransitionResult.ValidationFailed], passing the error list.
 */
inline fun <T, E : ValidationError> TransitionResult<T, E>.ifValidationFailed(
  block: (List<E>) -> Unit,
): TransitionResult<T, E> {
  contract { callsInPlace(block, InvocationKind.AT_MOST_ONCE) }

  if (this is TransitionResult.ValidationFailed) {
    block(errors)
  }

  return this
}
