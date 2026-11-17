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
package it.neckar.financial.economics

import it.neckar.financial.currency.Money
import it.neckar.open.annotations.GermanTerm
import it.neckar.open.unit.other.pct
import it.neckar.open.unit.time.pa
import kotlin.math.pow


/**
 * Represents a money value that increases by a fixed percentage each year.
 *
 * Example:
 * ```
 * val salary = Money.euros(50000.0)
 * val increasing = salary.increasingBy(0.03) // +3% per year
 * val after5Years = increasing.calculateValue(5)
 * ```
 */
@GermanTerm("Geldbetrag mit jährlicher Steigerung")
data class IncreasingMoney(
  /**
   * The base value (at the start of year 0)
   */
  @GermanTerm("Basiswert")
  val baseValue: Money,

  /**
   * The yearly relative increase as a fraction
   */
  @GermanTerm("Jährliche Steigerungsrate")
  val increase: @pa @pct Double,
) {

  /**
   * Calculates the value after a given number of years.
   *
   * Uses compound growth:
   * `value = baseValue * (1 + increase) ^ years`
   *
   * @return the increased value rounded according to [Money] rules
   */
  fun calculateValue(
    /**
     * The number of years (≥ 0) that have passed
     */
    years: Int
  ): Money {
    val factor = (1.0 + increase).pow(years)
    return baseValue.timesAndRound(factor)
  }
}

/**
 * Extension to wrap a [Money] value in an [IncreasingMoney] structure.
 *
 * @param yearlyIncrease the yearly relative increase as a fraction (e.g. 0.02 for 2 %)
 * @return an [IncreasingMoney] instance representing this base value with annual growth
 */
fun Money.increasingBy(yearlyIncrease: @pct Double): IncreasingMoney {
  return IncreasingMoney(this, yearlyIncrease)
}
