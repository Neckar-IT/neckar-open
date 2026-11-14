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

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.cors.CORSConfig
import io.ktor.server.plugins.cors.routing.CORS
import it.neckar.common.featureflags.FeatureFlags
import it.neckar.logging.Logger
import it.neckar.logging.LoggerFactory
import it.neckar.projects.common.DomainDescriptor
import it.neckar.projects.common.ServiceDescriptor
import it.neckar.rest.tracking.XClientId
import it.neckar.rest.tracking.XClientRole
import it.neckar.rest.tracking.XServedBy
import it.neckar.rest.tracking.XSessionId
import it.neckar.rest.tracking.XTraceId
import it.neckar.rest.version.RestProtocolVersionInfo

private val logger: Logger = LoggerFactory.getLogger("it.neckar.ktor.DeploymentCorsExtensions")

/**
 * The default of the additional CORS configuration: nothing beyond the domain hosts. Compared by
 * identity, so a call without domains has to name the origins itself.
 */
val NoAdditionalCorsConfig: CORSConfig.() -> Unit = {}

/**
 * Installs CORS configuration derived from the given [DomainDescriptor]s and the [service] answering
 * under them.
 *
 * Configures:
 * - **Hosts:** Production, dev, demo, localhost (Traefik), Vite dev server, Docker service port
 * - **Methods:** All standard HTTP methods (GET, POST, PUT, DELETE, PATCH, HEAD, OPTIONS)
 * - **Request headers (allow):** Standard headers the browser is allowed to send
 *   (Authorization, NeckarIT-Feature-Flags, NeckarIT-Protocol-Version, NeckarIT-Client-Id,
 *   NeckarIT-Client-Role, NeckarIT-Session-Id, X-Request-Id, ETag, If-None-Match, Cookie,
 *   ngrok-skip-browser-warning)
 * - **Response headers (expose):** Headers the browser is allowed to expose to JavaScript
 *   (NeckarIT-Trace-Id, X-Request-Id, X-Served-By, NeckarIT-Protocol-Version, ETag)
 * - **Always included:** mea.neckar.it (internal portal), localhost:63342 (IntelliJ built-in server)
 *
 * Use [additionalConfig] to add service-specific CORS configuration (e.g., `anyHost()` for Mattermost).
 * Without [domains], [additionalConfig] decides the origins beyond the two always included and the
 * port of [service].
 */
fun Application.installProductCors(
  service: ServiceDescriptor?,
  vararg domains: DomainDescriptor,
  additionalConfig: CORSConfig.() -> Unit = NoAdditionalCorsConfig,
) {
  require(domains.isNotEmpty() || additionalConfig !== NoAdditionalCorsConfig) {
    "installProductCors needs a DomainDescriptor or an additional config that decides the origins"
  }

  install(CORS) {
    // Standard HTTP methods
    allowMethod(HttpMethod.Get)
    allowMethod(HttpMethod.Post)
    allowMethod(HttpMethod.Put)
    allowMethod(HttpMethod.Delete)
    allowMethod(HttpMethod.Patch)
    allowMethod(HttpMethod.Head)
    allowMethod(HttpMethod.Options)

    // Standard platform headers — request headers the browser is allowed to send.
    allowHeader(FeatureFlags.HeaderKey)
    allowHeader(RestProtocolVersionInfo.ProtocolVersionHeaderKey)
    allowHeader(HttpHeaders.Authorization)
    allowHeader(HttpHeaders.ETag)
    allowHeader(HttpHeaders.IfNoneMatch)
    allowHeader(HttpHeaders.XClientId)
    allowHeader(HttpHeaders.XClientRole)
    allowHeader(HttpHeaders.XSessionId)
    allowHeader(HttpHeaders.XRequestId)
    allowHeader(HttpHeaders.Cookie)
    allowHeader("ngrok-skip-browser-warning")

    // Response headers the browser is allowed to expose to JavaScript.
    // Without these, fetch/axios cannot read the values even when the server sets them.
    // NeckarIT-Trace-Id powers the Grafana/Tempo deep-link on the error page;
    // X-Request-Id and X-Served-By are read by the global error boundary for support
    // diagnostics; NeckarIT-Protocol-Version drives the client version mismatch check; ETag
    // is needed for conditional GETs from custom axios interceptors.
    exposeHeader(HttpHeaders.XTraceId)
    exposeHeader(HttpHeaders.XRequestId)
    exposeHeader(HttpHeaders.XServedBy)
    exposeHeader(RestProtocolVersionInfo.ProtocolVersionHeaderKey)
    exposeHeader(HttpHeaders.ETag)

    allowNonSimpleContentTypes = true
    allowCredentials = true

    service?.port?.let { port ->
      // Docker service port
      allowHost("localhost:${port.value}", listOf("http", "https"))
    }
    // Hosts derived from DomainDescriptors
    for (domain in domains) {
      domain.ui?.devPort?.let { port ->
        // Vite dev server
        allowHost("localhost:${port.value}", listOf("http", "https"))
      }
      // Traefik local dev (e.g., "elektromeister.localdev")
      allowHost(domain.localhostDomain.value, listOf("http", "https"))
      // Production (e.g., "elektromeister.neckar.it")
      allowHost(domain.domain.value, listOf("https"))
      // Dev environment (e.g., "dev.elektromeister.neckar.it")
      allowHost(domain.devDomain.value, listOf("https"))
      // Demo environment (e.g., "demo.elektromeister.neckar.it")
      allowHost(domain.demoDomain.value, listOf("https"))
    }

    // Internal team portal that embeds dashboards/widgets from all services
    allowHost("mea.neckar.it", listOf("https"))
    // IntelliJ built-in web server for local HTML preview and REST client
    allowHost("localhost:63342", listOf("http"))

    // Service-specific extensions
    additionalConfig()
  }

  if (domains.isEmpty()) {
    logger.info("CORS configured without domain hosts, servicePort=${service?.port}; the additional config decides the other origins")
    return
  }
  logger.info(
    "CORS configured for ${domains.joinToString { it.domain.value }}, servicePort=${service?.port}: " +
      domains.joinToString(separator = "; ") { domain ->
        "${domain.domain}: localhostDomain=${domain.localhostDomain}, " +
          "devDomain=${domain.devDomain}, " +
          "demoDomain=${domain.demoDomain}, " +
          "uiDevPort=${domain.ui?.devPort}"
      }
  )
}
