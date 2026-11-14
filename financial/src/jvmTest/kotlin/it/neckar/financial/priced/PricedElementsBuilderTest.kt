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
import it.neckar.financial.currency.PriceWithProfit
import it.neckar.financial.currency.VatRate
import it.neckar.financial.currency.euroGross
import it.neckar.financial.currency.euroNet
import it.neckar.financial.quote.Amount
import it.neckar.financial.quote.Optionality
import it.neckar.financial.quote.Visibility
import it.neckar.financial.quote.query.CalculationRelevanceQuery
import it.neckar.financial.quote.query.VisibilityQuery
import it.neckar.open.kotlin.lang.percent
import it.neckar.open.test.utils.isEqualComparingLinesTrim
import org.junit.jupiter.api.Test

class PricedElementsBuilderTest {
  @Test
  fun testPricedElementsBuilderMethodEmpty() {
    val pricedElements = PricedElements {
    }

    assertThat(pricedElements.gross()).isEqualTo(0.0.euroGross)
  }

  @Test
  fun testItemNextToSection() {
    val pricedElements = PricedElements {
      item(
        headline = "Anfahrt",
        priceForOneElement = PriceWithProfit.noProfit(25.0.euroNet),
        relevance = Tags.empty,
        visibility = Visibility.Public,
        vatRate = VatRate.NineteenPercent,
      )

      section("Main section") {
        item(
          headline = "Item 1",
          priceForOneElement = PriceWithProfit.noProfit(10.0.euroNet),
          relevance = Tags.empty,
          visibility = Visibility.Public,
          vatRate = VatRate.NineteenPercent,
        )
      }
    }

    assertThat(pricedElements.elements[0]).isInstanceOf(PricedItem::class)
    assertThat(pricedElements.elements[1]).isInstanceOf(PricedSection::class)
    assertThat(pricedElements.net()).isEqualTo(35.0.euroNet)
  }

  private val pricedElementsToTest = PricedElements {
    section("Main section") {
      item(
        headline = "Item 1",
        priceForOneElement = PriceWithProfit.noProfit(10.0.euroNet),
        relevance = Tags.empty,
        visibility = Visibility.Public,
        vatRate = VatRate.NineteenPercent,
      ) {
        amount = Amount(2.0)
      }

      section("SubSection 1", Visibility.Public) {
        item(
          headline = "Item A", priceForOneElement = PriceWithProfit.noProfit(11.0.euroNet),
          relevance = Tags.empty,
          visibility = Visibility.Public,
          vatRate = VatRate.NineteenPercent,
        ) {
          amount = Amount(1.5)
        }
        item(
          headline = "Item B", priceForOneElement = PriceWithProfit.noProfit(2.0.euroNet),
          relevance = Tags.empty,
          visibility = Visibility.Public,
          vatRate = VatRate.NineteenPercent,
        ) {
          amount = Amount(3.0)
        }
      }

      section("with hidden items", Visibility.Public) {
        item(
          headline = "Hidden A", priceForOneElement = PriceWithProfit.surcharge(30.euroNet, 10.0.percent),
          relevance = Tags.empty,
          visibility = Visibility.Internal,
          vatRate = VatRate.NineteenPercent,
        )
        item(
          headline = "Hidden B", priceForOneElement = PriceWithProfit.surcharge(40.euroNet, 10.0.percent),
          relevance = Tags.empty,
          visibility = Visibility.Internal,
          vatRate = VatRate.NineteenPercent,
        )
      }

      item(
        headline = "An optional item", priceForOneElement = PriceWithProfit.surcharge(100.euroNet, 10.0.percent),
        relevance = Tags.empty,
        visibility = Visibility.Public,
        vatRate = VatRate.NineteenPercent,
      ) {
        optionality = Optionality.Optional
      }
    }
  }

  @Test
  fun testPricedElementsBuilderMethod1() {
    assertThat(
      pricedElementsToTest.dump(
        visibilityQuery = VisibilityQuery.onlyPublic,
        sumQuery = CalculationRelevanceQuery.onlyMandatory,
      ).trim()
    ).isEqualComparingLinesTrim(
      """
        1       S Main section                                                                                                             112.50 € / 119.50 €
        1.1     I Item 1                                                                        10.00 € / 10.00 €              2             20.00 € / 20.00 €
        1.2     S SubSection 1                                                                                                               22.50 € / 22.50 €
        1.2.1   I Item A                                                                        11.00 € / 11.00 €            1.5             16.50 € / 16.50 €
        1.2.2   I Item B                                                                          2.00 € / 2.00 €              3               6.00 € / 6.00 €
        1.3     S with hidden items                                                                                                          70.00 € / 77.00 €
        1.4     I [An optional item]                                                          100.00 € / 110.00 €              1             [0.00 € / 0.00 €]
        ------------------------------------------------------------------------------------------------------------------------------------------------------
                                                               Rechnungssumme                                                              112.50 € / 119.50 €
                                                               Rabatt (0 %)                                                                             0.00 €
                                                               Nettosumme                                                                             119.50 €
                                                               MwSt. 19 %                                                                              22.71 €
        ------------------------------------------------------------------------------------------------------------------------------------------------------
                                                               Gesamtsumme                                                                            142.21 €
        ------------------------------------------------------------------------------------------------------------------------------------------------------
      """
    )
  }

  @Test
  fun testPricedElementsBuilderMethod2() {
    assertThat(
      pricedElementsToTest.dump(
        visibilityQuery = VisibilityQuery.allVisibleOnlyMandatory,
        sumQuery = CalculationRelevanceQuery.onlyMandatory,
      ).trim()
    ).isEqualComparingLinesTrim(
      """
        1       S Main section                                                                                                             112.50 € / 119.50 €
        1.1     I Item 1                                                                        10.00 € / 10.00 €              2             20.00 € / 20.00 €
        1.2     S SubSection 1                                                                                                               22.50 € / 22.50 €
        1.2.1   I Item A                                                                        11.00 € / 11.00 €            1.5             16.50 € / 16.50 €
        1.2.2   I Item B                                                                          2.00 € / 2.00 €              3               6.00 € / 6.00 €
        1.3     S with hidden items                                                                                                          70.00 € / 77.00 €
        ------------------------------------------------------------------------------------------------------------------------------------------------------
                                                               Rechnungssumme                                                              112.50 € / 119.50 €
                                                               Rabatt (0 %)                                                                             0.00 €
                                                               Nettosumme                                                                             119.50 €
                                                               MwSt. 19 %                                                                              22.71 €
        ------------------------------------------------------------------------------------------------------------------------------------------------------
                                                               Gesamtsumme                                                                            142.21 €
        ------------------------------------------------------------------------------------------------------------------------------------------------------
      """.trimIndent()
    )
  }

  @Test
  fun testPricedElementsBuilderMethod3() {
    //This does not make any sense - we do not see all elements that are relevant for the sum
    assertThat(
      pricedElementsToTest.dump(
        visibilityQuery = VisibilityQuery.allVisibleOnlyMandatory,
        sumQuery = CalculationRelevanceQuery.allIncludingOptional,
      ).trim()
    ).isEqualComparingLinesTrim(
      """
        1       S Main section                                                                                                             212.50 € / 229.50 €
        1.1     I Item 1                                                                        10.00 € / 10.00 €              2             20.00 € / 20.00 €
        1.2     S SubSection 1                                                                                                               22.50 € / 22.50 €
        1.2.1   I Item A                                                                        11.00 € / 11.00 €            1.5             16.50 € / 16.50 €
        1.2.2   I Item B                                                                          2.00 € / 2.00 €              3               6.00 € / 6.00 €
        1.3     S with hidden items                                                                                                          70.00 € / 77.00 €
        ------------------------------------------------------------------------------------------------------------------------------------------------------
                                                               Rechnungssumme                                                              212.50 € / 229.50 €
                                                               Rabatt (0 %)                                                                             0.00 €
                                                               Nettosumme                                                                             229.50 €
                                                               MwSt. 19 %                                                                              43.61 €
        ------------------------------------------------------------------------------------------------------------------------------------------------------
                                                               Gesamtsumme                                                                            273.11 €
        ------------------------------------------------------------------------------------------------------------------------------------------------------
       """.trimIndent()
    )
  }

  @Test
  fun testPricedElementsBuilderMethod4() {
    assertThat(
      pricedElementsToTest.dump(
        visibilityQuery = VisibilityQuery.onlyPublic,
        sumQuery = CalculationRelevanceQuery.allIncludingOptional,
      ).trim()
    ).isEqualComparingLinesTrim(
      """
        1       S Main section                                                                                                             212.50 € / 229.50 €
        1.1     I Item 1                                                                        10.00 € / 10.00 €              2             20.00 € / 20.00 €
        1.2     S SubSection 1                                                                                                               22.50 € / 22.50 €
        1.2.1   I Item A                                                                        11.00 € / 11.00 €            1.5             16.50 € / 16.50 €
        1.2.2   I Item B                                                                          2.00 € / 2.00 €              3               6.00 € / 6.00 €
        1.3     S with hidden items                                                                                                          70.00 € / 77.00 €
        1.4     I [An optional item]                                                          100.00 € / 110.00 €              1         [100.00 € / 110.00 €]
        ------------------------------------------------------------------------------------------------------------------------------------------------------
                                                               Rechnungssumme                                                              212.50 € / 229.50 €
                                                               Rabatt (0 %)                                                                             0.00 €
                                                               Nettosumme                                                                             229.50 €
                                                               MwSt. 19 %                                                                              43.61 €
        ------------------------------------------------------------------------------------------------------------------------------------------------------
                                                               Gesamtsumme                                                                            273.11 €
        ------------------------------------------------------------------------------------------------------------------------------------------------------
      """.trimIndent()
    )
  }

}
