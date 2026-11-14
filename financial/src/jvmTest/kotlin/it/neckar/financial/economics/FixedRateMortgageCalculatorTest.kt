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

import assertk.*
import assertk.assertions.*
import it.neckar.financial.currency.Money
import it.neckar.open.kotlin.lang.percent
import org.junit.jupiter.api.Test

class FixedRateMortgageCalculatorTest {
  @Test
  fun testCalcVeryFewPercent() {
    val calculator = FixedRateMortgageCalculator(Money.euros(10_000.0), 0.1.percent, 10)

    assertThat(calculator.yearlyPayment().euros).isEqualTo(1005.46)
    assertThat(calculator.yearlyPayment().timesAndRound(10.0).euros).isEqualTo(10_054.60)
  }

  @Test
  fun testCalc() {
    val calculator = FixedRateMortgageCalculator(Money.euros(10_000.0), 3.percent, 10)

    assertThat(calculator.yearlyPayment().euros).isEqualTo(1172.29)
    assertThat(calculator.yearlyPayment().timesAndRound(10.0).euros).isEqualTo(11_722.9)

    assertThat(calculator.monthlyPayment().euros).isEqualTo(97.69)
  }

  @Test
  fun testExample() {
    val calculator = FixedRateMortgageCalculator(Money.euros(1_200_000.0), 5.percent, 3)

    //Example from https://welt-der-bwl.de/Annuit%C3%A4tendarlehen
    assertThat(calculator.yearlyPayment().euros).isEqualTo(440_650.28)

    assertThat(calculator.yearlyRepayment(YearIndex.base).euros).isEqualTo(380_650.28)
    assertThat(calculator.yearlyRepayment(YearIndex.second).euros).isEqualTo(399_682.79)
    assertThat(calculator.yearlyRepayment(YearIndex.third).euros).isEqualTo(419_666.93)

    assertThat(calculator.yearlyInterest(YearIndex.base).euros).isEqualTo(60_000.0)
    assertThat(calculator.yearlyInterest(YearIndex.second).euros).isEqualTo(40_967.49)
    assertThat(calculator.yearlyInterest(YearIndex.third).euros).isEqualTo(20_983.35)

    assertThat(calculator.residualDebtAtStartOfYear(YearIndex.base).euros).isEqualTo(1_200_000.0)
    assertThat(calculator.residualDebtAtStartOfYear(YearIndex.second).euros).isEqualTo(819_349.72)
    assertThat(calculator.residualDebtAtStartOfYear(YearIndex.third).euros).isEqualTo(419_666.93)

    assertThat(calculator.residualDebtAfterRepayment(YearIndex.base).euros).isEqualTo(819_349.72)
    assertThat(calculator.residualDebtAfterRepayment(YearIndex.second).euros).isEqualTo(419_666.93)
    assertThat(calculator.residualDebtAfterRepayment(YearIndex.third).euros).isEqualTo(0.0)
  }
}
