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
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotEmpty
import it.neckar.open.resources.getResourceSafe
import it.neckar.open.time.VirtualNowProvider
import it.neckar.open.time.nowProvider
import it.neckar.open.time.resetNowProvider
import kotlin.time.Instant
import kotlinx.datetime.YearMonth
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory

/**
 * Holds [ExpiryDate] to `ExpiryDate.cases.json`, which the Vitest of `parseExpiryDate`, `formatExpiryDate` and `validateExpiryDateShape` in
 * `@neckar-hub/typescript-utils/payment-formats` reads as well.
 */
class ExpiryDateTest {

  private val corpus: Corpus = Json.decodeFromString(javaClass.getResourceSafe("/ExpiryDate.cases.json").readText())

  @BeforeEach
  fun fixNow() {
    nowProvider = VirtualNowProvider(Instant.parse(corpus.now).toEpochMilliseconds().toDouble())
  }

  @AfterEach
  fun resetNow() {
    resetNowProvider()
  }

  @TestFactory
  fun `encodes year and month as the wire form`(): List<DynamicTest> = corpus.cases.mapNotNull { case ->
    val wire = case.wire ?: return@mapNotNull null
    DynamicTest.dynamicTest(wire) {
      assertThat(Json.encodeToString(ExpiryDate(case.year, case.month))).isEqualTo(Json.encodeToString(wire))
    }
  }

  @TestFactory
  fun `decodes the wire form into year and month`(): List<DynamicTest> = corpus.cases.mapNotNull { case ->
    val wire = case.wire ?: return@mapNotNull null
    DynamicTest.dynamicTest(wire) {
      val decoded: ExpiryDate = Json.decodeFromString(Json.encodeToString(wire))
      assertThat(decoded.yearMonth).isEqualTo(YearMonth(case.year, case.month))
    }
  }

  @TestFactory
  fun `refuses a month before now`(): List<DynamicTest> {
    val inPast = corpus.cases.filter { it.violation == InPast }
    assertThat(inPast).isNotEmpty()
    return inPast.map { case ->
      DynamicTest.dynamicTest("${case.year}-${case.month}") {
        assertFailure { ExpiryDate(case.year, case.month) }.isInstanceOf<IllegalArgumentException>()
      }
    }
  }

  @Serializable
  private data class Corpus(val purpose: String, val now: String, val frontendDetectable: List<String>, val cases: List<Case>)

  @Serializable
  private data class Case(val wire: String? = null, val year: Int, val month: Int, val violation: String? = null)

  private companion object {
    const val InPast: String = "InPast"
  }
}
