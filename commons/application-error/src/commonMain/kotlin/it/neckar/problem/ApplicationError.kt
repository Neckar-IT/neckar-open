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
import kotlinx.serialization.Serializable

/**
 * Represents application errors.
 * There are two classes of errors:
 * - [ExpectedApplicationError] for expected errors (e.g., database not available)
 * - [UnexpectedApplicationError] for unexpected errors (bugs)
 *
 * This is the body a service responds with when a request fails. The JSON carries a `type`
 * discriminator holding the subtype's `@SerialName` - `"ExpectedError"` or `"UnexpectedError"`.
 * The TypeScript type guards in `typescript-utils/src/type-guards/applicationErrors.ts` compare
 * against exactly those two strings, so a renamed `@SerialName` silently breaks every client.
 */
@Serializable
sealed interface ApplicationError {
  /**
   * The message in english, to be shown when the client cannot translate the error.
   *
   * Never translated on the server. [ExpectedApplicationError] carries an error code the client
   * translates instead. Null if the underlying exception had no message; for
   * [UnexpectedApplicationError] see its own field.
   */
  val fallbackMessage: String?

  /**
   * URI identifying this one occurrence of the error - the `instance` member of RFC 9457
   * problem details.
   *
   * Null in practice: neither [toExpectedProblem] nor `Throwable.toUnexpectedProblem` fills it in,
   * and no service in this repository sets it. It exists for services that can point at a
   * per-occurrence resource (a log entry, an incident page).
   */
  val instanceUri: String?

  /**
   * The trace of the request that produced this error, so the caller can open the server-side span
   * in Grafana/Tempo instead of reading a text the server chose for it.
   *
   * Null when no OpenTelemetry exporter is configured — local development without
   * `OTEL_TRACES_EXPORTER` — because there is no span to point at. The same value also travels in
   * the `NeckarIT-Trace-Id` response header; the field exists so a stored or forwarded error body
   * keeps it.
   */
  val traceId: TraceId?
}
