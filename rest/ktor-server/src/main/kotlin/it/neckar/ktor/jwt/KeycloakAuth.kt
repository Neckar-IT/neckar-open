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
import com.auth0.jwt.exceptions.JWTDecodeException
import com.auth0.jwt.interfaces.Claim
import com.auth0.jwt.interfaces.DecodedJWT
import com.auth0.jwt.interfaces.Payload
import io.ktor.http.*
import io.ktor.http.auth.HttpAuthHeader
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import it.neckar.ktor.jwt.jwks.HeldJwkProvider
import it.neckar.ktor.jwt.jwks.JwkKeyId
import it.neckar.ktor.jwt.jwks.JwksKeyManager
import it.neckar.logging.Logger
import it.neckar.logging.LoggerFactory
import it.neckar.logging.debug
import it.neckar.rest.jwt.JwtAudience
import it.neckar.rest.jwt.JwtTokenClaim
import it.neckar.rest.jwt.JwtTokenUsageType

private val logger: Logger = LoggerFactory.getLogger("it.neckar.ktor.jwt.KeycloakAuth")

/**
 * Standard OIDC claim names used by Keycloak.
 *
 * These are defined by the OpenID Connect Core specification and Keycloak's default claim mappings.
 */
object KeycloakClaim {
  const val Subject: String = "sub"
  const val PreferredUsername: String = "preferred_username"
  const val GivenName: String = "given_name"
  const val FamilyName: String = "family_name"
  const val Email: String = "email"
}

/**
 * Installs Keycloak JWT authentication using JWKS-based signature verification.
 *
 * Tokens are issued by Keycloak, the backend only validates them.
 *
 * The signature keys come from [jwksKeyManager] and from nowhere else: the request path reads the
 * held set and never fetches. This installation starts the manager's fetch job and loads the set it
 * starts from, so the first request after a restart finds keys instead of waiting for Keycloak.
 *
 * Keycloak tokens carry no `td` (tenant descriptor) claim, so `tenantId()` fails fast for
 * Keycloak-authenticated requests. A multi-tenant service must resolve the tenant from the
 * registered user, not from the token.
 *
 * The [KeycloakClaim.PreferredUsername] claim is mapped to `subject` so that `loginName()` returns
 * the human-readable username instead of the Keycloak user UUID.
 */
@IgnorableReturnValue
fun Application.installKeycloakJwksAuthentication(
  keycloakConfiguration: KeycloakConfiguration,
  name: AuthProviderName,
  jwksKeyManager: JwksKeyManager,
): Authentication {
  jwksKeyManager.loadHeldSet()
  jwksKeyManager.fetchOnceBeforeServing()
  jwksKeyManager.start(this)

  return install(Authentication) {
    jwt(name.value) {
      verifyKeycloakToken(keycloakConfiguration, jwksKeyManager)

      validate { credential ->
        val payload = credential.payload
        if (payload.audience.contains(keycloakConfiguration.clientId.value)) {
          JWTPrincipal(KeycloakPayloadAdapter(payload))
        } else {
          logger.warn("JWT audience mismatch: expected '${keycloakConfiguration.clientId}', got ${payload.audience}")
          null
        }
      }
    }
  }
}

/**
 * Adds the provider [name] for tokens that name [audienceOf] the request in `aud`, beside [installKeycloakJwksAuthentication], which installs
 * [Authentication] and starts [jwksKeyManager]. A route selects it to accept tokens meant for that route alone, which the provider of the service refuses.
 */
fun Application.addKeycloakJwksAudienceProvider(
  keycloakConfiguration: KeycloakConfiguration,
  name: AuthProviderName,
  jwksKeyManager: JwksKeyManager,
  audienceOf: (ApplicationCall) -> JwtAudience,
) {
  authentication {
    jwt(name.value) {
      verifyKeycloakToken(keycloakConfiguration, jwksKeyManager)

      validate { credential ->
        val payload = credential.payload
        if (payload.audience.contains(audienceOf(this).value)) {
          JWTPrincipal(KeycloakPayloadAdapter(payload))
        } else {
          // DEBUG: a route that selects this provider also selects the one of the service, whose tokens arrive here first.
          logger.debug { "JWT audience ${payload.audience} is none of ${name.value}" }
          null
        }
      }
    }
  }
}

/**
 * Signature, issuer and challenge of a Keycloak token; `validate` decides on the audience.
 */
private fun JWTAuthenticationProvider.Config.verifyKeycloakToken(keycloakConfiguration: KeycloakConfiguration, jwksKeyManager: JwksKeyManager) {
  realm = "Neckar IT"

  // The one place an incoming token is visible before verification. Reading it here keeps the
  // unknown-kid decision out of the verifier, which only ever sees the held set.
  authHeader { call -> call.authorizationHeaderPlanningRefresh(jwksKeyManager) }

  verifier(HeldJwkProvider(jwksKeyManager), keycloakConfiguration.issuer)

  // DEBUG, not WARN: this fires for every request that fails to authenticate, header or not. The
  // lambda form matters as much — a `{}` placeholder defers the formatting, not the argument.
  challenge { _, _ ->
    logger.debug { "JWT authentication challenge triggered; validating against ${jwksKeyManager.describeHeldSet()}" }
    // The request carries no valid credential.
    @Suppress("Status4xxIsAProtocolError")
    call.respondText("", status = HttpStatusCode.Unauthorized)
  }
}

/**
 * The `Authorization` header of this call, and — as a side effect — an out-of-turn JWKS refresh
 * planned when the token names a `kid` the held set does not know.
 *
 * The request never waits for that refresh and is rejected either way; the next request after a
 * successful one goes through. Returns the header unchanged, so what the verifier sees is what the
 * caller sent.
 */
private fun ApplicationCall.authorizationHeaderPlanningRefresh(jwksKeyManager: JwksKeyManager): HttpAuthHeader? {
  val header = try {
    request.parseAuthorizationHeader()
  } catch (e: IllegalArgumentException) {
    logger.debug("Illegal Authorization header", e)
    return null
  } ?: return null

  val bearer = (header as? HttpAuthHeader.Single)?.takeIf { it.authScheme.equals(BearerScheme, ignoreCase = true) }
    ?: return header

  val token: DecodedJWT = try {
    JWT.decode(bearer.blob)
  } catch (e: JWTDecodeException) {
    logger.debug("Authorization header carries no decodable JWT", e)
    return header
  }

  val keyId = token.keyId?.takeIf { it.isNotBlank() }?.let { JwkKeyId(it) } ?: return header
  if (jwksKeyManager.keyOf(keyId) != null) {
    return header
  }

  // WARN only for the planned refresh, which the planner bounds; the rejection stays DEBUG, because
  // a flood of distinct kids would otherwise write one line per request.
  if (jwksKeyManager.requestRefreshFor(keyId, token)) {
    logger.warn("Token names the unknown signing key '$keyId' — rejecting it and refreshing the JWKS out of turn")
  } else {
    logger.debug("Rejecting a token that names the unknown signing key '$keyId'; no refresh planned")
  }
  return header
}

private const val BearerScheme: String = "Bearer"

/**
 * Wraps a Keycloak JWT [Payload] to make it compatible with existing claim extraction code.
 *
 * Keycloak tokens differ from service-issued tokens:
 * - `sub` contains a UUID, not the username → this adapter returns [KeycloakClaim.PreferredUsername] as subject
 * - No `tu` (token usage type) claim → always returns [JwtTokenUsageType.Access]
 *
 * Keycloak tokens also carry no `td` (tenant descriptor) claim. The adapter does NOT synthesize one:
 * `tenantId()` fails fast, and multi-tenant services resolve the tenant from the registered user.
 */
class KeycloakPayloadAdapter(
  private val delegate: Payload,
) : Payload by delegate {

  override fun getSubject(): String {
    return delegate.getClaim(KeycloakClaim.PreferredUsername)?.asString()
      ?: delegate.subject
  }

  override fun getClaim(name: String): Claim {
    return when (name) {
      JwtTokenClaim.TokenUsageType.key.value -> SyntheticStringClaim(JwtTokenClaim.TokenUsageType.serialize(JwtTokenUsageType.Access))
      else -> delegate.getClaim(name)
    }
  }

  override fun getClaims(): Map<String, Claim> {
    return delegate.claims + mapOf(
      JwtTokenClaim.TokenUsageType.key.value to getClaim(JwtTokenClaim.TokenUsageType.key.value),
    )
  }

  /**
   * The Keycloak user UUID from the original `sub` claim.
   * Use [getSubject] for the human-readable username.
   */
  val keycloakUserId: String?
    get() = delegate.subject
}

/**
 * A minimal [Claim] implementation that holds a single string value.
 * Used to inject the synthetic token-usage-type claim into Keycloak payloads where it
 * doesn't exist natively.
 */
internal class SyntheticStringClaim(private val value: String) : Claim {
  override fun isNull(): Boolean = false
  override fun isMissing(): Boolean = false
  override fun asBoolean(): Boolean? = null
  override fun asInt(): Int? = null
  override fun asLong(): Long? = null
  override fun asDouble(): Double? = null
  override fun asString(): String = value
  override fun asDate(): java.util.Date? = null

  @Suppress("UNCHECKED_CAST")
  override fun <T : Any?> `as`(clazz: Class<T>?): T? {
    return if (clazz == String::class.java) value as T else null
  }

  override fun <T : Any?> asArray(clazz: Class<T>?): Array<T>? = null
  override fun <T : Any?> asList(clazz: Class<T>?): List<T>? = null
  override fun asMap(): Map<String, Any>? = null
  override fun toString(): String = value
}
