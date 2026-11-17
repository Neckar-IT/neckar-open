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
import it.neckar.open.http.UrlParameterName
import it.neckar.reflect.ClassName
import it.neckar.rest.filtering.TypedFilterKey.Companion
import kotlin.reflect.KClass
import kotlin.reflect.KType

/**
 * Represents a filter key together with its native value type.
 *
 * Not serialized over the wire — the marker lives only in Kotlin to drive
 * OpenAPI parameter generation and routing. Filter values travel as URL
 * parameters (raw strings) via [Parameters.get] / [HttpRequestBuilder.filterBy].
 */
data class TypedFilterKey(
  override val filterKey: FilterKey,
  val type: FilterType,
): HasFilterKey {

  sealed interface FilterType {
    /** Free text: matches a part of the stored value, ignoring case. */
    data object String : FilterType

    /** An email address: matches the whole normalized address, ignoring case. */
    data object Email : FilterType

    data object Boolean : FilterType
    data object Int : FilterType

    /** A whole number wider than [Int] — an amount of money bounded in cents is one. */
    data object Long : FilterType

    data object Double : FilterType

    /** An id: matches the whole value. */
    data object Uuid : FilterType

    data object Instant : FilterType

    data object Date : FilterType

    /**
     * Represents a Kotlin enum type filter.
     * Used for filters that reference a Kotlin enum class and should generate
     * OpenAPI schema references to the enum type.
     */
    data class Enum(
      /**
       * The enum type that is represented by this filter
       */
      val enumType: ClassName,

      /**
       * The list of allowed enum values
       */
      val allowedValues: List<kotlin.String>,

      ) : FilterType {

      init {
        require(allowedValues.isNotEmpty()) { "Enum filter must have at least one allowed value" }
      }
    }

    /**
     * Represents an advanced class (serialized as JSON)
     */
    data class Complex(
      val complexType: KType
    ) : FilterType

    companion object {
      /**
       * Backwards-compatibility alias for the renamed [Instant] marker.
       * Maintained so that maintenance-only modules compile without edits.
       */
      @Deprecated("Renamed to Instant", ReplaceWith("Instant"))
      val DateTime: Instant = Instant
    }
  }

  override fun toString(): String {
    return "TypedFilterKey(key=$filterKey, type=$type)"
  }

  companion object
}

expect operator fun Companion.invoke(key: FilterKey, clazz: KClass<*>): TypedFilterKey


fun FilterKey.string(): TypedFilterKey {
  return TypedFilterKey(
    this,
    TypedFilterKey.FilterType.String,
  )
}

fun FilterKey.email(): TypedFilterKey {
  return TypedFilterKey(
    this,
    TypedFilterKey.FilterType.Email,
  )
}

fun FilterKey.uuid(): TypedFilterKey {
  return TypedFilterKey(
    this,
    TypedFilterKey.FilterType.Uuid,
  )
}
fun FilterKey.instant(): TypedFilterKey {
  return TypedFilterKey(
    this,
    TypedFilterKey.FilterType.Instant,
  )
}

fun FilterKey.date(): TypedFilterKey {
  return TypedFilterKey(
    this,
    TypedFilterKey.FilterType.Date,
  )
}

fun FilterKey.double(): TypedFilterKey {
  return TypedFilterKey(
    this,
    TypedFilterKey.FilterType.Double,
  )
}

fun FilterKey.int(): TypedFilterKey {
  return TypedFilterKey(
    this,
    TypedFilterKey.FilterType.Int,
  )
}

fun FilterKey.long(): TypedFilterKey {
  return TypedFilterKey(
    this,
    TypedFilterKey.FilterType.Long,
  )
}

fun FilterKey.boolean(): TypedFilterKey {
  return TypedFilterKey(
    this,
    TypedFilterKey.FilterType.Boolean,
  )
}


inline operator fun Parameters.get(name: TypedFilterKey): String? {
  return get(name.filterKey.value)
}

/**
 * Creates a url parameter name matching this [FilterKey]
 */
fun TypedFilterKey.toUrlParameterName(): UrlParameterName {
  return filterKey.toUrlParameterName()
}

/**
 * Associates a filter key with a string value - which is parsed
 */
fun HasTypedFilterKey.withValue(valueAsString: String): FilterDescriptor {
  return FilterDescriptor(this, valueAsString)
}
