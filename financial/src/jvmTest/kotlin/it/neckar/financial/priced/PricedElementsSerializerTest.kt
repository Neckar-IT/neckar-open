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
import it.neckar.open.kotlin.lang.percent
import it.neckar.open.kotlin.serializers.JsonInclusionStrategy
import it.neckar.open.serialization.roundTrip
import it.neckar.open.serialization.testSerialization
import kotlinx.serialization.MissingFieldException
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import kotlin.uuid.Uuid

class PricedElementsSerializerTest {
  @Test
  fun testRoundTrip() {
    val pricedItem = PricedItem(
      uuid = Uuid.parse("2c721ca4-a4fd-4ca0-841f-2ebb86a020ad"),
      headline = "headline",
      details = "details",
      priceForOneElement = PriceWithProfit.surcharge(100.0.euroNet, 10.percent),
      amount = Amount(1.0),
      factor = 100.percent,
      visibility = Visibility.Public,
      relevance = Tags.empty,
      vatRate = VatRate.NineteenPercent,
    )

    roundTrip(pricedItem) {
      //language=JSON
      """
        {
          "uuid" : "2c721ca4-a4fd-4ca0-841f-2ebb86a020ad",
          "headline" : "headline",
          "details" : "details",
          "priceForOneElement" : {
            "inputPrice" : 10000,
            "sellingPrice" : 11000
          },
          "amount" : {
            "amount" : 1.0,
            "unit" : "None"
          },
          "factor" : 1.0,
          "visibility" : "Public",
          "relevance" :  [ ],
          "optionality" : "Mandatory",
          "vatRate" : 0.19,
          "sellingPrice" : 11000,
          "netPrice" : 11000
        }
      """
    }
  }

  /**
   * A document that leaves the prices out is a [it.neckar.financial.priced.input.PricedItemInput], not a [PricedItem].
   */
  @Test
  fun `testDecode rejects a document without prices`() {
    assertFailure {
      Json.decodeFromString(
        PricedItem.serializer(),
        //language=JSON
        """
          {
            "uuid" : "2c721ca4-a4fd-4ca0-841f-2ebb86a020ad",
            "headline" : "headline",
            "details" : "details",
            "priceForOneElement" : {
              "inputPrice" : 10000,
              "sellingPrice" : 11000
            },
            "amount" : {
              "amount" : 3.0,
              "unit" : "None"
            },
            "factor" : 0.5,
            "visibility" : "Public",
            "relevance" :  [ ],
            "optionality" : "Mandatory",
            "vatRate" : 0.19
          }
        """.trimIndent()
      )
    }.hasClass(MissingFieldException::class)
  }

  @Test
  fun `testRoundTrip with explicitly set prices`() {
    val pricedItem = PricedItem(
      uuid = Uuid.parse("2c721ca4-a4fd-4ca0-841f-2ebb86a020ad"),
      headline = "headline",
      details = "details",
      priceForOneElement = PriceWithProfit.surcharge(100.0.euroNet, 10.percent),
      amount = Amount(1.0),
      factor = 100.percent,
      visibility = Visibility.Public,
      relevance = Tags.empty,
      vatRate = VatRate.NineteenPercent,
      sellingPrice = 90.0.euroNet,
      netPrice = 80.0.euroNet,
    )

    roundTrip(pricedItem) {
      //language=JSON
      """
        {
          "uuid" : "2c721ca4-a4fd-4ca0-841f-2ebb86a020ad",
          "headline" : "headline",
          "details" : "details",
          "priceForOneElement" : {
            "inputPrice" : 10000,
            "sellingPrice" : 11000
          },
          "amount" : {
            "amount" : 1.0,
            "unit" : "None"
          },
          "factor" : 1.0,
          "visibility" : "Public",
          "relevance" :  [ ],
          "optionality" : "Mandatory",
          "vatRate" : 0.19,
          "sellingPrice" : 9000,
          "netPrice" : 8000
        }
      """
    }
  }

  @Test
  fun testPricedSection() {
    val pricedSection = PricedSection(
      uuid = Uuid.parse("1ee716d9-9f96-425e-97e2-93dfd6c6123b"),
      headline = "Batteriesystem der neuesten Generation",
      visibility = Visibility.Public,
      children = listOf(
        PricedSection(
          uuid = Uuid.parse("7c4c513b-5309-492b-bf58-7dc8507067d1"),
          headline = "Niedervoltsystem",
          visibility = Visibility.Public,
          children = listOf(
            PricedItem(
              uuid = Uuid.parse("4660f46d-385b-405a-9081-601b18a7f635"),
              "BYD Battery Box Premium LVL 15,4 (kWh)", null, PriceWithProfit(6000.00.euroNet, 7002.93.euroNet), Amount(1.0), 100.percent, Visibility.Public, relevance, vatRate = VatRate.NineteenPercent
            ),
            PricedItem(
              uuid = Uuid.parse("854aa0e1-21b7-4d08-be57-1c6053e80e7b"),
              "Ein Super-Extra, keine Ahnung was", null, PriceWithProfit(350.00.euroNet, 435.11.euroNet), Amount(7.2), 100.percent, Visibility.Public, relevance, vatRate = VatRate.NineteenPercent
            ),
          ),
        ),
        PricedSection(
          uuid = Uuid.parse("7a47c34f-30ad-4b65-9929-739cb873baef"),
          headline = "Batteriewechselrichter",
          visibility = Visibility.Public,
          children = listOf(
            PricedItem(uuid = Uuid.parse("7d5b31c4-38d9-4327-baa3-c0f5dd56e4bd"), "SMA SUNNY ISLAND 4.4", null, PriceWithProfit(1600.00.euroNet, 2097.27.euroNet), Amount(2.0), 100.percent, Visibility.Public, relevance, vatRate = VatRate.NineteenPercent),
          ),
        ),
        PricedItem(
          uuid = Uuid.parse("8456425e-4e5c-4c92-9d3d-d8c74de4dea4"),
          headline = "Kleinteile", details = "alle Kleinteile", priceForOneElement = PriceWithProfit(310.00.euroNet, 390.00.euroNet), amount = Amount(1.0), factor = 100.percent, visibility = Visibility.Public, relevance = relevance,
          optionality = Optionality.Optional, vatRate = VatRate.NineteenPercent
        ),
      )
    )

    roundTrip(pricedSection) {
      //language=JSON
      """
        {
          "uuid" : "1ee716d9-9f96-425e-97e2-93dfd6c6123b",
          "headline" : "Batteriesystem der neuesten Generation",
          "amount" : null,
          "factor" : null,
          "visibility" : "Public",
          "details" : null,
          "children" : [ {
            "type" : "PricedSection",
            "uuid" : "7c4c513b-5309-492b-bf58-7dc8507067d1",
            "headline" : "Niedervoltsystem",
            "amount" : null,
            "factor" : null,
            "visibility" : "Public",
            "details" : null,
            "children" : [ {
              "type" : "PricedItem",
              "uuid" : "4660f46d-385b-405a-9081-601b18a7f635",
              "headline" : "BYD Battery Box Premium LVL 15,4 (kWh)",
              "details" : null,
              "priceForOneElement" : {
                "inputPrice" : 600000,
                "sellingPrice" : 700293
              },
              "amount" : {
                "amount" : 1.0,
                "unit" : "None"
              },
              "factor" : 1.0,
              "visibility" : "Public",
              "relevance" : [ "economicsCalculation" ],
              "optionality" : "Mandatory",
              "vatRate" : 0.19,
              "sellingPrice" : 700293,
              "netPrice" : 700293
            }, {
              "type" : "PricedItem",
              "uuid" : "854aa0e1-21b7-4d08-be57-1c6053e80e7b",
              "headline" : "Ein Super-Extra, keine Ahnung was",
              "details" : null,
              "priceForOneElement" : {
                "inputPrice" : 35000,
                "sellingPrice" : 43511
              },
              "amount" : {
                "amount" : 7.2,
                "unit" : "None"
              },
              "factor" : 1.0,
              "visibility" : "Public",
              "relevance" : [ "economicsCalculation" ],
              "optionality" : "Mandatory",
              "vatRate" : 0.19,
              "sellingPrice" : 313279,
              "netPrice" : 313279
            } ],
            "optionality" : "Mandatory",
            "netPrice" : 1013572
          }, {
            "type" : "PricedSection",
            "uuid" : "7a47c34f-30ad-4b65-9929-739cb873baef",
            "headline" : "Batteriewechselrichter",
            "amount" : null,
            "factor" : null,
            "visibility" : "Public",
            "details" : null,
            "children" : [ {
              "type" : "PricedItem",
              "uuid" : "7d5b31c4-38d9-4327-baa3-c0f5dd56e4bd",
              "headline" : "SMA SUNNY ISLAND 4.4",
              "details" : null,
              "priceForOneElement" : {
                "inputPrice" : 160000,
                "sellingPrice" : 209727
              },
              "amount" : {
                "amount" : 2.0,
                "unit" : "None"
              },
              "factor" : 1.0,
              "visibility" : "Public",
              "relevance" : [ "economicsCalculation" ],
              "optionality" : "Mandatory",
              "vatRate" : 0.19,
              "sellingPrice" : 419454,
              "netPrice" : 419454
            } ],
            "optionality" : "Mandatory",
            "netPrice" : 419454
          }, {
            "type" : "PricedItem",
            "uuid" : "8456425e-4e5c-4c92-9d3d-d8c74de4dea4",
            "headline" : "Kleinteile",
            "details" : "alle Kleinteile",
            "priceForOneElement" : {
              "inputPrice" : 31000,
              "sellingPrice" : 39000
            },
            "amount" : {
              "amount" : 1.0,
              "unit" : "None"
            },
            "factor" : 1.0,
            "visibility" : "Public",
            "relevance" : [ "economicsCalculation" ],
            "optionality" : "Optional",
            "vatRate" : 0.19,
            "sellingPrice" : 39000,
            "netPrice" : 39000
          } ],
          "optionality" : "Mandatory",
          "netPrice" : 1472026
        }
      """.trimIndent()
    }
  }

  /**
   * The services encode with `encodeDefaults = false` - the calculated values must survive that, or a REST consumer
   * would have to re-derive every price itself.
   */
  @Test
  fun `testEncode with defaults skipped keeps the calculated values`() {
    val pricedElements = PricedElements(
      elements = listOf(
        PricedSection(
          uuid = Uuid.parse("1ee716d9-9f96-425e-97e2-93dfd6c6123b"),
          headline = "Section",
          visibility = Visibility.Public,
          children = listOf(
            PricedItem(
              uuid = Uuid.parse("4660f46d-385b-405a-9081-601b18a7f635"),
              headline = "Item",
              details = null,
              priceForOneElement = PriceWithProfit(100.0.euroNet, 110.0.euroNet),
              amount = Amount(1.0),
              visibility = Visibility.Public,
              relevance = Tags.empty,
              vatRate = VatRate.NineteenPercent,
            ),
          ),
        ),
      ),
      discountPercentage = 0.0,
    )

    testSerialization(pricedElements, inclusionStrategy = JsonInclusionStrategy.SkipDefaultsIncludeNulls) {
      //language=JSON
      """
        {
          "elements" : [ {
            "type" : "PricedSection",
            "uuid" : "1ee716d9-9f96-425e-97e2-93dfd6c6123b",
            "headline" : "Section",
            "visibility" : "Public",
            "children" : [ {
              "type" : "PricedItem",
              "uuid" : "4660f46d-385b-405a-9081-601b18a7f635",
              "headline" : "Item",
              "details" : null,
              "priceForOneElement" : {
                "inputPrice" : 10000,
                "sellingPrice" : 11000
              },
              "amount" : {
                "amount" : 1.0
              },
              "visibility" : "Public",
              "relevance" : [ ],
              "vatRate" : 0.19,
              "sellingPrice" : 11000,
              "netPrice" : 11000
            } ],
            "optionality" : "Mandatory",
            "netPrice" : 11000
          } ],
          "discountPercentage" : 0.0,
          "sums" : {
            "preDiscountNet" : 11000,
            "discount" : 0,
            "net" : 11000,
            "vatBreakdown" : [ {
              "net" : 11000,
              "vatRate" : 0.19,
              "vatAmount" : 2090,
              "gross" : 13090
            } ],
            "gross" : 13090
          }
        }
      """
    }
  }

  private val tag = Tag("economicsCalculation")
  private val relevance = Tags(tag)
}
