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
package it.neckar.financial.quote

import it.neckar.financial.currency.Money
import it.neckar.financial.currency.VatRate
import it.neckar.financial.priced.PricedElement
import it.neckar.financial.priced.PricedElements
import it.neckar.financial.quote.query.CalculationRelevanceQuery
import it.neckar.open.collections.fastMap
import it.neckar.open.kotlin.lang.percent
import it.neckar.open.unit.currency.EUR
import it.neckar.open.unit.other.pct
import kotlinx.serialization.Serializable

/**
 * The sum block a quote prints.
 *
 * Answers what the quote *states*, while the query methods of [PricedElements] answer what its elements add up to
 * for a given [CalculationRelevanceQuery] - the two differ exactly when the sums are set rather than calculated.
 */
@Serializable
data class QuoteSums(
  /**
   * The sum of the line amounts before the discount.
   */
  val preDiscountNet: @EUR Money.Net,

  /**
   * What the discount takes off, so for the usual positive quote a *negative* amount.
   */
  val discount: @EUR Money.Net,

  /**
   * The net sum: [preDiscountNet] plus [discount].
   */
  val net: @EUR Money.Net,

  /**
   * One entry per tax rate that occurs, ascending by rate.
   */
  val vatBreakdown: List<Money.WithVat>,

  /**
   * [net] plus every tax amount, so a reader can recalculate it from the block itself.
   */
  val gross: @EUR Money.Gross,
) {

  /**
   * The tax amount for [vatRate] - zero for a rate that does not occur.
   */
  operator fun get(vatRate: VatRate): @EUR Money.Vat = findLine(vatRate)?.vatAmount ?: Money.Vat.Zero

  /**
   * The tax line for [vatRate] - `null` for a rate that does not occur.
   */
  fun findLine(vatRate: VatRate): Money.WithVat? = vatBreakdown.firstOrNull { it.vatRate == vatRate }

  val vatRates: List<VatRate>
    get() = vatBreakdown.fastMap { it.vatRate }

  companion object {
    /**
     * The block the way a quote prints it: the discount is applied per item and rounded there; tax and gross follow
     * [Money.WithVat].
     */
    fun calculate(
      elements: List<PricedElement>,
      discountPercentage: @pct Double,
      sumQuery: CalculationRelevanceQuery = CalculationRelevanceQuery.default,
    ): QuoteSums {
      val pricedItems = elements.flatMap { it.listPricedItems(sumQuery) }
      val discountFactor = 100.percent - discountPercentage

      val preDiscountNet: Money.Net = Money.Net.fastSumBy(pricedItems) { it.netPrice }

      val vatBreakdown: List<Money.WithVat> = pricedItems
        .groupBy { it.vatRate }
        .toList()
        .sortedBy { (vatRate, _) -> vatRate.vat }
        .fastMap { (vatRate, itemsForRate) ->
          Money.WithVat(net = Money.Net.fastSumBy(itemsForRate) { it.netPrice.timesAndRound(discountFactor) }, vatRate = vatRate)
        }
      // The net is the sum of the taxable bases, so the block adds up by construction.
      val net: Money.Net = Money.Net.fastSumBy(vatBreakdown) { it.net }

      return QuoteSums(
        preDiscountNet = preDiscountNet,
        discount = net - preDiscountNet,
        net = net,
        vatBreakdown = vatBreakdown,
        gross = net + Money.Vat.fastSumBy(vatBreakdown) { it.vatAmount },
      )
    }
  }
}

