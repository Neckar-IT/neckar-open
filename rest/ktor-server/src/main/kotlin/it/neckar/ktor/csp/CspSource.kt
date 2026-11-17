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

import it.neckar.html.csp.CspNonce

/**
 * One entry in the source list of a [CspDirective] — where this kind of resource may come from.
 *
 * A sum type rather than a string because [Nonce] renders differently per response, which should
 * be visible in the type instead of hidden in a string someone must remember to substitute.
 */
sealed interface CspSource {
  /**
   * Renders this source as it appears in the policy header of a response with the given [nonce].
   */
  fun render(nonce: CspNonce): String

  /**
   * The document's own origin: `'self'`.
   */
  data object Self : CspSource {
    override fun render(nonce: CspNonce): String = "'self'"
  }

  /**
   * The nonce of the response being rendered: `'nonce-…'`. Use it in a directive whose resources
   * the response embeds inline; the matching `nonce` attribute comes from [cspNonce].
   */
  data object Nonce : CspSource {
    override fun render(nonce: CspNonce): String = nonce.asSourceExpression()
  }

  /**
   * Inline content of any kind: `'unsafe-inline'`. Last resort, and only for
   * [CspDirective.StyleSrc] — in `script-src` it gives up on XSS, which is the point of a policy.
   *
   * In `style-src` it is sometimes unavoidable: a `style="…"` attribute cannot carry a nonce by
   * specification. CSS can still only exfiltrate by fetching, which the fetch directives govern.
   */
  data object UnsafeInline : CspSource {
    override fun render(nonce: CspNonce): String = "'unsafe-inline'"
  }

  /**
   * Data URIs: `data:`. For documents that carry their resources rather than reference them — an
   * image inlined as base64, a font a bundler chose to embed.
   */
  data object Data : CspSource {
    override fun render(nonce: CspNonce): String = "data:"
  }

  /**
   * Somewhere other than this origin, as a CSP source expression: `https://use.typekit.net`.
   *
   * Name the narrowest thing that works: a bare host on a package CDN allows every package that CDN
   * carries. Adding the path narrows it — but only until a redirect, after which CSP matches the
   * host alone.
   */
  @JvmInline
  value class Origin(val source: String) : CspSource {
    init {
      require(source.isNotBlank()) { "CspSource.Origin must not be blank" }
    }

    override fun render(nonce: CspNonce): String = source
  }
}
