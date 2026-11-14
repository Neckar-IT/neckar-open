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
package it.neckar.ktor.tracking

import io.ktor.server.application.*
import io.ktor.server.plugins.callid.*
import io.ktor.server.plugins.calllogging.*
import kotlinx.serialization.Serializable
import org.slf4j.MDC

/**
 * The keys for the MDC (Mapped Diagnostic Context).
 *
 * MDC is an SLF4J/Logback feature that binds key-value pairs to the current thread.
 * These entries are automatically included in log output (e.g. as `mdc_` prefixed fields in loki4j JSON logs).
 */
object MDCKeys {
  /**
   * The key for the client ID in the MDC
   */
  val ClientId: MDCKey = MDCKey("ClientId")

  /**
   * The key for the client role in the MDC
   */
  val ClientRole: MDCKey = MDCKey("ClientRole")

  /**
   * The key for the call ID in the MDC
   */
  val CallId: MDCKey = MDCKey("CallId")

  /**
   * The login name of the authenticated user, extracted from the JWT subject.
   * Pseudonymous — not a real name or email.
   */
  val UserId: MDCKey = MDCKey("UserId")

  /**
   * The tenant ID (UUID) of the authenticated user, extracted from JWT claims.
   */
  val TenantId: MDCKey = MDCKey("TenantId")

  /**
   * The browser session ID (UUID per tab), propagated from the frontend via `NeckarIT-Session-Id` header.
   */
  val SessionId: MDCKey = MDCKey("SessionId")

  /**
   * OpenTelemetry trace ID, injected automatically by the OTel Java Agent.
   * The key name matches the OTel convention for Logback MDC integration.
   */
  val TraceId: MDCKey = MDCKey("trace_id")

  /**
   * OpenTelemetry span ID, injected automatically by the OTel Java Agent.
   * The key name matches the OTel convention for Logback MDC integration.
   */
  val SpanId: MDCKey = MDCKey("span_id")
}


inline fun CallLoggingConfig.callIdMdc(key: MDCKey) {
  this.callIdMdc(key.value)
}

inline fun CallLoggingConfig.mdc(key: MDCKey, noinline provider: (ApplicationCall) -> String?) {
  this.mdc(key.value, provider)
}


/**
 * A type-safe key for the SLF4J Mapped Diagnostic Context (MDC).
 *
 * MDC stores key-value pairs per thread that are automatically included in log output.
 */
@JvmInline
@Serializable
value class MDCKey(val value: String) {
  fun get(): String? {
    return MDC.get(this.value)
  }
}


