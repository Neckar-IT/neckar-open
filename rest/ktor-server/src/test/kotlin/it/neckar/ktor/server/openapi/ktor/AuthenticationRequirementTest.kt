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
package it.neckar.ktor.server.openapi.ktor

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.auth0.jwt.JWT
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.testing.*
import it.neckar.ktor.installJson
import it.neckar.ktor.AuthenticationRequirement
import it.neckar.ktor.jwt.AuthProviderName
import it.neckar.ktor.jwt.JwtSigningKey
import it.neckar.ktor.jwt.addJwtHeaderAuth
import it.neckar.ktor.jwt.withAudience
import it.neckar.ktor.jwt.withClaim
import it.neckar.ktor.metaRouting
import it.neckar.open.http.Url
import it.neckar.open.test.utils.DisableLogging
import it.neckar.projects.common.Port
import it.neckar.projects.common.ServiceDescriptor
import it.neckar.rest.jwt.JwtAudience
import it.neckar.rest.jwt.JwtTokenClaim
import it.neckar.rest.jwt.JwtTokenUsageType
import org.junit.jupiter.api.Test
import java.time.Instant

/**
 * Verifies the deny-by-default posture of [AuthenticationRequirement.Required]:
 * the OpenAPI-doc and meta diagnostics endpoints require an access token, while the
 * liveness/health whitelist (consumed anonymously by Docker healthchecks and monitoring)
 * stays public.
 */
@DisableLogging
class AuthenticationRequirementTest {
  private val audience = JwtAudience("DocEndpointAuthTest")
  private val signingKey = JwtSigningKey.HMAC256("doc-endpoint-auth-test-secret")
  private val accessTokenProvider = AuthProviderName("accessToken")
  private val baseUrl = Url.pattern("api/test")

  private val serviceDescriptor: ServiceDescriptor = ServiceDescriptor(
    serviceName = "test",
    apiPrefix = "api/test",
    port = Port(9099),
  )

  private fun ApplicationTestBuilder.installProtectedService() {
    application {
      installJson { }
      install(Authentication) {
        addJwtHeaderAuth(name = accessTokenProvider.value, audience = audience, tokenUsageType = JwtTokenUsageType.Access) { signingKey }
      }
      metaRouting(serviceDescriptor, baseUrl, authenticationRequirement = AuthenticationRequirement.Required(accessTokenProvider))
      install(OpenApiPlugin) {
        serviceDescriptor(serviceDescriptor)
        authenticationRequirement = AuthenticationRequirement.Required(accessTokenProvider)
      }
    }
  }

  private fun accessToken(): String {
    val now = Instant.now()
    return JWT.create()
      .withIssuedAt(now)
      .withSubject("doc-endpoint-auth-test")
      .withAudience(audience)
      .withClaim(JwtTokenClaim.TokenUsageType, JwtTokenUsageType.Access)
      .withNotBefore(now)
      .withExpiresAt(now.plusSeconds(600))
      .sign(signingKey)
  }

  @Test
  fun `doc and dump endpoints reject anonymous requests`() = testApplication {
    installProtectedService()
    startApplication()

    assertThat(client.get("/api/test/doc/openapi").status).isEqualTo(HttpStatusCode.Unauthorized)
    assertThat(client.get("/api/test/doc/openapi/admin").status).isEqualTo(HttpStatusCode.Unauthorized)
    assertThat(client.get("/api/test/doc/api").status).isEqualTo(HttpStatusCode.Unauthorized)
    assertThat(client.get("/api/test/dump-routes").status).isEqualTo(HttpStatusCode.Unauthorized)
    assertThat(client.get("/api/test/dump-headers").status).isEqualTo(HttpStatusCode.Unauthorized)
    assertThat(client.get("/api/test/runtime-context").status).isEqualTo(HttpStatusCode.Unauthorized)
    assertThat(client.get("/api/test/version").status).isEqualTo(HttpStatusCode.Unauthorized)
  }

  @Test
  fun `liveness and health whitelist stays public`() = testApplication {
    installProtectedService()
    startApplication()

    assertThat(client.get("/api/test/global-health").status).isEqualTo(HttpStatusCode.OK)
    assertThat(client.get("/api/test/meta-tags").status).isEqualTo(HttpStatusCode.OK)
    assertThat(client.get("/api/test/heartbeat").status).isEqualTo(HttpStatusCode.OK)
  }

  @Test
  fun `doc and dump endpoints accept a valid access token`() = testApplication {
    installProtectedService()
    startApplication()

    val token = accessToken()
    assertThat(client.get("/api/test/doc/openapi") { bearerAuth(token) }.status).isEqualTo(HttpStatusCode.OK)
    assertThat(client.get("/api/test/dump-routes") { bearerAuth(token) }.status).isEqualTo(HttpStatusCode.OK)
  }
}
