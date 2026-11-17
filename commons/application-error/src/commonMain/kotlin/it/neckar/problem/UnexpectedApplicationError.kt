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
package it.neckar.problem

import it.neckar.open.tracing.TraceId
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents an unexpected problem - an exception nobody classified, usually a bug.
 *
 * Unlike [ExpectedApplicationError] there is no error code, so the client has nothing to translate:
 * it shows one fixed "unexpected error" page for all of them, and the only error-specific
 * information the user gets is [exceptionType], [fallbackMessage] and [traceId]. Whenever an error is
 * one we know can happen, throwing an `ApplicationException` and responding with
 * [ExpectedApplicationError] is the better answer.
 */
@Serializable
@SerialName("UnexpectedError")
data class UnexpectedApplicationError(
  /**
   * The message of the exception. Null in a 5xx answer, see `respondUnexpectedProblem` in `ktor-server`.
   */
  override val fallbackMessage: String? = null,

  /**
   * The class name of the exception from `exceptionTypeName()`, for diagnosis only. Its fallbacks make
   * it no stable identifier: never branch on it.
   */
  val exceptionType: String,

  override val instanceUri: String? = null,

  override val traceId: TraceId? = null,
) : ApplicationError
