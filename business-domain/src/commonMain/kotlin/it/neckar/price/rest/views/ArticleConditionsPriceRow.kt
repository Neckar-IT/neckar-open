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
package it.neckar.price.rest.views

import it.neckar.financial.currency.Money
import it.neckar.open.annotations.DataView
import it.neckar.open.annotations.GermanTerm
import it.neckar.open.unit.other.pct
import it.neckar.price.rest.messages.EnteredPriceWithProfit
import kotlinx.serialization.Serializable

/**
 * One purchased position of an [ArticleConditionsPriceSummary] — the part or its accessory.
 */
@DataView("Purchased position of the article-conditions price overview — prices and surcharge")
@Serializable
data class ArticleConditionsPriceRow(
  @GermanTerm("Einkaufspreis")
  val inputPrice: Money.Net?,
  @GermanTerm("Verkaufspreis")
  val sellingPrice: Money.Net?,
  /** Selling minus purchase price; `null` while one of the two is missing. */
  @GermanTerm("Aufschlag")
  val surcharge: Money.Net?,
  /** [surcharge] relative to [inputPrice]; also `null` at a purchase price of 0 €, which has no rate. */
  val surchargeRate: @pct Double?,
) {
  companion object {
    fun of(entered: EnteredPriceWithProfit): ArticleConditionsPriceRow {
      val price = entered.toPriceWithProfit()
      return ArticleConditionsPriceRow(
        inputPrice = entered.inputPrice,
        sellingPrice = entered.sellingPrice,
        surcharge = price?.profit,
        surchargeRate = price?.takeIf { it.inputPrice.isZero().not() }?.let { it.profit / it.inputPrice },
      )
    }
  }
}
