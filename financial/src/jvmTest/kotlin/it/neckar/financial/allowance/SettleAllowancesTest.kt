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
package it.neckar.financial.allowance

import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import org.junit.jupiter.api.Test

class SettleAllowancesTest {

  @Test
  fun `the delta is ordered minus included, positive, zero or negative`() {
    val settled = settleAllowances(
      ordered = listOf(OrderedQuantity("outlet", 5), OrderedQuantity("switch", 2), OrderedQuantity("lamp", 1)),
      allowances = listOf(
        QuantityAllowance("outlet", includedQuantity = 3, scope = AllowanceScope.PerSection),
        QuantityAllowance("switch", includedQuantity = 2, scope = AllowanceScope.PerSection),
        QuantityAllowance("lamp", includedQuantity = 3, scope = AllowanceScope.PerSection),
      ),
    )

    assertThat(settled.map { it.delta }).containsExactly(2, 0, -2)
  }

  @Test
  fun `a key only the allowance names is a credit of the whole allowance`() {
    val settled = settleAllowances(
      ordered = listOf(OrderedQuantity("outlet", 1)),
      allowances = listOf(QuantityAllowance("switch", includedQuantity = 2, scope = AllowanceScope.PerSection)),
    )

    assertThat(settled).containsExactly(SettledQuantity("outlet", ordered = 1, included = 0), SettledQuantity("switch", ordered = 0, included = 2))
  }

  @Test
  fun `quantities of several sections settle an across-sections allowance once`() {
    val groundFloor = OrderedQuantity("stair switch", 1)
    val firstFloor = OrderedQuantity("stair switch", 1)

    val settled = settleAllowances(
      ordered = listOf(groundFloor, firstFloor),
      allowances = listOf(QuantityAllowance("stair switch", includedQuantity = 2, scope = AllowanceScope.AcrossSections)),
    )

    assertThat(settled).containsExactly(SettledQuantity("stair switch", ordered = 2, included = 2))
    assertThat(settled.single().delta).isEqualTo(0)
  }

  @Test
  fun `a negative ordered or included quantity is refused`() {
    assertFailure { OrderedQuantity("outlet", -1) }.isInstanceOf<IllegalArgumentException>()
    assertFailure { QuantityAllowance("outlet", includedQuantity = -1, scope = AllowanceScope.PerSection) }.isInstanceOf<IllegalArgumentException>()
  }

  @Test
  fun `allowances of two scopes are refused`() {
    assertFailure {
      settleAllowances(
        ordered = emptyList(),
        allowances = listOf(
          QuantityAllowance("outlet", includedQuantity = 1, scope = AllowanceScope.PerSection),
          QuantityAllowance("switch", includedQuantity = 2, scope = AllowanceScope.AcrossSections),
        ),
      )
    }.isInstanceOf<IllegalArgumentException>()
  }

  @Test
  fun `two allowances for one key are refused`() {
    assertFailure {
      settleAllowances(
        ordered = emptyList(),
        allowances = listOf(
          QuantityAllowance("outlet", includedQuantity = 1, scope = AllowanceScope.PerSection),
          QuantityAllowance("outlet", includedQuantity = 2, scope = AllowanceScope.PerSection),
        ),
      )
    }.isInstanceOf<IllegalArgumentException>()
  }
}
