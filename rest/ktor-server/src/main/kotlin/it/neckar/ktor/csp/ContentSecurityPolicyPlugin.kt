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
package it.neckar.ktor.csp

import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCallPipeline
import io.ktor.server.application.ApplicationPlugin
import io.ktor.server.application.call
import io.ktor.server.application.createApplicationPlugin
import io.ktor.server.application.install
import io.ktor.server.request.path
import io.ktor.server.response.header
import it.neckar.html.csp.CspNonce
import it.neckar.open.http.Url
import kotlinx.coroutines.withContext

/**
 * Configuration for [ContentSecurityPolicyPlugin].
 */
class ContentSecurityPolicyConfig {
  /**
   * Defaults to [CspMode.ReportOnly] so switching on cannot break running applications; flip to [CspMode.Enforce] once
   * reports in browser console are clean.
   */
  var mode: CspMode = CspMode.ReportOnly

  /**
   * Root-relative path prefixes whose responses carry no policy at all, matched on segment
   * boundaries — e.g. `/api/patterns/doc` exempts `/api/patterns/doc/api` but not
   * `/api/patterns/doctor`. Last resort for pages the service only hosts, not authors
   * (a third-party API reference UI); see
   * [it.neckar.ktor.server.openapi.ktor.OpenApiEndpoints.docRootFullUrl].
   */
  var exemptPathPrefixes: List<Url.RootRelative> = emptyList()

  /**
   * The baseline every service starts with, for two reasons.
   *
   * [CspDirective.DefaultSrc] is the fallback of every fetch directive, so `'self'` there covers
   * the ones a service does not name. `frame-ancestors`, `base-uri` and `form-action` have no such
   * fallback — omitting them would leave framing, `<base>` and form targets unrestricted.
   *
   * `object-src` does fall back to `default-src`, which would leave same-origin plugin content
   * allowed; it is forbidden outright instead, matching the nginx policies of the SPAs.
   */
  private val directives: MutableMap<CspDirective, List<CspSource>> =
    mutableMapOf(
      CspDirective.DefaultSrc to listOf(CspSource.Self),
      CspDirective.FrameAncestors to listOf(CspSource.Self),
      CspDirective.BaseUri to listOf(CspSource.Self),
      CspDirective.FormAction to listOf(CspSource.Self),
      CspDirective.ObjectSrc to emptyList(),
    )

  /**
   * Sets where the given [directive] kind may come from, replacing any previous value.
   * Passing no [sources] forbids the kind entirely.
   */
  fun directive(directive: CspDirective, vararg sources: CspSource) {
    directives[directive] = sources.toList()
  }

  internal fun policy(): CspPolicy = CspPolicy(directives.toMap())
}

/**
 * Installs the [ContentSecurityPolicyPlugin]. Not part of [it.neckar.ktor.installNeckarServiceDefaults]:
 * the policy names the origins of the service it protects, so it is opted into per service.
 */
fun Application.installContentSecurityPolicy(configure: ContentSecurityPolicyConfig.() -> Unit = {}) {
  install(ContentSecurityPolicyPlugin, configure)
}

/**
 * Emits the CSP header on every response and mints the per-response [CspNonce] in the same place,
 * so header and markup can never carry different nonces. Read the nonce with [cspNonce].
 */
val ContentSecurityPolicyPlugin: ApplicationPlugin<ContentSecurityPolicyConfig> =
  createApplicationPlugin("ContentSecurityPolicy", ::ContentSecurityPolicyConfig) {
    val mode: CspMode = pluginConfig.mode
    val policy: CspPolicy = pluginConfig.policy()
    val exemptPathPrefixes: List<String> = pluginConfig.exemptPathPrefixes.map { prefix -> prefix.value }

    application.intercept(ApplicationCallPipeline.Plugins) {
      val path = call.request.path()

      if (exemptPathPrefixes.any { prefix -> path == prefix || path.startsWith("$prefix/") }) {
        proceed()
        return@intercept
      }

      val nonce = CspNonce.random()
      call.response.header(mode.headerName, policy.render(nonce))

      withContext(CspNonceElement(nonce)) {
        proceed()
      }
    }
  }
