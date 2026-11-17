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

/**
 * Represents a single page. Can be used to select elements from a list.
 *
 * There is no `mapNotNull`: dropped elements leave [paginationInfo] still counting them, and a
 * response whose count exceeds its element list is indistinguishable from a rendering bug. A
 * transform that cannot produce a value for an element throws instead.
 */
data class Page<out T>(
  /**
   * The elements on this page
   */
  val pageElements: List<T>,
  /**
   * The information about pagination
   */
  val paginationInfo: PaginationInfo,
) {
  /**
   * Returns a new page with the elements transformed by the given function.
   * The elements are mapped immediately
   */
  inline fun <R> map(function: (T) -> R): Page<R> {
    return Page(pageElements.map(function), paginationInfo)
  }

  /**
   * Returns a new list containing the first n elements
   */
  fun take(n: Int): List<T> {
    return pageElements.take(n)
  }

  companion object {
    /**
     * Creates a page that contains all provided elements
     */
    fun <T> single(elements: List<T>, totalElementsCount: Int = elements.size): Page<T> {
      return Page(elements, PaginationInfo.singlePageFromList(elements, totalElementsCount))
    }

    fun <T> singleElement(element: T): Page<T> {
      return single(listOf(element), 1)
    }

    /**
     * Creates a new page from the given elements (all elements) and the given pagination information by selecting the elements for the given page.
     * The page only contains the elements that are visible on that page.
     */
    @Deprecated("Potentially dangerous, filter in the database instead")
    fun <T> select(filteredElements: List<T>, pageIndex: PageIndex, pageSize: PageSize?, totalElementsCount: Int): Page<T> {
      val paginationInfo = PaginationInfo.create(filteredElementsCount = filteredElements.size, pageIndex = pageIndex, pageSize = pageSize, totalElementsCount = totalElementsCount)
      val pageElements = paginationInfo.selectElements(filteredElements)

      return Page(pageElements, paginationInfo)
    }

    fun <T> empty(): Page<T> {
      return Page(emptyList(), PaginationInfo.empty)
    }
  }
}
