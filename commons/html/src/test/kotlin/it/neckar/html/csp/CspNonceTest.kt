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

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.Base64

/**
 * A nonce is a security control, and every property it rests on is a property nothing else checks:
 * the value reaches the header verbatim, it is long enough to be unguessable, and it is never the
 * same twice. `ContentSecurityPolicyTest` in `rest/ktor-server` covers the plugin that hands nonces
 * out; this covers the type itself.
 */
class CspNonceTest {
  @Test
  fun testSourceExpression() {
    assertThat(CspNonce("cnRhbmRvbUJ5dGVzMTIzNA==").asSourceExpression())
      .isEqualTo("'nonce-cnRhbmRvbUJ5dGVzMTIzNA=='")
  }

  /**
   * An empty `nonce=""` attribute matches nothing, so a blank nonce silently turns every inline
   * block it marks into a blocked one.
   */
  @Test
  fun testBlankIsRejected() {
    assertThrows<IllegalArgumentException> { CspNonce("") }.also {
      assertThat(it.message).isNotNull().contains("blank")
    }
    assertThrows<IllegalArgumentException> { CspNonce("   ") }
  }

  /**
   * 128 bit, the length the CSP specification requires — Base64 of 16 bytes.
   */
  @Test
  fun testRandomIsLongEnough() {
    assertThat(Base64.getDecoder().decode(CspNonce.random().value).toList()).hasSize(16)
  }

  @Test
  fun testRandomNeverRepeats() {
    val nonces = List(1000) { CspNonce.random().value }

    assertThat(nonces.toSet()).hasSize(nonces.size)
  }
}
