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
import it.neckar.financial.quote.Amount
import it.neckar.financial.quote.Optionality
import it.neckar.financial.quote.Visibility
import it.neckar.open.unit.other.pct
import it.neckar.uuid.randomForEnvironment
import kotlin.uuid.Uuid

@PricedDsl
class PricedItemBuilder(
  var uuid: Uuid = Uuid.randomForEnvironment(),

  var headline: String,

  var priceForOneElement: PriceWithProfit,

  var amount: Amount = Amount(1),

  var factor: @pct Double = 1.0,

  var relevance: Tags,

  var visibility: Visibility,

  var optionality: Optionality,

  var vatRate: VatRate,

  var details: String? = null,

  ) : PricedElementBuilder {

  constructor(
    uuid: Uuid = Uuid.randomForEnvironment(),
    headline: String,
    priceForOneElement: PriceWithProfit,
    amount: Amount = Amount(1),
    factor: @pct Double = 1.0,
    relevance: Tag,
    visibility: Visibility,
    optionality: Optionality,
    vatRate: VatRate,
    details: String? = null,
  ) : this(
    uuid = uuid,
    headline = headline,
    priceForOneElement = priceForOneElement,
    amount = amount,
    factor = factor,
    relevance = relevance.toTagsOrEmpty(),
    visibility = visibility,
    optionality = optionality,
    vatRate = vatRate,
    details = details,
  )

  @PricedDsl
  fun optional() {
    optionality = Optionality.Optional
  }

  override fun build(): PricedItem {
    return PricedItem(
      uuid = uuid,
      headline = headline,
      details = details,
      priceForOneElement = priceForOneElement,
      amount = amount,
      factor = factor,
      visibility = visibility,
      relevance = relevance,
      optionality = optionality,
      vatRate = vatRate,
    )
  }
}

fun pricedItem(
  headline: String,
  priceForOneElement: PriceWithProfit,

  amount: Double,

  factor: @pct Double,

  relevance: Tags,
  visibility: Visibility,

  optionality: Optionality,

  vatRate: VatRate,
  config: PricedItemBuilder.() -> Unit = {},
): PricedItemBuilder {
  return PricedItemBuilder(
    headline = headline,
    priceForOneElement = priceForOneElement,
    amount = Amount(amount),
    factor = factor,
    relevance = relevance,
    visibility = visibility,
    optionality = optionality,
    vatRate = vatRate,
  ).also(config)
}

fun pricedItem(headline: String, priceForOneElement: PriceWithProfit, amount: Double, factor: @pct Double, relevance: Tag, visibility: Visibility, optionality: Optionality, vatRate: VatRate, config: PricedItemBuilder.() -> Unit = {}): PricedItemBuilder {
  return pricedItem(headline, priceForOneElement, amount, factor, relevance.toTagsOrEmpty(), visibility, optionality, vatRate, config)
}
