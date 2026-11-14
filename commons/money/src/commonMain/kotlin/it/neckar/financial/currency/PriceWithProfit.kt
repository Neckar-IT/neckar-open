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
package it.neckar.financial.currency

import it.neckar.open.annotations.CrossLayer
import it.neckar.open.annotations.GermanTerm
import it.neckar.open.i18n.CurrentI18nConfiguration
import it.neckar.open.i18n.I18nConfiguration
import it.neckar.open.kotlin.lang.WhitespaceConfig
import it.neckar.open.unit.currency.EUR
import it.neckar.open.unit.other.pct
import kotlinx.serialization.Serializable

/**
 * Contains information about a selling price and profit
 */
@CrossLayer("Net price value object — identical shape across Domain, Mongo entity, and REST layers")
@Serializable
@GermanTerm("Preis mit Gewinn")
data class PriceWithProfit(
  /**
   * The input price ("real" cost).
   *
   * This is the price:
   * * one is paying when buying by a third party.
   * * the calculatory, direct cost when producing/creating/working
   */
  val inputPrice: Money.Net,
  /**
   * The selling price
   */
  val sellingPrice: Money.Net,

  ) {

  /**
   * The profit
   */
  val profit: Money.Net
    get() = sellingPrice - inputPrice

  val greaterThanZero: Boolean
    get() = sellingPrice > Money.Net.Zero

  val lessThanZero: Boolean
    get() = sellingPrice < Money.Net.Zero

  operator fun plus(other: PriceWithProfit): PriceWithProfit {
    return PriceWithProfit(this.inputPrice + other.inputPrice, this.sellingPrice + other.sellingPrice)
  }

  operator fun minus(other: PriceWithProfit): PriceWithProfit {
    return PriceWithProfit(this.inputPrice - other.inputPrice, this.sellingPrice - other.sellingPrice)
  }

  fun timesAndRound(factor: Double): PriceWithProfit {
    return PriceWithProfit(this.inputPrice.timesAndRound(factor), this.sellingPrice.timesAndRound(factor))
  }

  operator fun times(factor: Int): PriceWithProfit {
    return PriceWithProfit(this.inputPrice * factor, this.sellingPrice * factor)
  }

  fun divAndRound(other: Double): PriceWithProfit {
    return PriceWithProfit(this.inputPrice.divAndRound(other), this.sellingPrice.divAndRound(other))
  }

  operator fun unaryMinus(): PriceWithProfit {
    return PriceWithProfit(-inputPrice, -sellingPrice)
  }


  fun dump(i18nConfiguration: I18nConfiguration = CurrentI18nConfiguration): String {
    /**
     * return without non-breaking space because it is not necessary for testing
     * */
    return "${inputPrice.format(i18nConfiguration, WhitespaceConfig.Spaces)} / ${sellingPrice.format(i18nConfiguration, WhitespaceConfig.Spaces)}"
  }


  companion object {

    val Zero: PriceWithProfit = PriceWithProfit(Money.Net.Zero, Money.Net.Zero)

    operator fun invoke(inputEuros: @EUR Double, sellingEuros: @EUR Double): PriceWithProfit {
      return PriceWithProfit(inputEuros.euroNet, sellingEuros.euroNet)
    }

    /**
     * Creates a new instance *without* any profit
     */
    fun noProfit(selling: Money.Net): PriceWithProfit {
      return PriceWithProfit(selling, selling)
    }

    /**
     * Creates a price with profit from an input price and a surcharge percentage
     */
    @GermanTerm("Aufschlag")
    fun surcharge(inputPrice: Money.Net, surcharge: @pct Double): PriceWithProfit {
      return PriceWithProfit(
        inputPrice = inputPrice,
        sellingPrice = inputPrice.timesAndRound((1.0 + surcharge))
      )
    }

  }
}


/**
 * Calculates the sum
 */
fun Iterable<PriceWithProfit>.sum(): PriceWithProfit {
  var inputPriceSum: Money.Net = Money.Net.Zero
  var sellingPriceSum: Money.Net = Money.Net.Zero

  this.forEach {
    inputPriceSum += it.inputPrice
    sellingPriceSum += it.sellingPrice
  }

  return PriceWithProfit(inputPriceSum, sellingPriceSum)
}
