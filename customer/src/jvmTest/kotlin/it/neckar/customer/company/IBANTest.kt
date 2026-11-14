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

class IBANTest {

  @Test
  fun `normalizes German IBAN`() {
    assertThat(IBAN("DE89370400440532013000").value).isEqualTo("DE89 3704 0044 0532 0130 00")
  }

  @Test
  fun `normalizes IBAN with spaces`() {
    assertThat(IBAN("DE89 3704 0044 0532 0130 00").value).isEqualTo("DE89 3704 0044 0532 0130 00")
  }

  @Test
  fun `normalizes lowercase input`() {
    assertThat(IBAN("de89370400440532013000").value).isEqualTo("DE89 3704 0044 0532 0130 00")
  }

  @Test
  fun `trims whitespace`() {
    assertThat(IBAN("  DE89370400440532013000  ").value).isEqualTo("DE89 3704 0044 0532 0130 00")
  }

  @Test
  fun `different input formats produce equal instances`() {
    val compact = IBAN("DE89370400440532013000")
    val spaced = IBAN("DE89 3704 0044 0532 0130 00")
    val lowercase = IBAN("de89370400440532013000")

    assertThat(compact).isEqualTo(spaced)
    assertThat(compact).isEqualTo(lowercase)
  }

  @Test
  fun `accepts short IBAN (15 characters, Norway)`() {
    // Norwegian IBANs are 15 characters
    assertThat(IBAN("NO9386011117947").value).isEqualTo("NO93 8601 1117 947")
  }

  @Test
  fun `accepts 30-character IBAN (Malta)`() {
    assertThat(IBAN("MT84MALT011000012345MTLCAST001S").value).isEqualTo("MT84 MALT 0110 0001 2345 MTLC AST0 01S")
  }

  @Test
  fun `accepts maximum length IBAN (34 characters)`() {
    // 34 alphanumeric characters is the maximum per ISO 13616
    val iban34 = IBAN("MT84MALT011000012345MTLCAST001SABC")
    assertThat(iban34.value).isEqualTo("MT84 MALT 0110 0001 2345 MTLC AST0 01SA BC")
  }

  @Test
  fun `rejects blank input`() {
    assertThrows<IllegalArgumentException> {
      IBAN("   ")
    }
  }

  @Test
  fun `rejects too short input`() {
    assertThrows<IllegalArgumentException> {
      IBAN("DE8937040044")
    }
  }

  @Test
  fun `rejects too long input`() {
    assertThrows<IllegalArgumentException> {
      IBAN("DE89370400440532013000123456789012345")
    }
  }

  @Test
  fun `serialization roundtrip preserves canonical format`() {
    val iban = IBAN("DE89370400440532013000")
    val json = Json.encodeToString(iban)
    assertThat(json).isEqualTo("\"DE89 3704 0044 0532 0130 00\"")

    val deserialized = Json.decodeFromString<IBAN>(json)
    assertThat(deserialized).isEqualTo(iban)
  }

  @Test
  fun `deserialization normalizes input`() {
    val deserialized = Json.decodeFromString<IBAN>("\"de89370400440532013000\"")
    assertThat(deserialized.value).isEqualTo("DE89 3704 0044 0532 0130 00")
  }

  @Test
  fun `toString returns canonical format`() {
    assertThat(IBAN("DE89370400440532013000").toString()).isEqualTo("DE89 3704 0044 0532 0130 00")
  }

  @Test
  fun `parse returns Valid for a well-formed input in any format`() {
    val result = IBAN.parse("de89 3704 0044 0532 0130 00")
    assertThat(result).isEqualTo(IbanParseResult.Valid(IBAN("DE89370400440532013000")))
  }

  @Test
  fun `parse returns Invalid InvalidLength for a too short input`() {
    val result = IBAN.parse("DE8937040044")
    assertThat(result).isEqualTo(IbanParseResult.Invalid(IbanFormatViolation.InvalidLength))
  }

  @Test
  fun `examples carry their canonical value`() {
    assertThat(IBAN.Example.germany.value).isEqualTo("DE89 3704 0044 0532 0130 00")
    assertThat(IBAN.Example.austria.value).isEqualTo("AT61 1904 3002 3457 3201")
    assertThat(IBAN.Example.switzerland.value).isEqualTo("CH93 0076 2011 6238 5295 7")
    assertThat(IBAN.Example.france.value).isEqualTo("FR76 3000 6000 0112 3456 7890 189")
    assertThat(IBAN.Example.unitedKingdom.value).isEqualTo("GB29 NWBK 6016 1331 9268 19")
    assertThat(IBAN.Example.netherlands.value).isEqualTo("NL91 ABNA 0417 1643 00")
    assertThat(IBAN.Example.spain.value).isEqualTo("ES91 2100 0418 4502 0005 1332")
    assertThat(IBAN.Example.italy.value).isEqualTo("IT60 X054 2811 1010 0000 0123 456")
  }
}
