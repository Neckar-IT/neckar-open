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
import assertk.assertions.isEqualTo
import com.auth0.jwt.JWT
import io.ktor.client.request.cookie
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.authenticate
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import it.neckar.open.test.utils.DisableLogging
import it.neckar.rest.jwt.JwtAudience
import it.neckar.rest.jwt.JwtCookies
import it.neckar.rest.jwt.JwtTokenClaim
import it.neckar.rest.jwt.JwtTokenUsageType
import org.junit.jupiter.api.Test
import java.time.Instant
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.toJavaDuration

/**
 * Tests for [addJwtCookieAuth]: which cookie the token is read from, and the status a rejected
 * request is answered with.
 */
@DisableLogging
class JwtCookieAuthTest {
  private val signingKey: JwtSigningKey = JwtSigningKey.HMAC256("the-very-secret-secret")
  private val audience: JwtAudience = JwtAudience("the-audience")
  private val providerName: AuthProviderName = AuthProviderName("cookieAuth")

  /** Long enough that no test runs into the expiry; expiry itself is not what these tests are about. */
  private val tokenLifetime: Duration = 1.hours

  @Test
  fun `the access token is read from the access token cookie`() = testApplication {
    installGuardedRoute(JwtTokenUsageType.Access)

    val response = client.get("/guarded") {
      cookie(name = JwtCookies.accessToken.value, value = token(JwtTokenUsageType.Access))
    }

    assertThat(response.status).isEqualTo(HttpStatusCode.OK)
  }

  @Test
  fun `the refresh token is read from the refresh token cookie`() = testApplication {
    installGuardedRoute(JwtTokenUsageType.Refresh)

    val response = client.get("/guarded") {
      cookie(name = JwtCookies.refreshToken.value, value = token(JwtTokenUsageType.Refresh))
    }

    assertThat(response.status).isEqualTo(HttpStatusCode.OK)
  }

  /**
   * The two cookies are not interchangeable: the refresh token is confined to the refresh endpoint by
   * its path, so a guard expecting the access token must not fall back to it.
   */
  @Test
  fun `a refresh token in the refresh cookie does not open an access token route`() = testApplication {
    installGuardedRoute(JwtTokenUsageType.Access)

    val response = client.get("/guarded") {
      cookie(name = JwtCookies.refreshToken.value, value = token(JwtTokenUsageType.Refresh))
    }

    assertThat(response.status).isEqualTo(HttpStatusCode.Unauthorized)
  }

  /**
   * 401, not 403 — a client refreshes its access token on a 401 and ends the session on a 403, so the
   * status decides whether an expired token costs the user their session.
   */
  @Test
  fun `a request without a cookie is answered 401`() = testApplication {
    installGuardedRoute(JwtTokenUsageType.Access)

    val response = client.get("/guarded")

    assertThat(response.status).isEqualTo(HttpStatusCode.Unauthorized)
  }

  @Test
  fun `a token of the wrong usage type is answered 401`() = testApplication {
    installGuardedRoute(JwtTokenUsageType.Access)

    val response = client.get("/guarded") {
      cookie(name = JwtCookies.accessToken.value, value = token(JwtTokenUsageType.Refresh))
    }

    assertThat(response.status).isEqualTo(HttpStatusCode.Unauthorized)
  }

  @Test
  fun `a token signed with another key is answered 401`() = testApplication {
    installGuardedRoute(JwtTokenUsageType.Access)

    val response = client.get("/guarded") {
      cookie(name = JwtCookies.accessToken.value, value = token(JwtTokenUsageType.Access, JwtSigningKey.HMAC256("another-secret")))
    }

    assertThat(response.status).isEqualTo(HttpStatusCode.Unauthorized)
  }

  private fun ApplicationTestBuilder.installGuardedRoute(tokenUsageType: JwtTokenUsageType) {
    application {
      install(Authentication) {
        addJwtCookieAuth(name = providerName.value, audience = audience, tokenUsageType = tokenUsageType) { signingKey }
      }

      routing {
        authenticate(providerName.value) {
          get("/guarded") { call.respondText("the guarded resource") }
        }
      }
    }
  }

  private fun token(tokenUsageType: JwtTokenUsageType, key: JwtSigningKey = signingKey): String {
    return JWT.create()
      .withAudience(audience.value)
      .withClaim(JwtTokenClaim.TokenUsageType.key.value, JwtTokenClaim.TokenUsageType.serialize(tokenUsageType))
      .withExpiresAt(Instant.now().plus(tokenLifetime.toJavaDuration()))
      .sign(key)
  }

}
