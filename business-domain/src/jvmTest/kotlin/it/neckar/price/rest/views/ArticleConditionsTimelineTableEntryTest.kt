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
@file:Suppress("RETURN_VALUE_NOT_USED") // Test scope
package it.neckar.price.rest.views

import assertk.*
import assertk.assertions.*
import it.neckar.financial.currency.PricePerHour
import it.neckar.financial.currency.QuantityUnit
import it.neckar.financial.currency.PriceWithProfit
import it.neckar.financial.currency.euroNet
import it.neckar.open.serialization.roundTrip
import it.neckar.open.time.TimeInstantExample
import it.neckar.price.ArticleConditionsExample
import it.neckar.price.ArticleConditionsWithAccessory
import it.neckar.timeline.TimelineEntry
import org.junit.jupiter.api.Test

class ArticleConditionsTimelineTableEntryTest {
  private val hourlyRate: PricePerHour = PricePerHour.euros(60.0)

  @Test
  fun `a timeline entry and its view yield the same row`() {
    val entry = TimelineEntry(value = ArticleConditionsExample.withAccessoryAndInstallationTime, validFrom = TimeInstantExample._2026_04_24_03_10_00_000Z)

    assertThat(entry.toView().toTableEntry(hourlyRate)).isEqualTo(entry.toTableEntry(hourlyRate))
  }

  @Test
  fun `prices the row with the calculation of the form companion`() {
    val row = TimelineEntry(value = ArticleConditionsExample.withAccessoryAndInstallationTime, validFrom = TimeInstantExample._2026_04_24_03_10_00_000Z)
      .toTableEntry(hourlyRate)

    assertThat(row.priceSummary).isEqualTo(ArticleConditionsPriceSummary.calculate(ArticleConditionsExample.withAccessoryAndInstallationTime, hourlyRate))
    // 2,50 € part + 0,21 € accessory + 8,5 h × 60,00 € labour
    assertThat(row.priceSummary.customerPrice).isEqualTo(512.71.euroNet)
  }

  @Test
  fun `serializes the conditions, validFrom and the price overview`() {
    val row = TimelineEntry(
      value = ArticleConditionsWithAccessory(
        part = PriceWithProfit(inputPrice = 1.20.euroNet, sellingPrice = 2.50.euroNet),
        unit = QuantityUnit("Meter"),
        accessory = null,
      ),
      validFrom = TimeInstantExample._2020_05_21_15_00_41_500Z,
    ).toTableEntry(hourlyRate = null)

    roundTrip(row) {
      //language=JSON
      """
        {
          "conditions": {
            "part": {
              "inputPrice": 120,
              "sellingPrice": 250
            },
            "unit": "Meter",
            "accessory": null
          },
          "validFrom": "2020-05-21T15:00:41.500Z",
          "priceSummary": {
            "unit": "Meter",
            "part": { "inputPrice": 120, "sellingPrice": 250, "surcharge": 130, "surchargeRate": 1.0833333333333333 },
            "accessory": null,
            "labour": null,
            "customerPrice": 250
          }
        }
      """.trimIndent()
    }
  }
}
