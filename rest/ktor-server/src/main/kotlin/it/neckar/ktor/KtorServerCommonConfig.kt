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
import it.neckar.ktor.health.MetricsRouting
import it.neckar.ktor.health.MicrometerSupport
import it.neckar.ktor.meta.MetaRouting
import it.neckar.open.http.UrlPattern
import it.neckar.projects.common.ServiceDescriptor


/**
 * Adds the default meta-routing to the application
 */
fun Application.metaRouting(
  service: ServiceDescriptor,
  /**
   * The base URL for the service. Usually "api/${service-name}"
   * "metrics" will be appended to this
   */
  baseUrl: UrlPattern.Relative0 = UrlPattern.relative(service.apiPrefix),
  /**
   * Whether the sensitive meta endpoints require an access token. No default — every service
   * decides explicitly: [AuthenticationRequirement.Required] (requires the access-token
   * provider to be installed) or [AuthenticationRequirement.None] (service installs no
   * provider). The liveness/health whitelist stays public regardless.
   */
  authenticationRequirement: AuthenticationRequirement,
) {
  MetaRouting(
    service = service,
    baseUrl = baseUrl,
    authenticationRequirement = authenticationRequirement,
  ).configure(this)

  HeartbeatRouting(
    baseUrl = baseUrl,
  ).configure(this)
}

fun Application.metricsRouting(
  /**
   * The service descriptor that will be used to extract the base url
   */
  service: ServiceDescriptor,

  /**
   * The metrics provider
   */
  metricsProvider: () -> String,
) {
  MetricsRouting(
    baseUrl = UrlPattern.relative(service.apiPrefix),
    metricsProvider = metricsProvider,
  ).configure(this)
}

fun Application.metricsRouting(
  service: ServiceDescriptor,

  /**
   * The metrics configuration.
   */
  metricsConfiguration: (micrometerSupport: MicrometerSupport) -> Unit = { it.registerDefaultMetrics() },

  /**
   * If set to false, the default metrics will not be added
   */
  addDefaultMetrics: Boolean = true,
) {
  metricsRouting(UrlPattern.relative(service.apiPrefix), metricsConfiguration, addDefaultMetrics)
}

fun Application.metricsRouting(
  /**
   * The base URL for the service. Usually "api/${service-name}".
   * "metrics" will be appended to this
   */
  baseUrl: UrlPattern.Relative,

  /**
   * Additional metrics configuration for custom metrics.
   * Use this to register custom gauges, counters, etc. beyond the default metrics.
   */
  metricsConfiguration: (micrometerSupport: MicrometerSupport) -> Unit = { },

  /**
   * If set to false, the default metrics will not be added
   */
  addDefaultMetrics: Boolean = true,
) {
  val micrometerSupport = MicrometerSupport()
  if (addDefaultMetrics) {
    micrometerSupport.registerDefaultMetrics()
  }
  metricsConfiguration(micrometerSupport)

  MetricsRouting(
    baseUrl = baseUrl,
    metricsProvider = micrometerSupport.prometheusRegistry::scrape,
  ).configure(this)
}

/**
 * Adds the default metrics routing to the application
 */
fun Application.metricsRouting(
  /**
   * The base URL for the service. Usually "api/${service-name}"
   * "metrics" will be appended to this
   */
  baseUrl: UrlPattern.Relative,

  /**
   * The metrics provider
   */
  metricsProvider: () -> String,

  ) {
  MetricsRouting(
    baseUrl = baseUrl,
    metricsProvider = metricsProvider,
  ).configure(this)
}
