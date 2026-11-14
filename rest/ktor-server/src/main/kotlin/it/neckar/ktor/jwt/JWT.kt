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

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTVerificationException
import com.auth0.jwt.interfaces.JWTVerifier
import com.auth0.jwt.interfaces.Verification
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import it.neckar.logging.Logger
import it.neckar.logging.LoggerFactory
import it.neckar.rest.jwt.JwtAudience
import it.neckar.rest.jwt.JwtCookies
import it.neckar.rest.jwt.JwtToken
import it.neckar.rest.jwt.JwtTokenClaim
import it.neckar.rest.jwt.JwtTokenUsageType


/**
 * Better class name for the signing key
 */
typealias JwtSigningKey = Algorithm

/**
 * Installs the access-token authentication under [name], reading the token from the `Authorization`
 * header.
 *
 * [AuthenticationTypes.jwtCookieAuth] is registered alongside it, reading the same token from the
 * access-token cookie. Routes that name it themselves are the browser-facing HTML views, which carry
 * no bearer token.
 */
@IgnorableReturnValue
fun Application.installAccessTokenHeaderAuthentication(
  jwtConfiguration: JwtConfiguration,
  name: AuthProviderName? = null,
): Authentication {
  return install(Authentication) {
    addJwtHeaderAuth(name?.value, jwtConfiguration.audience, JwtTokenUsageType.Access, jwtConfiguration::jwtSigningKey)
    addJwtCookieAuth(name = AuthenticationTypes.jwtCookieAuth.value, jwtConfiguration = jwtConfiguration)
  }
}

object AuthenticationTypes {
  val jwtCookieAuth: AuthProviderName = AuthProviderName("jwtCookieAuth")
}

/**
 * Installs the refresh-token authentication under [name], reading the token from the refresh-token
 * cookie. Belongs next to an access-token provider: this one guards the refresh endpoint alone.
 */
fun Authentication.installRefreshTokenCookieAuthentication(jwtConfiguration: JwtConfiguration, name: AuthProviderName) {
  configure {
    addJwtCookieAuth(name = name.value, tokenUsageType = JwtTokenUsageType.Refresh, jwtConfiguration = jwtConfiguration)
  }
}

/**
 * Installs the refresh-token authentication under [name], reading the token from the `Authorization`
 * header. Belongs next to an access-token provider: this one guards the refresh endpoint alone.
 */
fun Authentication.installRefreshTokenHeaderAuthentication(jwtConfiguration: JwtConfiguration, name: AuthProviderName) {
  configure {
    jwt(name.value) {
      verifier(
        JWT
          .require(jwtConfiguration.jwtSigningKey)
          .withAudience(jwtConfiguration.audience)
          .withClaim(JwtTokenClaim.TokenUsageType, JwtTokenUsageType.Refresh)
          .build()
      )

      validate { credential ->
        if (credential.payload.audience.contains(jwtConfiguration.audience.value)) {
          JWTPrincipal(credential.payload)
        } else {
          null
        }
      }
    }
  }
}

/**
 * Registers the JWT authentication to an [AuthenticationConfig].
 * Most of the time [installAccessTokenHeaderAuthentication] should be used instead.
 *
 * Use it like this:
 *
 * ```
 * install(Authentication) {
 *  addJwtHeaderAuth(name, jwtSigningKeyProvider)
 * }
 * ```
 *
 * The [audience] is also checked to be the audience of the token.
 */
fun AuthenticationConfig.addJwtHeaderAuth(name: String? = null, audience: JwtAudience, tokenUsageType: JwtTokenUsageType = JwtTokenUsageType.Access, jwtSigningKeyProvider: () -> JwtSigningKey) {
  jwt(name) {
    realm = "Neckar IT"

    verifier(jwtVerifier(audience, tokenUsageType, jwtSigningKeyProvider))

    validate { credential ->
      if (credential.payload.audience.contains(audience.value)) {
        JWTPrincipal(credential.payload)
      } else {
        null
      }
    }
  }
}

fun AuthenticationConfig.addJwtHeaderAuth(name: String? = null, tokenUsageType: JwtTokenUsageType = JwtTokenUsageType.Access, jwtConfiguration: JwtConfiguration) {
  addJwtHeaderAuth(name, jwtConfiguration.audience, tokenUsageType, jwtConfiguration::jwtSigningKey)
}

/**
 * Authorization using cookies. The cookie the token is read from follows [tokenUsageType].
 */
fun AuthenticationConfig.addJwtCookieAuth(name: String? = null, audience: JwtAudience, tokenUsageType: JwtTokenUsageType = JwtTokenUsageType.Access, jwtSigningKeyProvider: () -> JwtSigningKey) {
  provider(name) {
    authenticate { authenticationContext ->
      val token = findTokenCookie(authenticationContext, tokenUsageType)

      if (token == null) {
        logger.debug("No ${tokenUsageType.name} token cookie found")
        authenticationContext.respondUnauthorized(AuthenticationFailedCause.NoCredentials)
        return@authenticate
      }

      try {
        jwtVerifier(audience, tokenUsageType, jwtSigningKeyProvider).verify(token.token)
      } catch (e: JWTVerificationException) {
        logger.debug(buildTokenVerificationFailureMessage(token, tokenUsageType, e))
        authenticationContext.respondUnauthorized(AuthenticationFailedCause.InvalidCredentials)
        return@authenticate
      }

      //Provide the payload as principal - compatible with "normal" JWT authentication
      val payload = JWT.decode(token.token)
      authenticationContext.principal(JWTPrincipal(payload))
    }
  }
}

/** The key [addJwtCookieAuth] registers its challenge under. */
private object JwtCookieAuthKey

/**
 * Answers 401 — the status the client reacts to by refreshing the access token. A 403 would say the
 * credential was accepted and the permission missing, and the client would end the session instead.
 */
private fun AuthenticationContext.respondUnauthorized(cause: AuthenticationFailedCause) {
  challenge(JwtCookieAuthKey, cause) { challenge, call ->
    call.respond(UnauthorizedResponse())
    challenge.complete()
  }
}

/**
 * Returns the cookie carrying a token of [tokenUsageType].
 *
 * The two tokens travel in separate cookies: the refresh token is confined to the refresh endpoint
 * by its cookie path and never reaches a route guarded by the access token.
 */
private fun findTokenCookie(authenticationContext: AuthenticationContext, tokenUsageType: JwtTokenUsageType): JwtToken? {
  return when (tokenUsageType) {
    JwtTokenUsageType.Access -> findAccessTokenCookie(authenticationContext)
    JwtTokenUsageType.Refresh -> findRefreshTokenCookie(authenticationContext)
  }
}

/**
 * Returns the access token cookie from the authentication context.
 */
fun findAccessTokenCookie(authenticationContext: AuthenticationContext): JwtToken? {
  return authenticationContext.call.request.cookies[JwtCookies.accessToken.value]?.let {
    JwtToken(it)
  }
}

fun RoutingContext.findAccessTokenCookie(): JwtToken? {
  return findAccessTokenCookie(call.authentication)
}


fun findRefreshTokenCookie(authenticationContext: AuthenticationContext): JwtToken? {
  return authenticationContext.call.request.cookies[JwtCookies.refreshToken.value]?.let {
    JwtToken(it)
  }
}

fun RoutingContext.findRefreshTokenCookie(): JwtToken? {
  return findRefreshTokenCookie(call.authentication)
}


fun AuthenticationConfig.addJwtCookieAuth(name: String? = null, tokenUsageType: JwtTokenUsageType = JwtTokenUsageType.Access, jwtConfiguration: JwtConfiguration) {
  addJwtCookieAuth(name, jwtConfiguration.audience, tokenUsageType, jwtConfiguration::jwtSigningKey)
}

/**
 * Creates a JWT verifier.
 * This verifier also verifies the token usage type.
 */
fun jwtVerifier(audience: JwtAudience, tokenUsageType: JwtTokenUsageType = JwtTokenUsageType.Access, jwtSigningKeyProvider: () -> JwtSigningKey): JWTVerifier = JWT
  .require(jwtSigningKeyProvider())
  .withAudience(audience)
  .withClaim(JwtTokenClaim.TokenUsageType, tokenUsageType)
  .build()


fun Verification.withAudience(audience: JwtAudience): Verification = withAudience(
  audience.value
)

/**
 * Registers the claim to the verification
 */
fun <T> Verification.withClaim(tokenUsageType: JwtTokenClaim<T>, value: T): Verification {
  withClaim(tokenUsageType.key.value, tokenUsageType.serialize(value))
  return this
}

/**
 * Builds a descriptive error message for JWT verification failures.
 * If the failure is due to a token usage type mismatch, includes expected and actual values.
 */
internal fun buildTokenVerificationFailureMessage(token: JwtToken, expectedUsageType: JwtTokenUsageType, cause: JWTVerificationException): String {
  val actualUsageType: JwtTokenUsageType? = try {
    val decoded = JWT.decode(token.token)
    JwtTokenClaim.TokenUsageType.getOrNull(decoded)
  } catch (_: Exception) {
    null
  }

  return if (actualUsageType != null && actualUsageType != expectedUsageType) {
    "${expectedUsageType.name} Token validation failed: Token usage type mismatch - expected '${expectedUsageType.name}', got '${actualUsageType.name}'"
  } else {
    "${expectedUsageType.name} Token validation failed: ${cause.message}"
  }
}

private val logger: Logger = LoggerFactory.getLogger("it.neckar.ktor.jwt.JWT")
