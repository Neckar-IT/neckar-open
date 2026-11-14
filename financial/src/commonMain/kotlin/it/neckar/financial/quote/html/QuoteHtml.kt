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
package it.neckar.financial.quote.html

import it.neckar.financial.currency.MoneyAmount
import it.neckar.financial.currency.formatAsPercentageUpToTwoDecimals
import it.neckar.financial.priced.PricedElements
import it.neckar.financial.quote.Optionality
import it.neckar.financial.quote.query.CalculationRelevanceQuery
import it.neckar.financial.quote.query.VisibilityQuery
import it.neckar.financial.quote.rest.views.FlatQuoteLineDetails
import it.neckar.financial.quote.rest.views.visibleSums
import it.neckar.open.i18n.I18nConfiguration
import kotlinx.html.FlowContent
import kotlinx.html.TBODY
import kotlinx.html.div
import kotlinx.html.style
import kotlinx.html.table
import kotlinx.html.tbody
import kotlinx.html.td
import kotlinx.html.th
import kotlinx.html.thead
import kotlinx.html.tr

/**
 * The line table of a quote: one row per line, the position in parentheses and "(Optional)" behind the headline of an
 * optional line, the class `credit` on a line with a negative quantity.
 */
fun FlowContent.quoteLinesTable(lines: List<FlatQuoteLineDetails>, i18nConfiguration: I18nConfiguration) {
  table(QuoteHtmlClasses.Lines) {
    thead {
      tr {
        th(classes = QuoteHtmlClasses.Position) { +"Pos." }
        th { +"Bezeichnung" }
        th(classes = QuoteHtmlClasses.Amount) { +"Menge" }
        th(classes = QuoteHtmlClasses.Money) { +"Einzelpreis" }
        th(classes = QuoteHtmlClasses.Money) { +"Gesamt" }
      }
    }
    tbody {
      lines.forEach { quoteLineRow(it, i18nConfiguration) }
    }
  }
}

private fun TBODY.quoteLineRow(line: FlatQuoteLineDetails, i18nConfiguration: I18nConfiguration) {
  val optional = line.optionality == Optionality.Optional

  val rowClasses = buildList {
    add("${QuoteHtmlClasses.DepthPrefix}${line.depth}")
    if (line is FlatQuoteLineDetails.Section) add(QuoteHtmlClasses.Section)
    if (optional) add(QuoteHtmlClasses.Optional)
    // A credit is a negative quantity; a negative unit price at a positive quantity is a discount line.
    if ((line.amount?.amount ?: 0.0) < 0.0) add(QuoteHtmlClasses.Credit)
  }

  tr(rowClasses.joinToString(" ")) {
    td(QuoteHtmlClasses.Position) {
      +(if (optional) "(${line.position.value})" else line.position.value)
    }
    td {
      div(QuoteHtmlClasses.Headline) {
        style = "padding-left: ${line.depth - 1}em"
        +line.headline
        if (optional) {
          +" (Optional)"
        }
      }
      line.details?.let { details ->
        div(QuoteHtmlClasses.Details) { +details }
      }
    }
    td(QuoteHtmlClasses.Amount) {
      line.amount?.let { +it.format(i18nConfiguration) }
    }
    td(QuoteHtmlClasses.Money) {
      // At a quantity of one the unit price only repeats the line total.
      if (line is FlatQuoteLineDetails.Item && line.amount.amount != 1.0) {
        +line.unitPrice.format(i18nConfiguration)
      }
    }
    td(QuoteHtmlClasses.Money) {
      // An optional line totals zero; it states what it would cost, in parentheses.
      +(if (optional) "(${line.totalIncludingOptional.format(i18nConfiguration)})" else line.total.format(i18nConfiguration))
    }
  }
}

/**
 * The sums over the elements [visibilityQuery] matches, the query the line table above was flattened with. The discount
 * rows appear for a nonzero discount, the total including optional lines when they change the gross.
 */
fun FlowContent.quoteSumsBlock(pricedElements: PricedElements, visibilityQuery: VisibilityQuery, i18nConfiguration: I18nConfiguration) {
  val sums = pricedElements.visibleSums(visibilityQuery)
  val grossIncludingOptional = pricedElements.visibleSums(visibilityQuery, CalculationRelevanceQuery.allIncludingOptional).gross

  table(QuoteHtmlClasses.Sums) {
    tbody {
      if (sums.discount.isZero().not()) {
        sumRow("Rechnungssumme", sums.preDiscountNet, i18nConfiguration)
        sumRow("Rabatt (${pricedElements.discountPercentage.formatAsPercentageUpToTwoDecimals(i18nConfiguration)})", sums.discount, i18nConfiguration)
      }
      sumRow("Nettosumme", sums.net, i18nConfiguration)
      sums.vatBreakdown.forEach { line ->
        sumRow("${line.vatRate.formatAsPercentage(i18nConfiguration)} auf ${line.net.format(i18nConfiguration)}", line.vatAmount, i18nConfiguration)
      }
      sumRow("Gesamtsumme", sums.gross, i18nConfiguration, QuoteHtmlClasses.Total)
      if (grossIncludingOptional != sums.gross) {
        sumRow("Gesamtsumme inklusive optionaler Positionen", grossIncludingOptional, i18nConfiguration, QuoteHtmlClasses.Optional)
      }
    }
  }
}

private fun TBODY.sumRow(label: String, amount: MoneyAmount, i18nConfiguration: I18nConfiguration, rowClass: String? = null) {
  tr(rowClass) {
    td { +label }
    td(QuoteHtmlClasses.Money) { +amount.format(i18nConfiguration) }
  }
}

/**
 * The CSS class names [quoteLinesTable] and [quoteSumsBlock] write, and [quoteHtmlStyles] styles.
 */
object QuoteHtmlClasses {
  const val Lines: String = "quote-lines"
  const val Sums: String = "quote-sums"
  const val Position: String = "position"
  const val Headline: String = "line-headline"
  const val Details: String = "line-details"
  const val Amount: String = "amount"
  const val Money: String = "money"
  const val Section: String = "section"
  const val Optional: String = "optional"
  const val Credit: String = "credit"
  const val Total: String = "total"

  /** Followed by the depth of the line, `depth-1` for a top-level line. */
  const val DepthPrefix: String = "depth-"
}

/**
 * The CSS for [quoteLinesTable] and [quoteSumsBlock]; the caller embeds it in a `<style>` element.
 */
val quoteHtmlStyles: String = with(QuoteHtmlClasses) {
  """
  table.$Lines, table.$Sums { width: 100%; border-collapse: collapse; font-size: 10pt; }
  table.$Lines th { text-align: left; border-bottom: 1px solid rgb(51, 51, 51); padding: 4px; }
  table.$Lines td { vertical-align: top; padding: 4px; border-bottom: 1px solid #ddd; }
  table.$Lines .$Position { width: 3em; white-space: nowrap; }
  table.$Lines .$Amount { width: 5em; text-align: right; white-space: nowrap; }
  table.$Lines .$Money, table.$Sums .$Money { width: 7em; text-align: right; white-space: nowrap; }
  table.$Lines tr.$Section { font-weight: bold; }
  table.$Lines .$Details { color: rgb(85, 85, 85); font-size: 9pt; white-space: pre-line; }
  table.$Lines tr.$Optional { color: rgb(119, 119, 119); }
  table.$Lines tr.$Credit { color: rgb(27, 122, 58); }
  table.$Sums { margin-top: 12px; break-inside: avoid; }
  table.$Sums td { padding: 3px 4px; }
  table.$Sums tr.$Total td { font-weight: bold; border-top: 2px solid rgb(51, 51, 51); }
  table.$Sums tr.$Optional td { color: rgb(119, 119, 119); }
  """.trimIndent()
}
