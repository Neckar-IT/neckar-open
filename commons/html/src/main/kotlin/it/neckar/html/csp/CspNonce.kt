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
package it.neckar.html.csp

import java.security.SecureRandom
import java.util.Base64

/**
 * A Content Security Policy nonce — marks an inline `<script>` or `<style>` block as one the server
 * put there, as opposed to one an injection put there.
 *
 * The browser runs an inline block only if its `nonce` attribute matches a `'nonce-…'` source in the
 * policy header. That only holds while the value is unpredictable and never reused — hence [random]
 * per response.
 *
 * Lives beside [it.neckar.html.HtmlInlineSupport] rather than in the Ktor server module, because the
 * inlining is what creates the blocks the nonce marks: the policy machinery
 * (`it.neckar.ktor.csp.CspPolicy`, `ContentSecurityPolicyPlugin`) depends on this type, not the other
 * way round.
 */
@JvmInline
value class CspNonce(val value: String) {
  init {
    require(value.isNotBlank()) { "CspNonce must not be blank" }
  }

  /**
   * Renders the nonce as it appears in the policy header: `'nonce-cnRhbmRvbUJ5dGVzMTIzNA=='`.
   */
  fun asSourceExpression(): String = "'nonce-$value'"

  companion object {
    /**
     * 128 bit, the length the CSP specification requires for a nonce.
     */
    private const val ByteCount: Int = 16

    private val secureRandom: SecureRandom = SecureRandom()

    /**
     * Creates a fresh nonce from a cryptographically secure source. Call once per response.
     */
    fun random(): CspNonce {
      val bytes = ByteArray(ByteCount)
      secureRandom.nextBytes(bytes)
      return CspNonce(Base64.getEncoder().encodeToString(bytes))
    }
  }
}
