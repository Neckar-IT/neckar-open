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

import it.neckar.rest.RestResponseSorts
import it.neckar.rest.sorting.Sort

/**
 * Sorts this list, already filtered, and selects the page that [page] asks for.
 *
 * Without [PageQuery.sortBy] the list is sorted by [defaultSort] in [defaultOrder]; an explicit
 * [PageQuery.sortBy] uses [PageQuery.sortOrder]. `Desc` reverses the stable ascending sort, so
 * elements with equal keys swap places too.
 *
 * The page counts the receiver as filtered elements and [totalCount] as all elements.
 */
fun <T, S> List<T>.pageInMemory(
  page: PageQuery<S>,
  defaultSort: S,
  defaultOrder: Sort.Order,
  totalCount: Int,
  comparatorOf: (S) -> Comparator<T>,
): Page<T> where S : Enum<S>, S : RestResponseSorts {
  val sortBy = page.sortBy ?: defaultSort
  val order = if (page.sortBy == null) defaultOrder else page.sortOrder
  val ascending = sortedWith(comparatorOf(sortBy))
  val sorted = if (order == Sort.Order.Desc) ascending.reversed() else ascending

  val paginationInfo = PaginationInfo.create(filteredElementsCount = sorted.size, pageIndex = page.pageIndex, pageSize = page.pageSize, totalElementsCount = totalCount)
  return Page(paginationInfo.selectElements(sorted), paginationInfo)
}
