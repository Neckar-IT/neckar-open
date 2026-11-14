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
package it.neckar.ktor.pagination

import io.ktor.http.*
import io.ktor.server.routing.*
import it.neckar.open.http.UrlParameterName
import it.neckar.rest.InvalidQueryParameterException
import it.neckar.rest.RestResponseSorts
import it.neckar.rest.ktor.get
import it.neckar.rest.pagination.PageIndex
import it.neckar.rest.pagination.PageQuery
import it.neckar.rest.pagination.PageSize
import it.neckar.rest.sorting.Sort
import it.neckar.rest.sorting.InvalidSortValueException
import it.neckar.rest.sorting.parseSortBy

/**
 * Returns the requested page information.
 * Contains page size, index and sorting direction. Does *not* contain filtering information.
 */
inline fun <reified SortByType> RoutingContext.requestedPage(): PageQuery<SortByType>
  where SortByType : Enum<SortByType>, SortByType : RestResponseSorts {

  val requestedPage: PageQuery<SortByType> = PageQuery<SortByType>(
    pageIndex = call.parameters.pageIndex() ?: PageIndex.Zero,
    pageSize = call.parameters.pageSize() ?: PageSize.Default,
    sortBy = call.parameters.pageSortBy<SortByType>(),
    sortOrder = call.parameters.pageSortOrder() ?: Sort.Order.Asc,
  )
  return requestedPage
}

/**
 * Returns the requested page information - when sorting is not supported.
 * In most cases [requestedPage] should be used.
 *
 * Rejects a request that names either sort parameter.
 */
fun RoutingContext.requestedPageNoSort(): PageQuery<Nothing> {
  checkNoSortParameters()

  return PageQuery<Nothing>(
    pageIndex = call.parameters.pageIndex() ?: PageIndex.Zero,
    pageSize = call.parameters.pageSize() ?: PageSize.Default,
    sortBy = null,
  )
}

/**
 * Checks that the request carries neither sort parameter.
 *
 * @throws it.neckar.rest.InvalidQueryParameterException if [PageQuery.SortQueryParamName] or
 * [PageQuery.SortOrderQueryParamName] is present. An endpoint that cuts its page in one fixed order
 * would answer that order whatever the request asked for, so the request is rejected rather than
 * answered with something it did not ask for.
 */
fun RoutingContext.checkNoSortParameters() {
  listOf(PageQuery.SortQueryParamName, PageQuery.SortOrderQueryParamName).forEach { parameterName ->
    val value: String? = call.parameters[parameterName.value]
    if (value != null) {
      throw InvalidQueryParameterException(invalidValueMessage(value, parameterName, "Sorting is not supported by this endpoint."))
    }
  }
}

/**
 * Returns the page index.
 * Returns null if the page index is not present.
 *
 * @throws it.neckar.rest.InvalidQueryParameterException if the parameter is present but is not a valid page index.
 */
fun Parameters.pageIndex(): PageIndex? = this[PageQuery.PageIndexQueryParamName]?.let { raw ->
  raw.parseAs(PageQuery.PageIndexQueryParamName, ::PageIndex)
}

/**
 * Returns the page index.
 * Returns null if the page index is not present.
 */
fun RoutingContext.pageIndex(): PageIndex? {
  return call.parameters.pageIndex()
}

/**
 * Returns the page size.
 * Returns null if the page size is not present.
 *
 * @throws it.neckar.rest.InvalidQueryParameterException if the parameter is present but is not a valid page size.
 */
fun Parameters.pageSize(): PageSize? = this[PageQuery.PageSizeQueryParamName]?.let { raw ->
  raw.parseAs(PageQuery.PageSizeQueryParamName, ::PageSize)
}

/**
 * Returns the page size.
 * Returns null if the page size is not present.
 */
fun RoutingContext.pageSize(): PageSize? {
  return call.parameters.pageSize()
}

/**
 * Returns the raw `sortBy` query parameter value, or `null` if not present.
 *
 * Use [pageSortBy] for a typed lookup against a sort criteria enum.
 */
fun Parameters.rawSortBy(): String? = this[PageQuery.SortQueryParamName]

/**
 * Returns the page sort criterion as the matching enum entry of [SortByType].
 * Returns null if the sort by parameter is not present.
 *
 * @throws it.neckar.rest.sorting.InvalidSortValueException if the parameter is present but does not match any entry of [SortByType].
 */
inline fun <reified SortByType> RoutingContext.pageSortBy(): SortByType?
  where SortByType : Enum<SortByType>, SortByType : RestResponseSorts {
  return call.parameters.pageSortBy<SortByType>()
}

inline fun <reified SortByType> Parameters.pageSortBy(): SortByType?
  where SortByType : Enum<SortByType>, SortByType : RestResponseSorts {
  return parseSortBy<SortByType>(rawSortBy())
}

/**
 * Returns the page sorting order from the search parameters.
 * Returns null if the sort order parameter is not present.
 *
 * @throws it.neckar.rest.sorting.InvalidSortValueException if the parameter is present but names no order.
 */
fun Parameters.pageSortOrder(): Sort.Order? = this[PageQuery.SortOrderQueryParamName]?.let { raw ->
  try {
    Sort.Order.fromString(raw)
  } catch (e: IllegalArgumentException) {
    //Sort.Order.fromString throws for an unknown order, and a bare IllegalArgumentException answers
    //500 - the same translation parseAs does for PageIndex and PageSize.
    throw InvalidSortValueException(raw, Sort.Order.entries.map { it.name }, PageQuery.SortOrderQueryParamName)
  }
}

/**
 * Parses this raw query parameter value as an [Int].
 *
 * [String.toInt] alone reports `For input string: "abc"` - the value, but not the parameter it came
 * from. With `pageIndex` and `pageSize` in the same query string that leaves the caller guessing which
 * one was rejected, and the resulting [NumberFormatException] is logged as a generic illegal argument
 * with a stack trace instead of as the query parameter problem it is.
 */
private fun String.parseAsInt(parameterName: UrlParameterName): Int {
  return toIntOrNull() ?: throw InvalidQueryParameterException(invalidValueMessage(this, parameterName, "Value must be an integer."))
}

/**
 * Parses this raw query parameter value as an [Int] and wraps it with [create].
 *
 * [create] rejects values outside its range with an [IllegalArgumentException] whose message lacks the parameter name; it is rethrown naming it.
 */
private fun <T> String.parseAs(parameterName: UrlParameterName, create: (Int) -> T): T {
  val value: Int = parseAsInt(parameterName)
  try {
    return create(value)
  } catch (e: IllegalArgumentException) {
    throw InvalidQueryParameterException(invalidValueMessage(this, parameterName, e.message.orEmpty()), e)
  }
}

/**
 * Builds the message for a rejected query parameter value - the shape [it.neckar.rest.sorting.InvalidSortValueException]
 * uses for `sortBy`: the offending value, the parameter it belongs to, and why it was rejected.
 */
private fun invalidValueMessage(rawValue: String, parameterName: UrlParameterName, reason: String): String {
  return "Invalid value [$rawValue] for query parameter [${parameterName.value}]. $reason"
}
