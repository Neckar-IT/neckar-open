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

import com.auth0.jwt.JWTCreator
import io.ktor.client.request.*
import io.ktor.http.*
import it.neckar.ktor.client.appendJwtTokenBearer
import it.neckar.ktor.jwt.JwtConfiguration
import it.neckar.open.tenancy.Slug
import it.neckar.open.tenancy.Tenant
import it.neckar.open.tenancy.TenantDescriptor
import it.neckar.rest.jwt.JwtToken
import it.neckar.rest.jwt.RefreshTokenChainId
import it.neckar.rest.jwt.appendJwtTokenBearer
import it.neckar.user.UserLoginName
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.uuid.Uuid


/**
 * Creates a new access token and appends the newly created token to the request — as the
 * access-token cookie and as a bearer token, the pair a browser sends.
 */
fun HttpRequestBuilder.appendJwtAccessTokenBearer(
  /**
   * The JWT configuration
   */
  jwtConfiguration: JwtConfiguration,
  /**
   * The login name for the token
   */
  loginName: UserLoginName = UserLoginName("test"),

  tenantDescriptor: TenantDescriptor = TestTenantDescriptor,

  /**
   * Additional claims that should be added to the token
   */
  additionalClaims: JWTCreator.Builder.() -> Unit = {},
) {
  headers {
    appendJwtAccessTokenBearer(jwtConfiguration, loginName, tenantDescriptor, additionalClaims)
  }
}

/**
 * Creates a new access token for *each* request — as the access-token cookie and as a bearer token,
 * the pair a browser sends.
 */
fun HeadersBuilder.appendJwtAccessTokenBearer(
  /**
   * The JWT configuration
   */
  jwtConfiguration: JwtConfiguration,
  /**
   * The login name for the token
   */
  loginName: UserLoginName = UserLoginName("test"),

  /**
   * The tenant descriptor to use for the token
   */
  tenantDescriptor: TenantDescriptor = TestTenantDescriptor,

  /**
   * Additional claims that should be added to the token
   */
  additionalClaims: JWTCreator.Builder.() -> Unit = {},
): JwtToken {

  val accessToken = createAccessToken(jwtConfiguration = jwtConfiguration, loginName = loginName, tenantDescriptor = tenantDescriptor, now = Instant.now(), additionalClaims = additionalClaims)
  appendJwtTokenBearer(accessToken)
  accessTokenCookie(accessToken)

  return accessToken
}

/**
 * Creates a new access token for tests
 */
fun createAccessToken(
  /**
   * The JWT configuration
   */
  jwtConfiguration: JwtConfiguration,
  /**
   * The login name for the token
   */
  loginName: UserLoginName = UserLoginName("test"),

  /**
   * The tenant descriptor to use for the token
   */
  tenantDescriptor: TenantDescriptor = TestTenantDescriptor,
  /**
   * The time when the token should be issued
   */
  now: Instant = Instant.now(),
  /**
   * The refresh token chain of the token - a chain of its own unless the test needs a specific one
   */
  refreshTokenChainId: RefreshTokenChainId = RefreshTokenChainId.random(),
  /**
   * Additional claims that should be added to the token
   */
  additionalClaims: JWTCreator.Builder.() -> Unit = {},

  ): JwtToken {

  val accessToken = jwtConfiguration.createAccessToken(
    loginName = loginName,
    issuedAt = now,
    notBefore = now,
    expiresAt = now.plus(100, ChronoUnit.DAYS),
    tenantDescriptor = tenantDescriptor,
    refreshTokenChainId = refreshTokenChainId,
    additionalClaims = additionalClaims,
  )
  return accessToken
}

/**
 * Creates a new refresh token and appends the newly created token to the request — as the
 * refresh-token cookie and as a bearer token, the pair a browser sends.
 */
fun HttpRequestBuilder.appendJwtRefreshTokenBearer(
  /**
   * The JWT configuration
   */
  jwtConfiguration: JwtConfiguration,
  /**
   * The login name for the token
   */
  loginName: UserLoginName = UserLoginName("test"),

  tenantDescriptor: TenantDescriptor = TestTenantDescriptor,

  /**
   * The refresh token chain id to use
   */
  refreshTokenChainId: RefreshTokenChainId = RefreshTokenChainId.random(),

  /**
   * Additional claims that should be added to the token
   */
  additionalClaims: JWTCreator.Builder.() -> Unit = {},
): JwtToken {

  val now = Instant.now()

  val refreshToken = jwtConfiguration.createRefreshToken(
    loginName = loginName,
    refreshTokenChainId = refreshTokenChainId,
    issuedAt = now,
    notBefore = now,
    expiresAt = now.plus(100, ChronoUnit.DAYS),
    tenantDescriptor = tenantDescriptor,
    additionalClaims = additionalClaims,
  )

  headers {
    appendJwtTokenBearer(refreshToken)
  }
  refreshTokenCookie(refreshToken)

  return refreshToken
}

/**
 * The tenant descriptor that is used for tests - if no multi tenancy is tested
 */
val TestTenantDescriptor: TenantDescriptor = TenantDescriptor(
  id = Tenant.Id(Uuid.parse("6711d101-f964-4fa0-930d-2423706d4b5d")),
  slug = Slug("test-tenant")
)

/**
 * A second tenant descriptor, distinct from [TestTenantDescriptor]. Use it to verify tenant
 * isolation: store data for [TestTenantDescriptor], then query it as [OtherTestTenantDescriptor]
 * and assert that nothing is found.
 */
val OtherTestTenantDescriptor: TenantDescriptor = TenantDescriptor(
  id = Tenant.Id(Uuid.parse("b3a4de52-1a2c-4de1-9c2f-90a6e0a48a3f")),
  slug = Slug("other-test-tenant")
)
