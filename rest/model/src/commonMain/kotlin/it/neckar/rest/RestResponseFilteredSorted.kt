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
package it.neckar.rest

import it.neckar.rest.filtering.FilterKey
import it.neckar.rest.filtering.HasTypedFilterKey
import it.neckar.rest.filtering.TypedFilterKey

/**
 * Also contains information about filtering and sorting
 */
interface RestResponseFilteredSorted<out DataType, out FilterByType, out SortByType>
  : RestResponse<DataType>

  where SortByType : Enum<out SortByType>, SortByType : RestResponseSorts,
        FilterByType : Enum<out FilterByType>, FilterByType : RestResponseFilters


/**
 * Interface for filter criteria enums.
 *
 * Implementing enums **must** declare a constructor parameter for [filterType]:
 * ```kotlin
 * enum class FilterCriteria(
 *   override val filterType: TypedFilterKey.FilterType = TypedFilterKey.FilterType.String,
 * ) : RestResponseFilters {
 *   Title,
 *   Year(TypedFilterKey.FilterType.Int),
 *   Status(enumFilterType<MyStatus>()),
 * }
 * ```
 *
 * [typedFilterKey] is auto-derived from the enum entry name (decapitalized) and [filterType].
 * Override [typedFilterKey] only when the key itself differs from the enum name.
 */
interface RestResponseFilters : HasTypedFilterKey {
  /**
   * The filter value type. Must be provided via enum constructor parameter
   * with a default of [TypedFilterKey.FilterType.String].
   */
  val filterType: TypedFilterKey.FilterType

  override val typedFilterKey: TypedFilterKey
    get() = TypedFilterKey(
      filterKey = FilterKey((this as Enum<*>).name.replaceFirstChar { it.lowercaseChar() }),
      type = filterType,
    )
}
