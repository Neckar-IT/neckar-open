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
package it.neckar.open.app

import it.neckar.open.app.password.InvalidPasswordException
import it.neckar.open.app.password.PasswordUtils.calculateMD5Hash
import it.neckar.open.app.password.PasswordUtils.hasExpectedHash
import it.neckar.open.app.password.PasswordUtils.validatePasswordHash
import org.apache.commons.codec.DecoderException
import org.apache.commons.codec.binary.Hex
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.nio.charset.StandardCharsets
import kotlin.test.assertFailsWith

/**
 *
 */
class PasswordUtilsTest {
  @Test
  @Throws(DecoderException::class)
  fun testIt() {
    Assertions.assertEquals("1a1dc91c907325c69271ddf0c944bc72", String(Hex.encodeHex(calculateMD5Hash("pass"))))
    Assertions.assertEquals("ea847988ba59727dbf4e34ee75726dc3", String(Hex.encodeHex(calculateMD5Hash("topsecret"))))
    Assertions.assertTrue(hasExpectedHash("pass", Hex.decodeHex("1a1dc91c907325c69271ddf0c944bc72".toCharArray())))
    Assertions.assertTrue(hasExpectedHash("topsecret", Hex.decodeHex("ea847988ba59727dbf4e34ee75726dc3".toCharArray())))
    Assertions.assertFalse(hasExpectedHash("topsecret", null))
    Assertions.assertFalse(hasExpectedHash("topsecret", Hex.decodeHex("1234".toCharArray())))
  }

  @Test
  @Throws(InvalidPasswordException::class)
  fun testEx() {
    validatePasswordHash("a".toByteArray(StandardCharsets.UTF_8), "a".toByteArray(StandardCharsets.UTF_8))
    assertFailsWith<InvalidPasswordException> {
      validatePasswordHash("a".toByteArray(StandardCharsets.UTF_8), "b".toByteArray(StandardCharsets.UTF_8))
    }
  }

  @Test
  @Throws(InvalidPasswordException::class)
  fun testEx2() {
    assertFailsWith<InvalidPasswordException> {
      validatePasswordHash(null, null)
    }
  }

  @Test
  @Throws(InvalidPasswordException::class)
  fun testEx3() {
    assertFailsWith<InvalidPasswordException> {
      validatePasswordHash("a".toByteArray(StandardCharsets.UTF_8), "ab".toByteArray(StandardCharsets.UTF_8))
    }
  }

  @Test
  @Throws(InvalidPasswordException::class)
  fun testEx4() {
    assertFailsWith<InvalidPasswordException> {
      validatePasswordHash("a".toByteArray(StandardCharsets.UTF_8), null)
    }
  }
}
