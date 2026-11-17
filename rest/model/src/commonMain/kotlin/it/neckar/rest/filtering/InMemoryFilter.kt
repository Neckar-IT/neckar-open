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

import io.ktor.http.*
import it.neckar.open.unit.si.ms
import kotlin.uuid.Uuid

/**
 * Represents a filter for a specific object. Can be used to filter objects in the JVM.
 * Do *not* use this for filtering in the database - use the database query language instead.
 *
 * @param ToFilterType the object type to filter
 * @param FilterConstraintType the type of the filter criteria
 */
open class InMemoryFilter<ToFilterType, FilterConstraintType>(
  /**
   * The key for this filter
   */
  val key: TypedFilterKey,

  /**
   * Resolves the value of the filter from the parameters.
   *
   * This [filterConstraintExtractor] is called immediately when calling [matcher] to resolve the filter constraint as soon as possible.
   * Exceptions thrown by this function result in 400 Bad Request responses.
   */
  val filterConstraintExtractor: FilterConstraintExtractor<FilterConstraintType>,

  /**
   * The matcher for this filter. Will be called with the object to filter and the resolved filter criteria.
   */
  val matches: (toFilter: ToFilterType, filterCriteria: FilterConstraintType) -> Boolean,
) {
  /**
   * Creates a filter with a [HasFilterKey] key.
   */
  constructor(
    hasTypedFilterKey: HasTypedFilterKey,
    /**
     * Resolves the value of the filter from the parameters
     */
    filterConstraintExtractor: FilterConstraintExtractor<FilterConstraintType>,

    /**
     * The matcher for this filter
     */
    matcher: (toFilter: ToFilterType, filterCriteria: FilterConstraintType) -> Boolean,

    ) : this(hasTypedFilterKey.typedFilterKey, filterConstraintExtractor, matcher)

  /**
   * Returns a matcher for this filter
   */
  fun matcher(parameters: Parameters): (ToFilterType) -> Boolean {
    val filterDescriptor = parameters.filterDescriptorFromParameter(key)
    return matcher(filterDescriptor ?: return { true })
  }

  /**
   * Returns a matcher for this filter using the provided [FilterDescriptor].
   */
  fun matcher(filterDescriptor: FilterDescriptor): (ToFilterType) -> Boolean {
    try {
      val filterCriteria = filterConstraintExtractor(filterDescriptor)

      return {
        matches(it, filterCriteria)
      }
    } catch (e: InvalidFilterConstraintException) {
      //Directly throw the exception
      throw e
    } catch (e: Exception) {
      //Throw a well-known exception
      throw InvalidFilterConstraintException(key, filterDescriptor.constraint, null, e)
    }
  }

  companion object {
    /**
     * Creates a filter that parses the filter value from an ISO date time string. The matcher is called with a millisecond value.
     */
    fun <ToFilter> IsoDateTime(
      hasFilterKey: HasTypedFilterKey,
      /**
       * The matcher for this filter
       */
      matches: (toFilter: ToFilter, filterCriteria: Double) -> Boolean,
    ): InMemoryFilter<ToFilter, @ms Double> {
      return InMemoryFilter<ToFilter, @ms Double>(hasFilterKey, { it.isoDateTimeValue() }, matches)
    }

    fun <ToFilter> Boolean(
      hasFilterKey: HasTypedFilterKey,
      /**
       * The matcher for this filter
       */
      matches: (toFilter: ToFilter, filterCriteria: Boolean) -> Boolean,
    ): InMemoryFilter<ToFilter, Boolean> {
      return InMemoryFilter(hasFilterKey, { it.booleanValue() }, matches)
    }

    fun <ToFilter> Double(
      hasFilterKey: HasTypedFilterKey,
      /**
       * The matcher for this filter
       */
      matches: (toFilter: ToFilter, filterCriteria: Double) -> Boolean,
    ): InMemoryFilter<ToFilter, Double> {
      return InMemoryFilter(hasFilterKey, { it.doubleValue() }, matches)
    }

    fun <ToFilter> Int(
      hasFilterKey: HasTypedFilterKey,
      /**
       * The matcher for this filter
       */
      matches: (toFilter: ToFilter, filterCriteria: Int) -> Boolean,
    ): InMemoryFilter<ToFilter, Int> {
      return InMemoryFilter(hasFilterKey, { it.intValue() }, matches)
    }

    fun <ToFilter> Long(
      hasFilterKey: HasTypedFilterKey,
      /**
       * The matcher for this filter
       */
      matches: (toFilter: ToFilter, filterCriteria: Long) -> Boolean,
    ): InMemoryFilter<ToFilter, Long> {
      return InMemoryFilter(hasFilterKey, { it.longValue() }, matches)
    }

    fun <ToFilter> Uuid(
      hasFilterKey: HasTypedFilterKey,
      /**
       * The matcher for this filter
       */
      matches: (toFilter: ToFilter, filterCriteria: Uuid) -> Boolean,
    ): InMemoryFilter<ToFilter, Uuid> {
      return InMemoryFilter(hasFilterKey, { it.uuidValue() }, matches)
    }

    fun <ToFilter> String(
      hasFilterKey: HasTypedFilterKey,
      /**
       * The matcher for this filter
       */
      matches: (toFilter: ToFilter, filterCriteria: String) -> Boolean,
    ): InMemoryFilter<ToFilter, String> {
      return InMemoryFilter(hasFilterKey, { it.constraint }, matches)
    }
  }
}

/**
 * Converts a filter descriptor to an In-Memory filter
 */
fun <ToFilterType> FilterDescriptor.toDoubleFilter(
  matcher: (toFilter: ToFilterType, filterCriteria: Double) -> Boolean,
): InMemoryFilter<ToFilterType, Double> {

  return InMemoryFilter(
    key = typedFilterKey,
    filterConstraintExtractor = { doubleValue() },
    matches = matcher
  )
}
