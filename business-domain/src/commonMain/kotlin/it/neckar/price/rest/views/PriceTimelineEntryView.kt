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

import it.neckar.timeline.TimelineEntry
import it.neckar.financial.currency.Money
import it.neckar.open.kotlin.serializers.NativeInstant
import kotlinx.serialization.Serializable

/**
 * REST projection of a single embedded price-timeline entry ([TimelineEntry]`<Money.Net>`).
 *
 * Structurally equivalent to the domain entry today, but decoupling the REST
 * type makes future evolution (e.g. adding a `note` or `source` field only on
 * the view) non-breaking for the domain / persisted types.
 *
 * Served as the row type of the paginated sub-endpoint
 * (`GET /api/.../price-timeline`) and embedded inside [PriceTimelineSummary]
 * for the current / next-upcoming entry on detail views.
 */
@Serializable
data class PriceTimelineEntryView(
  val price: Money.Net,
  val validFrom: NativeInstant,
)

fun TimelineEntry<Money.Net>.toView(): PriceTimelineEntryView = PriceTimelineEntryView(price = value, validFrom = validFrom)

/**
 * Row of the paginated sub-endpoint; a bare price has no derived column, so the row is the [PriceTimelineEntryView] itself.
 */
fun TimelineEntry<Money.Net>.toTableEntry(): PriceTimelineEntryView = toView()
