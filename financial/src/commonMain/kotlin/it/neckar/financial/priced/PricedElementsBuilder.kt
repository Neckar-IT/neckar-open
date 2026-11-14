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

import it.neckar.commons.tags.Tag
import it.neckar.commons.tags.Tags
import it.neckar.commons.tags.toTagsOrEmpty
import it.neckar.financial.currency.PriceWithProfit
import it.neckar.financial.currency.VatRate
import it.neckar.financial.quote.Optionality
import it.neckar.financial.quote.Visibility
import it.neckar.open.kotlin.lang.percent
import it.neckar.open.unit.other.pct

@PricedDsl
class PricedElementsBuilder {
  /**
   * The top level of the priced tree - a section or an item, matching [PricedElements.elements].
   */
  private val elements: MutableList<PricedElementBuilder> = mutableListOf()

  var discountPercentage: @pct Double = 0.0.percent

  fun add(pricedElementBuilder: PricedElementBuilder) {
    elements.add(pricedElementBuilder)
  }

  @PricedDsl
  fun section(headline: String, config: PricedSectionBuilder.() -> Unit) {
    add(PricedSectionBuilder(headline).also(config))
  }

  /**
   * Adds a new priced item next to the sections, for a document that states a position without a section around it.
   */
  @IgnorableReturnValue
  @PricedDsl
  fun item(
    headline: String,
    priceForOneElement: PriceWithProfit,
    amount: Double = 1.0,
    /**
     * An optional factor to multiply the price with - see [PricedItem.factor].
     */
    factor: @pct Double = 1.0,
    relevance: Tags,
    visibility: Visibility,
    vatRate: VatRate,
    config: PricedItemBuilder.() -> Unit = {},
  ): PricedItemBuilder {
    return pricedItem(headline, priceForOneElement, amount, factor, relevance, visibility, Optionality.Mandatory, vatRate, config)
      .also {
        add(it)
      }
  }

  @IgnorableReturnValue
  @PricedDsl
  fun item(headline: String, priceForOneElement: PriceWithProfit, amount: Double = 1.0, factor: @pct Double = 1.0, relevance: Tag, visibility: Visibility, vatRate: VatRate, config: PricedItemBuilder.() -> Unit = {}): PricedItemBuilder {
    return item(headline, priceForOneElement, amount, factor, relevance.toTagsOrEmpty(), visibility, vatRate, config)
  }

  fun build(): PricedElements {
    return PricedElements(elements = elements.map { it.build() }, discountPercentage = discountPercentage)
  }

  companion object {
    operator fun invoke(config: PricedElementsBuilder.() -> Unit): PricedElementsBuilder {
      return PricedElementsBuilder().also(config)
    }
  }
}

@Target(AnnotationTarget.CLASS, AnnotationTarget.TYPE, AnnotationTarget.TYPEALIAS, AnnotationTarget.FUNCTION)
@DslMarker
annotation class PricedDsl

