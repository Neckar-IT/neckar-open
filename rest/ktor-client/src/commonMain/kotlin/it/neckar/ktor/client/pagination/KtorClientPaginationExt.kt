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
package it.neckar.ktor.client.pagination

import io.ktor.client.request.*
import it.neckar.ktor.client.parameter
import it.neckar.rest.pagination.PageIndex
import it.neckar.rest.pagination.PageQuery
import it.neckar.rest.pagination.PageSize

/**
 * Request the given page, with the sort criterion it names.
 *
 * The sort order travels with the criterion only: [PageQuery.sortOrder] is non-nullable and defaults
 * to [it.neckar.rest.sorting.Sort.Order.Asc], so sending it alone would impose ascending on an
 * endpoint whose own default is another order.
 */
fun HttpRequestBuilder.requestPage(pageQuery: PageQuery<*>) {
  requestPage(pageQuery.pageIndex, pageQuery.pageSize)

  val sortBy: Enum<*> = pageQuery.sortBy ?: return
  parameter(PageQuery.SortQueryParamName, sortBy.name)
  parameter(PageQuery.SortOrderQueryParamName, pageQuery.sortOrder.name)
}

/**
 * Request the given page
 */
fun HttpRequestBuilder.requestPage(pageIndex: PageIndex?, pageSize: PageSize?) {
  if (pageIndex != null) {
    parameter(PageQuery.PageIndexQueryParamName, pageIndex.value)
  }
  if (pageSize != null) {
    parameter(PageQuery.PageSizeQueryParamName, pageSize.value)
  }
}

fun HttpRequestBuilder.requestPage(pageIndex: Int, pageSize: Int) {
  requestPage(PageIndex(pageIndex), PageSize(pageSize))
}
