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
package it.neckar.rest.filtering

import it.neckar.open.collections.removeIfOrNull

/**
 * Contains the filter descriptors for a request.
 */
data class FilterDescriptors<FilterCriteriaType>(
  private val filterDescriptors: Map<FilterCriteriaType, FilterDescriptor>,
) where FilterCriteriaType : Enum<FilterCriteriaType>, FilterCriteriaType : HasTypedFilterKey {

  /**
   * Returns the size of the [filterDescriptors] map
   */
  val size: Int
    get() = filterDescriptors.size

  /**
   * Returns the filter descriptor for the given filter key - if there is one
   */
  operator fun get(type: FilterCriteriaType): FilterDescriptor? {
    return filterDescriptors[type]
  }

  /**
   * Returns true if the filter descriptors are empty
   */
  fun isEmpty(): Boolean {
    return filterDescriptors.isEmpty()
  }

  /**
   * Splits the filter descriptors into scope filters and user filters.
   *
   * Scope filters define the implicit context for a query (e.g., "books for this author").
   * They affect `totalElementsCount` in pagination. User filters narrow within that scope
   * and only affect `filteredElementsCount`.
   *
   * @param scopeFilterKeys the [FilterKey]s that should be treated as scope filters
   * @return a pair of (scope filters, user filters)
   */
  fun splitByScope(scopeFilterKeys: Set<FilterKey>): Pair<FilterDescriptors<FilterCriteriaType>, FilterDescriptors<FilterCriteriaType>> {
    val scopeEntries = mutableMapOf<FilterCriteriaType, FilterDescriptor>()
    val userEntries = mutableMapOf<FilterCriteriaType, FilterDescriptor>()

    filterDescriptors.forEach { (key, descriptor) ->
      if (key.typedFilterKey.filterKey in scopeFilterKeys) {
        scopeEntries[key] = descriptor
      } else {
        userEntries[key] = descriptor
      }
    }

    return FilterDescriptors(scopeEntries) to FilterDescriptors(userEntries)
  }

  /**
   * Returns all filter descriptors as a list.
   *
   * Useful for serialization into REST responses where the generic enum key
   * is not needed — the [FilterDescriptor.typedFilterKey] already identifies
   * which criterion each descriptor belongs to.
   */
  fun toList(): List<FilterDescriptor> {
    return filterDescriptors.values.toList()
  }

  /**
   * Maps the filter descriptors
   */
  fun <T> map(action: (FilterCriteriaType, FilterDescriptor) -> T): List<T> {
    return filterDescriptors.map {
      val key = it.key
      action(key, it.value)
    }
  }

  companion object {
    inline fun <reified FilterCriteriaType> empty(): FilterDescriptors<FilterCriteriaType> where FilterCriteriaType : Enum<FilterCriteriaType>, FilterCriteriaType : HasTypedFilterKey {
      return FilterDescriptors(emptyMap())
    }

    /**
     * Creates a new [it.neckar.rest.filtering.FilterDescriptors] object from the given [FilterDescriptor]s.
     */
    inline fun <reified FilterCriteriaType> from(vararg filterDescriptors: FilterDescriptor?): FilterDescriptors<FilterCriteriaType>
      where FilterCriteriaType : Enum<FilterCriteriaType>, FilterCriteriaType
    : HasTypedFilterKey {
      return from(filterDescriptors.toList())
    }

    /**
     * Helper method to create a [FilterDescriptors] object from a list of [FilterDescriptor]s.
     */
    inline fun <reified FilterCriteriaType> from(filterDescriptors: List<FilterDescriptor?>): FilterDescriptors<FilterCriteriaType>
      where FilterCriteriaType : Enum<FilterCriteriaType>, FilterCriteriaType
    : HasTypedFilterKey {

      val remaining = filterDescriptors.filterNotNull().toMutableList()

      val map = buildMap {
        kotlin.enums.enumEntries<FilterCriteriaType>().toList().forEach { filterCriteriaType ->
          val descriptor = remaining.removeIfOrNull {
            it.typedFilterKey == filterCriteriaType.typedFilterKey
          }
          if (descriptor != null) {
            put(filterCriteriaType, descriptor)
          }
        }
      }

      //ensure that all entries have been consumed
      if (remaining.isNotEmpty()) {
        val remainingAsString = remaining.joinToString {
          it.typedFilterKey.filterKey.toString()
        }
        throw IllegalArgumentException("The following filter descriptors have not been consumed: [$remainingAsString]")
      }

      return FilterDescriptors(map)
    }

    /**
     * Creates a new instance from the given action.
     * The action is called for each enum value and may return a [FilterDescriptor].
     *
     * This method can be used to extract the filter descriptors from the URL parameters.
     */
    inline fun <reified T> create(action: (enumValue: T) -> FilterDescriptor?): FilterDescriptors<T> where T : Enum<T>, T : HasTypedFilterKey {
      val descriptors = buildMap {
        kotlin.enums.enumEntries<T>().toList().forEach { enumValue ->
          val filterDescriptor = action(enumValue)
          if (filterDescriptor != null) {
            put(enumValue, filterDescriptor)
          }
        }
      }

      return FilterDescriptors(descriptors)
    }
  }
}
