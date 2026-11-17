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

import assertk.*
import assertk.assertions.*
import it.neckar.open.serialization.roundTrip
import org.junit.jupiter.api.Test

class PriceWithProfitTest {
  @Test
  fun `serializes the two prices only, never the derived figures`() {
    roundTrip(PriceWithProfit(50.0, 68.0)) {
      //language=JSON
      """
        {
          "inputPrice": 5000,
          "sellingPrice": 6800
        }
      """.trimIndent()
    }
  }

  @Test
  fun `profit is the difference between selling and input price`() {
    assertThat(PriceWithProfit(50.0, 68.0).profit).isEqualTo(18.0.euroNet)
  }

  @Test
  fun `surcharge builds the selling price from the input price and the rate`() {
    // The example the TypeScript surcharge display is pinned against: 50,00 € at 36 % sells for 68,00 €.
    assertThat(PriceWithProfit.surcharge(50.0.euroNet, 0.36)).isEqualTo(PriceWithProfit(50.0, 68.0))
  }

  @Test
  fun `profit is negative when selling below the input price`() {
    assertThat(PriceWithProfit(50.0, 40.0).profit).isEqualTo((-10.0).euroNet)
  }

  @Test
  fun `profit is zero when selling for the input price`() {
    assertThat(PriceWithProfit.noProfit(50.0.euroNet).profit).isEqualTo(Money.Net.Zero)
  }
}
