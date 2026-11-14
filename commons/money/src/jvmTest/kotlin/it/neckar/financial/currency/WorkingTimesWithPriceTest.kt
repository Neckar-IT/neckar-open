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

import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.messageContains
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.hours

class WorkingTimesWithPriceTest {
  private val rate = PricePerHour(75.00.euroNet)

  @Test
  fun `sum adds up the working times of entries sharing one hourly rate`() {
    val sum = listOf(
      WorkingTimesWithPrice(2.hours, rate),
      WorkingTimesWithPrice(3.hours, rate),
    ).sum()

    assertThat(sum).isEqualTo(WorkingTimesWithPrice(5.hours, rate))
    assertThat(sum.priceWithProfit.sellingPrice).isEqualTo(375.00.euroNet)
  }

  @Test
  fun `sum of an empty list is zero`() {
    assertThat(emptyList<WorkingTimesWithPrice>().sum()).isEqualTo(WorkingTimesWithPrice.Zero)
  }

  @Test
  fun `adding zero adopts the hourly rate of the other side`() {
    val entry = WorkingTimesWithPrice(2.hours, rate)

    assertThat(WorkingTimesWithPrice.Zero + entry).isEqualTo(entry)
    assertThat(entry + WorkingTimesWithPrice.Zero).isEqualTo(entry)
  }

  @Test
  fun `subtracting zero adopts the hourly rate of the other side`() {
    val entry = WorkingTimesWithPrice(2.hours, rate)

    assertThat(entry - WorkingTimesWithPrice.Zero).isEqualTo(entry)
    assertThat(WorkingTimesWithPrice.Zero - entry).isEqualTo(-entry)
  }

  @Test
  fun `adding entries with different hourly rates is still rejected`() {
    assertFailure {
      WorkingTimesWithPrice(2.hours, rate) + WorkingTimesWithPrice(3.hours, PricePerHour(90.00.euroNet))
    }
      .isInstanceOf(IllegalArgumentException::class)
      .messageContains("need to be identical")
  }

  @Test
  fun `an hour at a zero rate is not the neutral element`() {
    // Only Zero itself carries no rate. One hour at 0,00 EUR/h is a real entry and must not adopt a foreign rate.
    assertFailure {
      WorkingTimesWithPrice(1.hours, PricePerHour.zero) + WorkingTimesWithPrice(3.hours, rate)
    }
      .isInstanceOf(IllegalArgumentException::class)
      .messageContains("need to be identical")
  }
}
