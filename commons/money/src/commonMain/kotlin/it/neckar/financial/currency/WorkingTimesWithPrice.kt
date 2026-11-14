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
import it.neckar.open.unit.currency.EUR
import it.neckar.open.unit.other.pct
import it.neckar.open.unit.time.h
import kotlinx.serialization.Serializable
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours

/**
 * Contains information about a selling price and profit
 */
@CrossLayer("Net price value object — identical shape across Domain, Mongo entity, and REST layers")
@Serializable
data class WorkingTimesWithPrice(
  /**
   * The input price ("real" cost).
   *
   * This is the price:
   * * one is paying when buying by a third party.
   * * the calculatory, direct cost when producing/creating/working
   */
  val workingTime: Duration,
  /**
   * The selling price
   */
  val workingHourPrice: PricePerHour,

  ) {

  val priceWithProfit: PriceWithProfit
    get() = workingTime * workingHourPrice

  operator fun plus(other: WorkingTimesWithPrice): WorkingTimesWithPrice {
    // Zero carries no working time and therefore no hourly rate of its own - it adopts the other side's rate.
    // Without this the sum extensions below, which fold over Zero, would fail on their very first element.
    if (this == Zero) return other
    if (other == Zero) return this

    require(workingHourPrice == other.workingHourPrice) { "workingHourPrices <$workingHourPrice> and <${other.workingHourPrice}> need to be identical to be added!" }
    return WorkingTimesWithPrice(this.workingTime + other.workingTime, workingHourPrice)
  }

  operator fun plus(other: Duration): WorkingTimesWithPrice {
    return WorkingTimesWithPrice(this.workingTime + other, workingHourPrice)
  }

  operator fun minus(other: WorkingTimesWithPrice): WorkingTimesWithPrice {
    // Zero is the neutral element here, too - see plus
    if (this == Zero) return -other
    if (other == Zero) return this

    require(workingHourPrice == other.workingHourPrice) { "workingHourPrices <$workingHourPrice> and <${other.workingHourPrice}> need to be identical to be subtracted!" }
    return WorkingTimesWithPrice(this.workingTime - other.workingTime, workingHourPrice)
  }

  operator fun minus(other: Duration): WorkingTimesWithPrice {
    return WorkingTimesWithPrice(this.workingTime - other, workingHourPrice)
  }

  operator fun times(factor: Double): WorkingTimesWithPrice {
    return WorkingTimesWithPrice(this.workingTime * factor, workingHourPrice)
  }

  operator fun times(factor: Int): WorkingTimesWithPrice {
    return WorkingTimesWithPrice(this.workingTime * factor, workingHourPrice)
  }

  operator fun div(other: Double): @pct WorkingTimesWithPrice {
    return WorkingTimesWithPrice(this.workingTime / other, workingHourPrice)
  }

  operator fun unaryMinus(): WorkingTimesWithPrice {
    return WorkingTimesWithPrice(-workingTime, workingHourPrice)
  }


  companion object {

    val Zero: WorkingTimesWithPrice = WorkingTimesWithPrice(Duration.ZERO, PricePerHour.zero)

    operator fun invoke(workingTimeHours: @h Double, workingHourPriceEuros: @EUR Double): WorkingTimesWithPrice {
      return WorkingTimesWithPrice(workingTimeHours.hours, PricePerHour(workingHourPriceEuros.euroNet))
    }

  }
}


/**
 * Calculates the sum
 */
fun Iterable<WorkingTimesWithPrice>.sum(): WorkingTimesWithPrice {
  var workingTime: WorkingTimesWithPrice = WorkingTimesWithPrice.Zero
  this.forEach { workingTime += it }
  return workingTime
}
