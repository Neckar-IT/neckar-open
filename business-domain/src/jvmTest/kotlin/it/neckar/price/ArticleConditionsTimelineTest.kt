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

package it.neckar.price

import assertk.*
import assertk.assertions.*
import it.neckar.timeline.TimelineEntry
import it.neckar.financial.currency.QuantityUnit
import it.neckar.financial.currency.PriceWithProfit
import it.neckar.financial.currency.euroNet
import it.neckar.open.serialization.roundTrip
import it.neckar.open.time.TimeInstantExample
import org.junit.jupiter.api.Test
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

/**
 * Tests the [ArticleConditions] instantiation of the value timeline: all commercial values of a
 * catalog position move forward together as one entry.
 *
 * The timeline is typed on one implementation — the shape belongs to the article type, so a cable's
 * timeline holds [FullArticleConditions] in every entry, whether or not an individual entry fills
 * the accessory and the installation time.
 */
class ArticleConditionsTimelineTest {
  private val newerConditions: FullArticleConditions = FullArticleConditions(
    part = PriceWithProfit(inputPrice = 130.00.euroNet, sellingPrice = 182.00.euroNet),
    unit = QuantityUnit("Meter"),
    accessory = PriceWithProfit(inputPrice = 15.00.euroNet, sellingPrice = 21.00.euroNet),
    installationTime = 1.5.hours,
  )
  private val olderConditions: FullArticleConditions = FullArticleConditions(
    part = PriceWithProfit(inputPrice = 120.00.euroNet, sellingPrice = 168.00.euroNet),
    accessory = null,
    installationTime = null,
  )

  private val timeline: ArticleConditionsTimeline<FullArticleConditions> = ArticleConditionsTimeline(
    entries = listOf(
      TimelineEntry(value = newerConditions, validFrom = TimeInstantExample._2026_04_24_03_10_00_000Z),
      TimelineEntry(value = olderConditions, validFrom = TimeInstantExample._2020_05_21_15_00_41_500Z),
    )
  )

  @Test
  fun `returns the conditions valid at a date`() {
    assertThat(timeline.conditionsAt(TimeInstantExample._2026_04_24_03_10_00_000Z)).isEqualTo(newerConditions)
    assertThat(timeline.conditionsAt(TimeInstantExample._2020_05_21_15_00_41_500Z.plus(5.minutes))).isEqualTo(olderConditions)
    assertThat(timeline.conditionsAtOrNull(TimeInstantExample._2020_05_21_15_00_41_500Z.minus(5.minutes))).isNull()
  }

  @Test
  fun `throws when no conditions are valid at a date`() {
    assertFailure {
      timeline.conditionsAt(TimeInstantExample._2020_05_21_15_00_41_500Z.minus(5.minutes))
    }
      .isInstanceOf(IllegalStateException::class)
      .all {
        messageContains("No conditions entry available")
        messageContains("the earliest entry is valid from ${TimeInstantExample._2020_05_21_15_00_41_500Z}")
      }
  }

  @Test
  fun `names the empty timeline as the reason no conditions were found`() {
    assertFailure {
      ArticleConditionsTimeline.empty<FullArticleConditions>().conditionsAt(TimeInstantExample._2026_04_24_03_10_00_000Z)
    }
      .isInstanceOf(IllegalStateException::class)
      .messageContains("the timeline is empty")
  }

  @Test
  fun `returns the newest conditions`() {
    assertThat(timeline.newestConditions()).isEqualTo(newerConditions)
    assertThat(ArticleConditionsTimeline.empty<FullArticleConditions>().newestConditionsOrNull()).isNull()
  }

  @Test
  fun `keeps the entries sorted newest first`() {
    assertThat(timeline.entries.map { it.value }).containsExactly(newerConditions, olderConditions)
  }

  @Test
  fun `carries all values forward as one entry`() {
    val corrected = timeline.withAdded(
      TimelineEntry(
        value = newerConditions.copy(installationTime = 2.hours),
        validFrom = TimeInstantExample._2026_09_30_17_38_12_123Z,
      )
    )

    val current = corrected.newestConditions()
    assertThat(current.installationTime).isEqualTo(2.hours)
    //the untouched values move forward with the entry
    assertThat(current.part).isEqualTo(newerConditions.part)
    assertThat(current.unit).isEqualTo(newerConditions.unit)
    assertThat(current.accessory).isEqualTo(newerConditions.accessory)
  }

  @Test
  fun `leaves the optional parts of an article that has none absent`() {
    assertThat(olderConditions.accessory).isNull()
    assertThat(olderConditions.installationTime).isNull()
  }

  @Test
  fun `fails fast on a negative part purchase price`() {
    assertFailure {
      BareArticleConditions(part = PriceWithProfit(inputPrice = (-1.00).euroNet, sellingPrice = 61.60.euroNet))
    }
      .isInstanceOf(IllegalArgumentException::class)
      .messageContains("part purchase price must be >= 0")
  }

  @Test
  fun `fails fast on a negative part selling price`() {
    assertFailure {
      BareArticleConditions(part = PriceWithProfit(inputPrice = 44.00.euroNet, sellingPrice = (-1.00).euroNet))
    }
      .isInstanceOf(IllegalArgumentException::class)
      .messageContains("part selling price must be >= 0")
  }

  @Test
  fun `rejects an explicitly set Piece unit - null is the only encoding for it`() {
    assertFailure {
      BareArticleConditions(part = olderConditions.part, unit = QuantityUnit.Piece)
    }
      .isInstanceOf(IllegalArgumentException::class)
      .messageContains("must be encoded as null")
  }

  @Test
  fun `fails fast on a non positive installation time`() {
    assertFailure {
      ArticleConditionsWithInstallationTime(part = olderConditions.part, installationTime = Duration.ZERO)
    }
      .isInstanceOf(IllegalArgumentException::class)
      .messageContains("installationTime must be > 0")
  }

  /**
   * The wire form carries no discriminator: the timeline is typed on one implementation, so the
   * reader already knows the shape. An entry of an article type that has all parts writes them all,
   * with `null` where this particular article has none.
   */
  @Test
  fun `serializes an entry of a full article type`() {
    roundTrip(
      TimelineEntry(value = olderConditions, validFrom = TimeInstantExample._2020_05_21_15_00_41_500Z),
    ) {
      //language=JSON
      """
        {
          "value": {
            "part": {
              "inputPrice": 12000,
              "sellingPrice": 16800
            },
            "unit": null,
            "accessory": null,
            "installationTime": null
          },
          "validFrom": "2020-05-21T15:00:41.500Z"
        }
      """.trimIndent()
    }
  }

  /**
   * A shape without the optional parts writes no key for them at all — they are not properties of
   * that article type, so there is nothing that could be `null`.
   */
  @Test
  fun `serializes an entry of a bare article type without the optional keys`() {
    roundTrip(
      TimelineEntry(
        value = BareArticleConditions(part = PriceWithProfit(inputPrice = 12.00.euroNet, sellingPrice = 19.90.euroNet)),
        validFrom = TimeInstantExample._2020_05_21_15_00_41_500Z,
      ),
    ) {
      //language=JSON
      """
        {
          "value": {
            "part": {
              "inputPrice": 1200,
              "sellingPrice": 1990
            },
            "unit": null
          },
          "validFrom": "2020-05-21T15:00:41.500Z"
        }
      """.trimIndent()
    }
  }
}
