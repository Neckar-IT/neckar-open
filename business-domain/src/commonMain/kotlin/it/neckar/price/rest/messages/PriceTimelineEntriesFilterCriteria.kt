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
package it.neckar.price.rest.messages

import it.neckar.rest.RestResponseFilters
import it.neckar.rest.filtering.FilterDescriptors
import it.neckar.rest.filtering.FilterKey
import it.neckar.rest.filtering.TypedFilterKey
import it.neckar.rest.filtering.scopeFiltersOf
import kotlinx.serialization.Serializable

/**
 * Filter criteria for the paginated price-timeline list endpoint — shared across all parents
 * with a price timeline (e.g. `MembershipPlan`).
 */
enum class PriceTimelineEntriesFilterCriteria(
  override val filterType: TypedFilterKey.FilterType = TypedFilterKey.FilterType.String,
) : RestResponseFilters {
  /**
   * Keeps only entries whose `validFrom` is strictly after the given instant —
   * e.g. `?validFromAfter=2025-01-01T00:00:00Z` to get upcoming entries.
   */
  ValidFromAfter(TypedFilterKey.FilterType.Instant),

  /**
   * Keeps only the entries of one [ValueTimelineEntryStatus], e.g. `?status=Future`.
   */
  Status,
  ;

  @Serializable
  data class State(
    val validFromAfter: String? = null,
    val status: ValueTimelineEntryStatus? = null,
    val scopeFilters: Set<PriceTimelineEntriesFilterCriteria> = emptySet(),
  ) {
    companion object {
      fun create(
        filterDescriptors: FilterDescriptors<PriceTimelineEntriesFilterCriteria>,
        scopeFilterKeys: Set<FilterKey> = emptySet(),
      ): State {
        return State(
          validFromAfter = filterDescriptors[ValidFromAfter]?.constraint,
          status = filterDescriptors[Status]?.timelineEntryStatusValue(),
          scopeFilters = scopeFiltersOf(entries, scopeFilterKeys),
        )
      }
    }
  }
}
