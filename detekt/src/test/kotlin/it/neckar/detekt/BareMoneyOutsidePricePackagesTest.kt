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
package it.neckar.detekt

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.hasSize
import assertk.assertions.isEmpty
import dev.detekt.test.TestConfig
import dev.detekt.test.lint
import org.junit.jupiter.api.Test

class BareMoneyOutsidePricePackagesTest {

  private val rule = BareMoneyOutsidePricePackages(
    TestConfig("pricePackages" to listOf("it.neckar.price", "it.neckar.financial.priced")),
  )

  @Test
  fun `flags a bare Money property in a price package`() {
    val findings = rule.lint(
      """
      package it.neckar.price.rest.views

      import it.neckar.financial.currency.Money

      data class PriceView(val price: Money)
      """.trimIndent(),
    )

    assertThat(findings).hasSize(1)
    assertThat(findings[0].message).contains("it.neckar.price.rest.views")
  }

  @Test
  fun `flags a nullable Money, a Money type argument and a Money return type`() {
    val findings = rule.lint(
      """
      package it.neckar.financial.priced

      import it.neckar.financial.currency.Money

      class Prices(val optional: Money?, val all: List<Money>) {
        fun first(): Money = all.first()
      }
      """.trimIndent(),
    )

    assertThat(findings).hasSize(3)
  }

  @Test
  fun `flags the fully qualified Money without an import`() {
    val findings = rule.lint(
      """
      package it.neckar.price

      data class PriceView(val price: it.neckar.financial.currency.Money)
      """.trimIndent(),
    )

    assertThat(findings).hasSize(1)
  }

  @Test
  fun `accepts the typed amounts in a price package`() {
    val findings = rule.lint(
      """
      package it.neckar.price

      import it.neckar.financial.currency.Money

      data class Sums(val net: Money.Net, val vat: Money.Vat?, val gross: List<Money.Gross>, val line: Money.WithVat)
      """.trimIndent(),
    )

    assertThat(findings).isEmpty()
  }

  @Test
  fun `accepts a bare Money outside the price packages`() {
    val findings = rule.lint(
      """
      package it.neckar.elektromeister.pv.loanterms

      import it.neckar.financial.currency.Money

      data class LoanTerms(val loanAmount: Money)
      """.trimIndent(),
    )

    assertThat(findings).isEmpty()
  }

  @Test
  fun `accepts another Money that is not the currency one`() {
    val findings = rule.lint(
      """
      package it.neckar.price

      import org.example.game.Money

      data class Score(val coins: Money)
      """.trimIndent(),
    )

    assertThat(findings).isEmpty()
  }

  @Test
  fun `a package entry never matches a longer package name by prefix`() {
    val findings = rule.lint(
      """
      package it.neckar.pricelist

      import it.neckar.financial.currency.Money

      data class ListEntry(val amount: Money)
      """.trimIndent(),
    )

    assertThat(findings).isEmpty()
  }

  @Test
  fun `flags Money reached through a star import or an import alias`() {
    val starImport = rule.lint(
      """
      package it.neckar.price

      import it.neckar.financial.currency.*

      data class PriceView(val price: Money)
      """.trimIndent(),
    )
    val alias = rule.lint(
      """
      package it.neckar.price

      import it.neckar.financial.currency.Money as Amount

      data class PriceView(val price: Amount, val score: Money)

      class Money
      """.trimIndent(),
    )

    assertThat(starImport).hasSize(1)
    assertThat(alias).hasSize(1)
  }

  @Test
  fun `flags Money in a function type, a type check and a type alias`() {
    val findings = rule.lint(
      """
      package it.neckar.price

      import it.neckar.financial.currency.Money

      typealias Amount = Money

      fun handle(any: Any, consumer: (Money) -> Unit) {
        if (any is Money) consumer(any)
      }
      """.trimIndent(),
    )

    assertThat(findings).hasSize(3)
  }
}
