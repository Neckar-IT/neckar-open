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

import it.neckar.financial.priced.PricedElements
import it.neckar.financial.quote.QuoteSums
import it.neckar.financial.quote.query.CalculationRelevanceQuery
import it.neckar.financial.quote.query.VisibilityQuery
import it.neckar.open.annotations.DataView
import kotlinx.serialization.Serializable

/**
 * The rows and their position numbers come from [toFlatLines], so a quote is numbered the same way
 * wherever it is shown.
 */
@DataView("A quote's elements as table rows, with their position numbers")
@Serializable
data class FlatQuoteDetails(
  val lines: List<FlatQuoteLineDetails>,
  /**
   * What the bill comes to, per tax rate and in total. It falls short of
   * [it.neckar.financial.priced.PricedElements.sums] by the items under a top-level element the
   * visibility query dropped.
   */
  val sums: QuoteSums,
)

/**
 * Flattens the elements into the rows of a bill. The rows carry the amounts before the discount;
 * [QuoteSums.discount] takes it off, so [QuoteSums.net] is [QuoteSums.preDiscountNet] plus [QuoteSums.discount].
 *
 * [VisibilityQuery.allVisibleOnlyMandatory] rather than [toFlatLines]'s default, which drops what is
 * priced at zero: a section that costs nothing is still part of the bill, while an optional line
 * would read as free, because the sums are the mandatory ones.
 */
fun PricedElements.toFlatDetails(visibilityQuery: VisibilityQuery = VisibilityQuery.allVisibleOnlyMandatory): FlatQuoteDetails {
  return FlatQuoteDetails(
    lines = toFlatLines(visibilityQuery = visibilityQuery),
    sums = visibleSums(visibilityQuery),
  )
}

/**
 * The sums over the top-level elements [visibilityQuery] matches. [PricedElements.sums] filters by optionality without
 * asking visibility, and the sums a table prints have to belong to the rows above them.
 */
fun PricedElements.visibleSums(visibilityQuery: VisibilityQuery, sumQuery: CalculationRelevanceQuery = CalculationRelevanceQuery.default): QuoteSums {
  return QuoteSums.calculate(elements.filter { visibilityQuery.matches(it) }, discountPercentage, sumQuery)
}
