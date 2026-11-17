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
import it.neckar.financial.currency.QuantityUnit
import it.neckar.financial.currency.PriceWithProfit
import it.neckar.financial.currency.euroNet
import it.neckar.open.serialization.roundTrip
import it.neckar.price.ArticleConditions
import it.neckar.price.ArticleConditionsExample
import it.neckar.price.ArticleConditionsWithAccessory
import it.neckar.price.ArticleConditionsWithInstallationTime
import it.neckar.price.BareArticleConditions
import it.neckar.price.FullArticleConditions
import it.neckar.price.rest.messages.ArticleConditionsPriceSummaryRequestExample
import it.neckar.price.rest.messages.EnteredInstallationTime
import it.neckar.price.rest.messages.EnteredPriceWithProfit
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.minutes

class ArticleConditionsPriceSummaryTest {
  private val hourlyRate = ArticleConditionsPriceSummaryRequestExample.hourlyRate

  @Test
  fun `part, accessory and labour add up to the customer price`() {
    val summary = ArticleConditionsPriceSummary.calculate(ArticleConditionsPriceSummaryRequestExample.full, hourlyRate)

    assertThat(summary.part).isEqualTo(
      ArticleConditionsPriceRow(inputPrice = 20.00.euroNet, sellingPrice = 25.00.euroNet, surcharge = 5.00.euroNet, surchargeRate = 0.25)
    )
    assertThat(summary.accessory).isEqualTo(
      ArticleConditionsPriceRow(inputPrice = 3.00.euroNet, sellingPrice = 4.50.euroNet, surcharge = 1.50.euroNet, surchargeRate = 0.5)
    )
    assertThat(summary.labour).isEqualTo(ArticleConditionsLabourPriceRow(installationTime = 30.minutes, hourlyRate = hourlyRate, sellingPrice = 37.50.euroNet))
    assertThat(summary.customerPrice).isEqualTo(67.00.euroNet)
  }

  @Test
  fun `an article without accessory and installation time has the part row only`() {
    val summary = ArticleConditionsPriceSummary.calculate(ArticleConditionsPriceSummaryRequestExample.partOnly, hourlyRate = null)

    assertThat(summary.accessory).isNull()
    assertThat(summary.labour).isNull()
    assertThat(summary.customerPrice).isEqualTo(18.00.euroNet)
  }

  @Test
  fun `an empty price field leaves its figures and the customer price empty`() {
    val request = ArticleConditionsPriceSummaryRequestExample.full.copy(
      accessory = EnteredPriceWithProfit(inputPrice = 3.00.euroNet, sellingPrice = null),
    )

    val summary = ArticleConditionsPriceSummary.calculate(request, hourlyRate)

    assertThat(summary.accessory).isEqualTo(
      ArticleConditionsPriceRow(inputPrice = 3.00.euroNet, sellingPrice = null, surcharge = null, surchargeRate = null)
    )
    assertThat(summary.part.surcharge).isEqualTo(5.00.euroNet)
    assertThat(summary.customerPrice).isNull()
  }

  @Test
  fun `a purchase price of 0 euro has a surcharge but no rate`() {
    val request = ArticleConditionsPriceSummaryRequestExample.partOnly.copy(
      part = EnteredPriceWithProfit(inputPrice = 0.00.euroNet, sellingPrice = 18.00.euroNet),
    )

    val part = ArticleConditionsPriceSummary.calculate(request, hourlyRate = null).part

    assertThat(part.surcharge).isEqualTo(18.00.euroNet)
    assertThat(part.surchargeRate).isNull()
  }

  @Test
  fun `a checked but empty installation time keeps the labour row without a price`() {
    val request = ArticleConditionsPriceSummaryRequestExample.full.copy(installation = EnteredInstallationTime(installationTime = null))

    val summary = ArticleConditionsPriceSummary.calculate(request, hourlyRate)

    assertThat(summary.labour).isEqualTo(ArticleConditionsLabourPriceRow(installationTime = null, hourlyRate = hourlyRate, sellingPrice = null))
    assertThat(summary.customerPrice).isNull()
  }

  @Test
  fun `an installation time without an hourly rate is rejected`() {
    assertFailure {
      ArticleConditionsPriceSummary.calculate(ArticleConditionsPriceSummaryRequestExample.full, hourlyRate = null)
    }.isInstanceOf<IllegalArgumentException>()
  }

  @Test
  fun `the unit is carried through`() {
    val request = ArticleConditionsPriceSummaryRequestExample.partOnly.copy(unit = QuantityUnit("Meter"))

    assertThat(ArticleConditionsPriceSummary.calculate(request, hourlyRate = null).unit).isEqualTo(QuantityUnit("Meter"))
  }

  @Test
  fun `stored full conditions yield the summary of the equivalent request`() {
    val conditions = FullArticleConditions(
      part = PriceWithProfit(inputPrice = 20.00.euroNet, sellingPrice = 25.00.euroNet),
      accessory = PriceWithProfit(inputPrice = 3.00.euroNet, sellingPrice = 4.50.euroNet),
      installationTime = 30.minutes,
    )

    assertThat(ArticleConditionsPriceSummary.calculate(conditions, hourlyRate))
      .isEqualTo(ArticleConditionsPriceSummary.calculate(ArticleConditionsPriceSummaryRequestExample.full, hourlyRate))
  }

  @Test
  fun `stored bare conditions yield the summary of the equivalent request`() {
    val conditions = BareArticleConditions(part = PriceWithProfit(inputPrice = 12.00.euroNet, sellingPrice = 18.00.euroNet))

    assertThat(ArticleConditionsPriceSummary.calculate(conditions, hourlyRate = null))
      .isEqualTo(ArticleConditionsPriceSummary.calculate(ArticleConditionsPriceSummaryRequestExample.partOnly, hourlyRate = null))
  }

  @Test
  fun `stored conditions with accessory price the accessory and no labour`() {
    val conditions = ArticleConditionsWithAccessory(
      part = PriceWithProfit(inputPrice = 20.00.euroNet, sellingPrice = 25.00.euroNet),
      unit = QuantityUnit("Meter"),
      accessory = PriceWithProfit(inputPrice = 3.00.euroNet, sellingPrice = 4.50.euroNet),
    )

    val summary = ArticleConditionsPriceSummary.calculate(conditions, hourlyRate = null)

    assertThat(summary.unit).isEqualTo(QuantityUnit("Meter"))
    assertThat(summary.accessory?.sellingPrice).isEqualTo(4.50.euroNet)
    assertThat(summary.labour).isNull()
    assertThat(summary.customerPrice).isEqualTo(29.50.euroNet)
  }

  @Test
  fun `stored conditions with installation time price the labour and no accessory`() {
    val conditions = ArticleConditionsWithInstallationTime(
      part = PriceWithProfit(inputPrice = 20.00.euroNet, sellingPrice = 25.00.euroNet),
      installationTime = 30.minutes,
    )

    val summary = ArticleConditionsPriceSummary.calculate(conditions, hourlyRate)

    assertThat(summary.accessory).isNull()
    assertThat(summary.labour).isEqualTo(ArticleConditionsLabourPriceRow(installationTime = 30.minutes, hourlyRate = hourlyRate, sellingPrice = 37.50.euroNet))
    assertThat(summary.customerPrice).isEqualTo(62.50.euroNet)
  }

  @Test
  fun `a stored empty accessory and installation time yield no rows`() {
    val summary = ArticleConditionsPriceSummary.calculate(ArticleConditionsExample.withoutAccessoryAndInstallationTime, hourlyRate = null)

    assertThat(summary.accessory).isNull()
    assertThat(summary.labour).isNull()
    assertThat(summary.customerPrice).isEqualTo(168.00.euroNet)
  }

  @Test
  fun `a stored installation time without an hourly rate is rejected`() {
    assertFailure {
      ArticleConditionsPriceSummary.calculate(ArticleConditionsExample.withAccessoryAndInstallationTime, hourlyRate = null)
    }.isInstanceOf<IllegalArgumentException>()
  }

  @Test
  fun `a conditions implementation outside the four shipped ones is rejected`() {
    val productConditions = object : ArticleConditions {
      override val part: PriceWithProfit = PriceWithProfit(inputPrice = 1.00.euroNet, sellingPrice = 2.00.euroNet)
      override val unit: QuantityUnit? = null
    }

    assertFailure {
      ArticleConditionsPriceSummary.calculate(productConditions, hourlyRate)
    }.isInstanceOf<IllegalArgumentException>()
  }

  @Test
  fun `serializes amounts in cents and the installation time as an ISO duration`() {
    roundTrip(ArticleConditionsPriceSummary.calculate(ArticleConditionsPriceSummaryRequestExample.full, hourlyRate)) {
      //language=JSON
      """
        {
          "unit": null,
          "part": { "inputPrice": 2000, "sellingPrice": 2500, "surcharge": 500, "surchargeRate": 0.25 },
          "accessory": { "inputPrice": 300, "sellingPrice": 450, "surcharge": 150, "surchargeRate": 0.5 },
          "labour": { "installationTime": "PT30M", "hourlyRate": 7500, "sellingPrice": 3750 },
          "customerPrice": 6700
        }
      """.trimIndent()
    }
  }
}
