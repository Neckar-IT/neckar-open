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

class TaxIdTest {

  @Test
  fun `normalizes German tax ID`() {
    assertThat(TaxId("DE192736450").value).isEqualTo("DE192736450")
  }

  @Test
  fun `normalizes tax ID with spaces`() {
    assertThat(TaxId("DE 192 736 450").value).isEqualTo("DE192736450")
  }

  @Test
  fun `normalizes lowercase input`() {
    assertThat(TaxId("de192736450").value).isEqualTo("DE192736450")
  }

  @Test
  fun `trims leading and trailing whitespace`() {
    assertThat(TaxId("  DE192736450  ").value).isEqualTo("DE192736450")
  }

  @Test
  fun `different input formats produce equal instances`() {
    val compact = TaxId("DE192736450")
    val spaced = TaxId("DE 192 736 450")
    val lowercase = TaxId("de192736450")

    assertThat(compact).isEqualTo(spaced)
    assertThat(compact).isEqualTo(lowercase)
  }

  @Test
  fun `accepts Netherlands tax ID with letter and digits after prefix`() {
    assertThat(TaxId("NL123456789B01").value).isEqualTo("NL123456789B01")
  }

  @Test
  fun `accepts Austrian tax ID starting with letter after prefix`() {
    assertThat(TaxId("ATU12345678").value).isEqualTo("ATU12345678")
  }

  @Test
  fun `rejects blank input`() {
    assertThrows<IllegalArgumentException> {
      TaxId("   ")
    }
  }

  @Test
  fun `rejects too short input`() {
    assertThrows<IllegalArgumentException> {
      TaxId("DE1")
    }
  }

  @Test
  fun `rejects too long input`() {
    assertThrows<IllegalArgumentException> {
      TaxId("DE12345678901234567890")
    }
  }

  @Test
  fun `rejects input without country prefix`() {
    assertThrows<IllegalArgumentException> {
      TaxId("1234567890")
    }
  }

  @Test
  fun `rejects input with non-alphanumeric characters in body`() {
    assertThrows<IllegalArgumentException> {
      TaxId("DE-12-34-56-78")
    }
  }

  @Test
  fun `serialization roundtrip preserves canonical format`() {
    val taxId = TaxId("DE 192 736 450")
    val json = Json.encodeToString(taxId)
    assertThat(json).isEqualTo("\"DE192736450\"")

    val deserialized = Json.decodeFromString<TaxId>(json)
    assertThat(deserialized).isEqualTo(taxId)
  }

  @Test
  fun `deserialization normalizes input`() {
    val deserialized = Json.decodeFromString<TaxId>("\"de 192 736 450\"")
    assertThat(deserialized.value).isEqualTo("DE192736450")
  }

  @Test
  fun `toString returns canonical format`() {
    assertThat(TaxId("de 192 736 450").toString()).isEqualTo("DE192736450")
  }

  @Test
  fun `examples carry their canonical value`() {
    assertThat(TaxIdExample.germany.value).isEqualTo("DE192736450")
    assertThat(TaxIdExample.austria.value).isEqualTo("ATU12345678")
    assertThat(TaxIdExample.france.value).isEqualTo("FR40303265045")
    assertThat(TaxIdExample.italy.value).isEqualTo("IT07643520567")
    assertThat(TaxIdExample.netherlands.value).isEqualTo("NL123456789B01")
  }
}
