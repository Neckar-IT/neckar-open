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
import it.neckar.commons.tags.Tag
import it.neckar.commons.tags.Tags
import it.neckar.financial.currency.PriceWithProfit
import it.neckar.financial.currency.VatRate
import it.neckar.financial.currency.euroNet
import it.neckar.financial.quote.Amount
import it.neckar.financial.quote.Optionality
import it.neckar.financial.quote.Visibility
import it.neckar.financial.quote.query.CalculationRelevanceQuery
import it.neckar.financial.quote.query.asQuery
import it.neckar.open.kotlin.lang.percent
import org.junit.jupiter.api.Test

class PricedItemTest {
  @Test
  fun `testQuery optional`() {
    val pricedItem = PricedItem(
      headline = "headline",
      details = "details",
      priceForOneElement = PriceWithProfit.noProfit(100.0.euroNet),
      amount = Amount(1.0),
      factor = 100.percent,
      visibility = Visibility.Public,
      relevance = Tags.empty,
      optionality = Optionality.Optional,
      vatRate = VatRate.NineteenPercent,
    )

    assertThat(pricedItem.queryPricedElement(CalculationRelevanceQuery.allIncludingOptional)?.netPrice).isEqualTo(100.euroNet)
    assertThat(pricedItem.queryPricedElement(CalculationRelevanceQuery.onlyMandatory)?.netPrice).isEqualTo(null)
  }

  @Test
  fun testBasics() {
    val pricedItem = PricedItem(
      headline = "headline",
      details = "details",
      priceForOneElement = PriceWithProfit.surcharge(100.0.euroNet, 10.percent),
      amount = Amount(1.0),
      factor = 100.percent,
      visibility = Visibility.Public,
      relevance = Tags.empty,
      vatRate = VatRate.NineteenPercent,
    )
    assertThat(pricedItem.queryPricedElement(CalculationRelevanceQuery.allIncludingOptional)?.netPrice).isEqualTo(110.0.euroNet)
  }

  @Test
  fun `testPrices calculated`() {
    val pricedItem = PricedItem(
      headline = "headline",
      details = "details",
      priceForOneElement = PriceWithProfit.noProfit(100.0.euroNet),
      amount = Amount(3.0),
      factor = 50.percent,
      visibility = Visibility.Public,
      relevance = Tags.empty,
      vatRate = VatRate.NineteenPercent,
    )

    assertThat(pricedItem.sellingPrice).isEqualTo(300.0.euroNet)
    assertThat(pricedItem.netPrice).isEqualTo(150.0.euroNet)
    assertThat(pricedItem.discount).isEqualTo((-150.0).euroNet)
    assertThat(pricedItem.priceWithProfit).isEqualTo(PriceWithProfit(inputPrice = 300.0.euroNet, sellingPrice = 150.0.euroNet))
  }

  @Test
  fun `testPrices set explicitly`() {
    val pricedItem = PricedItem(
      headline = "headline",
      details = "details",
      priceForOneElement = PriceWithProfit.noProfit(100.0.euroNet),
      amount = Amount(3.0),
      factor = 50.percent,
      visibility = Visibility.Public,
      relevance = Tags.empty,
      vatRate = VatRate.NineteenPercent,
      sellingPrice = 250.0.euroNet,
      netPrice = 200.0.euroNet,
    )

    assertThat(pricedItem.sellingPrice).isEqualTo(250.0.euroNet)
    assertThat(pricedItem.netPrice).isEqualTo(200.0.euroNet)
    assertThat(pricedItem.discount).isEqualTo((-50.0).euroNet)
    assertThat(pricedItem.priceWithProfit).isEqualTo(PriceWithProfit(inputPrice = 300.0.euroNet, sellingPrice = 200.0.euroNet))
  }

  /**
   * The net price follows the selling price that was set, not the one the inputs would have given.
   */
  @Test
  fun `testPrices with the selling price set only`() {
    val pricedItem = PricedItem(
      headline = "headline",
      details = "details",
      priceForOneElement = PriceWithProfit.noProfit(100.0.euroNet),
      amount = Amount(3.0),
      factor = 50.percent,
      visibility = Visibility.Public,
      relevance = Tags.empty,
      vatRate = VatRate.NineteenPercent,
      sellingPrice = 250.0.euroNet,
    )

    assertThat(pricedItem.sellingPrice).isEqualTo(250.0.euroNet)
    assertThat(pricedItem.netPrice).isEqualTo(125.0.euroNet)
  }

  @Test
  fun `testCopy keeps the prices`() {
    val pricedItem = PricedItem(
      headline = "headline",
      details = "details",
      priceForOneElement = PriceWithProfit.noProfit(100.0.euroNet),
      amount = Amount(3.0),
      factor = 50.percent,
      visibility = Visibility.Public,
      relevance = Tags.empty,
      vatRate = VatRate.NineteenPercent,
      sellingPrice = 250.0.euroNet,
      netPrice = 200.0.euroNet,
    )

    val copied = pricedItem.copy(amount = Amount(10.0), factor = 100.percent, priceForOneElement = PriceWithProfit.noProfit(7.0.euroNet))

    assertThat(copied.sellingPrice).isEqualTo(250.0.euroNet)
    assertThat(copied.netPrice).isEqualTo(200.0.euroNet)

    //A copied selling price does not pull the net price along either
    assertThat(pricedItem.copy(sellingPrice = 999.0.euroNet).netPrice).isEqualTo(200.0.euroNet)
  }

  @Test
  fun `testHeadline placeholder is rejected`() {
    assertFailure {
      PricedItem.createEmpty().copy(headline = "<-empty->")
    }.hasMessage("Invalid headline: [<-empty->]")
  }

  @Test
  fun testRelevance() {
    val categoryDb1 = Tag("db1")
    val categoryDb2 = Tag("db2")

    val pricedItem0 = PricedItem(
      headline = "headline",
      details = "details",
      priceForOneElement = PriceWithProfit.surcharge(100.0.euroNet, 10.percent),
      amount = Amount(3.0),
      factor = 100.percent,
      visibility = Visibility.Public,
      relevance = Tags(categoryDb1, categoryDb2),
      optionality = Optionality.Mandatory,
      vatRate = VatRate.NineteenPercent,
    )

    val pricedItem1 = PricedItem(
      headline = "headline",
      details = "details",
      priceForOneElement = PriceWithProfit.surcharge(200.0.euroNet, 10.percent),
      amount = Amount(2.0),
      factor = 100.percent,
      visibility = Visibility.Public,
      relevance = Tags(categoryDb1),
      optionality = Optionality.Mandatory,
      vatRate = VatRate.NineteenPercent,
    )

    val sumItem0 = (3 * 100.0 * 1.1).euroNet
    assertThat(pricedItem0.queryPricedElement(CalculationRelevanceQuery.allIncludingOptional)?.netPrice).isEqualTo(sumItem0)
    val sumItem1 = (2 * 200.0 * 1.1).euroNet
    assertThat(pricedItem1.queryPricedElement(CalculationRelevanceQuery.allIncludingOptional)?.netPrice).isEqualTo(sumItem1)


    //Calculate sums
    val queryForDb1 = categoryDb1.asQuery()
    val queryForDb2 = categoryDb2.asQuery()
    assertThat(pricedItem0.queryPricedElement(queryForDb1)?.netPrice).isEqualTo(sumItem0)
    assertThat(pricedItem1.queryPricedElement(queryForDb1)?.netPrice).isEqualTo(sumItem1)

    assertThat(pricedItem0.queryPricedElement(queryForDb2)?.netPrice).isEqualTo(sumItem0)
    assertThat(pricedItem1.queryPricedElement(queryForDb2)?.netPrice).isEqualTo(null)
  }
}
