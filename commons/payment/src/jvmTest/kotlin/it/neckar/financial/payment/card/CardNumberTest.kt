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
package it.neckar.financial.payment.card

import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.endsWith
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.messageContains
import it.neckar.open.resources.getResourceSafe
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory

/**
 * Holds [CardNumber] to the conformance corpus `CardNumber.cases.json`, which the Vitest of `validateCardNumberShape` in
 * `@neckar-hub/typescript-utils/payment-formats` reads as well.
 */
class CardNumberTest {

  private val corpus: Corpus = Json.decodeFromString(javaClass.getResourceSafe("/CardNumber.cases.json").readText())

  @TestFactory
  fun `invoke decides every conformance case`(): List<DynamicTest> = corpus.cases.map { case ->
    DynamicTest.dynamicTest("\"${case.input}\"") {
      if (case.violation == null) {
        assertThat(CardNumber(case.input).value).isEqualTo(case.canonical)
      } else {
        assertFailure { CardNumber(case.input) }
          .isInstanceOf(IllegalArgumentException::class)
          .messageContains(case.violation.messageFragment)
      }
    }
  }

  @Test
  fun `the corpus holds a case for every violation the test names`() {
    assertThat(corpus.cases.mapNotNull { it.violation }.toSet()).isEqualTo(CardNumberFormatViolation.entries.toSet())
  }

  @Test
  fun `masks every allowed length without throwing and keeps only the last four digits visible`() {
    (13..19).forEach { length ->
      val raw = "4".repeat(length - 4) + "1234"
      val cardNumber = CardNumber(raw)
      assertThat(cardNumber.value.filter { it.isDigit() }).isEqualTo("1234")
      assertThat(cardNumber.value).endsWith("1234")
    }
  }

  /** The violations the corpus names; [CardNumber] throws with a message containing [messageFragment]. */
  @Serializable
  private enum class CardNumberFormatViolation(val messageFragment: String) {
    InvalidDigitCount("digits"),
  }

  @Serializable
  private data class Corpus(val purpose: String, val frontendDetectable: List<CardNumberFormatViolation>, val cases: List<Case>)

  @Serializable
  private data class Case(val input: String, val canonical: String? = null, val violation: CardNumberFormatViolation? = null)
}
