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
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid
import kotlin.uuid.toKotlinUuid

/**
 *
 */
class KotlinUuidTest {
  @Test
  fun testIt() {
    val uuid: Uuid = Uuid.parse("a951d5c1-201d-4711-bab6-8d5cd089d2f2")
    assertThat(uuid.toString()).isEqualTo("a951d5c1-201d-4711-bab6-8d5cd089d2f2")
    assertThat(uuid.toHexString()).isEqualTo("a951d5c1201d4711bab68d5cd089d2f2")
  }

  @Test
  fun testDeserialization() {
    assertThat(Uuid.parse("1c896843-c222-4129-a5ac-130f0484e2ea").toString()).isEqualTo("1c896843-c222-4129-a5ac-130f0484e2ea")
  }

  @Test
  fun testJvm() {
    val javaUuid = UUID.fromString("e1cf0a5a-8073-4a4e-afd8-eb41e6a481da")
    val kotlinUuid = javaUuid.toKotlinUuid()
    assertThat(kotlinUuid.toString()).isEqualTo("e1cf0a5a-8073-4a4e-afd8-eb41e6a481da")
    assertThat(kotlinUuid.toJavaUuid()).isEqualTo(javaUuid)
  }

  @Test
  fun testDashHex() {
    assertThat(Uuid.parse("a951d5c1-201d-4711-bab6-8d5cd089d2f2").toHexString()).isEqualTo("a951d5c1201d4711bab68d5cd089d2f2")
    assertThat(Uuid.parse("a951d5c1-201d-4711-bab6-8d5cd089d2f2").toString()).isEqualTo("a951d5c1-201d-4711-bab6-8d5cd089d2f2")
  }
}
