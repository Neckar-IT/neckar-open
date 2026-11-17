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
package it.neckar.open.tracing

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

/**
 * The trace ID of an OpenTelemetry trace: 32 lowercase hex characters.
 *
 * The one value a service hands a caller to point at the diagnosis: the message and the stack trace
 * stay in the log and the span, the caller looks them up in Grafana/Tempo by this ID. Serializes as
 * the bare hex string, so the wire format is the one every OpenTelemetry tool expects.
 */
@Serializable
@JvmInline
value class TraceId(val value: String) {
  init {
    require(isWellFormed(value)) { "trace ID must be $Length lowercase hex characters but was [$value]" }
  }

  override fun toString(): String = value

  companion object {
    /**
     * Length of the hex representation — 16 bytes, two characters each.
     */
    const val Length: Int = 32

    private const val HexCharacters: String = "0123456789abcdef"

    /**
     * The all-zero ID OpenTelemetry returns for an invalid span — no exporter configured, or no span
     * in scope. Never handed to a caller: [orNull] answers `null` for it.
     */
    val Invalid: TraceId = TraceId("0".repeat(Length))

    /**
     * The trace ID for [value], or `null` when there is none to report: absent, malformed, or
     * [Invalid].
     */
    fun orNull(value: String?): TraceId? {
      if (value == null || isWellFormed(value).not()) {
        return null
      }

      return TraceId(value).takeIf { it != Invalid }
    }

    private fun isWellFormed(value: String): Boolean = value.length == Length && value.all { it in HexCharacters }
  }
}
