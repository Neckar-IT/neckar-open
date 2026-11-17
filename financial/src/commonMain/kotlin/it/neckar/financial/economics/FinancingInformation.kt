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
import kotlinx.serialization.Serializable

/**
 * Contains information about financing
 */
@Deprecated("Financing is no longer required")
@Serializable
data class FinancingInformation(
  /**
   * The residual debt at the start of the corresponding period *before* payment of the [repayment]
   */
  @GermanTerm("Restschuld vor Tilgung")
  val residualDebtBeforeRepayment: Money,

  /**
   * The interest rate
   * "Zinssatz"
   */
  @GermanTerm("Zinssatz")
  val interestRate: @pct Double,

  /**
   * The interest for this year
   * "Zins"
   */
  @GermanTerm("Zins")
  val interest: Money,

  /**
   * "Tilgung"
   */
  @GermanTerm("Tilgung")
  val repayment: Money,

  ) {

  /**
   * The amount spent this year ([interest] + [repayment])
   */
  @GermanTerm("Ausgaben")
  val spending: Money
    get() {
      return interest + repayment
    }

  /**
   * "Restschuld" (after payment of [repayment])
   */
  @GermanTerm("Restschuld nach Tilgung")
  val residualDebtAfterRepayment: Money
    get() {
      return residualDebtBeforeRepayment.minus(repayment)
    }

  class Calculator(
    /**
     * The initial dept at the start
     */
    var initialDept: Money,

    /**
     * The interest rate
     */
    var interestRate: @pct Double,

    /**
     * "Laufzeit in Jahren"
     */
    var termInYears: Int = 20
  ) {
    /**
     * Calculates the financing information for the given year index
     */
    fun calculate(yearIndex: YearIndex): FinancingInformation {
      val fixedRateMortgageCalculator = FixedRateMortgageCalculator(initialDept, interestRate, termInYears)
      val interest = fixedRateMortgageCalculator.yearlyInterest(yearIndex)
      val repayment = fixedRateMortgageCalculator.yearlyRepayment(yearIndex)

      return FinancingInformation(
        residualDebtBeforeRepayment = fixedRateMortgageCalculator.residualDebtAtStartOfYear(yearIndex),
        interestRate = interestRate,
        interest = interest,
        repayment = repayment,
      )
    }

    companion object {

      operator fun invoke(financesInformation: FinancesInformation): Calculator {
        return invoke(financesInformation.invest, financesInformation.financing)
      }

      operator fun invoke(
        /**
         * The net costs of the PV system (without optional costs)
         */
        netCosts: Money,
        financingType: FinancingType
      ): Calculator {
        return when (financingType) {
          FinancingType.EquityFinanced -> Calculator(Money.Zero, 1.0) //fallback interest rate that is > 0
          is FinancingType.Leveraged -> Calculator(netCosts, financingType.interestRate)
        }
      }
    }
  }
}
