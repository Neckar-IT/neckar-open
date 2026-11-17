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
import it.neckar.timeline.ValueTimeline
import it.neckar.financial.currency.Money
import it.neckar.financial.currency.euroNet
import it.neckar.open.serialization.roundTrip
import it.neckar.open.time.TimeInstantExample
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.minutes

/**
 *
 */
class PriceTimelineTest {
  @Test
  fun testAddOrReplace_Add() {
    val initial = PriceTimeline(
      entries = listOf(
        TimelineEntry(
          value = 199.99.euroNet,
          validFrom = TimeInstantExample._2026_04_24_03_10_00_000Z,
        ),
        TimelineEntry(
          value = 149.99.euroNet,
          validFrom = TimeInstantExample._2020_05_21_15_00_41_500Z,
        ),
      )
    )

    val entryBetween = TimelineEntry(
      value = 179.99.euroNet,
      validFrom = TimeInstantExample._2024_09_12_06_44_17_500Z,
    )
    val withAdded = initial.withAddedOrReplaced(entryBetween)

    assertThat(withAdded.entries).hasSize(3)
    assertThat(withAdded.entries.map { it.validFrom }).containsExactly(
      TimeInstantExample._2026_04_24_03_10_00_000Z,
      TimeInstantExample._2024_09_12_06_44_17_500Z,
      TimeInstantExample._2020_05_21_15_00_41_500Z,
    )
    assertThat(withAdded.priceAt(TimeInstantExample._2026_04_24_03_10_00_000Z)).isEqualTo(199.99.euroNet)
    assertThat(withAdded.priceAt(TimeInstantExample._2024_09_12_06_44_17_500Z.plus(5.minutes))).isEqualTo(179.99.euroNet)
    assertThat(withAdded.priceAt(TimeInstantExample._2020_05_21_15_00_41_500Z.plus(5.minutes))).isEqualTo(149.99.euroNet)
    assertThat(initial.entries).hasSize(2)

    val withReplaced = withAdded.withAddedOrReplaced(entryBetween.copy(value = 200.99.euroNet))
    assertThat(withReplaced.entries).hasSize(3)
    assertThat(withReplaced.priceAt(TimeInstantExample._2024_09_12_06_44_17_500Z.plus(5.minutes))).isEqualTo(200.99.euroNet)

  }

  /**
   * A price lookup that finds nothing names what the timeline covers: a timeline that was never
   * seeded reads differently from one whose entries all start later than the requested date.
   */
  @Test
  fun testPriceAtNamesWhatTheTimelineCovers() {
    val priceTimeline = PriceTimeline(
      entries = listOf(
        TimelineEntry(value = 149.99.euroNet, validFrom = TimeInstantExample._2020_05_21_15_00_41_500Z),
      )
    )

    assertFailure {
      priceTimeline.priceAt(TimeInstantExample._2020_05_21_15_00_41_500Z.minus(5.minutes))
    }
      .isInstanceOf(IllegalStateException::class)
      .all {
        messageContains("No price entry available")
        messageContains("the earliest entry is valid from ${TimeInstantExample._2020_05_21_15_00_41_500Z}")
      }

    assertFailure {
      ValueTimeline.empty<Money.Net>().priceAt(TimeInstantExample._2020_05_21_15_00_41_500Z)
    }
      .isInstanceOf(IllegalStateException::class)
      .messageContains("the timeline is empty")
  }

  @Test
  fun testFind() {
    val priceTimeline = PriceTimeline(
      entries = listOf(
        TimelineEntry(
          value = 199.99.euroNet,
          validFrom = TimeInstantExample._2026_04_24_03_10_00_000Z,
        ),
        TimelineEntry(
          value = 149.99.euroNet,
          validFrom = TimeInstantExample._2020_05_21_15_00_41_500Z,
        ),
      )
    )

    //Check for exact matches
    assertThat(priceTimeline.entryAt(TimeInstantExample._2026_04_24_03_10_00_000Z)?.value).isEqualTo(199.99.euroNet)
    assertThat(priceTimeline.entryAt(TimeInstantExample._2020_05_21_15_00_41_500Z)?.value).isEqualTo(149.99.euroNet)

    //Check for after matches
    assertThat(priceTimeline.entryAt(TimeInstantExample._2026_04_24_03_10_00_000Z.plus(5.minutes))?.value).isEqualTo(199.99.euroNet)
    assertThat(priceTimeline.entryAt(TimeInstantExample._2020_05_21_15_00_41_500Z.plus(5.minutes))?.value).isEqualTo(149.99.euroNet)

    //check before matches
    assertThat(priceTimeline.entryAt(TimeInstantExample._2020_05_21_15_00_41_500Z.minus(5.minutes))).isNull()
  }

  @Test
  fun testAdd() {
    var priceTimeline: PriceTimeline = PriceTimeline.empty()

    assertThat(priceTimeline.newestPriceOrNull()).isNull()

    priceTimeline += TimelineEntry(
      value = 1.99.euroNet,
      validFrom = TimeInstantExample._2020_05_21_15_00_41_500Z,
    )

    assertThat(priceTimeline.newestPriceOrNull()).isEqualTo(1.99.euroNet)

    assertFailure {
      //Duplicate entry!
      priceTimeline += TimelineEntry(
        value = 77.99.euroNet,
        validFrom = TimeInstantExample._2020_05_21_15_00_41_500Z,
      )
    }

    priceTimeline += TimelineEntry(
      value = 82.99.euroNet,
      validFrom = TimeInstantExample._2026_04_24_03_10_00_000Z,
    )

    assertThat(priceTimeline.newestPriceOrNull()).isEqualTo(82.99.euroNet)
  }

  @Test
  fun testCurrentPrice() {
    val priceTimeline = PriceTimeline(
      entries = listOf(
        TimelineEntry(
          value = 199.99.euroNet,
          validFrom = TimeInstantExample._2026_04_24_03_10_00_000Z,
        ),
        TimelineEntry(
          value = 149.99.euroNet,
          validFrom = TimeInstantExample._2020_05_21_15_00_41_500Z,
        ),
      )
    )

    assertThat(priceTimeline.newestPrice()).isEqualTo(199.99.euroNet)
  }

  @Test
  fun testValidation() {
    val younger = TimelineEntry(
      value = 199.99.euroNet,
      validFrom = TimeInstantExample._2026_04_24_03_10_00_000Z,
    )
    val older = TimelineEntry(
      value = 149.99.euroNet,
      validFrom = TimeInstantExample._2020_05_21_15_00_41_500Z,
    )

    //This works
    assertThat(
      PriceTimeline(
        entries = listOf(
          younger,
          older,
        )
      )
    ).isNotNull()

    assertFailure {
      PriceTimeline(
        entries = listOf(
          older,
          younger,
        )
      )
    }
  }

  @Test
  fun testJsonEmpty() {
    roundTrip(PriceTimeline(entries = emptyList())) {
      //language=JSON
      """
        {
          "entries" : [ ]
        }
      """.trimIndent()
    }
  }

  @Test
  fun testJsonRealistic() {
    roundTrip(
      PriceTimeline(
        entries = listOf(
          TimelineEntry(
            value = 199.99.euroNet,
            validFrom = TimeInstantExample._2026_04_24_03_10_00_000Z,
          ),
          TimelineEntry(
            value = 149.99.euroNet,
            validFrom = TimeInstantExample._2020_05_21_15_00_41_500Z,
          ),
        )
      )
    ) {
      //language=JSON
      """
        {
          "entries" : [ {
            "value" : 19999,
            "validFrom" : "2026-04-24T03:10:00Z"
          }, {
            "value" : 14999,
            "validFrom" : "2020-05-21T15:00:41.500Z"
          } ]
        }
      """.trimIndent()
    }
  }
}
