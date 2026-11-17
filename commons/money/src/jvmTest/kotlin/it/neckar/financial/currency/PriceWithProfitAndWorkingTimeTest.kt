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

import assertk.assertThat
import assertk.assertions.isEqualTo
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.hours

class PriceWithProfitAndWorkingTimeTest {
  @Test
  fun `sum adds up material prices and working times`() {
    val rate = PricePerHour(75.00.euroNet)

    val sum = listOf(
      PriceWithProfitAndWorkingTime(PriceWithProfit(50.0, 68.0), WorkingTimesWithPrice(2.hours, rate)),
      PriceWithProfitAndWorkingTime(PriceWithProfit(30.0, 40.0), WorkingTimesWithPrice(3.hours, rate)),
    ).sum()

    assertThat(sum.materialPrice).isEqualTo(PriceWithProfit(80.0, 108.0))
    assertThat(sum.workingTimes).isEqualTo(WorkingTimesWithPrice(5.hours, rate))
    assertThat(sum.sellingPrice).isEqualTo(483.00.euroNet)
  }

  @Test
  fun `sum of an empty list is zero`() {
    assertThat(emptyList<PriceWithProfitAndWorkingTime>().sum()).isEqualTo(PriceWithProfitAndWorkingTime.Zero)
  }
}
