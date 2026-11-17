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

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.opentelemetry.api.trace.Span
import it.neckar.ktor.extractFeatureFlags
import it.neckar.ktor.jwt.loginName
import it.neckar.ktor.jwt.tenantDescriptorOrNull
import it.neckar.rest.tracking.XSessionId
import org.slf4j.MDC

/**
 * Ktor plugin that enriches OpenTelemetry spans with request context:
 *
 * **Identity** (from JWT, only for authenticated requests):
 * - `enduser.id` — pseudonymous login name (no PII)
 * - `tenant.id` — tenant UUID
 * - MDC entries [MDCKeys.UserId] and [MDCKeys.TenantId] for log correlation
 *
 * **Session** (from `NeckarIT-Session-Id` header, all requests from browsers):
 * - `session.id` — browser tab session UUID
 * - MDC entry [MDCKeys.SessionId] for log correlation
 *
 * **Feature Flags** (from request headers/query params, all requests):
 * - `feature.flags` — comma-separated list of active flag keys
 *
 * Installed automatically by `installOpenTelemetry()` — services should not install this
 * plugin directly. JWT-derived attributes are only populated for requests that pass through
 * an installed Authentication plugin; otherwise `call.principal()` returns null and the
 * JWT branch is skipped. The session and feature-flags branches run on every request.
 */
val OtelIdentityPlugin: ApplicationPlugin<Unit> = createApplicationPlugin("OtelIdentity") {
  onCallRespond { call ->
    val span = Span.current()
    val spanIsValid = span.spanContext.isValid

    // Identity from JWT (authenticated requests only)
    val principal = call.principal<JWTPrincipal>()
    if (principal != null) {
      val loginName = principal.payload.loginName().value
      // Keycloak tokens carry no tenant claim — tracing must tolerate that, so use the nullable variant.
      val tenantId = principal.payload.tenantDescriptorOrNull()?.id?.toString()

      if (spanIsValid) {
        span.setAttribute("enduser.id", loginName)
        if (tenantId != null) {
          span.setAttribute("tenant.id", tenantId)
        }
      }

      MDC.put(MDCKeys.UserId.value, loginName)
      if (tenantId != null) {
        MDC.put(MDCKeys.TenantId.value, tenantId)
      }
    }

    // Session ID from browser (NeckarIT-Session-Id header)
    val sessionId = call.request.header(HttpHeaders.XSessionId)
    if (sessionId != null) {
      if (spanIsValid) {
        span.setAttribute("session.id", sessionId)
      }
      MDC.put(MDCKeys.SessionId.value, sessionId)
    }

    // Feature flags (all requests)
    if (spanIsValid) {
      val featureFlags = call.extractFeatureFlags()
      if (featureFlags.isEmpty().not()) {
        span.setAttribute("feature.flags", featureFlags.encodeToString())
      }
    }
  }
}
