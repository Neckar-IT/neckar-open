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
package it.neckar.price.rest.views

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import it.neckar.financial.currency.Money
import it.neckar.financial.currency.euroNet
import it.neckar.open.time.TimeInstantExample
import it.neckar.price.PriceTimeline
import it.neckar.timeline.TimelineEntry
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.days

class PriceTimelineSummaryTest {
  private val now = TimeInstantExample._2026_04_24_03_10_00_000Z

  @Test
  fun `projects the latest entry valid at now as current and the earliest future entry as next`() {
    val pastLaunch = TimelineEntry(value = 4.00.euroNet, validFrom = now - 800.days)
    val startingNow = TimelineEntry(value = 5.00.euroNet, validFrom = now)
    val firstUpcoming = TimelineEntry(value = 5.50.euroNet, validFrom = now + 200.days)
    val farFutureDecoy = TimelineEntry(value = 7.00.euroNet, validFrom = now + 1300.days)

    val summary = PriceTimeline(entries = listOf(farFutureDecoy, firstUpcoming, startingNow, pastLaunch)).toSummary(now)

    // An entry starting exactly at now is current, not future.
    assertThat(summary.currentEntry).isEqualTo(startingNow.toView())
    assertThat(summary.nextFutureEntry).isEqualTo(firstUpcoming.toView())
  }

  @Test
  fun `projects an empty timeline as an empty summary`() {
    val summary = PriceTimeline.empty<Money.Net>().toSummary(now)

    assertThat(summary).isEqualTo(PriceTimelineSummary.empty)
  }

  @Test
  fun `projects a timeline of only future entries with no current entry`() {
    val earliest = TimelineEntry(value = 5.00.euroNet, validFrom = now + 10.days)
    val later = TimelineEntry(value = 6.00.euroNet, validFrom = now + 400.days)

    val summary = PriceTimeline(entries = listOf(later, earliest)).toSummary(now)

    assertThat(summary.currentEntry).isNull()
    assertThat(summary.nextFutureEntry).isEqualTo(earliest.toView())
  }
}
