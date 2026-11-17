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
import it.neckar.open.http.UrlParameterName
import it.neckar.rest.RestResponseSorts

/**
 * Pagination and sorting of a list request, usually read from query parameters.
 * `FilteredPageQuery` in `rest:model` adds filtering.
 */
@Suppress("UNCHECKED_CAST")
@JsExportForTs
data class PageQuery<SortByType>(
  /**
   * The index of the requested page
   */
  val pageIndex: PageIndex,

  /**
   * The size of the requested page
   */
  val pageSize: PageSize,

  /**
   * What to sort
   */
  val sortBy: SortByType?,

  /**
   * The order in which to sort the elements.
   */
  val sortOrder: it.neckar.rest.sorting.Sort.Order = it.neckar.rest.sorting.Sort.Order.Asc,

  ) where SortByType : Enum<SortByType>, SortByType : RestResponseSorts {

  /**
   * Selects a page from the given elements. Based upon the requested page.
   */
  @Deprecated("Potentially dangerous, filter in the database instead")
  fun <T> selectPage(filteredElements: List<T>, totalElementsCount: Int): Page<T> {
    return Page.select(filteredElements, pageIndex, pageSize, totalElementsCount)
  }

  companion object {
    val Default: PageQuery<Nothing> = PageQuery(PageIndex.Zero, PageSize.Default, null)

    /**
     * Creates an default page request
     */
    fun <SortByType> default(): PageQuery<SortByType> where SortByType : Enum<SortByType>, SortByType : RestResponseSorts {
      return Default as PageQuery<SortByType>
    }

    val PageIndexQueryParamName: UrlParameterName = UrlParameterName("pageIndex")
    val PageSizeQueryParamName: UrlParameterName = UrlParameterName("pageSize")

    val SortQueryParamName: UrlParameterName = UrlParameterName("sortBy")
    val SortOrderQueryParamName: UrlParameterName = UrlParameterName("sortOrder")
  }
}

