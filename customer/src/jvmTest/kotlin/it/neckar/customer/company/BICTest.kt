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
 * Holds [BIC] to the conformance corpus `BIC.cases.json`, which the Vitest of `validateBicShape` in
 * `@neckar-hub/typescript-utils/payment-formats` reads as well.
 */
class BICTest {

  private val corpus: Corpus = Json.decodeFromString(javaClass.getResourceSafe("/BIC.cases.json").readText())

  @TestFactory
  fun `parse decides every conformance case`(): List<DynamicTest> = corpus.cases.map { case ->
    DynamicTest.dynamicTest("\"${case.input}\"") {
      when (val parsed = BIC.parse(case.input)) {
        is BicParseResult.Valid -> {
          assertThat(case.violation, "expected a violation for \"${case.input}\"").isNull()
          assertThat(parsed.bic.value).isEqualTo(case.canonical)
        }

        is BicParseResult.Invalid -> {
          assertThat(case.canonical, "expected \"${case.input}\" to be valid").isNull()
          assertThat(parsed.violation).isEqualTo(case.violation)
        }
      }
    }
  }

  @Test
  fun `invoke throws for an input parse rejects`() {
    assertFailure { BIC("COBADE") }
      .isInstanceOf(IllegalArgumentException::class)
      .messageContains("InvalidLength")
  }

  @Test
  fun `the corpus covers every violation`() {
    assertThat(corpus.cases.mapNotNull { it.violation }.toSet()).isEqualTo(BicFormatViolation.entries.toSet())
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
  fun `examples carry their canonical value`() {
    assertThat(BICExample.commerzbank.value).isEqualTo("COBADEFFXXX")
    assertThat(BICExample.deutscheBank.value).isEqualTo("DEUTDEDBFRA")
    assertThat(BICExample.sparkasse.value).isEqualTo("SOLADES1TBB")
    assertThat(BICExample.volksbank.value).isEqualTo("GENODEFF701")
    assertThat(BICExample.shortForm.value).isEqualTo("COBADEFF")
  }

  @Serializable
  private data class Corpus(val purpose: String, val frontendDetectable: List<BicFormatViolation>, val cases: List<Case>)

  @Serializable
  private data class Case(val input: String, val canonical: String? = null, val violation: BicFormatViolation? = null)
}
