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

import assertk.*
import assertk.assertions.*
import it.neckar.commons.tags.Tag
import it.neckar.financial.currency.Money
import it.neckar.financial.currency.PriceWithProfit
import it.neckar.financial.currency.VatRate
import it.neckar.financial.currency.euroGross
import it.neckar.financial.currency.euroNet
import it.neckar.financial.currency.euroVat
import it.neckar.financial.priced.PricedElements
import it.neckar.financial.quote.Optionality
import it.neckar.financial.quote.Visibility
import it.neckar.financial.quote.query.VisibilityQuery
import it.neckar.open.kotlin.lang.percent
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import org.junit.jupiter.api.Test

class FlatQuoteDetailsTest {

  private val relevance = Tag("myTestRelevance")

  @Test
  fun `sections and items alike carry their position in the tree`() {
    val flat = nestedPricedElements().toFlatDetails()

    assertThat(flat.lines.map { "${it.position.value} ${it.headline}" }).isEqualTo(
      listOf(
        "1 Bücher",
        "1.1 Ein Buch",
        "1.2 Zubehör",
        "1.2.1 Lesezeichen",
        "2 Versand",
        "2.1 Verpackung",
      )
    )
  }

  @Test
  fun `the depth of a line counts from one`() {
    val flat = nestedPricedElements().toFlatDetails()

    assertThat(flat.lines.map { it.depth }).isEqualTo(listOf(1, 2, 2, 3, 1, 2))
  }

  @Test
  fun `a section is a section, everything below it an item`() {
    val flat = nestedPricedElements().toFlatDetails()

    assertThat(flat.lines.filterIsInstance<FlatQuoteLineDetails.Section>().map { it.headline })
      .isEqualTo(listOf("Bücher", "Zubehör", "Versand"))
  }

  @Test
  fun `a section carries no amount`() {
    val section = nestedPricedElements().toFlatDetails().lines.first()

    assertThat(section).isInstanceOf(FlatQuoteLineDetails.Section::class)
    assertThat(section.amount).isNull()
  }

  @Test
  fun `an item of amount one states its unit price`() {
    val flat = nestedPricedElements().toFlatDetails()

    val packaging = flat.lines.filterIsInstance<FlatQuoteLineDetails.Item>().single { it.headline == "Verpackung" }
    assertThat(packaging.amount.amount).isEqualTo(1.0)
    assertThat(packaging.unitPrice).isEqualTo(1.50.euroNet)
    assertThat(packaging.total).isEqualTo(1.50.euroNet)
  }

  @Test
  fun `an item of a larger amount is priced per unit`() {
    val book = nestedPricedElements().toFlatDetails().lines.filterIsInstance<FlatQuoteLineDetails.Item>().single { it.headline == "Ein Buch" }

    assertThat(book.amount.amount).isEqualTo(2.0)
    assertThat(book.unitPrice).isEqualTo(17.47.euroNet)
    assertThat(book.total).isEqualTo(34.94.euroNet)
  }

  @Test
  fun `a section priced at zero is still named`() {
    val pricedElements = PricedElements {
      section("Versand") {
        item(headline = "Verpackung", priceForOneElement = PriceWithProfit.noProfit(Money.Net.Zero), amount = 1.0, relevance = relevance, visibility = Visibility.Public, vatRate = VatRate.NineteenPercent)
      }
    }

    assertThat(pricedElements.toFlatDetails().lines.map { it.headline }).isEqualTo(listOf("Versand", "Verpackung"))
  }

  @Test
  fun `the net price is the total over the top-level lines`() {
    val flat = nestedPricedElements().toFlatDetails()

    assertThat(flat.sums.net).isEqualTo(39.24.euroNet)
    assertThat(flat.lines.filter { it.depth == 1 }.map { it.total }).isEqualTo(listOf(37.74.euroNet, 1.50.euroNet))
  }

  @Test
  fun `the sums break the bill down by tax rate`() {
    val sums = nestedPricedElements().toFlatDetails().sums

    assertThat(sums.vatRates).isEqualTo(listOf(VatRate.SevenPercent, VatRate.NineteenPercent))
    // The books at 7 %, the bookmark and the packaging at 19 %.
    assertThat(sums.vatBreakdown.map { it.net }).isEqualTo(listOf(34.94.euroNet, 4.30.euroNet))
    assertThat(sums.vatBreakdown.map { it.vatAmount }).isEqualTo(listOf(2.45.euroVat, 0.82.euroVat))
    assertThat(sums.gross).isEqualTo(42.51.euroGross)
  }

  @Test
  fun `an internal section is in neither the lines nor the total`() {
    val pricedElements = PricedElements {
      section("Bücher") {
        item(headline = "Ein Buch", priceForOneElement = PriceWithProfit(10.00.euroNet, 17.47.euroNet), amount = 1.0, relevance = relevance, visibility = Visibility.Public, vatRate = VatRate.SevenPercent)
      }

      section("Kalkulation") {
        visibility = Visibility.Internal
        item(headline = "Interner Aufschlag", priceForOneElement = PriceWithProfit(1.00.euroNet, 2.00.euroNet), amount = 1.0, relevance = relevance, visibility = Visibility.Internal, vatRate = VatRate.NineteenPercent)
      }
    }

    val flat = pricedElements.toFlatDetails()
    assertThat(flat.lines.map { it.headline }).isEqualTo(listOf("Bücher", "Ein Buch"))
    assertThat(flat.sums.net).isEqualTo(17.47.euroNet)
  }

  @Test
  fun `onlyMandatory shows an internal section the default hides`() {
    val pricedElements = PricedElements {
      section("Bücher") {
        item(headline = "Ein Buch", priceForOneElement = PriceWithProfit(10.00.euroNet, 17.47.euroNet), amount = 1.0, relevance = relevance, visibility = Visibility.Public, vatRate = VatRate.SevenPercent)
      }

      section("Kalkulation") {
        visibility = Visibility.Internal
        item(headline = "Interner Aufschlag", priceForOneElement = PriceWithProfit(1.00.euroNet, 2.00.euroNet), amount = 1.0, relevance = relevance, visibility = Visibility.Internal, vatRate = VatRate.NineteenPercent)
      }
    }

    val flat = pricedElements.toFlatDetails(visibilityQuery = VisibilityQuery.onlyMandatory)
    assertThat(flat.lines.map { it.headline }).isEqualTo(listOf("Bücher", "Ein Buch", "Kalkulation", "Interner Aufschlag"))
    assertThat(flat.sums.net).isEqualTo(19.47.euroNet)
  }

  @Test
  fun `a quote without elements bills nothing`() {
    val flat = PricedElements.empty.toFlatDetails()

    assertThat(flat.lines).isEmpty()
    assertThat(flat.sums.net).isEqualTo(Money.Net.Zero)
  }

  @Test
  fun `an item at the top level is a line of its own and carries the total`() {
    val pricedElements = PricedElements {
      item(headline = "Ein Buch", priceForOneElement = PriceWithProfit(10.00.euroNet, 17.47.euroNet), amount = 2.0, relevance = relevance, visibility = Visibility.Public, vatRate = VatRate.SevenPercent)
    }

    val flat = pricedElements.toFlatDetails()
    assertThat(flat.lines.map { "${it.position.value} ${it.headline}" }).isEqualTo(listOf("1 Ein Buch"))
    assertThat(flat.sums.net).isEqualTo(34.94.euroNet)
  }

  @Test
  fun `an internal item is no line of the bill`() {
    val pricedElements = PricedElements {
      section("Bücher") {
        item(headline = "Ein Buch", priceForOneElement = PriceWithProfit(10.00.euroNet, 17.47.euroNet), amount = 1.0, relevance = relevance, visibility = Visibility.Public, vatRate = VatRate.SevenPercent)
      }

      // A section holds children of one visibility only, so an internal item needs a section of its own.
      section("Kalkulation") {
        item(headline = "Interner Aufschlag", priceForOneElement = PriceWithProfit(1.00.euroNet, 2.00.euroNet), amount = 1.0, relevance = relevance, visibility = Visibility.Internal, vatRate = VatRate.NineteenPercent)
      }
    }

    val flat = pricedElements.toFlatDetails()
    assertThat(flat.lines.map { it.headline }).doesNotContain("Interner Aufschlag")
    // Its section is public and shows, so the internal item's price reaches the bill through that
    // section's total — the rows still add up, and the item itself is never named.
    assertThat(flat.sums.net).isEqualTo(19.47.euroNet)
  }

  @Test
  fun `an item of amount zero still states its unit price`() {
    val pricedElements = PricedElements {
      section("Bücher") {
        item(headline = "Noch ohne Stückzahl", priceForOneElement = PriceWithProfit(10.00.euroNet, 17.47.euroNet), amount = 0.0, relevance = relevance, visibility = Visibility.Public, vatRate = VatRate.SevenPercent)
      }
    }

    val line = pricedElements.toFlatDetails().lines.filterIsInstance<FlatQuoteLineDetails.Item>().single()
    assertThat(line.unitPrice).isEqualTo(17.47.euroNet)
    assertThat(line.total).isEqualTo(Money.Net.Zero)
  }

  @Test
  fun `an optional item is no line of the bill`() {
    val pricedElements = PricedElements {
      section("Bücher") {
        item(headline = "Ein Buch", priceForOneElement = PriceWithProfit(10.00.euroNet, 17.47.euroNet), amount = 1.0, relevance = relevance, visibility = Visibility.Public, vatRate = VatRate.SevenPercent)
      }

      section("Zusatz") {
        item(headline = "Geschenkverpackung", priceForOneElement = PriceWithProfit(1.00.euroNet, 2.00.euroNet), amount = 1.0, relevance = relevance, visibility = Visibility.Public, vatRate = VatRate.NineteenPercent) {
          optionality = Optionality.Optional
        }
      }
    }

    // The sums are the mandatory ones, so an optional line would stand there priced at zero.
    assertThat(pricedElements.toFlatDetails().lines.map { it.headline }).doesNotContain("Geschenkverpackung")
  }

  @Test
  fun `a discounted quote lists its lines before the discount`() {
    val pricedElements = PricedElements(
      elements = nestedPricedElements().elements,
      discountPercentage = 10.percent,
    )

    val flat = pricedElements.toFlatDetails()

    assertThat(flat.lines.filter { it.depth == 1 }.map { it.total }).isEqualTo(listOf(37.74.euroNet, 1.50.euroNet))
    assertThat(flat.sums.preDiscountNet).isEqualTo(39.24.euroNet)
    // Each item is discounted and rounded on its own: 31,45 € + 2,52 € + 1,35 €.
    assertThat(flat.sums.net).isEqualTo(35.32.euroNet)
    assertThat(flat.sums.discount).isEqualTo((-3.92).euroNet)
    assertThat(flat.sums.net).isEqualTo(flat.sums.preDiscountNet + flat.sums.discount)
  }

  @Test
  fun `a quote without a discount carries a discount of zero`() {
    val sums = nestedPricedElements().toFlatDetails().sums

    assertThat(sums.discount).isEqualTo(Money.Net.Zero)
    assertThat(sums.net).isEqualTo(sums.preDiscountNet)
  }

  @Test
  fun `a line states no purchase price`() {
    val json = Json.encodeToString(FlatQuoteDetails.serializer(), nestedPricedElements().toFlatDetails())
    val lines = Json.parseToJsonElement(json).jsonObject.getValue("lines").jsonArray.map { it.jsonObject }
    val sharedKeys = setOf("type", "position", "depth", "headline", "details", "amount", "total", "totalIncludingOptional", "optionality", "vatRates", "visibility")

    assertThat(json).doesNotContain("inputPrice")
    // "Bücher", then "Ein Buch".
    assertThat(lines[0].keys).isEqualTo(sharedKeys)
    assertThat(lines[1].keys).isEqualTo(sharedKeys + "unitPrice")
  }

  /**
   * A quote with a section inside a section, so the numbering has three levels to walk.
   *
   * Books: 2 × 17,47 € plus a bookmark at 2,80 €, shipping: packaging at 1,50 €.
   */
  private fun nestedPricedElements(): PricedElements = PricedElements {
    section("Bücher") {
      item(headline = "Ein Buch", priceForOneElement = PriceWithProfit(10.00.euroNet, 17.47.euroNet), amount = 2.0, relevance = relevance, visibility = Visibility.Public, vatRate = VatRate.SevenPercent)

      section("Zubehör", Visibility.Public) {
        item(headline = "Lesezeichen", priceForOneElement = PriceWithProfit(1.00.euroNet, 2.80.euroNet), amount = 1.0, relevance = relevance, visibility = Visibility.Public, vatRate = VatRate.NineteenPercent)
      }
    }

    section("Versand") {
      item(headline = "Verpackung", priceForOneElement = PriceWithProfit(1.00.euroNet, 1.50.euroNet), amount = 1.0, relevance = relevance, visibility = Visibility.Public, vatRate = VatRate.NineteenPercent)
    }
  }
}
