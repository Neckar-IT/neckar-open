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
package it.neckar.ktor.jwt

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isEqualTo
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTVerificationException
import it.neckar.rest.jwt.JwtToken
import it.neckar.rest.jwt.JwtTokenClaim
import it.neckar.rest.jwt.JwtTokenUsageType
import org.junit.jupiter.api.Test
import java.time.Instant

/**
 * Tests for [buildTokenVerificationFailureMessage]
 */
class BuildTokenVerificationFailureMessageTest {

  private val algorithm: Algorithm = Algorithm.HMAC256("test-secret")

  @Test
  fun `refresh token used as access token shows mismatch`() {
    val token = createToken(JwtTokenUsageType.Refresh)
    val cause = JWTVerificationException("The Claim 'tu' value doesn't match the required one.")

    val message = buildTokenVerificationFailureMessage(token, JwtTokenUsageType.Access, cause)

    assertThat(message).isEqualTo("Access Token validation failed: Token usage type mismatch - expected 'Access', got 'Refresh'")
  }

  @Test
  fun `access token used as refresh token shows mismatch`() {
    val token = createToken(JwtTokenUsageType.Access)
    val cause = JWTVerificationException("The Claim 'tu' value doesn't match the required one.")

    val message = buildTokenVerificationFailureMessage(token, JwtTokenUsageType.Refresh, cause)

    assertThat(message).isEqualTo("Refresh Token validation failed: Token usage type mismatch - expected 'Refresh', got 'Access'")
  }

  @Test
  fun `missing tu claim falls back to generic message`() {
    val tokenString = JWT.create()
      .withExpiresAt(Instant.now().plusSeconds(3600))
      .sign(algorithm)
    val token = JwtToken(tokenString)
    val cause = JWTVerificationException("The Claim 'tu' value doesn't match the required one.")

    val message = buildTokenVerificationFailureMessage(token, JwtTokenUsageType.Access, cause)

    assertThat(message).isEqualTo("Access Token validation failed: The Claim 'tu' value doesn't match the required one.")
  }

  @Test
  fun `matching usage type falls back to generic message`() {
    val token = createToken(JwtTokenUsageType.Access)
    val cause = JWTVerificationException("Token has expired.")

    val message = buildTokenVerificationFailureMessage(token, JwtTokenUsageType.Access, cause)

    assertThat(message).isEqualTo("Access Token validation failed: Token has expired.")
  }

  @Test
  fun `invalid token falls back to generic message`() {
    val token = JwtToken("not-a-valid-jwt")
    val cause = JWTVerificationException("Invalid token.")

    val message = buildTokenVerificationFailureMessage(token, JwtTokenUsageType.Access, cause)

    assertThat(message).contains("Access Token validation failed:")
  }

  private fun createToken(usageType: JwtTokenUsageType): JwtToken {
    val tokenString = JWT.create()
      .withClaim(JwtTokenClaim.TokenUsageType.key.value, JwtTokenClaim.TokenUsageType.serialize(usageType))
      .withExpiresAt(Instant.now().plusSeconds(3600))
      .sign(algorithm)
    return JwtToken(tokenString)
  }
}
