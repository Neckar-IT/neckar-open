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
import com.auth0.jwt.algorithms.Algorithm
import it.neckar.open.tenancy.TenantDescriptor
import it.neckar.rest.jwt.JwtAudience
import it.neckar.rest.jwt.JwtIssuer
import it.neckar.rest.jwt.JwtToken
import it.neckar.rest.jwt.JwtTokenClaim
import it.neckar.rest.jwt.JwtTokenPair
import it.neckar.rest.jwt.JwtTokenUsageType
import it.neckar.rest.jwt.RefreshTokenChainId
import it.neckar.user.UserLoginName
import it.neckar.uuid.randomForEnvironment
import java.time.Instant
import kotlin.uuid.Uuid

/**
 * Configuration for JWT authentication
 */
data class JwtConfiguration(
  /**
   * The audience
   */
  val audience: JwtAudience,
  /**
   * The issuer of the tokens
   */
  val issuer: JwtIssuer,

  /**
   * The signing key
   */
  val jwtSigningKey: JwtSigningKey,
) {
  /**
   * Creates an access token for this configuration
   */
  fun createAccessToken(
    issuedAt: Instant,
    notBefore: Instant,
    expiresAt: Instant,
    loginName: UserLoginName,
    /**
     * The tenant reference for which the token is created.
     */
    tenantDescriptor: TenantDescriptor,

    /**
     * The chain this token belongs to - lets the logout end the chain while authenticating with the
     * access token, so the refresh token can stay on its own endpoint (#788).
     */
    refreshTokenChainId: RefreshTokenChainId,

    /**
     * Adds additional claims to the tokens
     */
    additionalClaims: JWTCreator.Builder.() -> Unit,

    ): JwtToken {
    return JwtToken(
      JWT.create()
        .withJWTId(Uuid.randomForEnvironment().toString())
        .withIssuedAt(issuedAt)
        .withIssuer(issuer.value)
        .withSubject(loginName.value)
        .withAudience(audience.value)
        .also {
          it.additionalClaims()
        }
        // Everything this function takes as a parameter is written after the additional claims, so a
        // caller cannot overwrite what it explicitly asked for - the last write to a claim wins.
        .withTenantDescriptor(tenantDescriptor)
        .withClaim(JwtTokenClaim.RefreshTokenChainId, refreshTokenChainId)
        .withNotBefore(notBefore)
        .withExpiresAt(expiresAt)
        .withClaim(JwtTokenClaim.TokenUsageType, JwtTokenUsageType.Access)
        .sign(jwtSigningKey)
    )
  }

  /**
   * Creates a new refresh token
   */
  fun createRefreshToken(
    issuedAt: Instant,
    notBefore: Instant,
    expiresAt: Instant,
    loginName: UserLoginName,
    /**
     * The tenant reference for which the token is created.
     */
    tenantDescriptor: TenantDescriptor,

    /**
     * The refresh token chain id - used to identify the refresh tokens and if they are still valid
     */
    refreshTokenChainId: RefreshTokenChainId,

    /**
     * Adds additional claims to the tokens
     */
    additionalClaims: JWTCreator.Builder.() -> Unit,

    ): JwtToken {
    return JwtToken(
      JWT.create()
        .withJWTId(Uuid.randomForEnvironment().toString())
        .withIssuedAt(issuedAt)
        .withIssuer(issuer.value)
        .withSubject(loginName.value)
        .withAudience(audience.value)
        .also {
          it.additionalClaims()
        }
        // Everything this function takes as a parameter is written after the additional claims, so a
        // caller cannot overwrite what it explicitly asked for - the last write to a claim wins.
        .withTenantDescriptor(tenantDescriptor)
        .withClaim(JwtTokenClaim.RefreshTokenChainId, refreshTokenChainId)
        .withNotBefore(notBefore)
        .withExpiresAt(expiresAt)
        .withClaim(JwtTokenClaim.TokenUsageType, JwtTokenUsageType.Refresh)
        .sign(jwtSigningKey)
    )
  }

  /**
   * Creates a token pair
   */
  fun createTokenPair(
    issuedAt: Instant,
    notBefore: Instant,
    expiresAtAccessToken: Instant,
    expiresAtRefreshToken: Instant,
    loginName: UserLoginName,
    /**
     * The tenant reference for which the token is created.
     */
    tenantDescriptor: TenantDescriptor,

    refreshTokenChainId: RefreshTokenChainId,

    additionalClaims: JWTCreator.Builder.() -> Unit,
  ): JwtTokenPair {

    val accessToken = createAccessToken(
      issuedAt = issuedAt,
      notBefore = notBefore,
      expiresAt = expiresAtAccessToken,
      loginName = loginName,
      tenantDescriptor = tenantDescriptor,

      refreshTokenChainId = refreshTokenChainId,
      additionalClaims = additionalClaims
    )

    //Create the first refresh token - that has a longer expiration time
    val refreshToken = createRefreshToken(
      issuedAt = issuedAt,
      notBefore = notBefore,
      expiresAt = expiresAtRefreshToken,
      loginName = loginName,
      tenantDescriptor = tenantDescriptor,

      refreshTokenChainId = refreshTokenChainId,
      additionalClaims = additionalClaims
    )

    return JwtTokenPair(accessToken = accessToken, refreshToken = refreshToken)
  }
}

/**
 * Creates a JWT configuration from the given algorithm
 */
fun Algorithm.toJwtConfiguration(audience: JwtAudience, issuer: JwtIssuer): JwtConfiguration {
  return JwtConfiguration(audience, issuer, this)
}
