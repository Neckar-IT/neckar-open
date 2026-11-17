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
package it.neckar.financial.priced.input

import it.neckar.commons.tags.Tags
import it.neckar.financial.currency.Money
import it.neckar.financial.currency.PriceWithProfit
import it.neckar.financial.currency.VatRate
import it.neckar.financial.priced.PricedItem
import it.neckar.financial.quote.Amount
import it.neckar.financial.quote.Optionality
import it.neckar.financial.quote.Visibility
import it.neckar.open.kotlin.lang.percent
import it.neckar.open.unit.currency.EUR
import it.neckar.open.unit.other.pct
import it.neckar.uuid.NativeUuid
import it.neckar.uuid.randomUuid4
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A [PricedItem] as a producer states it - [sellingPrice] and [netPrice] may be a negotiated flat price
 * instead of the multiplication.
 */
@Serializable
@SerialName("PricedItemInput")
data class PricedItemInput(
  val uuid: NativeUuid? = null,

  val headline: String,

  val details: String?,

  /**
   * The price for *one* item, multiplied with [amount] unless [sellingPrice] states the total.
   */
  val priceForOneElement: PriceWithProfit,

  val amount: Amount,

  val factor: @pct Double = 100.percent,

  val visibility: Visibility,

  val relevance: Tags,

  val optionality: Optionality = Optionality.Mandatory,

  val vatRate: VatRate,

  val sellingPrice: @EUR Money.Net? = null,

  val netPrice: @EUR Money.Net? = null,
) : PricedElementInput {

  override fun toPricedElement(): PricedItem = PricedItem.invoke(
    uuid = uuid ?: randomUuid4(),
    headline = headline,
    details = details,
    priceForOneElement = priceForOneElement,
    amount = amount,
    factor = factor,
    visibility = visibility,
    relevance = relevance,
    optionality = optionality,
    vatRate = vatRate,
    sellingPrice = sellingPrice,
    netPrice = netPrice,
  )
}
