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
package it.neckar.customer.company

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class RegistrationNumberTest {

  @Test
  fun `normalizes Handelsregister B registration number`() {
    assertThat(RegistrationNumber("HRB 25478").value).isEqualTo("HRB 25478")
  }

  @Test
  fun `uppercases known prefix`() {
    assertThat(RegistrationNumber("hrb 25478").value).isEqualTo("HRB 25478")
  }

  @Test
  fun `strips colon after prefix`() {
    assertThat(RegistrationNumber("HRB: 745905").value).isEqualTo("HRB 745905")
  }

  @Test
  fun `collapses multiple spaces`() {
    assertThat(RegistrationNumber("HRB   25478").value).isEqualTo("HRB 25478")
  }

  @Test
  fun `trims leading and trailing whitespace`() {
    assertThat(RegistrationNumber("  HRB 25478  ").value).isEqualTo("HRB 25478")
  }

  @Test
  fun `different input formats produce equal instances`() {
    val plain = RegistrationNumber("HRB 25478")
    val withColon = RegistrationNumber("HRB: 25478")
    val lowercase = RegistrationNumber("hrb 25478")

    assertThat(plain).isEqualTo(withColon)
    assertThat(plain).isEqualTo(lowercase)
  }

  @Test
  fun `accepts Handelsregister Abteilung A prefix`() {
    assertThat(RegistrationNumber("hra 95175").value).isEqualTo("HRA 95175")
  }

  @Test
  fun `normalizes Genossenschaftsregister prefix to official mixed-case form`() {
    assertThat(RegistrationNumber("gnr 1234").value).isEqualTo("GnR 1234")
    assertThat(RegistrationNumber("GNR 1234").value).isEqualTo("GnR 1234")
    assertThat(RegistrationNumber("GnR 1234").value).isEqualTo("GnR 1234")
  }

  @Test
  fun `accepts Partnerschaftsregister prefix`() {
    assertThat(RegistrationNumber("pr 4711").value).isEqualTo("PR 4711")
  }

  @Test
  fun `accepts Vereinsregister prefix`() {
    assertThat(RegistrationNumber("vr 8888").value).isEqualTo("VR 8888")
  }

  @Test
  fun `leaves unknown prefix unchanged`() {
    assertThat(RegistrationNumber("Reg 12345").value).isEqualTo("Reg 12345")
  }

  @Test
  fun `rejects blank input`() {
    assertThrows<IllegalArgumentException> {
      RegistrationNumber("   ")
    }
  }

  @Test
  fun `rejects input without any digit`() {
    assertThrows<IllegalArgumentException> {
      RegistrationNumber("HRB")
    }
  }

  @Test
  fun `serialization roundtrip preserves canonical format`() {
    val number = RegistrationNumber("hrb: 25478")
    val json = Json.encodeToString(number)
    assertThat(json).isEqualTo("\"HRB 25478\"")

    val deserialized = Json.decodeFromString<RegistrationNumber>(json)
    assertThat(deserialized).isEqualTo(number)
  }

  @Test
  fun `deserialization normalizes input`() {
    val deserialized = Json.decodeFromString<RegistrationNumber>("\"  hrb:   25478  \"")
    assertThat(deserialized.value).isEqualTo("HRB 25478")
  }

  @Test
  fun `toString returns canonical format`() {
    assertThat(RegistrationNumber("hrb: 25478").toString()).isEqualTo("HRB 25478")
  }

  @Test
  fun `examples carry their canonical value`() {
    assertThat(RegistrationNumberExample.handelsregisterAbteilungA.value).isEqualTo("HRA 95175")
    assertThat(RegistrationNumberExample.handelsregisterAbteilungB.value).isEqualTo("HRB 25478")
    assertThat(RegistrationNumberExample.genossenschaftsregister.value).isEqualTo("GnR 1234")
    assertThat(RegistrationNumberExample.vereinsregister.value).isEqualTo("VR 4321")
  }
}
