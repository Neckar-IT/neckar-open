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
import com.auth0.jwt.JWTCreator
import com.auth0.jwt.interfaces.Claim
import com.auth0.jwt.interfaces.DecodedJWT
import com.auth0.jwt.interfaces.Payload
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.routing.*
import it.neckar.open.exceptions.PermissionDeniedException
import it.neckar.open.tenancy.Tenant
import it.neckar.open.tenancy.TenantDescriptor
import it.neckar.open.time.nowMillis
import it.neckar.rest.jwt.JwtAudience
import it.neckar.rest.jwt.JwtClaimKey
import it.neckar.rest.jwt.JwtToken
import it.neckar.rest.jwt.JwtTokenClaim
import it.neckar.rest.jwt.JwtTokenUsageType
import it.neckar.rest.jwt.RefreshTokenChainId
import it.neckar.user.UserLoginName
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * Extracts the [RefreshTokenChainId] from the JWT token
 */
fun JWTPrincipal.refreshTokenChainId(): RefreshTokenChainId {
  return payload.refreshTokenChainId()
}

fun Payload.refreshTokenChainId(): RefreshTokenChainId {
  return JwtTokenClaim.RefreshTokenChainId.get(this)
}

fun JWTPrincipal.tokenUsage(): JwtTokenUsageType {
  return payload.tokenUsage()
}

/**
 * Extracts the [JwtTokenUsageType] from the JWT token
 */
fun Payload.tokenUsage(): JwtTokenUsageType {
  return JwtTokenClaim.TokenUsageType.get(this)
}

/**
 * Extracts the claim
 */
fun <T> JwtTokenClaim<T>.get(payload: Payload): T {
  val claimValue = payload.getClaimValue(this.key.value)
  return this.deserialize(claimValue)
}

fun <T> JwtTokenClaim<T>.getOrNull(payload: Payload): T? {
  val claimValue = payload.getClaimValueOrNull(this.key.value) ?: return null
  return this.deserialize(claimValue)
}

/**
 * Extracts the claim value
 */
fun <T> Payload.getClaimValue(claim: JwtTokenClaim<T>): T {
  return claim.get(this)
}

fun <T> Payload.getClaimValueOrNull(claim: JwtTokenClaim<T>): T? {
  return claim.getOrNull(this)
}

/**
 * Returns true if the token is an access token
 */
fun Payload.isAccessToken(): Boolean {
  return tokenUsage() == JwtTokenUsageType.Access
}

fun Payload.isRefreshToken(): Boolean {
  return tokenUsage() == JwtTokenUsageType.Refresh
}

/**
 * Returns the JWT token
 * Throws an [PermissionDeniedException] if no principal could be found
 */
fun ApplicationCall.jwtPrincipal(): JWTPrincipal {
  return principal() ?: throw PermissionDeniedException("No principal found")
}

/**
 * Extracts the JWT token from the authorization header.
 *
 * This method can be used *without* the Ktor authorization plugin.
 */
fun ApplicationCall.jwtToken(): JwtToken {
  return findJwtToken() ?: throw PermissionDeniedException("No authorization header found")
}

/**
 * Extracts the JWT token from the authorization header.
 */
fun ApplicationCall.findJwtToken(): JwtToken? {
  return request.headers[HttpHeaders.Authorization]?.let { authHeader ->
    JwtToken(authHeader.removePrefix("Bearer "))
  }
}

fun JwtToken.decode(): DecodedJWT {
  return JWT.decode(token)
}

/**
 * Adds a claim to the builder
 */
fun <T> JWTCreator.Builder.withClaim(claim: JwtTokenClaim<T>, value: T): JWTCreator.Builder {
  this.withClaim(claim.key.value, claim.serialize(value))
  return this
}

/**
 * Sets the audience
 */
fun JWTCreator.Builder.withAudience(audience: JwtAudience): JWTCreator.Builder {
  this.withAudience(audience.value)
  return this
}

/**
 * Appends the tenant descriptor to the JWT token.
 *
 * Does not do anything if the tenant descriptor is null.
 */
fun JWTCreator.Builder.withTenantDescriptor(tenantDescriptor: TenantDescriptor): JWTCreator.Builder {
  return withClaim(JwtTokenClaim.TenantDescriptor, tenantDescriptor)
}
/**
 * Common JWT claims related to login
 */


/**
 * Returns the login name from the JWT token
 */
fun ApplicationCall.jwtLoginName(): UserLoginName {
  return jwtPrincipal().payload.loginName()
}

/**
 * Extracts the login name from the JWT
 */
fun JWTPrincipal.loginName(): UserLoginName {
  return payload.loginName()
}

/**
 * Returns the [UserLoginName] from the JWT
 */
fun Payload.loginName(): UserLoginName {
  return subject?.let { UserLoginName(it) } ?: error("Token [$id] carries no subject")
}

fun ApplicationCall.jwtTenantDescriptor(): TenantDescriptor {
  return jwtPrincipal().tenantDescriptor()
}

fun JWTPrincipal.tenantId(): Tenant.Id {
  return payload.tenantDescriptor().id
}

fun JWTPrincipal.tenantDescriptor(): TenantDescriptor {
  return payload.tenantDescriptor()
}

/**
 * Extracts the tenant descriptor from the JWT token, or null if the token carries no `td` claim.
 *
 * Keycloak access tokens carry no tenant claim — use this for cross-cutting concerns that must
 * tolerate tenant-less tokens (e.g. tracing). Endpoints that require a tenant use [tenantDescriptor].
 */
fun Payload.tenantDescriptorOrNull(): TenantDescriptor? {
  return getClaimValueOrNull(JwtTokenClaim.TenantDescriptor)
}

/**
 * Extracts the tenant descriptor from the JWT token. Fails fast if the token carries no `td` claim.
 *
 * Keycloak access tokens carry no tenant claim, so this throws for Keycloak-authenticated requests.
 * A multi-tenant service behind Keycloak must resolve the tenant from the registered user, not from
 * the token.
 */
fun Payload.tenantDescriptor(): TenantDescriptor {
  return tenantDescriptorOrNull()
    ?: error(
      "No tenant descriptor (td) claim in the JWT (subject='${subject ?: "?"}'). Keycloak access " +
        "tokens carry no tenant — a multi-tenant service must resolve the tenant from the " +
        "registered user (e.g. registeredUser().tenantId), not from jwtTenantId().",
    )
}

/**
 * Extracts the [AuthContext] from the JWT token.
 * Use the destructor to get the relevant data
 */
fun ApplicationCall.jwtAuthContext(): AuthContext {
  val jwtPrincipal = jwtPrincipal()
  return AuthContext(
    loginName = jwtPrincipal.loginName(),
    tenant = jwtPrincipal.tenantDescriptor(),
  )
}

/**
 * Extracts the tenant id from the JWT `td` claim.
 *
 * Named with the `jwt` prefix on purpose: this is the tenant carried by the token, NOT necessarily
 * the user's real tenant. Behind Keycloak the token has no `td` claim and this fails fast — a
 * multi-tenant service must resolve the tenant from the registered user instead.
 *
 * To get the loginName use [jwtAuthContext].
 */
fun ApplicationCall.jwtTenantId(): Tenant.Id {
  return jwtPrincipal().tenantId()
}

fun RoutingContext.jwtTenantId(): Tenant.Id {
  return call.jwtTenantId()
}


/**
 * Converts a claim to the structurally equal [JsonElement] (for debugging purposes).
 *
 * Throws an [IllegalStateException] for a claim that carries no JSON value, such as a missing claim.
 */
fun Claim.toJsonElementFromClaim(): JsonElement {
  if (this.isNull) {
    return JsonNull
  }

  val value: Any = this.`as`(Any::class.java) ?: error("Claim cannot be represented as JSON: $this")
  return value.toJsonElementFromClaimValue(this)
}

/**
 * Converts a value decoded from [claim] to a [JsonElement]; [claim] names the source in the error message.
 */
private fun Any?.toJsonElementFromClaimValue(claim: Claim): JsonElement {
  return when (this) {
    null -> JsonNull
    is Boolean -> JsonPrimitive(this)
    is Number -> JsonPrimitive(this)
    is String -> JsonPrimitive(this)
    is Map<*, *> -> JsonObject(this.entries.associate { (key, value) -> key.toString() to value.toJsonElementFromClaimValue(claim) })
    is List<*> -> JsonArray(this.map { it.toJsonElementFromClaimValue(claim) })
    else -> error("Claim cannot be represented as JSON: $claim")
  }
}

/**
 * Extracts the JWT token. Throws exceptions if the token is missing or invalid
 */
fun RoutingContext.extractJwtToken(): JwtToken {
  val jwtToken = call.request.cookies["accessToken"]?.let { tokenAsString -> JwtToken(tokenAsString) } ?: throw PermissionDeniedException("No JWT Cookie found!")

  val expirationDate = call.jwtPrincipal().expiresAt ?: throw PermissionDeniedException("Expiration Date for Access Token required!")
  if ((expirationDate.time >= nowMillis()).not()) throw PermissionDeniedException("Expiration Date for Access Token must be valid!")
  return jwtToken
}
