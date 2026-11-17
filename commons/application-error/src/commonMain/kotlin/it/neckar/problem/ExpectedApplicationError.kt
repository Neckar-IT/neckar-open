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

import it.neckar.open.exceptions.ApplicationException
import it.neckar.open.exceptions.ErrorCode
import it.neckar.open.kotlin.serializers.AnyNullableSerializer
import it.neckar.open.tracing.TraceId
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents an (expected) problem.
 *
 * All strings are in english.
 * use the [errorCode] for translation on the client side.
 */
@Serializable
@SerialName("ExpectedError")
data class ExpectedApplicationError(
  /**
   * The error code for this problem - the one field that separates an expected error from an
   * [UnexpectedApplicationError].
   *
   * The client uses it as the translation key, so it is part of the public contract of the
   * endpoint: changing a code for an existing error leaves the client without a translation and
   * it falls back to [fallbackMessage].
   */
  val errorCode: ErrorCode,

  /**
   * A short, human-readable summary of the problem type.
   * *NOT translated*.
   *
   * Translation should be done on the client side - using the [errorCode] as key.
   */
  override val fallbackMessage: String? = null,

  override val instanceUri: String? = null,

  /**
   * Optional, additional attributes - the placeholders belonging to [errorCode], so the client can
   * fill its translated message (e.g. `missingCapability` for a 403).
   *
   * Only values [AnyNullableSerializer] understands may be put in here: `String`, `Number`,
   * `Boolean`, `JsonPrimitive` or null. Anything else throws `IllegalArgumentException` while the
   * error response is being serialized - which turns a clean 400 into a broken response.
   */
  val attributes: Map<String, @Serializable(with = AnyNullableSerializer::class) Any?>? = null,

  override val traceId: TraceId? = null,
) : ApplicationError

/**
 * Converts an [ApplicationException] into the error body that is sent to the client.
 *
 * Keeps the error code, so the client can translate. Note that [ApplicationException.parameters]
 * are handed to [ExpectedApplicationError.attributes] unchanged - see the restriction documented
 * there. [ApplicationException.fallbackMessage] is used rather than `message`, because `message`
 * only renders the error code again.
 *
 * @param traceId the trace of the request being answered, so the response points at the span
 * instead of at a text.
 */
fun ApplicationException.toExpectedProblem(traceId: TraceId? = null): ExpectedApplicationError {
  return ExpectedApplicationError(
    errorCode = errorCode,
    fallbackMessage = fallbackMessage,
    instanceUri = null,
    attributes = this.parameters,
    traceId = traceId,
  )
}

/**
 * Exists only to make the conversion a compile error.
 *
 * An [ApplicationException] is a `Throwable`, so without this overload the more general
 * `Throwable.toUnexpectedProblem()` would apply and quietly drop the error code - the client would
 * receive an untranslatable [UnexpectedApplicationError] for an error we classified perfectly well.
 * This more specific overload wins overload resolution and fails the build instead.
 * Use [toExpectedProblem].
 *
 * Its parameters mirror the general overload's, or a call that passes one would go there instead.
 */
@Deprecated("Convert to expected problem instead", level = DeprecationLevel.ERROR)
fun ApplicationException.toUnexpectedProblem(traceId: TraceId? = null): UnexpectedApplicationError {
  throw UnsupportedOperationException("Must not convert to UnexpectedProblem.")
}
