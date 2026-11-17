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

import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.containsExactlyInAnyOrder
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isNull
import assertk.assertions.prop
import com.auth0.jwt.JWT
import com.auth0.jwt.JWTCreator
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.interfaces.Payload
import org.junit.jupiter.api.Test

class JwtClaimTextAccessTest {

  @Test
  fun `a required claim answers its trimmed text`() {
    val payload = payloadOf { withClaim(KeycloakClaim.Email, "  js@neckar.it ") }

    assertThat(payload.getClaimValue(KeycloakClaim.Email)).isEqualTo("js@neckar.it")
  }

  @Test
  fun `a claim the token never carried names the claims that did arrive`() {
    val payload = payloadOf { withClaim(KeycloakClaim.GivenName, "Johannes") }

    val failure = assertFailure { payload.getClaimValue(KeycloakClaim.Email) }
      .isInstanceOf(IncompleteIdentityException.ClaimAbsent::class)

    failure.prop(IncompleteIdentityException.ClaimAbsent::claimName).isEqualTo(KeycloakClaim.Email)
    failure.prop(IncompleteIdentityException.ClaimAbsent::claimsInToken)
      .containsExactlyInAnyOrder(KeycloakClaim.GivenName, "sub")
  }

  @Test
  fun `blank text and a JSON null are the same empty claim`() {
    assertFailure { payloadOf { withClaim(KeycloakClaim.Email, "   ") }.getClaimValue(KeycloakClaim.Email) }
      .isInstanceOf(IncompleteIdentityException.ClaimBlank::class)

    assertFailure { payloadOf { withNullClaim(KeycloakClaim.Email) }.getClaimValue(KeycloakClaim.Email) }
      .isInstanceOf(IncompleteIdentityException.ClaimBlank::class)
  }

  @Test
  fun `a claim holding something other than text is told apart from a missing one`() {
    // asString() answers null for both, which is what made a broken protocol mapper look like a
    // missing scope.
    assertFailure { payloadOf { withClaim(KeycloakClaim.Email, 42) }.getClaimValue(KeycloakClaim.Email) }
      .isInstanceOf(IncompleteIdentityException.ClaimNotAString::class)
      .prop(IncompleteIdentityException.ClaimNotAString::claimName).isEqualTo(KeycloakClaim.Email)
  }

  @Test
  fun `an optional claim answers null for every way of not being there`() {
    assertThat(payloadOf { }.getClaimValueOrNull(KeycloakClaim.GivenName)).isNull()
    assertThat(payloadOf { withNullClaim(KeycloakClaim.GivenName) }.getClaimValueOrNull(KeycloakClaim.GivenName)).isNull()
    assertThat(payloadOf { withClaim(KeycloakClaim.GivenName, "  ") }.getClaimValueOrNull(KeycloakClaim.GivenName)).isNull()
  }

  @Test
  fun `an optional claim of the wrong type still throws`() {
    assertFailure { payloadOf { withClaim(KeycloakClaim.GivenName, 42) }.getClaimValueOrNull(KeycloakClaim.GivenName) }
      .isInstanceOf(IncompleteIdentityException.ClaimNotAString::class)
  }

  private fun payloadOf(claims: JWTCreator.Builder.() -> Unit): Payload {
    val token = JWT.create()
      .withSubject("johannes")
      .apply(claims)
      .sign(Algorithm.HMAC256("test-secret"))

    return JWT.decode(token)
  }
}
