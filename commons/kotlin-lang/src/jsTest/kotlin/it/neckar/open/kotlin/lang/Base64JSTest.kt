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
package it.neckar.open.kotlin.lang

import assertk.*
import assertk.assertions.*
import kotlin.test.Test


class Base64JSTest {
  @Test
  fun testEncoding() {
    assertThat("asdf1234".toBase64()).isEqualTo("YXNkZjEyMzQ=")
    assertThat("asdf1234".encodeToByteArray().toBase64()).isEqualTo("YXNkZjEyMzQ=")
  }

  @Test
  fun testDeltaUrl() {
    val content = byteArrayOf(252.toByte())
    assertThat(content.toBase64()).isNotEqualTo(content.toBase64Url())
  }

  @Test
  fun testBase64() {
    roundTripBase64("helloWorld".encodeToByteArray(), "aGVsbG9Xb3JsZA==")
    roundTripBase64("~g1\$".encodeToByteArray(), "fmcxJA==")
    roundTripBase64("$\"+~".encodeToByteArray(), "JCIrfg==")
    roundTripBase64(byteArrayOf(244.toByte(), 11.toByte(), Byte.MIN_VALUE, Byte.MAX_VALUE), "9AuAfw==")
    roundTripBase64(byteArrayOf(252.toByte()), "/A==")
  }

  private fun roundTripBase64(toEncode: ByteArray, expectedBase64: String) {
    val encoded = toEncode.toBase64()
    assertThat(encoded).isEqualTo(expectedBase64)
    assertThat(encoded.fromBase64String()).isEqualTo(toEncode.decodeToString())
  }

  @Test
  fun testBase64Url() {
    roundTripBase64Url("helloWorld".encodeToByteArray(), "aGVsbG9Xb3JsZA==")
    roundTripBase64Url("$\"+~".encodeToByteArray(), "JCIrfg==")
    roundTripBase64Url(byteArrayOf(244.toByte(), 11.toByte(), Byte.MIN_VALUE, Byte.MAX_VALUE), "9AuAfw==")
    roundTripBase64Url(byteArrayOf(252.toByte()), "_A==")
  }

  private fun roundTripBase64Url(toEncode: ByteArray, expectedBase64: String) {
    val encoded = toEncode.toBase64UrlString()
    assertThat(encoded).isEqualTo(expectedBase64)
    assertThat(encoded.fromBase64UrlString()).isEqualTo(toEncode.decodeToString())
  }
}
