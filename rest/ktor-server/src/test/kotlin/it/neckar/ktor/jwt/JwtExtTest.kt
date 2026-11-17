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
import assertk.assertions.isInstanceOf
import assertk.assertions.isNull
import assertk.assertions.messageContains
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import it.neckar.rest.jwt.JwtClaimKey
import it.neckar.rest.jwt.JwtTokenClaim
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import org.junit.jupiter.api.Test
import java.time.Instant

/**
 * Tests for [JwtExt] functions
 */
class JwtExtTest {

  private val testClaim = JwtTokenClaim<String>(
    key = JwtClaimKey("test_claim"),
    serialize = { it },
    deserialize = { it },
  )

  private val algorithm: Algorithm = Algorithm.HMAC256("test-secret")

  @Test
  fun `getOrNull returns value when claim is present`() {
    val token = JWT.create()
      .withClaim(testClaim.key.value, testClaim.serialize("hello"))
      .withExpiresAt(Instant.now().plusSeconds(3600))
      .sign(algorithm)

    val decoded = JWT.decode(token)

    assertThat(testClaim.getOrNull(decoded)).isEqualTo("hello")
  }

  @Test
  fun `getOrNull returns null when claim is missing`() {
    val token = JWT.create()
      .withExpiresAt(Instant.now().plusSeconds(3600))
      .sign(algorithm)

    val decoded = JWT.decode(token)

    assertThat(testClaim.getOrNull(decoded)).isNull()
  }

  @Test
  fun `get throws when claim is missing`() {
    val token = JWT.create()
      .withExpiresAt(Instant.now().plusSeconds(3600))
      .sign(algorithm)

    val decoded = JWT.decode(token)

    assertk.assertFailure {
      testClaim.get(decoded)
    }.isInstanceOf<IncompleteIdentityException.ClaimAbsent>()
      .messageContains("carries no claim [test_claim]")
  }

  @Test
  fun `getClaimValueOrNull returns null when claim is missing`() {
    val token = JWT.create()
      .withExpiresAt(Instant.now().plusSeconds(3600))
      .sign(algorithm)

    val decoded = JWT.decode(token)

    assertThat(decoded.getClaimValueOrNull(testClaim)).isNull()
  }

  @Test
  fun `getClaimValueOrNull returns value when claim is present`() {
    val token = JWT.create()
      .withClaim(testClaim.key.value, testClaim.serialize("world"))
      .withExpiresAt(Instant.now().plusSeconds(3600))
      .sign(algorithm)

    val decoded = JWT.decode(token)

    assertThat(decoded.getClaimValueOrNull(testClaim)).isEqualTo("world")
  }

  /**
   * A claim holding something other than a string is a token minted with a defect. It must not pass
   * as an absent claim - `asString()` returns null for both.
   */
  @Test
  fun `getClaimValueOrNull throws when the claim is not a string`() {
    val token = JWT.create()
      .withClaim(testClaim.key.value, 42)
      .withExpiresAt(Instant.now().plusSeconds(3600))
      .sign(algorithm)

    val decoded = JWT.decode(token)

    assertk.assertFailure {
      decoded.getClaimValueOrNull(testClaim)
    }.isInstanceOf<IncompleteIdentityException.ClaimNotAString>()
      .messageContains("something other than a string")
  }

  @Test
  fun `getOrNull throws when the claim is not a string`() {
    val token = JWT.create()
      .withClaim(testClaim.key.value, 42)
      .withExpiresAt(Instant.now().plusSeconds(3600))
      .sign(algorithm)

    val decoded = JWT.decode(token)

    assertk.assertFailure {
      testClaim.getOrNull(decoded)
    }.isInstanceOf<IncompleteIdentityException.ClaimNotAString>()
  }

  @Test
  fun `toJsonElementFromClaim maps a JSON object claim to the structurally equal JsonObject`() {
    val decoded = JWT.decode(JWT.create().withClaim("realm_access", mapOf("roles" to listOf("admin"))).sign(algorithm))

    assertThat(decoded.getClaim("realm_access").toJsonElementFromClaim()).isEqualTo(Json.parseToJsonElement("""{"roles":["admin"]}"""))
  }

  @Test
  fun `toJsonElementFromClaim maps a mixed array claim to the structurally equal JsonArray`() {
    val decoded = JWT.decode(JWT.create().withClaim("allowed_origins", listOf("account", 3, mapOf("nested" to true))).sign(algorithm))

    assertThat(decoded.getClaim("allowed_origins").toJsonElementFromClaim()).isEqualTo(Json.parseToJsonElement("""["account",3,{"nested":true}]"""))
  }

  @Test
  fun `toJsonElementFromClaim maps primitive claims to JsonPrimitive`() {
    val decoded = JWT.decode(
      JWT.create()
        .withClaim("preferred_username", "anna")
        .withClaim("email_verified", true)
        .withClaim("auth_time", 1_700_000_000L)
        .withClaim("ratio", 0.5)
        .sign(algorithm)
    )

    assertThat(decoded.getClaim("preferred_username").toJsonElementFromClaim()).isEqualTo(JsonPrimitive("anna"))
    assertThat(decoded.getClaim("email_verified").toJsonElementFromClaim()).isEqualTo(JsonPrimitive(true))
    assertThat(decoded.getClaim("auth_time").toJsonElementFromClaim()).isEqualTo(JsonPrimitive(1_700_000_000))
    assertThat(decoded.getClaim("ratio").toJsonElementFromClaim()).isEqualTo(JsonPrimitive(0.5))
  }

  @Test
  fun `toJsonElementFromClaim maps a JSON null claim to JsonNull`() {
    val decoded = JWT.decode(JWT.create().withNullClaim("acr").sign(algorithm))

    assertThat(decoded.getClaim("acr").toJsonElementFromClaim()).isEqualTo(JsonNull)
  }

  @Test
  fun `toJsonElementFromClaim throws for a claim without a JSON value`() {
    val decoded = JWT.decode(JWT.create().sign(algorithm))

    assertk.assertFailure {
      decoded.getClaim("absent").toJsonElementFromClaim()
    }.isInstanceOf<IllegalStateException>()
      .messageContains("Claim cannot be represented as JSON: Missing claim")
  }
}
