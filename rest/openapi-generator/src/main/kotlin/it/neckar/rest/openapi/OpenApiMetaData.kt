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
package it.neckar.rest.openapi

import it.neckar.open.http.Url
import it.neckar.open.version.Version
import it.neckar.projects.common.ServiceDescriptor
import kotlinx.serialization.Serializable

/**
 * Meta-data for the open API
 */
data class OpenApiMetaData(
  val title: String,
  /**
   * The description
   */
  val description: String,

  /**
   * The servers
   */
  val servers: List<Url> = emptyList(),

  /**
   * The version of the API
   */
  val version: Version = Version(1, 0, 0),

  /**
   * Optional OAuth2 Authorization Code configuration for the OpenAPI security scheme.
   * When set, an `oauth2` security scheme is added alongside `bearerAuth`.
   */
  val oAuth2Config: OAuth2Config? = null,

  //Optional info-object metadata. Kept at the end so positional constructor calls of the
  //original fields (title, description, servers, version, oAuth2Config) keep compiling.

  /**
   * An optional short summary of the API (`info.summary`, OpenAPI 3.1).
   */
  val summary: String? = null,

  /**
   * An optional URL to the Terms of Service for the API (`info.termsOfService`).
   */
  val termsOfService: String? = null,

  /**
   * Optional contact information for the API (`info.contact`).
   */
  val contact: OpenApiContact? = null,

  /**
   * Optional license information for the API (`info.license`).
   */
  val license: OpenApiLicense? = null,
) {
  /**
   * Returns a copy whose [servers] list contains only externally reachable URLs.
   *
   * Internal bind targets (localhost, `*.localdev`, loopback / unspecified / private addresses)
   * are dropped so they are never disclosed to public clients.
   */
  fun withoutInternalServers(): OpenApiMetaData = copy(servers = servers.filterNot { it.isInternalHost() })

  companion object {
    fun create(serviceDescriptor: ServiceDescriptor, servers: List<Url> = emptyList()): OpenApiMetaData {
      return OpenApiMetaData(
        title = serviceDescriptor.serviceName.value,
        description = serviceDescriptor.description ?: serviceDescriptor.serviceName.value,
        servers = servers,
      )
    }

    val default: OpenApiMetaData = OpenApiMetaData(
      title = "REST API",
      description = "REST API",
      servers = emptyList(),
    )
  }

  /**
   * The category of a route.
   * Can be used to filter routes in the OpenAPI documentation.
   *
   * This is helpful to group routes and generate multiple openapi.json files for different categories.
   */
  @Serializable
  data class Category(
    /**
     * The name of the category
     */
    val value: String
  ) {
    companion object {
      /**
       * Default category for production routes. Used to generate an openapi.json file for production routes.
       */
      val Default: Category = Category("default")

      /**
       * Meta routes (service metadata, feature flags, ...).
       */
      val Meta: Category = Category("meta")

      /**
       * Admin routes - that are usually not used by the client.
       * Carried by every route under [AdminRoutePathPrefix] and by no other route.
       */
      val Admin: Category = Category("admin")

      /**
       * Heartbeat routes used to check whether the service is running.
       */
      val Heartbeat: Category = Category("heartbeat")

      /**
       * Metrics routes (Prometheus metrics, JVM startup parameters, memory usage).
       */
      val Metrics: Category = Category("metrics")

      /**
       * Environment routes - additional types describing the runtime environment.
       */
      val Environment: Category = Category("environment")

      /**
       * The types a tenant script is called with and answers in. Carries no routes: the script
       * is the consumer, and it reaches these types as generated TypeScript, not over HTTP.
       */
      val Script: Category = Category("script")

      /**
       * The types a PDF template is rendered from. Carries no routes: the template is the consumer, and it reaches
       * these types as generated TypeScript, not over HTTP.
       */
      val Pdf: Category = Category("pdf")
    }
  }
}

private val PrivateClassBRange: Regex = Regex("^172\\.(1[6-9]|2\\d|3[01])\\.")

/**
 * Whether this URL points at an internal bind target that must not be disclosed to public clients:
 * localhost, `*.localdev`, or a loopback / unspecified / private IPv4 address.
 */
private fun Url.isInternalHost(): Boolean {
  val host = value
    .substringAfter("://", value)
    .substringBefore('/')
    .substringBefore(':')
    .lowercase()

  return host == "localhost" ||
    host.endsWith(".localdev") ||
    host.startsWith("127.") ||
    host == "0.0.0.0" ||
    host == "::1" ||
    host.startsWith("10.") ||
    host.startsWith("192.168.") ||
    PrivateClassBRange.containsMatchIn(host)
}
