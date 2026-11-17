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
package it.neckar.ktor.jwt.jwks

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.messageContains
import com.auth0.jwk.SigningKeyNotFoundException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class HeldJwkProviderTest {
  private val key = JwksTestKey.generate("held-key")

  /**
   * Answers nothing and counts every call: a provider that reads only the held set must never ask it.
   */
  private class NeverAskedSource : JwksSource {
    var fetchCount: Int = 0

    override suspend fun fetch(): JwksDocument {
      fetchCount++
      throw AssertionError("the request path fetched")
    }
  }

  private val source = NeverAskedSource()

  private val manager = JwksKeyManager(
    realmName = "internal",
    source = source,
    prefilter = JwksRefreshPrefilter(issuer = "https://auth.neckar.it/realms/internal", audience = "patterns.neckar.it"),
    seed = JwksSeed { jwksDocumentOf(key) },
  )

  private val provider = HeldJwkProvider(manager)

  @Test
  fun `answers from the held set without fetching`() {
    manager.loadHeldSet()

    assertThat(provider.get(key.keyId.value).publicKey).isEqualTo(key.publicKey)
    assertThat(source.fetchCount).isEqualTo(0)
  }

  @Test
  fun `an unknown kid throws and names the held set, it does not fetch`() {
    manager.loadHeldSet()

    val thrown = assertThrows<SigningKeyNotFoundException> { provider.get("someone-elses-key") }

    assertThat(thrown).messageContains("someone-elses-key")
    assertThat(thrown).messageContains("seed key set")
    assertThat(source.fetchCount).isEqualTo(0)
  }

  @Test
  fun `without a held set it says so rather than reaching for the realm`() {
    val thrown = assertThrows<SigningKeyNotFoundException> { provider.get(key.keyId.value) }

    assertThat(thrown).messageContains("no key set at all")
    assertThat(source.fetchCount).isEqualTo(0)
  }

  @Test
  fun `a token without a kid throws`() {
    manager.loadHeldSet()

    assertThrows<SigningKeyNotFoundException> { provider.get(null) }
  }
}
