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

import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.containsExactlyInAnyOrder
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isInstanceOf
import assertk.assertions.isNull
import assertk.assertions.messageContains
import org.junit.jupiter.api.Test

class JwksDocumentTest {
  private val signing = JwksTestKey.generate("signing-key")
  private val encryption = JwksTestKey.generate("encryption-key")

  @Test
  fun `parses every key of the document by its kid`() {
    val document = jwksDocumentOf(signing, encryption)

    assertThat(document.keyIds).containsExactlyInAnyOrder(signing.keyId, encryption.keyId)
    assertThat(document[signing.keyId]).isNotNull()
    assertThat(document[JwkKeyId("unknown")]).isNull()
  }

  @Test
  fun `keeps the raw text it was parsed from`() {
    val raw = jwksJsonOf(signing)
    assertThat(JwksDocument.parse(raw).raw).isEqualTo(raw)
  }

  @Test
  fun `parses the public key so the verifier can use it`() {
    val jwk = jwksDocumentOf(signing)[signing.keyId]

    assertThat(jwk!!.algorithm).isEqualTo("RS256")
    assertThat(jwk.publicKey).isEqualTo(signing.publicKey)
  }

  @Test
  fun `rejects a document without a keys array`() {
    assertFailure { JwksDocument.parse("""{"nope": []}""") }
      .isInstanceOf(IllegalArgumentException::class)
      .messageContains("no 'keys' array")
  }

  @Test
  fun `rejects a document whose keys array holds something other than a key`() {
    assertFailure { JwksDocument.parse("""{"keys": [42]}""") }
      .isInstanceOf(IllegalArgumentException::class)
      .messageContains("'keys[0]' holds a number instead of an object")
  }

  @Test
  fun `rejects a document whose kid is no string`() {
    assertFailure { JwksDocument.parse("""{"keys": [{"kid": 42, "kty": "RSA", "n": "abc", "e": "AQAB"}]}""") }
      .isInstanceOf(IllegalArgumentException::class)
      .messageContains("'keys[0].kid' holds a number instead of a string")
  }

  @Test
  fun `rejects a document whose keys are no array`() {
    assertFailure { JwksDocument.parse("""{"keys": {}}""") }
      .isInstanceOf(IllegalArgumentException::class)
      .messageContains("'keys' holds an object instead of an array")
  }

  @Test
  fun `drops the key without a kid and keeps the others`() {
    val document = JwksDocument.parse(
      """{"keys": [{"kty": "RSA", "n": "abc", "e": "AQAB"}, ${signing.jwkJson()}]}"""
    )

    assertThat(document.keyIds).containsExactlyInAnyOrder(signing.keyId)
  }

  @Test
  fun `drops a key whose kid is blank rather than failing the whole document`() {
    val document = JwksDocument.parse(
      """{"keys": [{"kid": "  ", "kty": "RSA", "n": "abc", "e": "AQAB"}, ${signing.jwkJson()}]}"""
    )

    assertThat(document.keyIds).containsExactlyInAnyOrder(signing.keyId)
  }

  @Test
  fun `rejects a document whose keys carry no kid`() {
    assertFailure { JwksDocument.parse("""{"keys": [{"kty": "RSA", "n": "abc", "e": "AQAB"}]}""") }
      .isInstanceOf(IllegalArgumentException::class)
      .messageContains("no key with a 'kid'")
  }
}
