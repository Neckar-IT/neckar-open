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

import it.neckar.financial.currency.Money
import it.neckar.financial.currency.PriceWithProfit
import it.neckar.financial.currency.VatRate
import it.neckar.financial.quote.Optionality
import it.neckar.financial.quote.QuoteSums
import it.neckar.financial.quote.query.CalculationRelevanceQuery
import it.neckar.financial.quote.query.VisibilityQuery
import it.neckar.open.collections.fastForEach
import it.neckar.open.collections.fastForEachIndexed
import it.neckar.open.collections.fastMap
import it.neckar.open.formatting.format
import it.neckar.open.formatting.percentageFormat
import it.neckar.open.i18n.CurrentI18nConfiguration
import it.neckar.open.i18n.I18nConfiguration
import it.neckar.open.kotlin.lang.WhitespaceConfig
import it.neckar.open.kotlin.lang.isPositiveOrZero
import it.neckar.open.kotlin.lang.replaceUnusualSpaces
import it.neckar.open.kotlin.lang.substr
import it.neckar.open.kotlin.lang.wrap
import it.neckar.open.time.sum
import it.neckar.open.unit.currency.EUR
import it.neckar.open.unit.other.pct
import kotlinx.serialization.Serializable
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.time.Duration

/**
 * The tree of sections and items a quote or an invoice prices, with its discount and the sum block it states.
 * [it.neckar.financial.quote.rest.views.toFlatLines] lists it as numbered rows.
 */
@Serializable
data class PricedElements(
  /**
   * The top level of the quote tree.
   */
  val elements: List<PricedElement>,

  /**
   * The discount as a fraction, *not* as a number out of 100: `0.1` means 10 % off.
   * That is what [it.neckar.open.kotlin.lang.percent] produces (`10.percent == 0.1`).
   *
   * Only the lower bound is checked. A value above `1.0` is accepted and turns the sums negative.
   */
  val discountPercentage: @pct Double,

  /**
   * The sum block this quote states, so a reader never re-sums the elements to learn the total.
   */
  val sums: QuoteSums,

  ) {

  init {
    require(discountPercentage.isPositiveOrZero()) {
      "Invalid discount percentage <$discountPercentage>"
    }
  }

  /**
   * The sum block calculated for [sumQuery]. [sums] is the block the document states, which equals the calculated one
   * for the default query unless the sums were set.
   */
  fun calculateSums(sumQuery: CalculationRelevanceQuery = CalculationRelevanceQuery.default): QuoteSums {
    return QuoteSums.calculate(elements, discountPercentage, sumQuery)
  }

  fun net(sumQuery: CalculationRelevanceQuery = CalculationRelevanceQuery.default): @EUR Money.Net {
    return calculateSums(sumQuery).net
  }

  fun gross(sumQuery: CalculationRelevanceQuery = CalculationRelevanceQuery.default): @EUR Money.Gross {
    return calculateSums(sumQuery).gross
  }

  fun preDiscountNet(sumQuery: CalculationRelevanceQuery = CalculationRelevanceQuery.default): @EUR Money.Net {
    return calculateSums(sumQuery).preDiscountNet
  }

  fun discount(sumQuery: CalculationRelevanceQuery = CalculationRelevanceQuery.default): @EUR Money.Net {
    return calculateSums(sumQuery).discount
  }

  fun vats(sumQuery: CalculationRelevanceQuery = CalculationRelevanceQuery.default): List<VatRate> {
    return calculateSums(sumQuery).vatRates
  }

  /**
   * The taxable base for [vatRate]: the discounted net of the items taxed at it.
   */
  fun netSumAssociatedWithVAT(vatRate: VatRate, sumQuery: CalculationRelevanceQuery = CalculationRelevanceQuery.default): @EUR Money.Net {
    return calculateSums(sumQuery).findLine(vatRate)?.net ?: Money.Net.Zero
  }

  /**
   * [QuoteSums.get] of the block for [sumQuery].
   */
  operator fun get(vatRate: VatRate, sumQuery: CalculationRelevanceQuery = CalculationRelevanceQuery.default): @EUR Money.Vat {
    return calculateSums(sumQuery)[vatRate]
  }

  fun workingTimes(sumQuery: CalculationRelevanceQuery = CalculationRelevanceQuery.default): Duration {
    return pricedItems(sumQuery).fastMap { it.workingTimes(sumQuery) }.sum()
  }

  private fun pricedItems(sumQuery: CalculationRelevanceQuery): List<PricedItem> {
    return elements.flatMap { it.listPricedItems(sumQuery) }
  }

  internal fun priceWithProfitSum(sumQuery: CalculationRelevanceQuery): PriceWithProfit {
    return pricedItems(sumQuery).priceWithProfit()
  }

  fun dump(
    visibilityQuery: VisibilityQuery = VisibilityQuery.allIncludingOptional,
    sumQuery: CalculationRelevanceQuery = CalculationRelevanceQuery.default,

    i18nConfiguration: I18nConfiguration = CurrentI18nConfiguration,
  ): String {
    val indexLength = 9
    val headlineLength = 65

    val priceLength = 30
    val amountLength = 15
    val totalLength = 30

    val lineLength = 150

    val sums: QuoteSums = calculateSums(sumQuery)
    return buildString {
      fun List<PricedElement>.renderChildren(baseIndex: String?) {
        fastForEachIndexed { elementIndex, pricedElement ->
          if (visibilityQuery.matches(pricedElement).not()) {
            return@fastForEachIndexed
          }

          val myIndex = (elementIndex + 1).format(i18nConfiguration)
          val indexFormatted = if (baseIndex != null) "$baseIndex.$myIndex" else myIndex

          append(indexFormatted.padEnd(indexLength - 2))
          val typeChar = when (pricedElement) {
            is PricedItem -> "I"
            is PricedSection -> "S"
          }
          append(" $typeChar ")

          if (pricedElement.isOptional()) {
            append(("[${pricedElement.headline.replaceUnusualSpaces()}]").ensureLength(headlineLength))
          } else {
            append(pricedElement.headline.replaceUnusualSpaces().ensureLength(headlineLength))
          }

          if (pricedElement is PricedItem) {
            append(pricedElement.priceForOneElement.dump(i18nConfiguration).padStart(priceLength))
            append(pricedElement.amount.format(i18nConfiguration, WhitespaceConfig.Spaces).padStart(amountLength))
          } else {
            append(" ".repeat(priceLength))
            pricedElement.amount?.let {
              append(it.format(i18nConfiguration, WhitespaceConfig.Spaces).padStart(amountLength))
            } ?: append(" ".repeat(amountLength))
          }

          val sum = pricedElement.listPricedItems(sumQuery).priceWithProfit()
          if (pricedElement.isOptional()) {
            append(("[${sum.dump(i18nConfiguration)}]").padStart(totalLength))
          } else {
            append((sum.dump(i18nConfiguration)).padStart(totalLength))
          }
          appendLine()

          pricedElement.details?.let { details ->
            val lines = details.split("\n").flatMap {
              it.wrap(headlineLength)
            }

            lines.fastForEach {
              append(" ".repeat(indexLength + 1))
              appendLine(it)
            }

            appendLine()
          }

          if (pricedElement is PricedSection) {
            pricedElement.children.renderChildren(indexFormatted)
          }
        }
      }

      elements.renderChildren(null)

      append("-".repeat(lineLength))
      appendLine()
      append(" ".repeat(indexLength + 1))
      append(" ".repeat(priceLength + amountLength))
      append("Rechnungssumme".padEnd(headlineLength))
      append(priceWithProfitSum(sumQuery).dump(i18nConfiguration).padStart(priceLength))
      appendLine()

      append(" ".repeat(indexLength + 1))
      append(" ".repeat(priceLength + amountLength))
      append("Rabatt (${percentageFormat.format(discountPercentage, i18nConfiguration, WhitespaceConfig.Spaces)})".padEnd(headlineLength))
      append(sums.discount.format(i18nConfiguration, WhitespaceConfig.Spaces).padStart(priceLength))
      appendLine()

      append(" ".repeat(indexLength + 1))
      append(" ".repeat(priceLength + amountLength))
      append("Nettosumme".padEnd(headlineLength))
      append(sums.net.format(i18nConfiguration, WhitespaceConfig.Spaces).padStart(priceLength))
      appendLine()

      sums.vatBreakdown.forEach { line ->
        append(" ".repeat(indexLength + 1))
        append(" ".repeat(priceLength + amountLength))
        append(line.vatRate.format(i18nConfiguration, WhitespaceConfig.Spaces).padEnd(headlineLength))
        append(line.vatAmount.format(i18nConfiguration, WhitespaceConfig.Spaces).padStart(priceLength))
        appendLine()
      }

      append("-".repeat(lineLength))
      appendLine()
      append(" ".repeat(indexLength + 1))
      append(" ".repeat(priceLength + amountLength))
      append("Gesamtsumme".padEnd(headlineLength))
      append(sums.gross.format(i18nConfiguration, WhitespaceConfig.Spaces).padStart(priceLength))

      appendLine()
      append("-".repeat(lineLength))
      appendLine()
    }
  }

  fun ifDiscounted(action: () -> Unit) {
    contract {
      callsInPlace(action, InvocationKind.AT_MOST_ONCE)
    }

    if (discountPercentage != 0.0) {
      action()
    }
  }

  companion object {
    val empty: PricedElements = PricedElements(emptyList(), 0.0)

    /**
     * Creates the elements, calculating the sum block when it is left null.
     */
    operator fun invoke(
      elements: List<PricedElement>,
      discountPercentage: @pct Double,
      sums: QuoteSums? = null,
    ): PricedElements = PricedElements(
      elements = elements,
      discountPercentage = discountPercentage,
      sums = sums ?: QuoteSums.calculate(elements, discountPercentage),
    )

    operator fun invoke(config: PricedElementsBuilder.() -> Unit): PricedElements {
      return PricedElementsBuilder.invoke(config).build()
    }

    fun builder(config: PricedElementsBuilder.() -> Unit): PricedElementsBuilder {
      return PricedElementsBuilder.invoke(config)
    }
  }
}

private fun String.ensureLength(headlineLength: Int): String {
  if (this.length <= headlineLength) {
    return this.padEnd(headlineLength)
  }

  return this.substr(0, headlineLength - 3) + "..."
}

private fun PricedElement.isOptional(): Boolean {
  if (this is PricedItem) {
    return this.optionality == Optionality.Optional
  }

  return false
}

