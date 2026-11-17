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
import kotlin.time.Duration.Companion.minutes

class PricePerHourWithProfitTest {
  @Test
  fun `times yields a price with a real purchase price, not zero input`() {
    val rate = PricePerHourWithProfit(purchasePricePerHour = 48.00.euroNet, sellingPricePerHour = 95.00.euroNet)

    val price: PriceWithProfit = rate * 2.hours

    assertThat(price.inputPrice).isEqualTo(96.00.euroNet)
    assertThat(price.sellingPrice).isEqualTo(190.00.euroNet)
    assertThat(price.profit).isEqualTo(94.00.euroNet)
  }

  @Test
  fun `contribution margin per hour is selling minus purchase`() {
    val rate = PricePerHourWithProfit(purchasePricePerHour = 32.00.euroNet, sellingPricePerHour = 75.00.euroNet)

    assertThat(rate.purchasePricePerHour).isEqualTo(32.00.euroNet)
    assertThat(rate.sellingPricePerHour).isEqualTo(75.00.euroNet)
    assertThat(rate.contributionMarginPerHour).isEqualTo(43.00.euroNet)
  }

  @Test
  fun `times handles fractional hours`() {
    val rate = PricePerHourWithProfit(purchasePricePerHour = 34.00.euroNet, sellingPricePerHour = 82.00.euroNet)

    val price: PriceWithProfit = rate * (2.hours + 30.minutes)

    assertThat(price.inputPrice).isEqualTo(85.00.euroNet)
    assertThat(price.sellingPrice).isEqualTo(205.00.euroNet)
    assertThat(price.profit).isEqualTo(120.00.euroNet)
  }

  @Test
  fun `duration times rate is symmetric`() {
    val rate = PricePerHourWithProfit(purchasePricePerHour = 48.00.euroNet, sellingPricePerHour = 95.00.euroNet)

    assertThat(2.hours * rate).isEqualTo(rate * 2.hours)
  }
}
