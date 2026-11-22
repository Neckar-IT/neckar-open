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
package it.neckar.financial.currency

import assertk.assertThat
import assertk.assertions.isEqualTo
import it.neckar.open.i18n.I18nConfiguration
import it.neckar.open.kotlin.lang.WhitespaceConfig
import it.neckar.open.resources.getResourceSafe
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory

/**
 * Holds the money rules the cent functions of `@neckar-hub/script-money` mirror to `scriptMoney.cases.json`, which
 * `scriptMoney.test.ts` reads as well.
 */
class ScriptMoneyCorpusTest {

  private val corpus: Corpus = Json.decodeFromString(javaClass.getResourceSafe("/scriptMoney.cases.json").readText())

  @TestFactory
  fun `the tax and the gross of a net amount are the ones of the corpus`(): List<DynamicTest> = corpus.vat.map { case ->
    DynamicTest.dynamicTest("${case.netCents} ct at ${case.vatRate}") {
      val withVat = Money.WithVat(Money.Net(case.netCents), VatRate(case.vatRate))

      assertThat(withVat.vatAmount.cents).isEqualTo(case.vatCents)
      assertThat(withVat.gross.cents).isEqualTo(case.grossCents)
    }
  }

  @TestFactory
  fun `an amount times a factor rounds to the one of the corpus`(): List<DynamicTest> = corpus.timesAndRound.map { case ->
    DynamicTest.dynamicTest("${case.cents} ct times ${case.factor}") {
      assertThat(Money(case.cents).timesAndRound(case.factor).cents).isEqualTo(case.resultCents)
    }
  }

  @TestFactory
  fun `the German euro text of an amount is the one of the corpus`(): List<DynamicTest> = corpus.euro.map { case ->
    DynamicTest.dynamicTest("${case.cents} ct") {
      assertThat(Money.Gross(case.cents).format(I18nConfiguration.Germany, WhitespaceConfig.Spaces)).isEqualTo(case.text)
    }
  }

  @Serializable
  private data class Corpus(val purpose: String, val vat: List<VatCase>, val timesAndRound: List<TimesAndRoundCase>, val euro: List<EuroCase>)

  @Serializable
  private data class VatCase(val netCents: Long, val vatRate: Double, val vatCents: Long, val grossCents: Long)

  @Serializable
  private data class TimesAndRoundCase(val cents: Long, val factor: Double, val resultCents: Long)

  @Serializable
  private data class EuroCase(val cents: Long, val text: String)
}
