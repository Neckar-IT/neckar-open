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
import it.neckar.open.unit.number.Positive
import it.neckar.open.unit.other.pct
import kotlin.math.pow

/**
 * Calculates a fixed rate
 */
class FixedRateMortgageCalculator(
  /**
   * "Kreditsumme"
   */
  val loanAmount: Money,
  /**
   * "Zinssatz"
   */
  val interestRate: @pct @Positive Double,

  /**
   * "Laufzeit in Jahren"
   */
  val termInYears: Int
) {

  /**
   * (1 + interest rate)
   */
  private val q: Double = 1 + interestRate

  init {
    require(interestRate > 0.0) {
      "Positive interest rate required but was <$interestRate>"
    }
  }

  /**
   * Calculates the yearly payment
   * (https://de.wikipedia.org/wiki/Annuit%C3%A4tendarlehen)
   */
  fun yearlyPayment(): Money {
    val upper = q.pow(termInYears)
    val lower = q.pow(termInYears) - 1
    return loanAmount.timesAndRound(upper).timesAndRound(interestRate).divAndRound(lower)
  }

  /**
   * The interest for the given year
   */
  fun yearlyInterest(yearIndex: YearIndex): Money {
    return loanAmount.timesAndRound(q.pow(termInYears) - q.pow(yearIndex.index)).divAndRound(q.pow(termInYears) - 1).timesAndRound(interestRate)
  }

  /**
   * The repayment for the given year
   */
  fun yearlyRepayment(yearIndex: YearIndex): Money {
    return loanAmount.timesAndRound(q - 1).divAndRound(q.pow(termInYears) - 1).timesAndRound(q.pow(yearIndex.index))
  }

  fun residualDebtAtStartOfYear(yearIndex: YearIndex): Money {
    if (yearIndex == YearIndex.base) {
      return loanAmount
    }

    return residualDebtAfterRepayment(yearIndex - 1)
  }

  /**
   * The residual debt after repayment (at the end of the year)
   */
  fun residualDebtAfterRepayment(yearIndex: YearIndex): Money {
    return loanAmount.timesAndRound(q.pow(termInYears) - q.pow(yearIndex.indexBase1)).divAndRound((q.pow(termInYears) - 1))
  }

  /**
   * Returns the monthly payment (interest and
   */
  fun monthlyPayment(): Money {
    return yearlyPayment().divAndRound(12.0)
  }
}
