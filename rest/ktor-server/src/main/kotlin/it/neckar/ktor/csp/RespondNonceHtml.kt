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

import io.ktor.http.CacheControl
import io.ktor.http.ContentType
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.cacheControl
import io.ktor.server.response.respondText
import it.neckar.html.csp.CspNonce

/**
 * Responds with HTML whose inline blocks carry the response's [CspNonce], and marks it `no-store`.
 *
 * A stored nonce is a reused nonce: whoever saw the document once knows the value an injected
 * `<script>` has to carry to be executed, and the policy no longer separates our markup from
 * injected markup. `no-store` keeps browsers and intermediaries from serving the same one twice.
 *
 * Use this instead of `respondText(html, ContentType.Text.Html)` wherever the body embeds a nonce.
 */
suspend fun ApplicationCall.respondNonceHtml(html: String) {
  response.cacheControl(CacheControl.NoStore(null))
  respondText(html, contentType = ContentType.Text.Html)
}
