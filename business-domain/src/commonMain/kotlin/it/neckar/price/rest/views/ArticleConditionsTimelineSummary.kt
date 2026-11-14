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

import it.neckar.price.ArticleConditions
import it.neckar.price.ArticleConditionsTimeline
import it.neckar.open.annotations.DataView
import kotlinx.serialization.Serializable
import kotlin.time.Instant

/**
 * Time-dependent summary of an [ArticleConditionsTimeline] — the information a caller needs to
 * display the current state at a glance without rendering the full entry list.
 *
 * The conditions counterpart of [PriceTimelineSummary]. This is the shape embedded directly in
 * detail DTOs (e.g. `it.neckar.patterns.book.rest.views.BookDetails.conditionsSummary`); the full
 * entry list is served separately via a paginated sub-endpoint so detail fetches stay bounded —
 * see the value-timeline pattern doc's "Detail-View carries the summary only — entries via the
 * paginated sub-endpoint" section.
 *
 * [Conditions] is the implementation the owning article type uses, carried through from the
 * timeline to the two embedded entries.
 *
 * Use [ArticleConditionsTimeline.toSummary] at the view boundary.
 */
@DataView("Current + next upcoming entry of an article-conditions timeline — summary projection")
@Serializable
data class ArticleConditionsTimelineSummary<Conditions : ArticleConditions>(
  /** The entry currently valid (latest with `validFrom <= now`), or `null`. */
  val currentEntry: ArticleConditionsTimelineEntryView<Conditions>?,
  /** The next upcoming entry (earliest with `validFrom > now`), or `null`. */
  val nextFutureEntry: ArticleConditionsTimelineEntryView<Conditions>?,
) {
  companion object {
    /** Empty summary — no current entry, no upcoming entry. */
    fun <Conditions : ArticleConditions> empty(): ArticleConditionsTimelineSummary<Conditions> =
      ArticleConditionsTimelineSummary(currentEntry = null, nextFutureEntry = null)
  }
}

/**
 * Projects the summary of an [ArticleConditionsTimeline] relative to a server-defined
 * reference time.
 */
fun <Conditions : ArticleConditions> ArticleConditionsTimeline<Conditions>.toSummary(now: Instant): ArticleConditionsTimelineSummary<Conditions> =
  ArticleConditionsTimelineSummary(
    currentEntry = entryAt(now)?.toView(),
    nextFutureEntry = firstFutureEntry(now)?.toView(),
  )
