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

import it.neckar.financial.currency.Money
import it.neckar.financial.currency.VatRate
import it.neckar.financial.priced.PricedElement
import it.neckar.financial.priced.PricedElements
import it.neckar.financial.priced.PricedItem
import it.neckar.financial.priced.PricedSection
import it.neckar.financial.priced.priceWithProfit
import it.neckar.financial.quote.Amount
import it.neckar.financial.quote.Optionality
import it.neckar.financial.quote.Visibility
import it.neckar.financial.quote.query.CalculationRelevanceQuery
import it.neckar.financial.quote.query.VisibilityQuery
import it.neckar.open.annotations.DataView
import it.neckar.open.unit.currency.EUR
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

/**
 * One line of a quote, flattened for tabular display — a [Section] or an [Item].
 *
 * Prices are [Money.Net], not [it.neckar.financial.currency.PriceWithProfit]: the purchase price behind
 * an item is the seller's margin, which neither a bill nor a quote states.
 */
@Serializable
sealed interface FlatQuoteLineDetails {
  val position: Position

  /** How deep the line sits — 1 for a top-level element. */
  val depth: Int

  val headline: String

  val details: String?

  /** Null where the line is not billed by a quantity. */
  val amount: Amount?

  /** The line total of an item, the sum over its descendants for a section. An optional line totals zero here. */
  val total: @EUR Money.Net

  /** [total] with the optional elements priced in — the figure an optional line states. */
  val totalIncludingOptional: @EUR Money.Net

  val optionality: Optionality

  /** The rates the mandatory items under this line are taxed at, ascending. */
  val vatRates: List<VatRate>

  /** [Visibility.Internal] marks a line the customer does not get to see. */
  val visibility: Visibility

  @DataView("A section of a quote, flattened for tabular display")
  @Serializable
  @SerialName("Section")
  data class Section(
    override val position: Position,
    override val depth: Int,
    override val headline: String,
    override val details: String?,
    /** Usually null: a section is rarely billed by a quantity. */
    override val amount: Amount?,
    override val total: @EUR Money.Net,
    override val totalIncludingOptional: @EUR Money.Net,
    /** Derived: a section counts as optional only if every one of its elements is. */
    override val optionality: Optionality,
    override val vatRates: List<VatRate>,
    override val visibility: Visibility,
  ) : FlatQuoteLineDetails

  @DataView("An item of a quote, flattened for tabular display")
  @Serializable
  @SerialName("Item")
  data class Item(
    override val position: Position,
    override val depth: Int,
    override val headline: String,
    override val details: String?,
    override val amount: Amount,
    /** What the item states for one element, taken from it rather than divided out of [total]. */
    val unitPrice: @EUR Money.Net,
    override val total: @EUR Money.Net,
    override val totalIncludingOptional: @EUR Money.Net,
    override val optionality: Optionality,
    override val vatRates: List<VatRate>,
    override val visibility: Visibility,
  ) : FlatQuoteLineDetails

  /**
   * The hierarchical position number of a line — `1`, `1.1`, `1.1.1`.
   *
   * Plain digits, never the number format: a grouping separator would make the 1000th sibling read
   * as a three-level position.
   */
  @Serializable
  @JvmInline
  value class Position(val value: String) {
    init {
      require(value.matches(PositionFormat)) { "Not a position number: <$value>" }
    }

    val depth: Int
      get() = value.count { it == '.' } + 1

    fun child(index: Int): Position = Position("$value.${index + 1}")

    companion object {
      private val PositionFormat: Regex = Regex("""\d+(\.\d+)*""")

      fun topLevel(index: Int): Position = Position("${index + 1}")
    }
  }
}

/**
 * Flattens the tree into rows, depth first — the one place a quote's position numbers are assigned,
 * so a quote is numbered the same way wherever it is shown. Only what [visibilityQuery] matches
 * becomes a row, and the numbering counts those rows, not the elements behind them.
 *
 * The rows are undiscounted: [PricedElements.discountPercentage] applies to the quote, and a quote
 * that carries one states it below its rows rather than in them.
 */
fun PricedElements.toFlatLines(visibilityQuery: VisibilityQuery = VisibilityQuery.onlyPublicSkipZero): List<FlatQuoteLineDetails> {
  return elements.toFlatLines(parentPosition = null, visibilityQuery = visibilityQuery)
}

private fun List<PricedElement>.toFlatLines(parentPosition: FlatQuoteLineDetails.Position?, visibilityQuery: VisibilityQuery): List<FlatQuoteLineDetails> {
  return filter { visibilityQuery.matches(it) }.flatMapIndexed { index, element ->
    val position = parentPosition?.child(index) ?: FlatQuoteLineDetails.Position.topLevel(index)

    buildList {
      add(element.toFlatLine(position))

      if (element is PricedSection) {
        addAll(element.children.toFlatLines(position, visibilityQuery))
      }
    }
  }
}

private fun PricedElement.toFlatLine(position: FlatQuoteLineDetails.Position): FlatQuoteLineDetails {
  val total = listPricedItems(CalculationRelevanceQuery.default).priceWithProfit().sellingPrice
  val totalIncludingOptional = listPricedItems(CalculationRelevanceQuery.allIncludingOptional).priceWithProfit().sellingPrice
  val vatRates = vats(CalculationRelevanceQuery.default)

  return when (this) {
    is PricedSection -> FlatQuoteLineDetails.Section(
      position = position,
      depth = position.depth,
      headline = headline,
      details = details,
      amount = amount,
      total = total,
      totalIncludingOptional = totalIncludingOptional,
      optionality = optionality,
      vatRates = vatRates,
      visibility = visibility,
    )

    is PricedItem -> FlatQuoteLineDetails.Item(
      position = position,
      depth = position.depth,
      headline = headline,
      details = details,
      amount = amount,
      unitPrice = priceForOneElement.sellingPrice,
      total = total,
      totalIncludingOptional = totalIncludingOptional,
      optionality = optionality,
      vatRates = vatRates,
      visibility = visibility,
    )
  }
}
