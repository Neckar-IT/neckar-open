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
package it.neckar.financial.quote.rest.views

import assertk.*
import assertk.assertions.*
import it.neckar.commons.tags.Tag
import it.neckar.commons.tags.Tags
import it.neckar.financial.currency.Money
import it.neckar.financial.currency.PriceWithProfit
import it.neckar.financial.currency.VatRate
import it.neckar.financial.currency.euroNet
import it.neckar.financial.priced.PricedElements
import it.neckar.financial.quote.Optionality
import it.neckar.financial.quote.Visibility
import it.neckar.financial.quote.query.VisibilityQuery
import org.junit.jupiter.api.Test

/**
 * The default of [toFlatLines] - the customer's quote, where an optional element is still shown.
 */
class FlatQuoteLinesTest {

  private val relevance = Tags(Tag("myTestRelevance"))

  @Test
  fun `an element priced at zero is no line`() {
    val pricedElements = PricedElements {
      section("Photovoltaikanlage") {
        item(headline = "With zero amount", priceForOneElement = PriceWithProfit(99.00.euroNet, 17.34.euroNet), amount = 0.0, relevance = relevance, visibility = Visibility.Public, vatRate = VatRate.NineteenPercent)

        item(headline = "With zero price", priceForOneElement = PriceWithProfit(0.00.euroNet, 0.00.euroNet), amount = 17.0, relevance = relevance, visibility = Visibility.Public, vatRate = VatRate.NineteenPercent)

        item(headline = "Normal", priceForOneElement = PriceWithProfit(15.00.euroNet, 17.34.euroNet), amount = 17.0, relevance = relevance, visibility = Visibility.Public, vatRate = VatRate.NineteenPercent)
      }
    }

    assertThat(pricedElements.toFlatLines().map { "${it.position.value} ${it.headline}" })
      .isEqualTo(listOf("1 Photovoltaikanlage", "1.1 Normal"))
  }

  @Test
  fun `an internal item reaches the quote through its section only`() {
    val lines = nestedPricedElements().toFlatLines()

    assertThat(lines.map { "${it.position.value} ${it.headline}" }).isEqualTo(
      listOf(
        "1 Photovoltaikanlage",
        "1.1 A public",
        "1.2 with internal",
        "1.3 Komplettanlage inklusive Montage",
        "1.3.1 F public",
        "1.3.2 E Optional",
      )
    )

    assertThat(lines.single { it.headline == "with internal" }.total).isEqualTo(55.02.euroNet)
  }

  @Test
  fun `a section totals its mandatory items`() {
    val lines = nestedPricedElements().toFlatLines()

    assertThat(lines.map { it.total }).isEqualTo(
      listOf(163.74.euroNet, 34.68.euroNet, 55.02.euroNet, 74.04.euroNet, 74.04.euroNet, Money.Net.Zero)
    )
  }

  @Test
  fun `an optional item totals zero and states its price beside it`() {
    val optional = nestedPricedElements().toFlatLines().filterIsInstance<FlatQuoteLineDetails.Item>().single { it.optionality == Optionality.Optional }

    assertThat(optional.total).isEqualTo(Money.Net.Zero)
    assertThat(optional.totalIncludingOptional).isEqualTo(1716.66.euroNet)
    // Taken from the item, so a line totalling zero still states what one element costs.
    assertThat(optional.unitPrice).isEqualTo(17.34.euroNet)
  }

  @Test
  fun `a priced section becomes a section line, a priced item an item line`() {
    val lines = nestedPricedElements().toFlatLines()

    assertThat(lines.single { it.headline == "Photovoltaikanlage" }).isInstanceOf(FlatQuoteLineDetails.Section::class)
    assertThat(lines.single { it.headline == "A public" }).isInstanceOf(FlatQuoteLineDetails.Item::class)
      .transform { it.unitPrice }.isEqualTo(17.34.euroNet)
  }

  @Test
  fun `a line states the rates its mandatory items are taxed at`() {
    val lines = nestedPricedElements().toFlatLines()

    assertThat(lines.first().vatRates).isEqualTo(listOf(VatRate.NineteenPercent))
    assertThat(lines.single { it.optionality == Optionality.Optional }.vatRates).isEmpty()
  }

  @Test
  fun `a line carries the visibility of its element`() {
    val lines = nestedPricedElements().toFlatLines(VisibilityQuery.allIncludingOptional)

    assertThat(lines.single { it.headline == "B internal" }.visibility).isEqualTo(Visibility.Internal)
    assertThat(lines.single { it.headline == "A public" }.visibility).isEqualTo(Visibility.Public)
    assertThat(lines.single { it.headline == "with internal" }.visibility).isEqualTo(Visibility.Public)
  }

  @Test
  fun `a section of internal items is an internal line`() {
    val pricedElements = PricedElements {
      section("Photovoltaikanlage") {
        section("Calculation only", Visibility.Internal) {
          item(headline = "Cable", priceForOneElement = PriceWithProfit(15.00.euroNet, 17.34.euroNet), amount = 2.0, relevance = relevance, visibility = Visibility.Internal, vatRate = VatRate.NineteenPercent)
        }
      }
    }

    assertThat(pricedElements.toFlatLines(VisibilityQuery.allIncludingOptional).map { "${it.headline}: ${it.visibility}" })
      .isEqualTo(listOf("Photovoltaikanlage: Public", "Calculation only: Internal", "Cable: Internal"))
  }

  /**
   * A quote with an internal item, a nested section and an optional item - one of each case the
   * flattening treats differently.
   */
  private fun nestedPricedElements(): PricedElements = PricedElements {
    section("Photovoltaikanlage") {
      item(headline = "A public", priceForOneElement = PriceWithProfit(15.00.euroNet, 17.34.euroNet), amount = 2.0, relevance = relevance, visibility = Visibility.Public, vatRate = VatRate.NineteenPercent)

      section("with internal", Visibility.Public) {
        item(headline = "B internal", priceForOneElement = PriceWithProfit(14.00.euroNet, 18.34.euroNet), amount = 3.0, relevance = relevance, visibility = Visibility.Internal, vatRate = VatRate.NineteenPercent)
      }

      section("Komplettanlage inklusive Montage", Visibility.Public) {
        item(headline = "F public", priceForOneElement = PriceWithProfit(9.74.euroNet, 12.34.euroNet), amount = 6.0, relevance = relevance, visibility = Visibility.Public, vatRate = VatRate.NineteenPercent)

        item(headline = "E Optional", priceForOneElement = PriceWithProfit(12.00.euroNet, 17.34.euroNet), amount = 99.0, relevance = relevance, visibility = Visibility.Public, vatRate = VatRate.NineteenPercent) {
          optionality = Optionality.Optional
        }
      }
    }
  }
}
