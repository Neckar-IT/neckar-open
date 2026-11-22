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

import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isNull
import assertk.assertions.messageContains
import it.neckar.open.resources.getResourceSafe
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory

/**
 * Holds [IBAN] to the conformance corpus `IBAN.cases.json`, which the Vitest of `validateIbanShape` in
 * `@neckar-hub/typescript-utils/payment-formats` reads as well.
 */
class IBANTest {

  private val corpus: Corpus = Json.decodeFromString(javaClass.getResourceSafe("/IBAN.cases.json").readText())

  @TestFactory
  fun `parse decides every conformance case`(): List<DynamicTest> = corpus.cases.map { case ->
    DynamicTest.dynamicTest("\"${case.input}\"") {
      when (val parsed = IBAN.parse(case.input)) {
        is IbanParseResult.Valid -> {
          assertThat(case.violation, "expected a violation for \"${case.input}\"").isNull()
          assertThat(parsed.iban.value).isEqualTo(case.canonical)
        }

        is IbanParseResult.Invalid -> {
          assertThat(case.canonical, "expected \"${case.input}\" to be valid").isNull()
          assertThat(parsed.violation).isEqualTo(case.violation)
        }
      }
    }
  }

  @Test
  fun `invoke throws for an input parse rejects`() {
    assertFailure { IBAN("DE8937040044") }
      .isInstanceOf(IllegalArgumentException::class)
      .messageContains("InvalidLength")
  }

  @Test
  fun `the corpus covers every violation`() {
    assertThat(corpus.cases.mapNotNull { it.violation }.toSet()).isEqualTo(IbanFormatViolation.entries.toSet())
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
  fun `examples carry their canonical value`() {
    assertThat(IBANExample.germany.value).isEqualTo("DE89 3704 0044 0532 0130 00")
    assertThat(IBANExample.austria.value).isEqualTo("AT61 1904 3002 3457 3201")
    assertThat(IBANExample.switzerland.value).isEqualTo("CH93 0076 2011 6238 5295 7")
    assertThat(IBANExample.france.value).isEqualTo("FR76 3000 6000 0112 3456 7890 189")
    assertThat(IBANExample.unitedKingdom.value).isEqualTo("GB29 NWBK 6016 1331 9268 19")
    assertThat(IBANExample.netherlands.value).isEqualTo("NL91 ABNA 0417 1643 00")
    assertThat(IBANExample.spain.value).isEqualTo("ES91 2100 0418 4502 0005 1332")
    assertThat(IBANExample.italy.value).isEqualTo("IT60 X054 2811 1010 0000 0123 456")
  }

  @Serializable
  private data class Corpus(val purpose: String, val frontendDetectable: List<IbanFormatViolation>, val cases: List<Case>)

  @Serializable
  private data class Case(val input: String, val canonical: String? = null, val violation: IbanFormatViolation? = null)
}
