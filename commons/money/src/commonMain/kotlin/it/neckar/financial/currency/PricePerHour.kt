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

import it.neckar.open.annotations.GermanTerm
import it.neckar.open.unit.number.IsFinite
import it.neckar.open.unit.time.PerHour
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.DurationUnit

/**
 * Represents costs per hour
 */
@JvmInline
@Serializable
@GermanTerm("Preis pro Stunde")
value class PricePerHour(
  /**
   * The price per hour
   */
  val price: @PerHour Money.Net,
) {

  @Deprecated("Does not make sense")
  val priceWithProfit: PriceWithProfit
    get() = PriceWithProfit(Money.Net.Zero, price)

  /**
   * Calculates the price for the given duration
   */
  operator fun times(duration: Duration): PriceWithProfit {
    return PriceWithProfit(Money.Net.Zero, price.timesAndRound(duration.toDouble(DurationUnit.HOURS)))
  }

  companion object {
    val zero: PricePerHour = PricePerHour(Money.Net.Zero)

    /**
     * Returns the price per hour (for a euros double value)
     */
    fun euros(euros: @IsFinite Double): PricePerHour {
      return PricePerHour(euros.euroNet)
    }

    fun euros(euros: Money.Net): PricePerHour {
      return PricePerHour(euros)
    }

  }
}

operator fun Duration.times(pricePerHour: PricePerHour): PriceWithProfit {
  return pricePerHour * this
}

operator fun PriceWithProfit.div(pricePerHour: PricePerHour): Duration {
  return (this.sellingPrice / pricePerHour.price).hours
}
