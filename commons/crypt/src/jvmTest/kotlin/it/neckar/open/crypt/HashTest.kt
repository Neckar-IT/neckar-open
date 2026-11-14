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
package it.neckar.open.crypt

import assertk.*
import assertk.assertions.*
import it.neckar.open.collections.fastForEach
import it.neckar.open.crypt.Hash.Companion.fromHex
import it.neckar.open.resources.getResourceSafe
import org.apache.commons.io.IOUtils
import org.junit.jupiter.api.Test
import java.nio.charset.StandardCharsets

class HashTest {
  @Test
  fun testDigestTest() {
    assertThat(HashAlgorithm.SHA256.createMessageDigest()).isNotSameInstanceAs(HashAlgorithm.SHA256.createMessageDigest())
  }

  @Test
  fun testToString() {
    assertThat(Hash.fromHex(HashAlgorithm.MD5, "aabb").toString()).isEqualTo("[MD5: aabb]")
    assertThat(Hash.fromHex(HashAlgorithm.SHA256, "aabb1111111111").toString()).isEqualTo("[SHA-256: aabb1111111111]")
  }

  @Test
  @Throws(Exception::class)
  fun testString() {
    assertThat(HashAlgorithm.SHA256.calculate("asdf").valueAsHex).isEqualTo("f0e4c2f76c58916ec258f246851bea091d14d4247a2fc3e18694461b1816e13b")
    assertThat(HashAlgorithm.SHA256.calculate("asdf").valueAsHex).isEqualTo("f0e4c2f76c58916ec258f246851bea091d14d4247a2fc3e18694461b1816e13b")
  }

  @Test
  fun testMessageDigests() {
    HashAlgorithm.values().forEach {
      assertThat(it.createMessageDigest()).isNotNull()
    }
  }

  @Test
  fun testIt() {
    val photo = javaClass.getResourceSafe("/generated-test-photo.jpg")
    assertThat(photo).isNotNull()

    assertThat(HashAlgorithm.MD5.calculate(resource = photo).valueAsHex).isEqualTo("0d98dbfbaed11d56b2719411dd5e60f9") //value read using md5sum cmd line tool
    assertThat(HashAlgorithm.SHA1.calculate(resource = photo).valueAsHex).isEqualTo("f22fb940f4c162c5f80b01eb4e59b29b327a761c") //value read using sha1sum cmd line tool
    assertThat(HashAlgorithm.SHA1.calculate(IOUtils.toByteArray(photo.openStream())).valueAsHex).isEqualTo("f22fb940f4c162c5f80b01eb4e59b29b327a761c")
    assertThat(HashAlgorithm.SHA1.calculate(photo.openStream()).valueAsHex).isEqualTo("f22fb940f4c162c5f80b01eb4e59b29b327a761c")
  }

  @Test
  fun testRound() {
    val hash = Hash(HashAlgorithm.SHA256, "asdf".toByteArray(StandardCharsets.UTF_8))
    assertThat(hash.valueAsHex).isEqualTo("61736466")
    assertThat(hash).isEqualTo(fromHex(hash.algorithm, hash.valueAsHex))
  }

  @Test
  fun testAlgos() {
    val photo = javaClass.getResourceSafe("/generated-test-photo.jpg")
    assertThat(photo).isNotNull()

    HashAlgorithm.entries.fastForEach { algorithm ->
      val value = algorithm.calculate(resource = photo).valueAsHex
      assertThat(value).isNotNull()
      assertThat(value.length).isGreaterThanOrEqualTo(10)
    }
  }
}
