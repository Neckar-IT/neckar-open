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
package it.neckar.uuid

import assertk.*
import assertk.assertions.*
import it.neckar.open.test.utils.RandomWithSeed
import org.junit.jupiter.api.Test
import kotlin.io.encoding.Base64.PaddingOption
import kotlin.uuid.Uuid

class UuidExtKtBase64Test {
  @Test
  fun testIt() {
    val base64 = kotlin.io.encoding.Base64.UrlSafe.withPadding(PaddingOption.ABSENT_OPTIONAL)
    val decoded = base64.decode("YXNkZg")
    assertThat(decoded.decodeToString()).isEqualTo("asdf")
  }

  @Test
  fun testBase64Url() {
    val id: Uuid = Uuid.parse("f4dbf95d-aed2-4219-9255-8aad57fec3df")
    roundTripBase64Url(id, "9Nv5Xa7SQhmSVYqtV_7D3w")
  }

  @RandomWithSeed(42)
  @Test
  fun testBase64UrlWithRandomUUID() {
    val id: Uuid = Uuid.pseudoRandom()
    roundTripBase64Url(id, "Oe_MGme_QECnqz_J-Ucx3A")
  }

  private fun roundTripBase64Url(toEncode: Uuid, expectedResult: String) {
    // Encoding to Base64 URL safe
    val encoded = toEncode.encodeForUrl()
    assertThat(encoded).isEqualTo(expectedResult)
    assertThat(encoded).hasLength(22)
    // Decoding back to Uuid
    val decoded = Uuid.decodeFromUrl(encoded)
    assertThat(toEncode).isEqualTo(decoded)
  }
}
