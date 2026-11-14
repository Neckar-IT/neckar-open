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
package it.neckar.rest.pagination

import it.neckar.open.annotations.JsExportForTs
import it.neckar.rest.RestResponseSorts
import it.neckar.rest.filtering.FilterDescriptors
import it.neckar.rest.filtering.FilterKey
import it.neckar.rest.filtering.HasTypedFilterKey

/**
 * Combines pagination, sorting and filtering.
 * This is a convenience wrapper around [PageQuery] and [FilterDescriptors].
 */
@JsExportForTs
data class FilteredPageQuery<SortByType, FilterCriteriaType>(
  /**
   * The page query containing pagination and sorting information.
   */
  val page: PageQuery<SortByType>,

  /**
   * The filter descriptors.
   */
  val filters: FilterDescriptors<FilterCriteriaType>,

  /**
   * The filter keys that define the scope of the query.
   *
   * Scope filters define the implicit context (e.g., "books for this author on the author details page").
   * They affect `totalElementsCount` in pagination — the total reflects only the scoped subset.
   * User filters (all filters NOT in this set) further narrow within that scope and only affect `filteredElementsCount`.
   *
   * The same filter criterion can be a scope filter or a user filter depending on context:
   * - Author details page: `authorId` is scope (user cannot remove it)
   * - Books main page: `authorId` is a user filter (user can add/remove it via dropdown)
   *
   * Parsed from the `scope` query parameter (comma-separated filter keys).
   */
  val scopeFilterKeys: Set<FilterKey> = emptySet(),
) where SortByType : Enum<SortByType>,
        SortByType : RestResponseSorts,
        FilterCriteriaType : Enum<FilterCriteriaType>,
        FilterCriteriaType : HasTypedFilterKey {

  // Convenience accessors for direct access to page properties
  val pageIndex: PageIndex get() = page.pageIndex
  val pageSize: PageSize get() = page.pageSize
  val sortBy: SortByType? get() = page.sortBy
  val sortOrder: it.neckar.rest.sorting.Sort.Order get() = page.sortOrder

  /**
   * Splits the filters into scope filters and user filters based on [scopeFilterKeys].
   *
   * @return a pair of (scope filters, user filters)
   */
  fun splitFiltersByScope(): Pair<FilterDescriptors<FilterCriteriaType>, FilterDescriptors<FilterCriteriaType>> {
    return filters.splitByScope(scopeFilterKeys)
  }

  companion object {
    /**
     * Creates a new [FilteredPageQuery] from individual parameters.
     */
    fun <SortByType, FilterCriteriaType> create(
      pageIndex: PageIndex,
      pageSize: PageSize,
      sortBy: SortByType?,
      sortOrder: it.neckar.rest.sorting.Sort.Order = it.neckar.rest.sorting.Sort.Order.Asc,
      filters: FilterDescriptors<FilterCriteriaType>,
    ): FilteredPageQuery<SortByType, FilterCriteriaType>
      where SortByType : Enum<SortByType>,
            SortByType : RestResponseSorts,
            FilterCriteriaType : Enum<FilterCriteriaType>,
            FilterCriteriaType : HasTypedFilterKey {
      return FilteredPageQuery(
        page = PageQuery(pageIndex, pageSize, sortBy, sortOrder),
        filters = filters,
      )
    }
  }
}
