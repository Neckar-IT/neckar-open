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
package it.neckar.ktor

import io.ktor.server.application.*
import io.ktor.server.plugins.cors.CORSConfig
import io.ktor.server.plugins.defaultheaders.*
import io.opentelemetry.api.OpenTelemetry
import it.neckar.ktor.proxy.installReverseProxySupport
import it.neckar.projects.common.DomainDescriptor
import it.neckar.projects.common.ServiceDescriptor

/**
 * Installs the canonical Neckar IT service plugin bundle that every Ktor service in this
 * codebase shares. Built against the Patterns reference setup so that diagnosis
 * workflows — trace lookup, request-id correlation, MDC identity, client-version check —
 * behave identically across all services.
 *
 * The bundle, in the order it is installed:
 * 1. [installOpenTelemetry] — server spans, `NeckarIT-Trace-Id` response header, identity
 *    enrichment. Installed **first** on purpose: [io.opentelemetry.instrumentation.ktor.v3_0.KtorServerTelemetry]
 *    creates the server span that authentication logic runs within, so authentication must be
 *    installed by the caller **after** this function returns.
 * 2. [installReverseProxySupport] — honor `X-Forwarded-*` from the reverse proxy so
 *    request-derived URLs resolve to the public scheme/host.
 * 3. [installProductCors] — CORS derived from the [serviceDescriptor] and the [domains]; extend per service via [corsConfig].
 * 4. [installFeatureFlagSupport] — `NeckarIT-Feature-Flags` request header support, the default flags of the local `run` and
 *    the `realisticLatency` delay on every request.
 * 5. [installCallId] — `X-Request-Id` propagation / generation.
 * 6. [installCallLogging] — request logging with MDC client identity.
 * 7. [installCompression] — response compression.
 * 8. [DefaultHeaders] with [neckarITServerHeader], [restVersionNumberHeader], [appGitHashHeader], [appImageDigestHeader] and
 *    [servedByHeader] (`Server`, `NeckarIT-Protocol-Version`, `X-App-Git-Hash`, `X-App-Image-Digest`, `X-Served-By`); the two
 *    identity headers only where the value is known.
 *
 * Plugins deliberately **not** bundled — install them per service after this call, because
 * they are either service-specific or order-sensitive relative to this bundle:
 * authentication, status pages, content negotiation / JSON, `IgnoreTrailingSlash`, routing,
 * `OpenApiPlugin`, meta / metrics routing.
 *
 * A service that deliberately omits one of the bundled plugins must not call this helper for
 * that plugin — it documents the omission with a comment at its own call site.
 */
fun Application.installNeckarServiceDefaults(
  serviceDescriptor: ServiceDescriptor,
  vararg domains: DomainDescriptor,
  openTelemetry: OpenTelemetry = OpenTelemetry.noop(),
  additionalCapturedRequestHeaders: List<String> = emptyList(),
  additionalCapturedResponseHeaders: List<String> = emptyList(),
  corsConfig: CORSConfig.() -> Unit = NoAdditionalCorsConfig,
) {
  installOpenTelemetry(
    openTelemetry = openTelemetry,
    additionalCapturedRequestHeaders = additionalCapturedRequestHeaders,
    additionalCapturedResponseHeaders = additionalCapturedResponseHeaders,
  )
  installReverseProxySupport()
  installProductCors(serviceDescriptor, *domains, additionalConfig = corsConfig)
  installFeatureFlagSupport()
  installCallId()
  installCallLogging()
  installCompression()

  install(DefaultHeaders) {
    neckarITServerHeader()
    restVersionNumberHeader()
    appGitHashHeader()
    appImageDigestHeader()
    servedByHeader(serviceDescriptor.serviceName)
  }
}
