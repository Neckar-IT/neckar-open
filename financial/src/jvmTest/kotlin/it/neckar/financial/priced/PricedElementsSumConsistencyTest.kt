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
import it.neckar.financial.currency.euroGross
import it.neckar.financial.currency.euroNet
import it.neckar.financial.currency.euroVat
import it.neckar.financial.currency.sum
import it.neckar.financial.quote.Amount
import it.neckar.financial.quote.Visibility
import it.neckar.financial.quote.query.CalculationRelevanceQuery
import it.neckar.open.kotlin.lang.percent
import it.neckar.open.unit.other.pct
import org.junit.jupiter.api.Test

/**
 * Verifies that the sum block of a quote adds up the way a reader recalculates it:
 * with a single tax rate the tax and the gross sum follow from the net sum shown, and the net sum plus every tax amount is the gross sum.
 *
 * The sums take the prices the items carry, so an item whose price is set rather than calculated belongs here too.
 */
class PricedElementsSumConsistencyTest {
  /**
   * [PricedElements.gross] derives the gross sum from the net sum and the tax amounts, so this holds by construction —
   * it guards the contract against a gross sum that gets its own rounding basis again, which is what made the sum block inconsistent.
   */
  @Test
  fun `every example quote has a consistent sum block for every calculation relevance`() {
    val sumQueries = mapOf(
      "onlyMandatory" to CalculationRelevanceQuery.onlyMandatory,
      "allIncludingOptional" to CalculationRelevanceQuery.allIncludingOptional,
    )

    PricedElementsExample.entries.forEach { (exampleName, pricedElements) ->
      sumQueries.forEach { (queryName, sumQuery) ->
        assertThat(pricedElements.netPlusVats(sumQuery), "net + vats of <$exampleName> for <$queryName>").isEqualTo(pricedElements.gross(sumQuery))
      }
    }
  }

  /**
   * With a single tax rate the whole sum block follows from the net sum alone — the recalculation a customer performs.
   */
  @Test
  fun `a quote taxed at one rate has a gross sum that follows from the net sum`() {
    PricedElementsExample.entries.forEach { (exampleName, pricedElements) ->
      val vatRate = pricedElements.vats().singleOrNull() ?: return@forEach

      assertThat(pricedElements.gross(), "gross of <$exampleName>").isEqualTo(pricedElements.net() + pricedElements.net() * vatRate)
    }
  }

  @Test
  fun `the fifteen item quote taxes its net sum`() {
    val pricedElements = PricedElementsExample.fifteenItems

    assertThat(pricedElements.net()).isEqualTo(1_694.17.euroNet)
    assertThat(pricedElements[VatRate.NineteenPercent]).isEqualTo(321.89.euroVat)
    assertThat(pricedElements.gross()).isEqualTo(2_016.06.euroGross)
  }

  @Test
  fun `two items of the same price are taxed on their aggregated net`() {
    val pricedElements = PricedElementsExample.twoItemsWithRoundingDivergence

    //rounding per item would give round(42.03 * 0.19) * 2 = 15.98
    assertThat(pricedElements.net()).isEqualTo(84.06.euroNet)
    assertThat(pricedElements[VatRate.NineteenPercent]).isEqualTo(15.97.euroVat)
    assertThat(pricedElements.gross()).isEqualTo(100.03.euroGross)
  }

  @Test
  fun `mixed vat rates are taxed on their own base`() {
    val pricedElements = PricedElementsExample.mixedVatRates

    assertThat(pricedElements.vats()).containsExactly(VatRate.SevenPercent, VatRate.NineteenPercent)
    assertThat(pricedElements.net()).isEqualTo(1_858.26.euroNet)
    assertThat(pricedElements.netSumAssociatedWithVAT(VatRate.NineteenPercent)).isEqualTo(1_323.70.euroNet)
    assertThat(pricedElements.netSumAssociatedWithVAT(VatRate.SevenPercent)).isEqualTo(534.56.euroNet)
    //round(1323.70 * 0.19) = 251.50, round(534.56 * 0.07) = 37.42
    assertThat(pricedElements[VatRate.NineteenPercent]).isEqualTo(251.50.euroVat)
    assertThat(pricedElements[VatRate.SevenPercent]).isEqualTo(37.42.euroVat)
  }

  @Test
  fun `credit items reduce the taxable base`() {
    val pricedElements = PricedElementsExample.withCreditItems

    //302.55 - 74.50 - 32.25 = 195.80, taxed as round(195.80 * 0.19) = 37.20
    assertThat(pricedElements.net()).isEqualTo(195.80.euroNet)
    assertThat(pricedElements[VatRate.NineteenPercent]).isEqualTo(37.20.euroVat)
  }

  /**
   * Discounting a credit item rounds a negative amount, where ties go towards positive infinity rather than away from zero.
   */
  @Test
  fun `a discounted credit item rounds towards positive infinity`() {
    val pricedElements = PricedElementsExample.withCreditItemsAndDiscount

    //302.55 * 0.93 = 281.3715 -> 281.37, -74.50 * 0.93 = -69.285 -> -69.28, -32.25 * 0.93 = -29.9925 -> -29.99
    assertThat(pricedElements.net()).isEqualTo(281.37.euroNet - 69.28.euroNet - 29.99.euroNet)
    assertThat(pricedElements.net()).isEqualTo(182.10.euroNet)
    assertThat(pricedElements.preDiscountNet()).isEqualTo(195.80.euroNet)
    //round(182.10 * 0.19) = 34.60
    assertThat(pricedElements[VatRate.NineteenPercent]).isEqualTo(34.60.euroVat)
    assertThat(pricedElements.gross()).isEqualTo(216.70.euroGross)
  }

  @Test
  fun `the discount is applied per item`() {
    val pricedElements = PricedElementsExample.withDiscountAndVat

    //1234.57 * 0.93 = 1148.1501 -> 1148.15, 89.13 * 0.93 = 82.8909 -> 82.89, 456.79 * 0.93 = 424.8147 -> 424.81
    assertThat(pricedElements.preDiscountNet()).isEqualTo(1_780.49.euroNet)
    assertThat(pricedElements.net()).isEqualTo(1_148.15.euroNet + 82.89.euroNet + 424.81.euroNet)
    assertThat(pricedElements.discount()).isEqualTo(pricedElements.net() - pricedElements.preDiscountNet())
    //round((1148.15 + 82.89) * 0.19) = 233.90, round(424.81 * 0.07) = 29.74
    assertThat(pricedElements[VatRate.NineteenPercent]).isEqualTo(233.90.euroVat)
    assertThat(pricedElements[VatRate.SevenPercent]).isEqualTo(29.74.euroVat)
  }

  @Test
  fun `the net sum follows a price set on the item`() {
    val pricedElements = pricedElementsWithCarriedNetPrice(discountPercentage = 0.percent)

    assertThat(pricedElements.net()).isEqualTo(250.0.euroNet)
    assertThat(pricedElements[VatRate.NineteenPercent]).isEqualTo(47.50.euroVat)
  }

  @Test
  fun `the discount is applied to a price set on the item`() {
    val pricedElements = pricedElementsWithCarriedNetPrice(discountPercentage = 7.percent)

    //250.00 * 0.93 = 232.50, the price the inputs imply would give 300.00 * 0.93 = 279.00
    assertThat(pricedElements.preDiscountNet()).isEqualTo(250.0.euroNet)
    assertThat(pricedElements.net()).isEqualTo(232.50.euroNet)
  }

  /**
   * The stated block and the calculator must not drift apart as long as nobody sets the sums.
   */
  @Test
  fun `the stated sums match what the elements add up to`() {
    PricedElementsExample.entries.forEach { (exampleName, pricedElements) ->
      val sums = pricedElements.sums

      assertThat(sums.preDiscountNet, "preDiscountNet of <$exampleName>").isEqualTo(pricedElements.preDiscountNet())
      assertThat(sums.net, "net of <$exampleName>").isEqualTo(pricedElements.net())
      assertThat(sums.discount, "discount of <$exampleName>").isEqualTo(pricedElements.discount())
      assertThat(sums.gross, "gross of <$exampleName>").isEqualTo(pricedElements.gross())
      assertThat(sums.vatRates, "vat rates of <$exampleName>").isEqualTo(pricedElements.vats())

      sums.vatBreakdown.forEach { breakdownLine ->
        assertThat(breakdownLine.net, "net base ${breakdownLine.vatRate} of <$exampleName>").isEqualTo(pricedElements.netSumAssociatedWithVAT(breakdownLine.vatRate))
        assertThat(breakdownLine.vatAmount, "tax ${breakdownLine.vatRate} of <$exampleName>").isEqualTo(pricedElements[breakdownLine.vatRate])
      }
    }
  }

  @Test
  fun `sums set explicitly are kept`() {
    val calculated = pricedElementsWithCarriedNetPrice(discountPercentage = 0.percent)
    val pricedElements = calculated.copy(sums = calculated.sums.copy(net = 42.0.euroNet))

    assertThat(pricedElements.sums.net).isEqualTo(42.0.euroNet)
    //The calculator keeps answering what the elements add up to - that is the other question
    assertThat(pricedElements.net()).isEqualTo(250.0.euroNet)
  }

  @Test
  fun `a quote without items has zero sums`() {
    val pricedElements = PricedElementsExample.empty

    assertThat(pricedElements.net()).isEqualTo(Money.Net.Zero)
    assertThat(pricedElements.gross()).isEqualTo(Money.Gross.Zero)
    assertThat(pricedElements.vats()).isEmpty()
  }

  @Test
  fun `a quote taxed at zero has gross equal to net`() {
    val pricedElements = PricedElementsExample.onlyZeroVat

    assertThat(pricedElements.net()).isEqualTo(6_834.56.euroNet)
    assertThat(pricedElements[VatRate.Zero]).isEqualTo(Money.Vat.Zero)
    assertThat(pricedElements.gross().cents).isEqualTo(pricedElements.net().cents)
  }

  @Test
  fun `a tax rate that does not occur contributes nothing`() {
    assertThat(PricedElementsExample.singleItem[VatRate.SevenPercent]).isEqualTo(Money.Vat.Zero)
  }
}

/**
 * The net sum plus every tax amount the sum block shows.
 */
private fun PricedElements.netPlusVats(sumQuery: CalculationRelevanceQuery): Money.Gross {
  return net(sumQuery) + vats(sumQuery).map { this[it, sumQuery] }.sum()
}

/**
 * A quote of one item whose net price is set rather than calculated - the inputs imply 300.00 €, the item carries 250.00 €.
 */
private fun pricedElementsWithCarriedNetPrice(discountPercentage: @pct Double): PricedElements = PricedElements(
  elements = listOf(
    PricedSection(
      headline = "section",
      visibility = Visibility.Public,
      children = listOf(
        PricedItem(
          headline = "item",
          details = null,
          priceForOneElement = PriceWithProfit.noProfit(100.0.euroNet),
          amount = Amount(3.0),
          factor = 100.percent,
          visibility = Visibility.Public,
          relevance = Tags.empty,
          vatRate = VatRate.NineteenPercent,
          netPrice = 250.0.euroNet,
        ),
      ),
    ),
  ),
  discountPercentage = discountPercentage,
)
