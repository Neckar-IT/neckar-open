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

import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import assertk.assertions.messageContains
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.interfaces.DecodedJWT
import it.neckar.rest.jwt.JwtAudience
import org.junit.jupiter.api.Test
import java.util.Date
import kotlin.time.Duration.Companion.hours

class JwksRefreshPrefilterTest {
  private val ourRealm = "https://auth.neckar.it/realms/internal"
  private val ourClient = "patterns.neckar.it"
  private val ourKey = JwksTestKey.generate("rotated-key")

  private val prefilter = JwksRefreshPrefilter(issuer = ourRealm, audience = ourClient)

  @Test
  fun `a token of our realm and our client that has not expired is worth a refresh`() {
    assertThat(prefilter.worthARefresh(token())).isTrue()
  }

  @Test
  fun `a token of a foreign realm is not`() {
    assertThat(prefilter.worthARefresh(token(issuer = "https://auth.example.com/realms/internal"))).isFalse()
  }

  @Test
  fun `a token for another client is not`() {
    assertThat(prefilter.worthARefresh(token(audience = "someone-else.neckar.it"))).isFalse()
  }

  @Test
  fun `a service of several clients takes a token of each and of no other`() {
    val gateway = JwksRefreshPrefilter(issuer = ourRealm, audiences = setOf(JwtAudience(ourClient), JwtAudience("mea.neckar.it")))

    assertThat(gateway.worthARefresh(token(audience = "mea.neckar.it"))).isTrue()
    assertThat(gateway.worthARefresh(token(audience = ourClient))).isTrue()
    assertThat(gateway.worthARefresh(token(audience = "someone-else.neckar.it"))).isFalse()
  }

  @Test
  fun `a prefilter without audience fails naming the realm`() {
    assertFailure { JwksRefreshPrefilter(issuer = ourRealm, audiences = emptySet()) }.messageContains(ourRealm)
  }

  @Test
  fun `an expired token is not`() {
    assertThat(prefilter.worthARefresh(token(expiresIn = (-1).hours))).isFalse()
  }

  @Test
  fun `a token signed with an algorithm the realm does not use is not`() {
    val hmac = JWT.create()
      .withKeyId(ourKey.keyId.value)
      .withIssuer(ourRealm)
      .withAudience(ourClient)
      .withExpiresAt(Date(System.currentTimeMillis() + 1.hours.inWholeMilliseconds))
      .sign(Algorithm.HMAC256("not-the-realms-key"))

    assertThat(prefilter.worthARefresh(JWT.decode(hmac))).isFalse()
  }

  private fun token(
    issuer: String = ourRealm,
    audience: String = ourClient,
    expiresIn: kotlin.time.Duration = 1.hours,
  ): DecodedJWT {
    val signed = JWT.create()
      .withKeyId(ourKey.keyId.value)
      .withIssuer(issuer)
      .withAudience(audience)
      .withExpiresAt(Date(System.currentTimeMillis() + expiresIn.inWholeMilliseconds))
      .sign(Algorithm.RSA256(ourKey.publicKey, ourKey.privateKey))

    return JWT.decode(signed)
  }
}
