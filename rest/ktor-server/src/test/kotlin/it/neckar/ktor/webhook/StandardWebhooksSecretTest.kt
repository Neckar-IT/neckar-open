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
package it.neckar.ktor.webhook

import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import org.junit.jupiter.api.Test
import java.util.Base64

class StandardWebhooksSecretTest {

  @Test
  fun `rejects a secret without the whsec prefix`() {
    assertFailure { StandardWebhooksSecret("MfKQ9r8GKYqrTwjUPD8ILPZIo2LaLaSw") }.isInstanceOf<IllegalArgumentException>()
  }

  @Test
  fun `accepts a secret whose key is 24 bytes`() {
    assertThat(StandardWebhooksSecret(secretOfKeyLength(24)).value).isEqualTo(secretOfKeyLength(24))
  }

  @Test
  fun `rejects a secret whose key is 23 bytes`() {
    assertFailure { StandardWebhooksSecret(secretOfKeyLength(23)) }.isInstanceOf<IllegalArgumentException>()
  }

  @Test
  fun `accepts a secret whose key is 64 bytes`() {
    assertThat(StandardWebhooksSecret(secretOfKeyLength(64)).value).isEqualTo(secretOfKeyLength(64))
  }

  @Test
  fun `rejects a secret whose key is 65 bytes`() {
    assertFailure { StandardWebhooksSecret(secretOfKeyLength(65)) }.isInstanceOf<IllegalArgumentException>()
  }

  @Test
  fun `rejects a secret that is no base64`() {
    assertFailure { StandardWebhooksSecret("whsec_not base64!") }.isInstanceOf<IllegalArgumentException>()
  }

  @Test
  fun `hides the secret in toString`() {
    assertThat(StandardWebhooksSecret("whsec_MfKQ9r8GKYqrTwjUPD8ILPZIo2LaLaSw").toString()).isEqualTo("whsec_***")
  }

  private fun secretOfKeyLength(bytes: Int): String = StandardWebhooksSecret.Prefix + Base64.getEncoder().encodeToString(ByteArray(bytes))
}
