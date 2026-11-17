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
package it.neckar.financial.priced

import assertk.*
import assertk.assertions.*
import it.neckar.commons.tags.Tags
import it.neckar.financial.currency.Money
import it.neckar.financial.currency.PriceWithProfit
import it.neckar.financial.currency.VatRate
import it.neckar.financial.currency.euroNet
import it.neckar.financial.quote.Amount
import it.neckar.financial.quote.Optionality
import it.neckar.financial.quote.Visibility
import it.neckar.financial.quote.query.CalculationRelevanceQuery
import it.neckar.open.kotlin.lang.percent
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith

class PricedSectionTest {
  @Test
  fun testVisibility() {
    pricedSectionBuilder(headline = "Headline") {
    }.build()
      .also { pricedSection ->
        assertThat(pricedSection.children).isEmpty()
      }

    pricedSectionBuilder(headline = "Headline") {
      item(headline = "Pos1", priceForOneElement = PriceWithProfit.Zero, relevance = Tags.empty, visibility = Visibility.Public, vatRate = VatRate.NineteenPercent)
      item(headline = "Pos2", priceForOneElement = PriceWithProfit.Zero, relevance = Tags.empty, visibility = Visibility.Public, vatRate = VatRate.NineteenPercent)
    }.build()
      .also { pricedSection ->
        assertThat(pricedSection.children).hasSize(2)
      }


    //Add items with different visibilities!
    assertFailsWith<IllegalArgumentException> {
      pricedSectionBuilder(headline = "Headline") {
        item(headline = "Pos1", priceForOneElement = PriceWithProfit.Zero, relevance = Tags.empty, visibility = Visibility.Public, vatRate = VatRate.NineteenPercent)
        item(headline = "Pos2", priceForOneElement = PriceWithProfit.Zero, relevance = Tags.empty, visibility = Visibility.Internal, vatRate = VatRate.NineteenPercent)
      }.build()
    }

    //Add sections with different visibilities!
    assertFailsWith<IllegalArgumentException> {
      pricedSectionBuilder(headline = "Headline") {
        section("Pos1", Visibility.Public) {
          visibility = Visibility.Public
        }
        section("Pos2", Visibility.Public) {
          visibility = Visibility.Internal
        }
      }.build()
    }

  }

  @Test
  fun `testValues calculated`() {
    val pricedSection = PricedSection(headline = "section", visibility = Visibility.Public, children = listOf(mandatoryItem(100.0.euroNet), optionalItem(250.0.euroNet)))

    assertThat(pricedSection.netPrice).isEqualTo(350.0.euroNet)
    assertThat(pricedSection.optionality).isEqualTo(Optionality.Mandatory)
  }

  @Test
  fun `testValues set explicitly`() {
    val pricedSection = PricedSection(
      headline = "section",
      visibility = Visibility.Public,
      children = listOf(mandatoryItem(100.0.euroNet), optionalItem(250.0.euroNet)),
      optionality = Optionality.Optional,
      netPrice = 999.0.euroNet,
    )

    assertThat(pricedSection.netPrice).isEqualTo(999.0.euroNet)
    assertThat(pricedSection.optionality).isEqualTo(Optionality.Optional)
  }

  @Test
  fun `testQuery calculates the values of the result`() {
    val pricedSection = PricedSection(
      headline = "section",
      visibility = Visibility.Public,
      children = listOf(mandatoryItem(100.0.euroNet), optionalItem(250.0.euroNet)),
      netPrice = 999.0.euroNet,
    )

    val onlyMandatory = pricedSection.queryPricedElement(CalculationRelevanceQuery.onlyMandatory)

    assertThat(onlyMandatory?.children).isNotNull().hasSize(1)
    assertThat(onlyMandatory?.netPrice).isEqualTo(100.0.euroNet)
  }

  @Test
  fun `testQuery keeps a set value when it drops nothing`() {
    val pricedSection = PricedSection(
      headline = "section",
      visibility = Visibility.Public,
      children = listOf(mandatoryItem(100.0.euroNet), optionalItem(250.0.euroNet)),
      netPrice = 999.0.euroNet,
    )

    assertThat(pricedSection.queryPricedElement(CalculationRelevanceQuery.allIncludingOptional)?.netPrice).isEqualTo(999.0.euroNet)
  }

  private fun mandatoryItem(price: Money.Net): PricedItem = item(price, Optionality.Mandatory)

  private fun optionalItem(price: Money.Net): PricedItem = item(price, Optionality.Optional)

  private fun item(price: Money.Net, optionality: Optionality): PricedItem = PricedItem(
    headline = "item",
    details = null,
    priceForOneElement = PriceWithProfit.noProfit(price),
    amount = Amount(1.0),
    factor = 100.percent,
    visibility = Visibility.Public,
    relevance = Tags.empty,
    optionality = optionality,
    vatRate = VatRate.NineteenPercent,
  )
}
