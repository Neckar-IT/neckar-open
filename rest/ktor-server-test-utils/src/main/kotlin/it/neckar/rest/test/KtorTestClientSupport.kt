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
package it.neckar.rest.test

import io.ktor.client.*
import io.ktor.client.engine.*
import assertk.assertThat
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.testing.*
import it.neckar.common.featureflags.FeatureFlags
import it.neckar.ktor.KtorRoutes
import it.neckar.ktor.KtorService
import it.neckar.ktor.client.installProtocolVersionHeader
import it.neckar.open.version.VersionInformation
import it.neckar.ktor.configure
import it.neckar.ktor.configureFor
import it.neckar.ktor.installFeatureFlagSupport
import it.neckar.ktor.installJson
import it.neckar.ktor.putKtorService
import it.neckar.ktor.jwt.JwtConfiguration
import it.neckar.open.tenancy.TenantDescriptor
import it.neckar.rest.jwt.JwtCookies
import it.neckar.rest.jwt.JwtToken
import it.neckar.rest.jwt.appendJwtTokenBearer
import it.neckar.user.UserLoginName
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule


/**
 * Sets up the client with the default settings for the test client
 */
fun HttpClientConfig<out HttpClientEngineConfig>.setupTestClient(
  /**
   * The serializers module that is used to read/write json
   */
  serializersModule: SerializersModule = SerializersModule { },
  /**
   * If provided, every request automatically appends the JWT token
   */
  jwtConfiguration: JwtConfiguration? = null,
  /**
   * The tenant the automatically appended access token belongs to.
   * Only relevant if [jwtConfiguration] is provided.
   */
  tenantDescriptor: TenantDescriptor = TestTenantDescriptor,
  /**
   * The user the automatically appended access token names. Services that check capabilities
   * resolve this login name against their user store, so such a test authenticates as a user it
   * has provisioned rather than as the default.
   * Only relevant if [jwtConfiguration] is provided.
   */
  loginName: UserLoginName = UserLoginName("test"),
) {
  expectSuccess = true

  installProtocolVersionHeader { VersionInformation.gitHash }

  install(ContentNegotiation) {
    json(
      Json {
        this.serializersModule = serializersModule
      }
    )
  }

  if (jwtConfiguration != null) {
    //If a jwtConfiguration is provided, we automatically append the access token to every request
    installAppendAccessToken(createAccessToken(jwtConfiguration, loginName = loginName, tenantDescriptor = tenantDescriptor))
  }
}

/**
 * Installs the [DefaultRequest] plugin that appends the access token to every request — as the
 * access-token cookie and as a bearer token, the pair a browser sends.
 *
 * This is useful for tests that require authentication.
 */
fun HttpClientConfig<out HttpClientEngineConfig>.installAppendAccessToken(accessToken: JwtToken) {
  install(DefaultRequest) {
    headers {
      appendJwtTokenBearer(accessToken)
    }
    accessTokenCookie(accessToken)
  }
}

/**
 * Sends [accessToken] in the access-token cookie — the credential a browser carries to every guarded
 * route.
 */
fun HttpMessageBuilder.accessTokenCookie(accessToken: JwtToken) {
  cookie(name = JwtCookies.accessToken.value, value = accessToken.token)
}

/**
 * Sends [accessToken] in the access-token cookie, for the callers that hold a [HeadersBuilder] —
 * it is no [HttpMessageBuilder], so Ktor's own `cookie()` is out of reach.
 */
fun HeadersBuilder.accessTokenCookie(accessToken: JwtToken) {
  append(HttpHeaders.Cookie, renderCookieHeader(Cookie(name = JwtCookies.accessToken.value, value = accessToken.token)))
}

/**
 * Sends [refreshToken] in the refresh-token cookie. A browser sends it to the refresh endpoint only —
 * the cookie's path confines it there.
 */
fun HttpMessageBuilder.refreshTokenCookie(refreshToken: JwtToken) {
  cookie(name = JwtCookies.refreshToken.value, value = refreshToken.token)
}

/**
 * Installs basic JSON support - without any special configuration
 */
fun Application.installJsonSupportForTests() {
  installJson {
    serializersModule = SerializersModule { }
  }
}

/**
 * Launches the service in the context of a test.
 *
 * Please use [launchRoutesInTest] for [KtorRoutes]
 */
fun ApplicationTestBuilder.launchServiceInTest(service: KtorService) {
  application {
    service.configureFor(this)
  }
}

@Deprecated("Inline!", ReplaceWith("launchRoutesInTest(routes)"))
fun ApplicationTestBuilder.launchServiceInTest(routes: KtorRoutes) {
  launchRoutesInTest(routes)
}

/**
 * Launches [service] in a fresh test application, sends [request] without any JWT bearer token,
 * and asserts the endpoint rejects it with `401 Unauthorized`.
 *
 * Intended for auth-routing tests that verify every endpoint of a service requires authentication.
 * The [request] lambda runs with an unauthenticated [HttpClient] as receiver, e.g.
 * `assertRejectedWithoutToken(service) { get(url) }`.
 */
fun assertRejectedWithoutToken(
  service: KtorService,
  request: suspend HttpClient.() -> HttpResponse,
) = testApplication {
  launchServiceInTest(service)

  val client = createClient {
    install(ContentNegotiation) { json() }
    expectSuccess = false
  }

  assertThat(client.request()).isUnauthorized()
}

/**
 * Launches the service in the context of a test.
 *
 * ATTENTION: This method automatically calls [installJson] if the [ContentNegotiation] plugin has not already been installed.
 */
fun ApplicationTestBuilder.launchRoutesInTest(
  routes: KtorRoutes,
  /**
   * The configuration block that is executed *before* applying the routes
   */
  prepareBlock: Application.() -> Unit = {},
) {
  application {
    prepareBlock()

    //Install FeatureFlagsPlugin to make feature flags available via coroutine context;
    //no default flags, so a NECKARIT_DEFAULT_FEATURE_FLAGS of the developer's shell never reaches these routes
    installFeatureFlagSupport(defaultFlags = FeatureFlags.empty)

    //Store the KtorService in application attributes so that Application.ktorService works
    //during route configuration (e.g. for dumpRoutes())
    if (routes is KtorService) {
      putKtorService(routes)
    }

    routes.configure(this)

    //Ensure ContentNegotiation is not already installed
    if (pluginOrNull(io.ktor.server.plugins.contentnegotiation.ContentNegotiation) == null) {
      installJson { }
    }
  }
}
