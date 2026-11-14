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
package it.neckar.price

import it.neckar.timeline.ValueTimeline
import kotlin.time.Instant

/**
 * The conditions timeline of a catalog position — the [ArticleConditions] instantiation of
 * [ValueTimeline].
 *
 * [Conditions] is the implementation the owning article type uses: a book's timeline is an
 * `ArticleConditionsTimeline<BareArticleConditions>`, a cable's an
 * `ArticleConditionsTimeline<FullArticleConditions>`. Pinning the implementation on the timeline is
 * what keeps the value out of polymorphic serialization — every entry of one timeline has the shape
 * its article type has.
 *
 * Every value of the entry is carried forward as one time-versioned state: a change writes a single
 * new entry holding all of them, so the parts can never become valid from different points in time.
 *
 * Entries serialize as `{value, validFrom}`.
 */
typealias ArticleConditionsTimeline<Conditions> = ValueTimeline<Conditions>

/**
 * Returns the conditions valid for the given date. Throws if no entry is valid at [date].
 */
fun <Conditions : ArticleConditions> ArticleConditionsTimeline<Conditions>.conditionsAt(date: Instant): Conditions {
  return conditionsAtOrNull(date) ?: throw IllegalStateException("No conditions entry available at date $date — ${describeCoverage()}")
}

fun <Conditions : ArticleConditions> ArticleConditionsTimeline<Conditions>.conditionsAtOrNull(date: Instant): Conditions? {
  return valueAtOrNull(date)
}

/**
 * Returns the newest conditions - might be future conditions!
 */
fun <Conditions : ArticleConditions> ArticleConditionsTimeline<Conditions>.newestConditions(): Conditions {
  return newestConditionsOrNull() ?: throw IllegalStateException("No conditions entries available")
}

/**
 * Returns the youngest conditions
 */
fun <Conditions : ArticleConditions> ArticleConditionsTimeline<Conditions>.newestConditionsOrNull(): Conditions? {
  return newestValueOrNull()
}
