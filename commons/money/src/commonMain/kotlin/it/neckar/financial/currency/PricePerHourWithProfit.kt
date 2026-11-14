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
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline
import kotlin.time.Duration
import kotlin.time.DurationUnit

/**
 * A [PriceWithProfit] per hour — the labour-cost analogue of [PricePerUnitWithProfit]: the wage cost
 * (purchase) and the charge-out rate (selling) per hour, both stated explicitly.
 *
 * Multiplying by a duration yields a [PriceWithProfit] with a real purchase price and a real
 * contribution margin — unlike [PricePerHour.times], which has no purchase side and reports the full
 * amount as profit.
 */
@CrossLayer("PricePerHourWithProfit — identical shape in Domain, Entity and REST")
@GermanTerm("Preis pro Stunde mit Gewinn")
@Serializable
@JvmInline
value class PricePerHourWithProfit(
  /** Wage cost ([PriceWithProfit.inputPrice]) and charge-out ([PriceWithProfit.sellingPrice]) per hour. */
  val price: PriceWithProfit,
) {
  init {
    require(price.inputPrice >= Money.Net.Zero) { "wage cost per hour must be >= 0 but was <${price.inputPrice}>" }
    require(price.sellingPrice >= Money.Net.Zero) { "charge-out rate per hour must be >= 0 but was <${price.sellingPrice}>" }
  }

  /** Wage cost per hour — the [PriceWithProfit.inputPrice] of [price]. */
  @GermanTerm("Lohnkosten pro Stunde")
  val purchasePricePerHour: Money.Net
    get() = price.inputPrice

  /** Charge-out rate per hour. Stated explicitly, not derived from a markup. */
  @GermanTerm("Verrechnungssatz pro Stunde")
  val sellingPricePerHour: Money.Net
    get() = price.sellingPrice

  /** Contribution margin per hour — charge-out minus wage cost. Derived, never stored. */
  @GermanTerm("Deckungsbeitrag pro Stunde")
  val contributionMarginPerHour: Money.Net
    get() = price.profit

  /** Wage cost and charge-out price for [duration] — a [PriceWithProfit] with a real purchase price. */
  operator fun times(duration: Duration): PriceWithProfit {
    return price.timesAndRound(duration.toDouble(DurationUnit.HOURS))
  }

  companion object {
    operator fun invoke(
      @GermanTerm("Lohnkosten pro Stunde") purchasePricePerHour: Money.Net,
      @GermanTerm("Verrechnungssatz pro Stunde") sellingPricePerHour: Money.Net,
    ): PricePerHourWithProfit {
      return PricePerHourWithProfit(PriceWithProfit(purchasePricePerHour, sellingPricePerHour))
    }
  }
}

operator fun Duration.times(pricePerHour: PricePerHourWithProfit): PriceWithProfit {
  return pricePerHour * this
}
