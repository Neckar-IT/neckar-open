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
import it.neckar.timeline.TimelineEntry
import it.neckar.open.kotlin.serializers.NativeInstant
import kotlinx.serialization.Serializable

/**
 * REST projection of a single article-conditions-timeline entry ([TimelineEntry]`<Conditions>`).
 *
 * The conditions counterpart of [PriceTimelineEntryView]: the entry carries the whole commercial
 * state of a catalog position instead of a single price.
 *
 * [Conditions] is the implementation the owning article type uses — the view is serialized with
 * that very type, so the entry writes exactly the properties the article type has and nothing is
 * serialized through the [ArticleConditions] interface.
 *
 * Structurally equivalent to the domain entry today, but decoupling the REST
 * type makes future evolution (e.g. adding a `note` or `source` field only on
 * the view) non-breaking for the domain / persisted types.
 *
 * Embedded inside [ArticleConditionsTimelineSummary] for the current / next-upcoming entry on detail
 * views. The paginated sub-endpoint serves [ArticleConditionsTimelineTableEntry] instead.
 */
@Serializable
data class ArticleConditionsTimelineEntryView<Conditions : ArticleConditions>(
  val conditions: Conditions,
  val validFrom: NativeInstant,
)

fun <Conditions : ArticleConditions> TimelineEntry<Conditions>.toView(): ArticleConditionsTimelineEntryView<Conditions> =
  ArticleConditionsTimelineEntryView(conditions = value, validFrom = validFrom)
