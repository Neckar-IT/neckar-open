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
import it.neckar.open.kotlin.lang.random
import it.neckar.open.test.utils.RandomWithSeed
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.uuid.Uuid
import kotlin.uuid.toKotlinUuid

/**
 *
 */
class UuidExtKtTest {
  @Test
  fun testEncodeToUrl() {
    assertThat(Uuid.parse("572d0e91-5b71-444c-9487-3ade5067f3a6").encodeForUrl()).isEqualTo("Vy0OkVtxREyUhzreUGfzpg")
  }

  @Test
  fun testDecodeFromUrl() {
    val uuid = Uuid.decodeFromUrl("xTjJk35SSAmll5i4yaFAgw")
    assertThat(uuid.toString()).isEqualTo("c538c993-7e52-4809-a597-98b8c9a14083")
  }

  @Test
  fun testencodeForUrl() {
    val uuid = Uuid.parse("877ce1f7-a677-473c-9b32-817fe5c46b92")

    val encodedForUrl = uuid.encodeForUrl()
    assertThat(encodedForUrl).isEqualTo("h3zh96Z3RzybMoF_5cRrkg")
    assertThat(Uuid.decodeFromUrl(encodedForUrl)).isEqualTo(uuid)

  }

  @RandomWithSeed(99)
  @Test
  fun testPlus() {
    val base = Uuid.pseudoRandom()
    assertThat(base.toString()).isEqualTo("89d2a8ea-60fc-4be7-a37f-7e7ddb3c84cb")
    assertThat(base.plus(1).toString()).isEqualTo("89d2a8ea-60fc-4be7-a37f-7e7ddb3c84cc")
  }

  @RandomWithSeed(99)
  @Test
  fun testEnumsUuidTest() {
    val base = Uuid.pseudoRandom()
    assertThat(MyFooBarEnum.Bar.name.toByteArray().sum()).isEqualTo(277)
    assertThat(MyFooBarEnum.Foo.name.toByteArray().sum()).isEqualTo(292)

    assertThat(base.plus(MyFooBarEnum.Bar)).isEqualTo(base.plus(277))
    assertThat(base.plus(MyFooBarEnum.Foo)).isEqualTo(base.plus(292))
  }

  private enum class MyFooBarEnum {
    Foo, Bar
  }

  @Test
  fun test() {
    val first: Uuid = UUID.fromString("fef44139-de86-4ff2-a2a4-8c939377f203").toKotlinUuid()
    val second = UUID.fromString("56ea54cc-3fb9-4705-a342-46f8fe235e3e").toKotlinUuid()

    val newUuid = first.xor(second)

    assertThat(newUuid, "First Uuid: $first, New Uuid: $newUuid; should have been different!").isNotEqualTo(first)
    assertThat(newUuid, "Second Uuid: $second, New Uuid: $newUuid; should have been different!").isNotEqualTo(second)

    assertThat(newUuid.toString()).isEqualTo("a81e15f5-e13f-08f7-01e6-ca6b6d54ac3d")
  }

  @RandomWithSeed(99)
  @Test
  fun testRandom() {
    assertThat(random.nextInt(100)).isEqualTo(97)
    assertThat(random.nextInt(100)).isEqualTo(51)

    assertThat(Uuid.pseudoRandom().toString()).isEqualTo("237f7e7d-db3c-44cb-beed-ec9b569ac964")
    assertThat(Uuid.pseudoRandom().toString()).isEqualTo("de4bd42b-70d1-481c-ad15-8476b776a4fe")
  }

  @RandomWithSeed(50)
  @Test
  fun testRandomSeed50() {
    assertThat(random.nextInt(100)).isEqualTo(54)
    assertThat(random.nextInt(100)).isEqualTo(86)

    assertThat(Uuid.pseudoRandom().toString()).isEqualTo("25f0f998-45db-47c5-8cd0-7e1e297d09a5")
    assertThat(Uuid.pseudoRandom().toString()).isEqualTo("96f66ffb-b5a9-477c-8bc4-b3cfb552db11")
  }

  @RandomWithSeed(50)
  @Test
  fun testRandomUuid() {
    assertThat(random.nextInt(100)).isEqualTo(54)
    assertThat(random.nextInt(100)).isEqualTo(86)

    //Works - because we are in a test Environment
    assertThat(Uuid.pseudoRandom().toString()).isEqualTo("25f0f998-45db-47c5-8cd0-7e1e297d09a5")
  }

  @RandomWithSeed(99)
  @Test
  fun pseudoRandomProducesValidRfc4122V4() {
    repeat(100) {
      val uuid = Uuid.pseudoRandom().toString()
      // xxxxxxxx-xxxx-Mxxx-Nxxx-xxxxxxxxxxxx — M (index 14) is the version nibble, N (index 19) the variant.
      assertThat(uuid[14], "version nibble of $uuid").isEqualTo('4')
      assertThat(uuid[19], "variant nibble of $uuid").isIn('8', '9', 'a', 'b')
    }
  }
}


