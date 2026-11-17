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
import it.neckar.ktor.filter.filterDescriptorsFromParameters
import it.neckar.open.http.UrlParameterName
import it.neckar.rest.InvalidQueryParameterException
import it.neckar.rest.RestResponseSorts
import it.neckar.rest.filtering.FilterKey
import it.neckar.rest.filtering.HasTypedFilterKey
import it.neckar.rest.ktor.get
import it.neckar.rest.pagination.FilteredPageQuery

/**
 * Extracts the pagination, sorting and filtering information from the parameters and returns it as a [FilteredPageQuery].
 *
 * Also parses the optional `scope` query parameter (comma-separated list of filter keys)
 * to determine which filters define the implicit query scope vs. user-controllable filters.
 */
inline fun <reified SortByType, reified FilterCriteriaType> RoutingContext.pageQueryFromParameters(): FilteredPageQuery<SortByType, FilterCriteriaType>
  where SortByType : Enum<SortByType>, SortByType : RestResponseSorts,
        FilterCriteriaType : Enum<FilterCriteriaType>, FilterCriteriaType : HasTypedFilterKey {

  val requestedPage = requestedPage<SortByType>()
  val filterDescriptors = filterDescriptorsFromParameters<FilterCriteriaType>()
  val scopeFilterKeys = scopeFilterKeysFromParameters<FilterCriteriaType>()

  return FilteredPageQuery(
    page = requestedPage,
    filters = filterDescriptors,
    scopeFilterKeys = scopeFilterKeys,
  )
}

/**
 * Parses the `scope` query parameter into a set of [FilterKey]s.
 *
 * The `scope` parameter is a comma-separated list of filter key names.
 * Example: `?scope=authorId` or `?scope=authorId,categoryId`
 */
inline fun <reified FilterCriteriaType> RoutingContext.scopeFilterKeysFromParameters(): Set<FilterKey>
  where FilterCriteriaType : Enum<FilterCriteriaType>, FilterCriteriaType : HasTypedFilterKey {
  return call.parameters.scopeFilterKeys<FilterCriteriaType>()
}

/**
 * Parses the `scope` query parameter into a set of [FilterKey]s.
 *
 * The `scope` parameter is a comma-separated list of filter key names.
 * Example: `?scope=authorId` or `?scope=authorId,categoryId`
 *
 * @throws it.neckar.rest.InvalidQueryParameterException if a name belongs to no filter criterion of
 * [FilterCriteriaType]. The scope decides `totalElementsCount`, so a name nobody recognizes would
 * silently answer the total over the unscoped set.
 */
inline fun <reified FilterCriteriaType> Parameters.scopeFilterKeys(): Set<FilterKey>
  where FilterCriteriaType : Enum<FilterCriteriaType>, FilterCriteriaType : HasTypedFilterKey {
  val scopeParam = this[UrlParameterName.scope] ?: return emptySet()

  //The allowed names are the ones the OpenAPI spec declares for the parameter, built from the same list.
  val allowedKeys: List<FilterKey> = enumValues<FilterCriteriaType>().map { it.typedFilterKey.filterKey }

  return scopeParam
    .splitToSequence(",")
    .map { it.trim() }
    .filter { it.isNotEmpty() }
    .map { name ->
      val filterKey = FilterKey(name)
      if (filterKey !in allowedKeys) {
        throw InvalidQueryParameterException(
          "Invalid value [$name] for query parameter [${UrlParameterName.scope.value}]. Value must be one of: [${allowedKeys.joinToString(", ") { it.value }}]"
        )
      }
      filterKey
    }
    .toSet()
}
