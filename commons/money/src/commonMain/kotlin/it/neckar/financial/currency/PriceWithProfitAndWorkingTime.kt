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
import it.neckar.open.unit.time.h
import kotlinx.serialization.Serializable
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours

/**
 * Represents a composed price structure consisting of
 * * a [PriceWithProfit] for material
 * * a [WorkingTimesWithPrice] for work effort
 *
 * Provides convenience accessors for total cost, selling price,
 * and arithmetic operations on combined items.
 */
@CrossLayer("Net price value object — identical shape across Domain, Mongo entity, and REST layers")
@Serializable
@GermanTerm("Preis mit Gewinn und Arbeitszeit")
data class PriceWithProfitAndWorkingTime(
  /**
   * The material cost and selling price.
   */
  val materialPrice: PriceWithProfit,

  /**
   * The working time and its associated cost and price.
   */
  val workingTimes: WorkingTimesWithPrice,
) {

  /**
   * The total profit (selling price - input price).
   * Includes material and working time.
   */
  val profit: Money.Net
    get() = sellingPrice - inputPrice

  /**
   * True if the selling price is greater than zero.
   * Indicates that this position represents a positive revenue.
   */
  val greaterThanZero: Boolean
    get() = sellingPrice > Money.Net.Zero

  /**
   * True if the selling price is less than zero.
   * Indicates that this position represents a negative revenue.
   */
  val lessThanZero: Boolean
    get() = sellingPrice < Money.Net.Zero

  /**
   * The aggregated [PriceWithProfit] including material and work.
   */
  val priceWithProfit: PriceWithProfit
    get() = materialPrice + workingTimes.priceWithProfit

  /**
   * The total input (cost) price.
   */
  val inputPrice: Money.Net
    get() = priceWithProfit.inputPrice

  /**
   * The total selling price.
   */
  val sellingPrice: Money.Net
    get() = priceWithProfit.sellingPrice

  /**
   * The total working duration.
   */
  val workingTime: Duration
    get() = workingTimes.workingTime

  /**
   * The hourly rate for the working time.
   */
  val workingHourPrice: PricePerHour
    get() = workingTimes.workingHourPrice

  /**
   * Adds another [PriceWithProfit] (material only).
   */
  operator fun plus(other: PriceWithProfit): PriceWithProfitAndWorkingTime {
    return PriceWithProfitAndWorkingTime(materialPrice + other, workingTimes)
  }

  /**
   * Adds another full [PriceWithProfitAndWorkingTime] object
   * (material and work parts combined).
   */
  operator fun plus(other: PriceWithProfitAndWorkingTime): PriceWithProfitAndWorkingTime {
    return PriceWithProfitAndWorkingTime(materialPrice + other.materialPrice, workingTimes + other.workingTimes)
  }

  /**
   * Subtracts another [PriceWithProfit] (material only).
   */
  operator fun minus(other: PriceWithProfit): PriceWithProfitAndWorkingTime {
    return PriceWithProfitAndWorkingTime(materialPrice - other, workingTimes)
  }

  /**
   * Subtracts another [PriceWithProfitAndWorkingTime] object.
   */
  operator fun minus(other: PriceWithProfitAndWorkingTime): PriceWithProfitAndWorkingTime {
    return PriceWithProfitAndWorkingTime(materialPrice - other.materialPrice, workingTimes - other.workingTimes)
  }

  /**
   * Scales the prices and working times by a given factor.
   */
  fun times(factor: Double): PriceWithProfitAndWorkingTime {
    return PriceWithProfitAndWorkingTime(materialPrice.timesAndRound(factor), workingTimes * factor)
  }

  /**
   * Scales the prices and working times by an integer factor.
   */
  operator fun times(factor: Int): PriceWithProfitAndWorkingTime {
    return PriceWithProfitAndWorkingTime(materialPrice * factor, workingTimes * factor)
  }

  /**
   * Divides all values by the given factor.
   */
  fun div(other: Double): PriceWithProfitAndWorkingTime {
    return PriceWithProfitAndWorkingTime(materialPrice.divAndRound(other), workingTimes / other)
  }

  /**
   * Negates all prices and working times.
   */
  operator fun unaryMinus(): PriceWithProfitAndWorkingTime {
    return PriceWithProfitAndWorkingTime(-materialPrice, -workingTimes)
  }

  /**
   * Adds a duration to the working time, keeping material price unchanged.
   */
  operator fun plus(other: Duration): PriceWithProfitAndWorkingTime {
    return PriceWithProfitAndWorkingTime(materialPrice, workingTimes + other)
  }

  /**
   * Subtracts a duration from the working time, keeping material price unchanged.
   */
  operator fun minus(other: Duration): PriceWithProfitAndWorkingTime {
    return PriceWithProfitAndWorkingTime(materialPrice, workingTimes - other)
  }

  /**
   * Returns a simple formatted representation
   * showing input and selling prices.
   */
  fun dump(i18nConfiguration: I18nConfiguration = CurrentI18nConfiguration): String {
    // No non-breaking spaces for testing purposes
    return "${inputPrice.format(i18nConfiguration, WhitespaceConfig.Spaces)} / ${sellingPrice.format(i18nConfiguration, WhitespaceConfig.Spaces)}"
  }

  companion object {
    /**
     * A constant zero-value instance.
     */
    val Zero: PriceWithProfitAndWorkingTime =
      PriceWithProfitAndWorkingTime(PriceWithProfit.Zero, WorkingTimesWithPrice.Zero)

    /**
     * Creates a new instance using raw euro values and hour information.
     */
    operator fun invoke(
      inputEuros: @EUR Double,
      sellingEuros: @EUR Double,
      workingTimeHours: @h Double,
      workingHourPriceEuros: @EUR Double,
    ): PriceWithProfitAndWorkingTime {
      return PriceWithProfitAndWorkingTime(
        PriceWithProfit(inputEuros.euroNet, sellingEuros.euroNet),
        WorkingTimesWithPrice(workingTimeHours.hours, PricePerHour(workingHourPriceEuros.euroNet))
      )
    }

    /**
     * Creates a new instance using an existing material price
     * and working time and hourly rate values.
     */
    operator fun invoke(
      materialPrice: PriceWithProfit,
      workingTimeHours: @h Double,
      workingHourPriceEuros: @EUR Double,
    ): PriceWithProfitAndWorkingTime {
      return PriceWithProfitAndWorkingTime(
        materialPrice,
        WorkingTimesWithPrice(workingTimeHours.hours, PricePerHour(workingHourPriceEuros.euroNet))
      )
    }

    /**
     * Creates a new instance using an existing material price,
     * a [Duration]-based working time, and a [PricePerHour].
     */
    operator fun invoke(
      materialPrice: PriceWithProfit,
      workingTimeHours: @h Duration,
      workingHourPrice: PricePerHour,
    ): PriceWithProfitAndWorkingTime {
      return PriceWithProfitAndWorkingTime(materialPrice, WorkingTimesWithPrice(workingTimeHours, workingHourPrice))
    }

    /**
     * Creates a new instance using explicit working time information.
     */
    operator fun invoke(
      inputEuros: @EUR Double,
      sellingEuros: @EUR Double,
      workingTimes: WorkingTimesWithPrice,
    ): PriceWithProfitAndWorkingTime {
      return PriceWithProfitAndWorkingTime(
        PriceWithProfit(inputEuros.euroNet, sellingEuros.euroNet),
        workingTimes
      )
    }
  }
}

/**
 * Calculates the total sum of an [Iterable] of [PriceWithProfitAndWorkingTime].
 * Adds up all material prices and working times.
 */
fun Iterable<PriceWithProfitAndWorkingTime>.sum(): PriceWithProfitAndWorkingTime {
  var materialPriceSum: PriceWithProfit = PriceWithProfit.Zero
  var workingTimes: WorkingTimesWithPrice = WorkingTimesWithPrice.Zero

  this.forEach {
    materialPriceSum += it.materialPrice
    workingTimes += it.workingTimes
  }

  return PriceWithProfitAndWorkingTime(materialPriceSum, workingTimes)
}
