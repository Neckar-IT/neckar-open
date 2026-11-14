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

class BICTest {

  @Test
  fun `normalizes to uppercase`() {
    assertThat(BIC("cobadeffxxx").value).isEqualTo("COBADEFFXXX")
  }

  @Test
  fun `trims whitespace`() {
    assertThat(BIC("  COBADEFFXXX  ").value).isEqualTo("COBADEFFXXX")
  }

  @Test
  fun `removes spaces`() {
    assertThat(BIC("COBA DEFF XXX").value).isEqualTo("COBADEFFXXX")
  }

  @Test
  fun `normalizes combined input`() {
    assertThat(BIC("  coba deff xxx  ").value).isEqualTo("COBADEFFXXX")
  }

  @Test
  fun `different input formats produce equal instances`() {
    val withSpaces = BIC("COBA DEFF XXX")
    val lowercase = BIC("cobadeffxxx")
    val trimmed = BIC("  COBADEFFXXX  ")

    assertThat(withSpaces).isEqualTo(lowercase)
    assertThat(withSpaces).isEqualTo(trimmed)
  }

  @Test
  fun `accepts 8-character BIC`() {
    assertThat(BIC("COBADEFF").value).isEqualTo("COBADEFF")
  }

  @Test
  fun `accepts 11-character BIC`() {
    assertThat(BIC("COBADEFFXXX").value).isEqualTo("COBADEFFXXX")
  }

  @Test
  fun `rejects blank input`() {
    assertThrows<IllegalArgumentException> {
      BIC("   ")
    }
  }

  @Test
  fun `rejects invalid length`() {
    assertThrows<IllegalArgumentException> {
      BIC("COBADE")
    }
    assertThrows<IllegalArgumentException> {
      BIC("COBADEFFXXXY")
    }
  }

  @Test
  fun `serialization roundtrip preserves canonical format`() {
    val bic = BIC("coba deff xxx")
    val json = Json.encodeToString(bic)
    assertThat(json).isEqualTo("\"COBADEFFXXX\"")

    val deserialized = Json.decodeFromString<BIC>(json)
    assertThat(deserialized).isEqualTo(bic)
  }

  @Test
  fun `deserialization normalizes input`() {
    val deserialized = Json.decodeFromString<BIC>("\"coba deff xxx\"")
    assertThat(deserialized.value).isEqualTo("COBADEFFXXX")
  }

  @Test
  fun `toString returns canonical format`() {
    assertThat(BIC("cobadeffxxx").toString()).isEqualTo("COBADEFFXXX")
  }

  @Test
  fun `parse returns Valid for a well-formed input in any format`() {
    val result = BIC.parse("coba deff xxx")
    assertThat(result).isEqualTo(BicParseResult.Valid(BIC("COBADEFFXXX")))
  }

  @Test
  fun `parse returns Invalid InvalidLength for a too short input`() {
    val result = BIC.parse("COBADE")
    assertThat(result).isEqualTo(BicParseResult.Invalid(BicFormatViolation.InvalidLength))
  }

  @Test
  fun `examples carry their canonical value`() {
    assertThat(BIC.Example.commerzbank.value).isEqualTo("COBADEFFXXX")
    assertThat(BIC.Example.deutscheBank.value).isEqualTo("DEUTDEDBFRA")
    assertThat(BIC.Example.sparkasse.value).isEqualTo("SOLADES1TBB")
    assertThat(BIC.Example.volksbank.value).isEqualTo("GENODEFF701")
    assertThat(BIC.Example.shortForm.value).isEqualTo("COBADEFF")
  }
}
