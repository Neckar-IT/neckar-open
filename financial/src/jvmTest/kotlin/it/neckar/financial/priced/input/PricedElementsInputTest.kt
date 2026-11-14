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
package it.neckar.financial.priced.input

import assertk.*
import assertk.assertions.*
import it.neckar.commons.tags.Tags
import it.neckar.financial.currency.Money
import it.neckar.financial.currency.PriceWithProfit
import it.neckar.financial.currency.VatRate
import it.neckar.financial.currency.euroGross
import it.neckar.financial.currency.euroNet
import it.neckar.financial.priced.PricedItem
import it.neckar.financial.priced.PricedSection
import it.neckar.financial.quote.Amount
import it.neckar.financial.quote.Optionality
import it.neckar.financial.quote.QuoteSums
import it.neckar.financial.quote.Visibility
import it.neckar.open.kotlin.lang.percent
import it.neckar.open.unit.other.pct
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import kotlin.uuid.Uuid

/**
 * What a producer states survives unchanged; what it leaves out is calculated.
 */
class PricedElementsInputTest {
  @Test
  fun `toPricedElements calculates the values left out`() {
    val domain = elementsInput().toPricedElements()
    val section = domain.elements.single() as PricedSection
    val item = section.children.single() as PricedItem

    assertThat(item.sellingPrice).isEqualTo(330.0.euroNet)
    assertThat(item.netPrice).isEqualTo(165.0.euroNet)
    assertThat(section.optionality).isEqualTo(Optionality.Mandatory)
    assertThat(section.netPrice).isEqualTo(165.0.euroNet)
    assertThat(domain.sums.net).isEqualTo(165.0.euroNet)
  }

  @Test
  fun `toPricedElements keeps the values that are stated`() {
    val statedSums = QuoteSums(
      preDiscountNet = 1.0.euroNet,
      discount = 0.0.euroNet,
      net = 1.0.euroNet,
      vatBreakdown = listOf(Money.WithVat(net = 1.0.euroNet, vatRate = VatRate.NineteenPercent)),
      gross = 1.19.euroGross,
    )

    val domain = elementsInput(
      item = itemInput(sellingPrice = 400.0.euroNet, netPrice = 300.0.euroNet),
      sectionAmount = Amount(4.0),
      sectionFactor = 80.percent,
      sectionDetails = "details",
      sectionOptionality = Optionality.Optional,
      sectionNetPrice = 500.0.euroNet,
      sums = statedSums,
    ).toPricedElements()
    val section = domain.elements.single() as PricedSection
    val item = section.children.single() as PricedItem

    assertThat(item.sellingPrice).isEqualTo(400.0.euroNet)
    assertThat(item.netPrice).isEqualTo(300.0.euroNet)
    assertThat(section.amount).isEqualTo(Amount(4.0))
    assertThat(section.factor).isEqualTo(80.percent)
    assertThat(section.details).isEqualTo("details")
    assertThat(section.optionality).isEqualTo(Optionality.Optional)
    assertThat(section.netPrice).isEqualTo(500.0.euroNet)
    assertThat(domain.sums).isEqualTo(statedSums)
  }

  /**
   * The shape a pricing script writes: the calculated fields are simply absent.
   */
  @Test
  fun `testDecode document without the calculated values`() {
    val decoded = Json.decodeFromString(
      PricedElementsInput.serializer(),
      //language=JSON
      """
        {
          "elements" : [ {
            "type" : "PricedSectionInput",
            "uuid" : "1ee716d9-9f96-425e-97e2-93dfd6c6123b",
            "headline" : "Section",
            "visibility" : "Public",
            "children" : [ {
              "type" : "PricedItemInput",
              "uuid" : "2c721ca4-a4fd-4ca0-841f-2ebb86a020ad",
              "headline" : "headline",
              "details" : null,
              "priceForOneElement" : {
                "inputPrice" : 10000,
                "sellingPrice" : 11000
              },
              "amount" : {
                "amount" : 3.0
              },
              "factor" : 0.5,
              "visibility" : "Public",
              "relevance" : [ ],
              "vatRate" : 0.19
            } ]
          } ],
          "discountPercentage" : 0.0
        }
      """.trimIndent()
    )

    assertThat(decoded).isEqualTo(elementsInput())
    assertThat(decoded.toPricedElements().sums.net).isEqualTo(165.0.euroNet)
  }

  private fun itemInput(sellingPrice: Money.Net? = null, netPrice: Money.Net? = null): PricedItemInput = PricedItemInput(
    uuid = Uuid.parse("2c721ca4-a4fd-4ca0-841f-2ebb86a020ad"),
    headline = "headline",
    details = null,
    priceForOneElement = PriceWithProfit.surcharge(100.0.euroNet, 10.percent),
    amount = Amount(3.0),
    factor = 50.percent,
    visibility = Visibility.Public,
    relevance = Tags.empty,
    vatRate = VatRate.NineteenPercent,
    sellingPrice = sellingPrice,
    netPrice = netPrice,
  )

  private fun elementsInput(
    item: PricedItemInput = itemInput(),
    sectionAmount: Amount? = null,
    sectionFactor: @pct Double? = null,
    sectionDetails: String? = null,
    sectionOptionality: Optionality? = null,
    sectionNetPrice: Money.Net? = null,
    sums: QuoteSums? = null,
  ): PricedElementsInput = PricedElementsInput(
    elements = listOf(
      PricedSectionInput(
        uuid = Uuid.parse("1ee716d9-9f96-425e-97e2-93dfd6c6123b"),
        headline = "Section",
        amount = sectionAmount,
        factor = sectionFactor,
        visibility = Visibility.Public,
        details = sectionDetails,
        children = listOf(item),
        optionality = sectionOptionality,
        netPrice = sectionNetPrice,
      ),
    ),
    discountPercentage = 0.0,
    sums = sums,
  )
}
