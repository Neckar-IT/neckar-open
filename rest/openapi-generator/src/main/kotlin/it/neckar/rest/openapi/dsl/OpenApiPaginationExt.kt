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
package it.neckar.rest.openapi.dsl
import it.neckar.open.collections.fastForEach
import it.neckar.open.http.UrlParameterName
import it.neckar.open.kotlin.lang.asEnumClass
import it.neckar.open.kotlin.lang.asKClass
import it.neckar.open.kotlin.lang.enumEntries
import it.neckar.rest.filtering.FilterKey
import it.neckar.rest.filtering.HasTypedFilterKey
import it.neckar.rest.filtering.TypedFilterKey
import it.neckar.rest.filtering.toUrlParameterName
import it.neckar.rest.pagination.PageQuery
import it.neckar.rest.sorting.Sort
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.starProjectedType
import it.neckar.rest.openapi.schema.SchemaDescriptor

/**
 * Registers the pagination parameters for the request
 */
fun OpenApiRouteBuilder.paginated() {
  request {
    paginated()
  }
}

/**
 * Registers the pagination parameters for the request
 */
fun OpenApiRequestBuilder.paginated() {
  parameterInt(PageQuery.PageIndexQueryParamName)
  parameterInt(PageQuery.PageSizeQueryParamName)
}


/**
 * Registers the sorting parameters for the request
 */
inline fun <reified T : Enum<T>> OpenApiRouteBuilder.sortedBy() {
  request {
    sortedBy<T>()
  }
}

fun OpenApiRouteBuilder.sortedBy(enumType: KType) {
  request {
    sortedBy(enumType)
  }
}

/**
 * Registers the sorting parameters for the request
 */
inline fun <reified T : Enum<T>> OpenApiRequestBuilder.sortedBy() {
  sortedBy(T::class.starProjectedType)
}

/**
 * Registers the sorting parameters for the request.
 *
 * The allowed `sortBy` values are the enum entry names of [enumType] — these are exactly
 * the values that [it.neckar.rest.sorting.parseSortBy] accepts on the backend.
 */
fun OpenApiRequestBuilder.sortedBy(enumType: KType) {
  parameterEnum(UrlParameterName.sortBy, enumType)
  parameterEnum(UrlParameterName.sortOrder, Sort.Order::class.starProjectedType)
}

/**
 * Registers the filter parameters for the request
 */
inline fun <reified T> OpenApiRouteBuilder.filteredBy() where T : HasTypedFilterKey, T : Enum<T> {
  request {
    filteredBy(T::class.enumEntries.toList(), T::class)
  }
}

fun OpenApiRouteBuilder.filteredBy(filterByType: KType) {
  val filterCriteriaType = filterByType.asKClass()
  filteredBy(filterCriteriaType.asEnumClass().enumEntries.toList() as List<HasTypedFilterKey>, filterCriteriaType)

}

fun OpenApiRouteBuilder.filteredBy(filterValues: List<HasTypedFilterKey>, filterType: KClass<*>?) {
  request {
    filteredBy(filterValues, filterType)
  }
}

/**
 * Registers filter parameters for the request.
 *
 * Generates one query parameter per filter key and an additional `scope` parameter
 * with an inline enum of all allowed filter key names.
 */
fun OpenApiRequestBuilder.filteredBy(filterValues: List<HasTypedFilterKey>, filterType: KClass<*>?) {
  filterValues.fastForEach { filterKey ->
    filteredBy(filterKey, filterType)
  }

  if (filterValues.isNotEmpty()) {
    val allowedScopeValues: List<FilterKey> = filterValues.map { it.typedFilterKey.filterKey }
    parameter<String>(UrlParameterName.scope, SchemaDescriptor.InlineEnumSchema(allowedScopeValues) { it.value }, filterType) {
      description = "Comma-separated list of filter keys that define the query scope. Scope filters affect totalElementsCount in pagination."
    }
  }
}

fun OpenApiRequestBuilder.filteredBy(key: HasTypedFilterKey, filterType: KClass<*>?) {
  val typedFilterKey = key.typedFilterKey

  when (val filterKeyType = typedFilterKey.type) {
    is TypedFilterKey.FilterType.Complex -> TODO("Complex types not implemented yet!")
    TypedFilterKey.FilterType.Boolean -> parameterBoolean(name = typedFilterKey.toUrlParameterName(), kotlinSourceType = filterType)
    TypedFilterKey.FilterType.Date -> parameterDate(name = typedFilterKey.toUrlParameterName(), kotlinSourceType = filterType)
    TypedFilterKey.FilterType.Instant -> parameterDateTime(name = typedFilterKey.toUrlParameterName(), kotlinSourceType = filterType)
    TypedFilterKey.FilterType.Email -> parameterEmail(name = typedFilterKey.toUrlParameterName(), kotlinSourceType = filterType)
    TypedFilterKey.FilterType.Double -> parameterDouble(name = typedFilterKey.toUrlParameterName(), kotlinSourceType = filterType)
    is TypedFilterKey.FilterType.Enum -> parameterEnum(name = typedFilterKey.toUrlParameterName(), enumTypeName = filterKeyType.enumType, kotlinSourceType = filterType)
    TypedFilterKey.FilterType.Int -> parameterInt(name = typedFilterKey.toUrlParameterName(), kotlinSourceType = filterType)
    TypedFilterKey.FilterType.Long -> parameterLong(name = typedFilterKey.toUrlParameterName(), kotlinSourceType = filterType)
    TypedFilterKey.FilterType.String -> parameterString(name = typedFilterKey.toUrlParameterName(), kotlinSourceType = filterType)
    TypedFilterKey.FilterType.Uuid -> parameterUuid(name = typedFilterKey.toUrlParameterName(), kotlinSourceType = filterType)
  }
}
