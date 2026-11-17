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
import it.neckar.price.BareArticleConditions
import it.neckar.price.FullArticleConditions
import it.neckar.price.ArticleConditionsExample
import it.neckar.price.ArticleConditionsTimeline
import it.neckar.timeline.TimelineEntry
import it.neckar.financial.currency.QuantityUnit
import it.neckar.financial.currency.PriceWithProfit
import it.neckar.financial.currency.euroNet
import it.neckar.open.serialization.roundTrip
import it.neckar.open.time.TimeInstantExample
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.days

/**
 * Tests the REST projection of an [ArticleConditionsTimeline]: which entry the detail view shows
 * as current and which one as upcoming, relative to a server-defined reference time.
 */
class ArticleConditionsTimelineSummaryTest {
  private val now = TimeInstantExample._2026_04_24_03_10_00_000Z

  private val current = ArticleConditionsExample.withoutAccessoryAndInstallationTime
  private val upcoming = ArticleConditionsExample.withAccessoryAndInstallationTime

  private val timeline: ArticleConditionsTimeline<FullArticleConditions> = ArticleConditionsTimeline(
    entries = listOf(
      TimelineEntry(value = upcoming, validFrom = now + 10.days),
      TimelineEntry(value = current, validFrom = now - 10.days),
    )
  )

  @Test
  fun `projects the entry in effect and the next upcoming one`() {
    val summary = timeline.toSummary(now)

    assertThat(summary.currentEntry?.conditions).isEqualTo(current)
    assertThat(summary.currentEntry?.validFrom).isEqualTo(now - 10.days)
    assertThat(summary.nextFutureEntry?.conditions).isEqualTo(upcoming)
    assertThat(summary.nextFutureEntry?.validFrom).isEqualTo(now + 10.days)
  }

  @Test
  fun `projects an entry that starts exactly now as current`() {
    val startingNow = ArticleConditionsTimeline.single(current, now)

    val summary = startingNow.toSummary(now)

    assertThat(summary.currentEntry?.conditions).isEqualTo(current)
    assertThat(summary.nextFutureEntry).isNull()
  }

  @Test
  fun `projects an empty timeline as an empty summary`() {
    val summary = ArticleConditionsTimeline.empty<FullArticleConditions>().toSummary(now)

    assertThat(summary).isEqualTo(ArticleConditionsTimelineSummary.empty<FullArticleConditions>())
  }

  @Test
  fun `serializes an entry view as conditions and validFrom`() {
    val view = TimelineEntry(
      value = BareArticleConditions(
        part = PriceWithProfit(inputPrice = 1.20.euroNet, sellingPrice = 2.50.euroNet),
        unit = QuantityUnit("Meter"),
      ),
      validFrom = TimeInstantExample._2020_05_21_15_00_41_500Z,
    ).toView()

    roundTrip(view) {
      //language=JSON
      """
        {
          "conditions": {
            "part": {
              "inputPrice": 120,
              "sellingPrice": 250
            },
            "unit": "Meter"
          },
          "validFrom": "2020-05-21T15:00:41.500Z"
        }
      """.trimIndent()
    }
  }
}
