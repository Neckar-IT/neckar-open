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
import it.neckar.open.kotlin.lang.divCeil
import kotlinx.serialization.Serializable
import kotlin.js.JsExport

/**
 * Represents the pagination information
 */
@JsExportForTs
@Serializable
data class PaginationInfo(
  /**
   * The index of the current page
   */
  val pageIndex: PageIndex,

  /**
   * The max size of (all) pages
   */
  val pageSize: PageSize,

  /**
   * The number of pages
   */
  val pageCount: Int,

  /**
   * The filtered items count.
   * [pageSize] and [pageCount] are calculated based on this value.
   *
   * This is the total number of elements that are returned by the query (for all pages).
   * This is *not* the number of elements in this page.
   */
  val filteredElementsCount: Int,

  /**
   * The total number of elements.
   * Including filtered elements.
   */
  val totalElementsCount: Int,
) {

  /**
   * Returns the elements for the given page from the given list.
   */
  fun <T> selectElements(elements: List<T>): List<T> {
    return when (val bounds = PageBounds.of(pageIndex, pageSize)) {
      PageBounds.Unbounded -> elements
      is PageBounds.Bounded -> elements.drop(bounds.skip).take(bounds.limit)
    }
  }

  @JsExport.Ignore
  companion object {
    val empty: PaginationInfo = PaginationInfo(
      pageIndex = PageIndex.Zero,
      pageSize = PageSize.Unlimited,
      pageCount = 0,
      filteredElementsCount = 0,
      totalElementsCount = 0,
    )

    /**
     * Creates a new [PaginationInfo] instance.
     */
    fun singlePageFromList(elements: List<*>, totalElementsCount: Int = elements.size): PaginationInfo {
      return singlePage(elements.size, totalElementsCount)
    }

    /**
     * Creates a pagination info for the given elements count.
     * Contains a single page with unlimited page size
     */
    fun singlePage(elementsCount: Int, totalElementsCount: Int = elementsCount): PaginationInfo {
      return PaginationInfo(
        pageIndex = PageIndex.Zero,
        pageSize = PageSize.Unlimited,
        pageCount = 1,
        filteredElementsCount = elementsCount,
        totalElementsCount = totalElementsCount,
      )
    }

    /**
     * Creates a [PaginationInfo] for the given elements, page index and page size.
     */
    fun create(filteredElementsCount: Int, pageIndex: PageIndex, pageSize: PageSize?, totalElementsCount: Int): PaginationInfo {
      //A null or unlimited page size means everything fits on a single page.
      //Without this guard divCeil(pageSize.value) would divide by -1 and produce a negative pageCount.
      if (pageSize == null || pageSize.isUnlimited()) return singlePage(filteredElementsCount, totalElementsCount)

      return PaginationInfo(
        pageIndex = pageIndex,
        pageSize = pageSize,
        pageCount = filteredElementsCount.divCeil(pageSize.value),
        filteredElementsCount = filteredElementsCount,
        totalElementsCount = totalElementsCount,
      )
    }
  }
}
