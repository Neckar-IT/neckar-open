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

import it.neckar.price.PriceTimeline
import it.neckar.open.annotations.DataView
import kotlinx.serialization.Serializable
import kotlin.time.Instant

/**
 * Time-dependent summary of a [PriceTimeline] — the information a caller needs
 * to display the current state at a glance without rendering the full entry list.
 *
 * This is the shape embedded directly in detail DTOs (e.g.
 * `it.neckar.patterns.membershipplan.rest.views.MembershipPlanDetails.priceSummary`). The full
 * entry list is served separately via a paginated sub-endpoint so detail
 * fetches stay bounded — see the value-timeline pattern doc's "Detail-View carries the summary
 * only — entries via the paginated sub-endpoint" section.
 *
 * Use [PriceTimeline.toSummary] at the view boundary.
 */
@DataView("Current + next upcoming entry of a price timeline — summary projection")
@Serializable
data class PriceTimelineSummary(
  /** The entry currently valid (latest with `validFrom <= now`), or `null`. */
  val currentEntry: PriceTimelineEntryView?,
  /** The next upcoming entry (earliest with `validFrom > now`), or `null`. */
  val nextFutureEntry: PriceTimelineEntryView?,
) {
  companion object {
    /** Empty summary — no current entry, no upcoming entry. */
    val empty: PriceTimelineSummary = PriceTimelineSummary(currentEntry = null, nextFutureEntry = null)
  }
}

/**
 * Projects the summary of a [PriceTimeline] relative to a server-defined
 * reference time.
 */
fun PriceTimeline.toSummary(now: Instant): PriceTimelineSummary = PriceTimelineSummary(
  currentEntry = entryAt(now)?.toView(),
  nextFutureEntry = firstFutureEntry(now)?.toView(),
)
