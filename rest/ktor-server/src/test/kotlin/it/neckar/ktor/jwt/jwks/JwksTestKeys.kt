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
package it.neckar.ktor.jwt.jwks

import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.util.Base64

/**
 * One RSA key pair with a `kid`, and the JWK it publishes — enough to build a JWKS document that
 * the production parser and the production verifier accept.
 */
class JwksTestKey(
  val keyId: JwkKeyId,
  keyPair: KeyPair,
) {
  val publicKey: RSAPublicKey = keyPair.public as RSAPublicKey
  val privateKey: RSAPrivateKey = keyPair.private as RSAPrivateKey

  /**
   * The JWK of this key, in the shape Keycloak publishes it.
   */
  fun jwkJson(): String = """
    {
      "kid": "${keyId.value}",
      "kty": "RSA",
      "alg": "RS256",
      "use": "sig",
      "n": "${publicKey.modulus.toBase64Url()}",
      "e": "${publicKey.publicExponent.toBase64Url()}"
    }
  """.trimIndent()

  companion object {
    private val generator: KeyPairGenerator = KeyPairGenerator.getInstance("RSA").also { it.initialize(2048) }

    fun generate(keyId: String): JwksTestKey = JwksTestKey(JwkKeyId(keyId), generator.generateKeyPair())

    private fun java.math.BigInteger.toBase64Url(): String =
      Base64.getUrlEncoder().withoutPadding().encodeToString(toByteArray().dropWhile { it == 0.toByte() }.toByteArray())
  }
}

/**
 * The JWKS document publishing [keys], as the realm's `openid-connect/certs` endpoint would.
 */
fun jwksJsonOf(vararg keys: JwksTestKey): String =
  """{"keys": [${keys.joinToString(",") { it.jwkJson() }}]}"""

fun jwksDocumentOf(vararg keys: JwksTestKey): JwksDocument = JwksDocument.parse(jwksJsonOf(*keys))
