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
import it.neckar.contact.EmailAddress
import it.neckar.datetime.minimal.LocalDate
import it.neckar.open.formatting.DateTimeFormatIso8601
import it.neckar.open.unit.si.ms
import kotlin.time.Instant
import kotlin.uuid.Uuid

/**
 * A static descriptor for a filter.
 * Does *not* contain code to filter the items itself.
 *
 * Can be used to instantiate a [InMemoryFilter] object
 */
data class FilterDescriptor(
  /**
   * The filter key
   */
  override val typedFilterKey: TypedFilterKey,

  /**
   * The string value representation to filter by
   * Will be parsed if necessary.
   *
   * Attention: Empty strings are not allowed!"
   */
  val constraint: String,
) : HasTypedFilterKey {

  init {
    require(constraint.isNotEmpty()) { "Constraint must not be empty for filter key: ${typedFilterKey.filterKey.value} but was [$constraint]" }
  }

  /**
   * Returns the constraint as double.
   * Throws an exception if the value is not a valid double
   */
  fun doubleValue(): Double {
    return constraint.toDoubleOrNull() ?: throw InvalidFilterConstraintException(typedFilterKey, constraint, "Double value required")
  }

  /**
   * Returns the constraint as boolean.
   */
  fun booleanValue(): Boolean {
    return constraint.toBooleanStrictOrNull() ?: throw InvalidFilterConstraintException(typedFilterKey, constraint, "Boolean value required")
  }

  fun intValue(): Int {
    return constraint.toIntOrNull() ?: throw InvalidFilterConstraintException(typedFilterKey, constraint, "Int value required")
  }

  fun longValue(): Long {
    return constraint.toLongOrNull() ?: throw InvalidFilterConstraintException(typedFilterKey, constraint, "Long value required")
  }

  /**
   * Returns the value parsed as UUID.
   */
  fun uuidValue(): Uuid {
    try {
      return Uuid.parse(constraint.trim())
    } catch (e: Exception) {
      throw InvalidFilterConstraintException(typedFilterKey, constraint, "UUID required", e)
    }
  }

  /**
   * Returns the value parsed as [EmailAddress].
   * Throws [InvalidFilterConstraintException] if the value is not a valid email address.
   */
  fun emailValue(): EmailAddress {
    try {
      return EmailAddress(constraint)
    } catch (e: IllegalArgumentException) {
      throw InvalidFilterConstraintException(typedFilterKey, constraint, "Email address required", e)
    }
  }

  /**
   * Parses the value as an ISO-8601 [Instant].
   * Throws [InvalidFilterConstraintException] if the value is not a valid ISO instant.
   */
  fun instantValue(): Instant {
    try {
      return Instant.parse(constraint)
    } catch (e: Exception) {
      throw InvalidFilterConstraintException(typedFilterKey, constraint, "ISO instant required but was [$constraint]", e)
    }
  }

  /**
   * Parses the value as an ISO calendar date (`yyyy-MM-dd`) — a timezone-free calendar day.
   * Any interpretation as an [Instant] requires an explicitly chosen time zone.
   * Throws [InvalidFilterConstraintException] if the value is not a valid ISO date.
   */
  fun localDateValue(): LocalDate {
    try {
      return LocalDate.parse(constraint)
    } catch (e: Exception) {
      throw InvalidFilterConstraintException(typedFilterKey, constraint, "ISO date (yyyy-MM-dd) required but was [$constraint]", e)
    }
  }

  /**
   * Legacy — new consumers use [instantValue] instead.
   *
   * Parses the value as an ISO date time and returns milliseconds since epoch.
   * Throws [InvalidFilterConstraintException] if the value is not a valid ISO date time.
   */
  fun isoDateTimeValue(): @ms Double {
    try {
      return DateTimeFormatIso8601.parse(constraint)
    } catch (e: Exception) {
      throw InvalidFilterConstraintException(typedFilterKey, constraint, "ISO date time required", e)
    }
  }

  /**
   * Returns the constraint value for an enum filter.
   * Validates that the value is one of the allowed values.
   * Throws an exception if the value is not in the allowed list.
   */
  fun enumValue(): String {
    val enumType = typedFilterKey.type as? TypedFilterKey.FilterType.Enum
      ?: throw InvalidFilterConstraintException(typedFilterKey, constraint, "Enum filter type required")

    if (constraint !in enumType.allowedValues) {
      throw InvalidFilterConstraintException(
        key = typedFilterKey,
        value = constraint,
        hint = "Value must be one of: [${enumType.allowedValues.joinToString(", ")}]"
      )
    }

    return constraint
  }

  override fun toString(): String {
    return "${typedFilterKey.filterKey.value}: $constraint [${typedFilterKey.type}]"
  }

  companion object {
    operator fun invoke(
      hasTypedFilterKey: HasTypedFilterKey,
      constraint: String,
    ): FilterDescriptor {
      return FilterDescriptor(hasTypedFilterKey.typedFilterKey, constraint)
    }
  }
}

/**
 * Helper method to extract the filter descriptor from the HTTP parameters
 */
fun Parameters.filterDescriptorFromParameter(filterKey: TypedFilterKey): FilterDescriptor? {
  return this[filterKey]?.let {
    val constraint = it.trim()
    if (constraint.isEmpty()) {
      return@let null
    }
    FilterDescriptor(filterKey, constraint)
  }
}

/**
 * Extracts the filter descriptors from the url parameters.
 *
 * @param FilterCriteriaType the type of the filter criteria (usually the enum `FilterCriteria` declared within the response)
 */
inline fun <reified FilterCriteriaType> filterDescriptorsFromParameters(parameters: Parameters): FilterDescriptors<FilterCriteriaType>
  where FilterCriteriaType : Enum<FilterCriteriaType>, FilterCriteriaType : HasTypedFilterKey {

  return FilterDescriptors.create<FilterCriteriaType> { enumValue ->
    parameters.filterDescriptorFromParameter(enumValue.typedFilterKey)
  }
}
