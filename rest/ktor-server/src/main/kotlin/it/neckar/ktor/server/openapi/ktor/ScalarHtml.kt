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
package it.neckar.ktor.server.openapi.ktor

import it.neckar.open.http.Url
import it.neckar.rest.openapi.OAuth2Config
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

/**
 * Renders a minimal HTML page that loads Scalar's API Reference from the official CDN
 * and points it at the given OpenAPI JSON URL.
 *
 * Scalar is a self-contained web component — a single script tag plus a `<script id="api-reference">`
 * holding the spec URL is enough. See https://github.com/scalar/scalar for details.
 */
object ScalarHtml {
  /**
   * Scalar API Reference CDN URL. Pinned to the major version to avoid surprise breakage.
   */
  const val cdnUrl: String = "https://cdn.jsdelivr.net/npm/@scalar/api-reference@1"

  /**
   * Builds the Scalar HTML page for the given spec URL.
   *
   * @param specUrl relative or absolute URL to the OpenAPI JSON document
   * @param title page title displayed in the browser tab
   * @param oAuth2Config optional OAuth2 configuration for interactive authentication
   */
  fun render(specUrl: Url, title: String = "API Reference", oAuth2Config: OAuth2Config? = null): String {
    val escapedSpecUrl = specUrl.value.htmlAttributeEscape()
    val escapedTitle = title.htmlTextEscape()
    val configJson = buildConfigurationJson(oAuth2Config)
    val dataConfigAttr = if (configJson != null) " data-configuration='${configJson.htmlAttributeEscapeSingleQuote()}'" else ""

    return """
      <!doctype html>
      <html lang="en">
        <head>
          <meta charset="utf-8">
          <meta name="viewport" content="width=device-width, initial-scale=1">
          <title>$escapedTitle</title>
        </head>
        <body>
          <script id="api-reference" data-url="$escapedSpecUrl"$dataConfigAttr></script>
          <script src="$cdnUrl"></script>
        </body>
      </html>
    """.trimIndent()
  }

  private fun buildConfigurationJson(oAuth2Config: OAuth2Config?): String? {
    if (oAuth2Config == null) return null

    return buildJsonObject {
      putJsonObject("authentication") {
        put("preferredSecurityScheme", "oauth2")
        putJsonObject("oAuth2") {
          put("clientId", oAuth2Config.clientId)
          putJsonArray("scopes") {
            oAuth2Config.scopes.keys.forEach { add(JsonPrimitive(it)) }
          }
        }
      }
    }.toString()
  }

  private fun String.htmlAttributeEscape(): String =
    replace("&", "&amp;").replace("\"", "&quot;").replace("<", "&lt;").replace(">", "&gt;")

  private fun String.htmlAttributeEscapeSingleQuote(): String =
    replace("&", "&amp;").replace("'", "&#39;").replace("<", "&lt;").replace(">", "&gt;")

  private fun String.htmlTextEscape(): String =
    replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
}
