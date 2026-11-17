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
package it.neckar.open.kotlin.bytearray

import assertk.*
import assertk.assertions.*
import it.neckar.open.kotlin.lang.hex
import org.junit.jupiter.api.Test

/**
 */
class ByteArrayBuilderTest {
  @Test
  internal fun testIt() {
    val builder = ByteArrayBuilder()
    builder.append(byteArrayOf(1))
    builder.append(byteArrayOf(2, 3))
    builder.append(4)
    builder.append(byteArrayOf(5))

    assertThat(builder.toByteArray().hex).isEqualTo("0102030405")
  }

  @Test
  internal fun testMore() {
    val builder = ByteArrayBuilder()
    assertThat(builder.toByteArray().hex).isEqualTo("")

    builder.f32(12.0f, true)
    builder.f32LE(12.0f)

    assertThat(builder.toByteArray().hex).isEqualTo("0000404100004041")
  }

  @Test
  internal fun testMinimal() {
    val builder = ByteArrayBuilder()
    assertThat(builder.toByteArray().hex).isEqualTo("")

    builder.s8(0)
    assertThat(builder.toByteArray().hex).isEqualTo("00")

    builder.s8(1)
    assertThat(builder.toByteArray().hex).isEqualTo("0001")

    builder.s8(0xff)
    assertThat(0xff).isEqualTo(255)
    assertThat(builder.toByteArray().hex).isEqualTo("0001ff")
  }
}
