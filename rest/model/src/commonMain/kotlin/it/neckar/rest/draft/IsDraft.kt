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
package it.neckar.rest.draft

import it.neckar.rest.transition.TransitionResult
import it.neckar.rest.transition.ValidationError
import it.neckar.rest.transition.ValidationResult
import kotlin.time.Instant

/**
 * Interface for draft objects.
 *
 * Implementations must override [validate] to return domain-specific validation errors.
 *
 * Domain-level drafts (e.g., `Author.Draft`, `Book.Draft`) must override [confirm]
 * to validate and transition to the confirmed state.
 *
 * Persistence-level drafts (e.g., entity classes) typically do not support
 * direct confirmation — the default [confirm] throws [UnsupportedOperationException].
 */
interface IsDraft : Confirmable {
  override val confirmationStatus: ConfirmationStatus
    get() = ConfirmationStatus.Draft

  /**
   * Validates this draft at the given reference time.
   *
   * [now] is explicit so validation is a pure function of (state, time).
   * Implementations that have purely structural rules (no time-dependent
   * constraints) may ignore the parameter. Implementations with rules such
   * as "a currently-valid timeline entry must exist" use it.
   *
   * Returns [ValidationResult.Valid] if valid, or [ValidationResult.Invalid] with all errors.
   */
  fun validate(now: Instant): ValidationResult<out ValidationError>

  /**
   * Validates and confirms this draft at the given reference time.
   *
   * Domain-level drafts must override this to validate required fields
   * and construct the confirmed variant.
   *
   * Default implementation throws [UnsupportedOperationException] — persistence-level
   * drafts (entities) do not support direct confirmation.
   */
  fun confirm(now: Instant): TransitionResult<IsConfirmed, ValidationError> {
    throw UnsupportedOperationException("confirm() is not supported on ${this::class.simpleName}. Override in domain-level drafts.")
  }
}
