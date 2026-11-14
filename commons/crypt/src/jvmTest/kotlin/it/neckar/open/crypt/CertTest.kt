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
package it.neckar.open.crypt

import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNotEqualTo
import assertk.assertions.isTrue
import it.neckar.open.resources.getResourceSafe
import org.junit.jupiter.api.Test
import java.nio.charset.StandardCharsets

class CertTest {
  /**
   * Confidentiality round-trip: encrypt with the public certificate, decrypt with the private key.
   */
  @Test
  fun `cipher then decipher round-trips the plaintext`() {
    val support = X509Support(javaClass.getResourceSafe("/test.crt"), javaClass.getResourceSafe("/test.der"))
    val roundTripped = support.decipher(support.cipher(Plaintext.toByteArray(StandardCharsets.UTF_8)))
    assertThat(String(roundTripped, StandardCharsets.UTF_8)).isEqualTo(Plaintext)
  }

  /**
   * Encryption only needs the public certificate — anyone holding the certificate can encrypt.
   */
  @Test
  fun `cipher works with the certificate alone`() {
    val certificateOnly = X509Support(javaClass.getResourceSafe("/test.crt"))
    val plainText = Plaintext.toByteArray(StandardCharsets.UTF_8)
    assertThat(certificateOnly.cipher(plainText).toList()).isNotEqualTo(plainText.toList())
  }

  /**
   * Decryption requires the private key — the distributed certificate must not be able to decipher.
   * This is what makes [X509Support.cipher] provide confidentiality.
   */
  @Test
  fun `decipher requires the private key`() {
    val certificateOnly = X509Support(javaClass.getResourceSafe("/test.crt"))
    val cipherText = X509Support(javaClass.getResourceSafe("/test.crt"), javaClass.getResourceSafe("/test.der"))
      .cipher(Plaintext.toByteArray(StandardCharsets.UTF_8))
    assertFailure { certificateOnly.decipher(cipherText) }
  }

  /**
   * OAEP padding is randomized: encrypting the same plaintext twice yields different cipher texts.
   */
  @Test
  fun `cipher is randomized`() {
    val support = X509Support(javaClass.getResourceSafe("/test.crt"))
    val plainText = Plaintext.toByteArray(StandardCharsets.UTF_8)
    assertThat(support.cipher(plainText).toList()).isNotEqualTo(support.cipher(plainText).toList())
  }

  @Test
  fun `sign then verify succeeds`() {
    val support = X509Support(javaClass.getResourceSafe("/test.crt"), javaClass.getResourceSafe("/test.der"))
    val signature = support.sign(Plaintext.toByteArray(StandardCharsets.UTF_8))
    assertThat(support.verifySignature(Plaintext.toByteArray(StandardCharsets.UTF_8), signature)).isTrue()

    val certificateOnly = X509Support(javaClass.getResourceSafe("/test.crt"))
    assertThat(certificateOnly.verifySignature(Plaintext.toByteArray(StandardCharsets.UTF_8), signature)).isTrue()
  }

  @Test
  fun `verifySignature rejects a signature for different content`() {
    val support = X509Support(javaClass.getResourceSafe("/test.crt"), javaClass.getResourceSafe("/test.der"))
    val signature = support.sign(Plaintext.toByteArray(StandardCharsets.UTF_8))
    assertThat(support.verifySignature("tampered".toByteArray(StandardCharsets.UTF_8), signature)).isFalse()
  }

  companion object {
    private const val Plaintext = "Klartext"
  }
}
