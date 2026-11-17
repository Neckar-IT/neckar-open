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

package it.neckar.timeline

import assertk.*
import assertk.assertions.*
import it.neckar.open.serialization.roundTrip
import it.neckar.open.time.TimeInstantExample
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.Test
import kotlin.jvm.JvmInline
import kotlin.time.Duration.Companion.minutes

/**
 * A new timeline is a typealias — no timeline or entry implementation needed.
 */
private typealias TaxRateTimeline = ValueTimeline<TaxRate>

@Serializable
@JvmInline
value class TaxRate(val percent: Int)

/**
 * Tests the generic [ValueTimeline] with a non-Money value type.
 */
class ValueTimelineTest {

  private val newerEntry = TimelineEntry(
    value = TaxRate(19),
    validFrom = TimeInstantExample._2026_04_24_03_10_00_000Z,
  )
  private val olderEntry = TimelineEntry(
    value = TaxRate(16),
    validFrom = TimeInstantExample._2020_05_21_15_00_41_500Z,
  )
  private val timeline: TaxRateTimeline = TaxRateTimeline(entries = listOf(newerEntry, olderEntry))

  @Test
  fun testEntryAt() {
    //Exact matches
    assertThat(timeline.entryAt(newerEntry.validFrom)).isEqualTo(newerEntry)
    assertThat(timeline.entryAt(olderEntry.validFrom)).isEqualTo(olderEntry)

    //After matches
    assertThat(timeline.entryAt(newerEntry.validFrom.plus(5.minutes))).isEqualTo(newerEntry)
    assertThat(timeline.entryAt(olderEntry.validFrom.plus(5.minutes))).isEqualTo(olderEntry)

    //Before the first entry
    assertThat(timeline.entryAt(olderEntry.validFrom.minus(5.minutes))).isNull()
  }

  @Test
  fun testValueAt() {
    assertThat(timeline.valueAt(olderEntry.validFrom.plus(5.minutes))).isEqualTo(TaxRate(16))
    assertThat(timeline.valueAtOrNull(olderEntry.validFrom.minus(5.minutes))).isNull()

    assertFailure {
      timeline.valueAt(olderEntry.validFrom.minus(5.minutes))
    }
      .isInstanceOf(IllegalStateException::class)
      .messageContains("the earliest entry is valid from ${olderEntry.validFrom}")
  }

  /**
   * The two reasons a lookup finds nothing are different defects — a timeline that was never
   * seeded, and one whose entries all start later than the requested date. The message names
   * which one it was.
   */
  @Test
  fun testValueAtOnEmptyTimeline() {
    assertFailure {
      ValueTimeline.empty<TaxRate>().valueAt(olderEntry.validFrom)
    }
      .isInstanceOf(IllegalStateException::class)
      .messageContains("the timeline is empty")
  }

  @Test
  fun testNewestValue() {
    assertThat(timeline.newestValue()).isEqualTo(TaxRate(19))
    assertThat(ValueTimeline.empty<TaxRate>().newestValueOrNull()).isNull()

    assertFailure {
      ValueTimeline.empty<TaxRate>().newestValue()
    }
  }

  @Test
  fun testSingle() {
    val single: TaxRateTimeline = ValueTimeline.single(TaxRate(19))

    assertThat(single.entries).hasSize(1)
    assertThat(single.newestValue()).isEqualTo(TaxRate(19))
    assertThat(single.entries[0].validFrom).isEqualTo(ValueTimeline.DistantPastForSingle)
  }

  @Test
  fun testFirstFutureEntry() {
    //Both entries in the future relative to a very early "now"
    assertThat(timeline.firstFutureEntry(olderEntry.validFrom.minus(5.minutes))).isEqualTo(olderEntry)
    //Only the newer entry is in the future
    assertThat(timeline.firstFutureEntry(olderEntry.validFrom)).isEqualTo(newerEntry)
    //No future entries
    assertThat(timeline.firstFutureEntry(newerEntry.validFrom)).isNull()
  }

  @Test
  fun testIsEmpty() {
    assertThat(ValueTimeline.empty<TaxRate>().isEmpty()).isTrue()
    assertThat(timeline.isEmpty()).isFalse()
  }

  @Test
  fun testContainsEntryWithExactValidFrom() {
    assertThat(timeline.containsEntryWithExactValidFrom(newerEntry.validFrom)).isTrue()
    assertThat(timeline.containsEntryWithExactValidFrom(newerEntry.validFrom.plus(5.minutes))).isFalse()
  }

  @Test
  fun testValidation() {
    //Correct order works
    assertThat(TaxRateTimeline(entries = listOf(newerEntry, olderEntry))).isNotNull()

    //Wrong order fails
    assertFailure {
      TaxRateTimeline(entries = listOf(olderEntry, newerEntry))
    }

    //Duplicate validFrom fails
    assertFailure {
      TaxRateTimeline(entries = listOf(newerEntry, newerEntry.copy(value = TaxRate(7))))
    }
  }

  @Test
  fun testPlusSortsEntries() {
    val entryBetween = TimelineEntry(
      value = TaxRate(7),
      validFrom = TimeInstantExample._2024_09_12_06_44_17_500Z,
    )
    val added = timeline + entryBetween

    assertThat(added.entries).containsExactly(newerEntry, entryBetween, olderEntry)
    //The original timeline is unchanged
    assertThat(timeline.entries).hasSize(2)

    //Duplicate validFrom fails via the constructor validation
    assertFailure {
      timeline + newerEntry.copy(value = TaxRate(7))
    }
  }

  @Test
  fun testWithAddedStrict() {
    val entryBetween = TimelineEntry(
      value = TaxRate(7),
      validFrom = TimeInstantExample._2024_09_12_06_44_17_500Z,
    )
    assertThat(timeline.withAdded(entryBetween).entries).containsExactly(newerEntry, entryBetween, olderEntry)

    assertFailure {
      timeline.withAdded(newerEntry.copy(value = TaxRate(7)))
    }
  }

  @Test
  fun testWithAddedOrReplaced() {
    val replacement = newerEntry.copy(value = TaxRate(7))
    val replaced = timeline.withAddedOrReplaced(replacement)

    assertThat(replaced.entries).containsExactly(replacement, olderEntry)
    assertThat(replaced.valueAt(newerEntry.validFrom)).isEqualTo(TaxRate(7))
  }

  @Test
  fun testJsonEmpty() {
    roundTrip(ValueTimeline.empty<TaxRate>()) {
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
    roundTrip(timeline) {
      //language=JSON
      """
        {
          "entries" : [ {
            "value" : 19,
            "validFrom" : "2026-04-24T03:10:00Z"
          }, {
            "value" : 16,
            "validFrom" : "2020-05-21T15:00:41.500Z"
          } ]
        }
      """.trimIndent()
    }
  }
}
