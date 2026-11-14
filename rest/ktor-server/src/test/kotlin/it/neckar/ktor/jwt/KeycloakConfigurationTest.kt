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
import assertk.assertions.messageContains
import it.neckar.open.http.Url
import it.neckar.rest.jwt.JwtAudience
import org.junit.jupiter.api.Test

class KeycloakConfigurationTest {
  private fun configurationFor(realmUrl: String): KeycloakConfiguration =
    KeycloakConfiguration(realmUrl = Url.Absolute(realmUrl), clientId = JwtAudience("patterns.neckar.it"))

  /**
   * The realm name names the seed resource and the background job, so a wrong one is a service that
   * silently starts without keys.
   */
  @Test
  fun `takes the realm name from the last segment of the realm url`() {
    assertThat(configurationFor("https://auth.neckar.it/realms/internal").realmName).isEqualTo("internal")
    assertThat(configurationFor("https://auth.neckar.it/realms/customers/").realmName).isEqualTo("customers")
  }

  @Test
  fun `derives the jwks endpoint and the issuer from the realm url`() {
    val configuration = configurationFor("https://auth.neckar.it/realms/internal")

    assertThat(configuration.jwksUrl.value).isEqualTo("https://auth.neckar.it/realms/internal/protocol/openid-connect/certs")
    assertThat(configuration.issuer).isEqualTo("https://auth.neckar.it/realms/internal")
  }

  /**
   * All three derivations, not just the realm name: an issuer carrying the slash never equals the
   * `iss` Keycloak mints, and the JWKS URL would carry a doubled separator.
   */
  @Test
  fun `a trailing slash on the realm url changes none of the derived values`() {
    val withSlash = configurationFor("https://auth.neckar.it/realms/internal/")
    val withoutSlash = configurationFor("https://auth.neckar.it/realms/internal")

    assertThat(withSlash.issuer).isEqualTo(withoutSlash.issuer)
    assertThat(withSlash.jwksUrl.value).isEqualTo(withoutSlash.jwksUrl.value)
    assertThat(withSlash.realmName).isEqualTo(withoutSlash.realmName)
  }

  /**
   * The realm URL comes from a command-line option or an environment variable. Everything past the
   * realm path used to end up inside the realm name, which names the seed resource — a service that
   * looks configured, finds no seed and validates no token.
   */
  @Test
  fun `refuses a realm url that is not one`() {
    listOf(
      "https://auth.neckar.it/realms/internal?redirect=x",
      "https://auth.neckar.it/realms/internal#fragment",
      "https://auth.neckar.it",
      "https://auth.neckar.it/auth/internal",
    ).forEach { rejected ->
      assertFailure { configurationFor(rejected) }
        .isInstanceOf(IllegalArgumentException::class)
        .messageContains("realms")
    }
  }

  @Test
  fun `accepts the shapes a real deployment writes`() {
    //A context path, a port, an IPv6 literal, and a scheme in the case RFC 3986 also allows.
    assertThat(configurationFor("https://auth.neckar.it/auth/realms/internal").realmName).isEqualTo("internal")
    assertThat(configurationFor("http://localhost:8080/realms/internal").realmName).isEqualTo("internal")
    assertThat(configurationFor("http://[::1]:8080/realms/internal").realmName).isEqualTo("internal")
    assertThat(configurationFor("HTTPS://auth.neckar.it/realms/internal").realmName).isEqualTo("internal")
  }
}
