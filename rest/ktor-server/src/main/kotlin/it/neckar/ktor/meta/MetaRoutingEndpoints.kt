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
package it.neckar.ktor.meta

import it.neckar.open.annotations.Effective
import it.neckar.open.http.Url
import it.neckar.open.http.UrlPattern

/**
 * Meta routing endpoints that can be bound to different base URLs.
 *
 * Execute `MetaRoutingEndpointsEffectiveTest` to update the @Effective annotations
 */
object MetaRoutingEndpoints {
  @Effective("boom")
  val Boom: UrlPattern.Relative0 = Url.pattern("boom")

  @Effective("dump-routes")
  val DumpRoutes: UrlPattern.Relative0 = Url.pattern("dump-routes")

  @Effective("dump-headers")
  val DumpHeaders: UrlPattern.Relative0 = Url.pattern("dump-headers")

  @Effective("dump-request")
  val DumpRequest: UrlPattern.Relative0 = Url.pattern("dump-request")

  @Effective("dump-cookies")
  val DumpCookies: UrlPattern.Relative0 = Url.pattern("dump-cookies")

  @Effective("dump-proxy")
  val DumpProxy: UrlPattern.Relative0 = Url.pattern("dump-proxy")

  @Effective("version")
  val Version: UrlPattern.Relative0 = Url.pattern("version")

  @Effective("version/verify-client-version")
  val Version_VerifyClientVersion: UrlPattern.Relative0 = Version + "verify-client-version"

  @Effective("global-health")
  val GlobalHealth: UrlPattern.Relative0 = Url.pattern("global-health")

  @Effective("meta-tags")
  val MetaTags: UrlPattern.Relative0 = Url.pattern("meta-tags")

  @Effective("service-info")
  val ServiceInfo: UrlPattern.Relative0 = Url.pattern("service-info")

  @Effective("runtime-context")
  val RuntimeContext: UrlPattern.Relative0 = Url.pattern("runtime-context")

  @Effective("client")
  val Client: UrlPattern.Relative0 = Url.pattern("client")

  @Effective("client/role")
  val Client_Role: UrlPattern.Relative0 = Client + "role"

  @Effective("client/id")
  val Client_Id: UrlPattern.Relative0 = Client + "id"

  @Effective("feature-flags")
  val FeatureFlags: UrlPattern.Relative0 = Url.pattern("feature-flags")

  @Effective("feature-flags/available")
  val FeatureFlags_Available: UrlPattern.Relative0 = FeatureFlags + "available"

  /**
   * Creates meta routing endpoints bound to a specific base URL.
   * Use this for service-specific meta endpoint paths.
   *
   * Example:
   * ```
   * val emMeta = MetaRoutingEndpoints.forBaseUrl(Url.pattern("api/elektromeister"))
   * routing {
   *   get(emMeta.Version) { ... }
   * }
   * ```
   */
  fun forBaseUrl(baseUrl: UrlPattern.Relative0): Paths = Paths(baseUrl)

  class Paths(private val baseUrl: UrlPattern.Relative0) {
    /**
     * The base path for all meta endpoints
     */
    val BasePath: UrlPattern.Relative0 = baseUrl
    fun basePathUrl(): Url.Relative = BasePath.resolve()

    val Boom: UrlPattern.Relative0 = baseUrl + MetaRoutingEndpoints.Boom
    fun boomUrl(): Url.Relative = Boom.resolve()

    val DumpRoutes: UrlPattern.Relative0 = baseUrl + MetaRoutingEndpoints.DumpRoutes
    fun dumpRoutesUrl(): Url.Relative = DumpRoutes.resolve()

    val DumpHeaders: UrlPattern.Relative0 = baseUrl + MetaRoutingEndpoints.DumpHeaders
    fun dumpHeadersUrl(): Url.Relative = DumpHeaders.resolve()

    val DumpRequest: UrlPattern.Relative0 = baseUrl + MetaRoutingEndpoints.DumpRequest
    fun dumpRequestUrl(): Url.Relative = DumpRequest.resolve()

    val DumpCookies: UrlPattern.Relative0 = baseUrl + MetaRoutingEndpoints.DumpCookies
    fun dumpCookiesUrl(): Url.Relative = DumpCookies.resolve()

    val DumpProxy: UrlPattern.Relative0 = baseUrl + MetaRoutingEndpoints.DumpProxy
    fun dumpProxyUrl(): Url.Relative = DumpProxy.resolve()

    val Version: UrlPattern.Relative0 = baseUrl + MetaRoutingEndpoints.Version
    fun versionUrl(): Url.Relative = Version.resolve()

    val Version_VerifyClientVersion: UrlPattern.Relative0 = baseUrl + MetaRoutingEndpoints.Version_VerifyClientVersion
    fun versionVerifyClientVersionUrl(): Url.Relative = Version_VerifyClientVersion.resolve()

    val ServiceInfo: UrlPattern.Relative0 = baseUrl + MetaRoutingEndpoints.ServiceInfo
    fun serviceInfoUrl(): Url.Relative = ServiceInfo.resolve()

    val RuntimeContext: UrlPattern.Relative0 = baseUrl + MetaRoutingEndpoints.RuntimeContext
    fun runtimeContextUrl(): Url.Relative = RuntimeContext.resolve()

    val GlobalHealth: UrlPattern.Relative0 = baseUrl + MetaRoutingEndpoints.GlobalHealth
    fun globalHealthUrl(): Url.Relative = GlobalHealth.resolve()

    val MetaTags: UrlPattern.Relative0 = baseUrl + MetaRoutingEndpoints.MetaTags
    fun metaTagsUrl(): Url.Relative = MetaTags.resolve()

    val Client: UrlPattern.Relative0 = baseUrl + MetaRoutingEndpoints.Client
    fun clientUrl(): Url.Relative = Client.resolve()

    val Client_Role: UrlPattern.Relative0 = baseUrl + MetaRoutingEndpoints.Client_Role
    fun clientRoleUrl(): Url.Relative = Client_Role.resolve()

    val Client_Id: UrlPattern.Relative0 = baseUrl + MetaRoutingEndpoints.Client_Id
    fun clientIdUrl(): Url.Relative = Client_Id.resolve()

    val FeatureFlags: UrlPattern.Relative0 = baseUrl + MetaRoutingEndpoints.FeatureFlags
    fun featureFlagsUrl(): Url.Relative = FeatureFlags.resolve()

    val FeatureFlags_Available: UrlPattern.Relative0 = baseUrl + MetaRoutingEndpoints.FeatureFlags_Available
    fun featureFlagsAvailableUrl(): Url.Relative = FeatureFlags_Available.resolve()
  }
}
