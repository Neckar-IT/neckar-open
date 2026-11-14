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
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.prop
import com.auth0.jwt.JWT
import com.auth0.jwt.JWTCreator
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.server.auth.jwt.JWTPrincipal
import it.neckar.contact.EmailAddress
import it.neckar.user.UserLoginName
import org.junit.jupiter.api.Test

/**
 * That the three claims are demanded at all is what this covers; how a claim can fail to be text
 * belongs to [JwtClaimValueAccessTest] next to the extraction itself.
 */
class JwtUserExtractionTest {
  @Test
  fun `a token with every claim yields the user behind it`() {
    val user = principal().extractUser()

    assertThat(user.loginName).isEqualTo(UserLoginName("johannes"))
    assertThat(user.firstName).isEqualTo("Johannes")
    assertThat(user.lastName).isEqualTo("Schneider")
    assertThat(user.email).isEqualTo(EmailAddress("js@neckar.it"))
  }

  @Test
  fun `the id of an HMAC token is derived from its login name and stays the same`() {
    assertThat(principal().extractUser().id).isEqualTo(principal().extractUser().id)
  }

  @Test
  fun `a token without an email claim is rejected`() {
    assertFailure { principal(email = null).extractUser() }
      .isInstanceOf(IncompleteIdentityException::class)
      .prop(IncompleteIdentityException::claimName).isEqualTo(KeycloakClaim.Email)
  }

  @Test
  fun `a token with a blank given name is rejected`() {
    assertFailure { principal(givenName = "  ").extractUser() }
      .isInstanceOf(IncompleteIdentityException::class)
      .prop(IncompleteIdentityException::claimName).isEqualTo(KeycloakClaim.GivenName)
  }

  @Test
  fun `a token without a family name claim is rejected`() {
    assertFailure { principal(familyName = null).extractUser() }
      .isInstanceOf(IncompleteIdentityException::class)
      .prop(IncompleteIdentityException::claimName).isEqualTo(KeycloakClaim.FamilyName)
  }

  private fun principal(
    email: String? = "js@neckar.it",
    givenName: String? = "Johannes",
    familyName: String? = "Schneider",
  ): JWTPrincipal = principalOf {
    email?.let { withClaim(KeycloakClaim.Email, it) }
    givenName?.let { withClaim(KeycloakClaim.GivenName, it) }
    familyName?.let { withClaim(KeycloakClaim.FamilyName, it) }
  }

  /** Carries the login name; every identity claim is the caller's to set, including none at all. */
  private fun principalOf(claims: JWTCreator.Builder.() -> Unit): JWTPrincipal {
    val token = JWT.create()
      .withSubject("johannes")
      .apply(claims)
      .sign(Algorithm.HMAC256("test-secret"))

    return JWTPrincipal(JWT.decode(token))
  }
}
