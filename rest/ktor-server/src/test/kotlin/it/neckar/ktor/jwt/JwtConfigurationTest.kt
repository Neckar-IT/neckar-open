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
import com.auth0.jwt.algorithms.Algorithm
import it.neckar.open.tenancy.Tenant
import it.neckar.open.tenancy.TenantDescriptor
import it.neckar.rest.jwt.JwtAudience
import it.neckar.rest.jwt.JwtIssuer
import it.neckar.rest.jwt.JwtTokenClaim
import it.neckar.rest.jwt.RefreshTokenChainId
import it.neckar.user.UserLoginName
import org.junit.jupiter.api.Test
import java.time.Instant
import kotlin.uuid.Uuid

/**
 * Tests for [JwtConfiguration]
 */
class JwtConfigurationTest {

  private val jwtConfiguration = JwtConfiguration(
    audience = JwtAudience("test-audience"),
    issuer = JwtIssuer("test-issuer"),
    jwtSigningKey = Algorithm.HMAC256("test-secret"),
  )

  private val now: Instant = Instant.now()

  /**
   * The logout authenticates with the access token and ends the chain it carries (#788). An access
   * token without the chain claim would make that logout impossible, so every access token carries
   * it - this test is the contract.
   */
  @Test
  fun `the access token carries the refresh token chain`() {
    val refreshTokenChainId = RefreshTokenChainId.random()

    val accessToken = jwtConfiguration.createAccessToken(
      issuedAt = now,
      notBefore = now,
      expiresAt = now.plusSeconds(3600),
      loginName = UserLoginName("foo"),
      tenantDescriptor = TenantDescriptor.NeckarIT,
      refreshTokenChainId = refreshTokenChainId,
    ) {
    }

    assertThat(JwtTokenClaim.RefreshTokenChainId.get(accessToken.decode())).isEqualTo(refreshTokenChainId)
  }

  /**
   * Every parameter of the mint functions is written after the additional claims, so a caller cannot
   * overwrite what it explicitly asked for. Checked here for the chain the token is minted for.
   */
  @Test
  fun `additional claims cannot overwrite the refresh token chain`() {
    // Both spelled out: two random draws could collide and let the test pass without asserting anything.
    val refreshTokenChainId = RefreshTokenChainId(Uuid.parse("11111111-1111-4111-8111-111111111111"))
    val chainIdFromAdditionalClaims = RefreshTokenChainId(Uuid.parse("22222222-2222-4222-8222-222222222222"))

    val accessToken = jwtConfiguration.createAccessToken(
      issuedAt = now,
      notBefore = now,
      expiresAt = now.plusSeconds(3600),
      loginName = UserLoginName("foo"),
      tenantDescriptor = TenantDescriptor.NeckarIT,
      refreshTokenChainId = refreshTokenChainId,
    ) {
      withClaim(JwtTokenClaim.RefreshTokenChainId, chainIdFromAdditionalClaims)
    }

    assertThat(JwtTokenClaim.RefreshTokenChainId.get(accessToken.decode())).isEqualTo(refreshTokenChainId)
  }

  /**
   * The same guarantee for the tenant: a token that claims a tenant its minter did not ask for
   * would carry the wrong isolation boundary into every service that trusts the claim.
   */
  @Test
  fun `additional claims cannot overwrite the tenant descriptor`() {
    val tenantDescriptor = TenantDescriptor.NeckarIT
    val tenantDescriptorFromAdditionalClaims = Tenant.NeckarITe2e.descriptor()

    val accessToken = jwtConfiguration.createAccessToken(
      issuedAt = now,
      notBefore = now,
      expiresAt = now.plusSeconds(3600),
      loginName = UserLoginName("foo"),
      tenantDescriptor = tenantDescriptor,
      refreshTokenChainId = RefreshTokenChainId.random(),
    ) {
      withTenantDescriptor(tenantDescriptorFromAdditionalClaims)
    }

    assertThat(accessToken.decode().tenantDescriptor()).isEqualTo(tenantDescriptor)
  }

  /**
   * Both tokens of a pair share one chain - the access token ends the chain its refresh token
   * belongs to.
   */
  @Test
  fun `both tokens of a pair carry the same chain`() {
    val refreshTokenChainId = RefreshTokenChainId.random()

    val tokenPair = jwtConfiguration.createTokenPair(
      issuedAt = now,
      notBefore = now,
      expiresAtAccessToken = now.plusSeconds(3600),
      expiresAtRefreshToken = now.plusSeconds(86400),
      loginName = UserLoginName("foo"),
      tenantDescriptor = TenantDescriptor.NeckarIT,
      refreshTokenChainId = refreshTokenChainId,
    ) {
    }

    assertThat(JwtTokenClaim.RefreshTokenChainId.get(tokenPair.accessToken.decode())).isEqualTo(refreshTokenChainId)
    assertThat(JwtTokenClaim.RefreshTokenChainId.get(tokenPair.refreshToken.decode())).isEqualTo(refreshTokenChainId)
  }
}
