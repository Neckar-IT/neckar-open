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

import assertk.*
import assertk.assertions.*
import it.neckar.commons.tags.Tags
import it.neckar.financial.currency.PriceWithProfit
import it.neckar.financial.currency.VatRate
import it.neckar.financial.currency.euroNet
import it.neckar.financial.priced.PricedElements
import it.neckar.financial.priced.PricedItem
import it.neckar.financial.priced.PricedSection
import it.neckar.financial.quote.Amount
import it.neckar.financial.quote.Optionality
import it.neckar.financial.quote.Visibility
import it.neckar.financial.quote.query.VisibilityQuery
import it.neckar.financial.quote.rest.views.toFlatLines
import it.neckar.open.i18n.I18nConfiguration
import kotlinx.html.BODY
import kotlinx.html.body
import kotlinx.html.html
import kotlinx.html.stream.createHTML
import org.junit.jupiter.api.Test
import kotlin.uuid.Uuid

class QuoteHtmlTest {

  @Test
  fun `the sums block states discount, one row per rate and the totals`() {
    val html = sumsHtml(pricedElements(discountPercentage = 0.05))

    assertThat(html).contains("<td>Rechnungssumme</td><td class=\"money\">26,00 €</td>")
    assertThat(html).contains("<td>Rabatt (5 %)</td><td class=\"money\">-1,30 €</td>")
    assertThat(html).contains("<td>Nettosumme</td><td class=\"money\">24,70 €</td>")
    assertThat(html).contains("<td>7 % auf 13,30 €</td><td class=\"money\">0,93 €</td>")
    assertThat(html).contains("<td>19 % auf 11,40 €</td><td class=\"money\">2,17 €</td>")
    assertThat(html).contains("<tr class=\"total\"><td>Gesamtsumme</td><td class=\"money\">27,80 €</td>")
    assertThat(html).contains("<td>Gesamtsumme inklusive optionaler Positionen</td><td class=\"money\">31,19 €</td>")
  }

  @Test
  fun `the sums block leaves out the discount rows without a discount`() {
    val html = sumsHtml(pricedElements(discountPercentage = 0.0))

    assertThat(html).doesNotContain("Rechnungssumme")
    assertThat(html).doesNotContain("Rabatt")
    assertThat(html).contains("<td>Nettosumme</td><td class=\"money\">26,00 €</td>")
  }

  @Test
  fun `the sums block leaves out the total including optional lines without an optional line`() {
    val html = sumsHtml(pricedElements(discountPercentage = 0.0, withOptionalLine = false))

    assertThat(html).contains("Gesamtsumme")
    assertThat(html).doesNotContain("inklusive optionaler")
  }

  @Test
  fun `the sums block counts only the lines the table prints`() {
    val withInternalLine = pricedElements(discountPercentage = 0.0).let {
      PricedElements(it.elements + item(7, "Interner Aufschlag", amount = 1.0, price = 1.00, vatRate = VatRate.NineteenPercent, visibility = Visibility.Internal), it.discountPercentage)
    }

    assertThat(sumsHtml(withInternalLine)).contains("<td>Nettosumme</td><td class=\"money\">26,00 €</td>")
  }

  @Test
  fun `a line is indented by its depth`() {
    val html = linesHtml()

    assertThat(html.rowOf("Bücher")).contains("style=\"padding-left: 0em\"")
    assertThat(html.rowOf("Mockingbird")).contains("style=\"padding-left: 1em\"")
  }

  @Test
  fun `an optional line carries its position in parentheses and states its price in parentheses`() {
    val row = linesHtml().rowOf("Geschenkverpackung")

    assertThat(row).contains("class=\"depth-2 optional\"")
    assertThat(row).contains("<td class=\"position\">(1.4)</td>")
    assertThat(row).contains("Geschenkverpackung (Optional)")
    assertThat(row).contains("<td class=\"money\">(3,00 €)</td>")
  }

  @Test
  fun `a negative quantity marks the row as credit, a negative unit price does not`() {
    val html = linesHtml()

    assertThat(html.rowOf("Gutschrift")).contains("class=\"depth-2 credit\"")
    assertThat(html.rowOf("Gutschrift")).contains("<td class=\"money\">-5,00 €</td>")
    assertThat(html.rowOf("Nachlass")).doesNotContain("credit")
  }

  @Test
  fun `the unit price stays empty at a quantity of one`() {
    val html = linesHtml()

    assertThat(html.rowOf("Lesung")).contains("<td class=\"money\"></td><td class=\"money\">12,00 €</td>")
    assertThat(html.rowOf("Mockingbird")).contains("<td class=\"money\">10,00 €</td><td class=\"money\">20,00 €</td>")
  }

  @Test
  fun `the details of a line stand below its headline`() {
    assertThat(linesHtml().rowOf("Mockingbird")).contains("<div class=\"line-details\">Harper Lee\nTaschenbuch</div>")
  }

  @Test
  fun `a section is numbered on the top level and its items below it`() {
    val html = linesHtml()

    assertThat(html.rowOf("Bücher")).contains("class=\"depth-1 section\"")
    assertThat(html.rowOf("Mockingbird")).contains("<td class=\"position\">1.1</td>")
    assertThat(html.rowOf("Lesung")).contains("<td class=\"position\">2</td>")
  }

  private fun linesHtml(): String = render { quoteLinesTable(pricedElements(discountPercentage = 0.05).toFlatLines(VisibilityQuery.onlyPublic), I18nConfiguration.Germany) }

  private fun sumsHtml(pricedElements: PricedElements): String = render { quoteSumsBlock(pricedElements, VisibilityQuery.onlyPublic, I18nConfiguration.Germany) }

  private fun render(content: BODY.() -> Unit): String {
    return createHTML(prettyPrint = false).html { body { content() } }.replace('\u00A0', ' ').replace('\u202F', ' ')
  }

  /** The `<tr …>` of the one row that names [text]. */
  private fun String.rowOf(text: String): String = split("<tr").single { it.contains(text) }

  private fun pricedElements(discountPercentage: Double, withOptionalLine: Boolean = true): PricedElements = PricedElements(
    elements = listOf(
      PricedSection(
        uuid = Uuid.parse("00000000-0000-0000-0000-000000000001"),
        headline = "Bücher",
        visibility = Visibility.Public,
        children = listOfNotNull(
          item(2, "Mockingbird", amount = 2.0, price = 10.00, vatRate = VatRate.SevenPercent, details = "Harper Lee\nTaschenbuch"),
          item(3, "Gutschrift", amount = -1.0, price = 5.00, vatRate = VatRate.SevenPercent),
          item(4, "Nachlass", amount = 1.0, price = -1.00, vatRate = VatRate.SevenPercent),
          item(5, "Geschenkverpackung", amount = 1.0, price = 3.00, vatRate = VatRate.NineteenPercent, optionality = Optionality.Optional).takeIf { withOptionalLine },
        ),
      ),
      item(6, "Lesung", amount = 1.0, price = 12.00, vatRate = VatRate.NineteenPercent),
    ),
    discountPercentage = discountPercentage,
  )

  private fun item(index: Int, headline: String, amount: Double, price: Double, vatRate: VatRate, optionality: Optionality = Optionality.Mandatory, visibility: Visibility = Visibility.Public, details: String? = null): PricedItem = PricedItem(
    uuid = Uuid.parse("00000000-0000-0000-0000-00000000000$index"),
    headline = headline,
    details = details,
    priceForOneElement = PriceWithProfit.noProfit(price.euroNet),
    amount = Amount(amount),
    visibility = visibility,
    relevance = Tags.empty,
    optionality = optionality,
    vatRate = vatRate,
  )
}
