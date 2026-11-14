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

import it.neckar.open.kotlin.lang.className
import it.neckar.open.kotlin.lang.enumEntries
import it.neckar.rest.filtering.TypedFilterKey.FilterType.Enum
import kotlin.reflect.KClass
import kotlin.reflect.full.createType

actual operator fun TypedFilterKey.Companion.invoke(key: FilterKey, clazz: KClass<*>): TypedFilterKey {
  return TypedFilterKey(
    filterKey = key,
    type = TypedFilterKey.FilterType.Complex(
      complexType = clazz.createType()
    )
  )
}

/**
 * Creates a TypedFilterKey with enum values extracted from a Kotlin enum class.
 * The enum constant names are used as the allowed values.
 *
 * @param enumClass The Kotlin enum class to extract values from
 * @return TypedFilterKey with FilterType.KotlinEnum containing all enum constant names
 */
fun <T : kotlin.Enum<T>> FilterKey.enum(enumClass: KClass<T>): TypedFilterKey {
  return TypedFilterKey(
    filterKey = this,
    type = Enum(
      enumType = enumClass.className,
      allowedValues = enumClass.enumEntries.map { it.name },
    )
  )
}

/**
 * Creates a TypedFilterKey with enum values extracted from a Kotlin enum class.
 * The enum constant names are used as the allowed values.
 *
 * This is an inline reified variant that allows you to use the type parameter directly:
 * ```
 * FilterKey("status").enum<Status>()
 * ```
 *
 * @return TypedFilterKey with FilterType.Enum containing all enum constant names
 */
inline fun <reified T : kotlin.Enum<T>> FilterKey.enum(): TypedFilterKey {
  return enum(T::class)
}

/**
 * Creates a FilterType.Enum with enum values extracted from a Kotlin enum class.
 * The enum constant names are used as the allowed values.
 *
 * This is an inline reified variant that allows you to use the type parameter directly:
 * ```
 * Enum<Status>()
 * ```
 */
inline operator fun <reified T : kotlin.Enum<T>> Enum.Companion.invoke(enumClass: KClass<T> = T::class): Enum {
  return Enum(
    enumType = enumClass.className,
    allowedValues = enumClass.enumEntries.map { it.name },
  )
}

/**
 * Creates a [TypedFilterKey.FilterType.Enum] for use as `filterType` constructor parameter
 * in `RestResponseFilters` enums.
 *
 * ```kotlin
 * enum class FilterCriteria(
 *   override val filterType: TypedFilterKey.FilterType = TypedFilterKey.FilterType.String,
 * ) : RestResponseFilters {
 *   ConfirmationStatus(enumFilterType<ConfirmationStatus>()),
 * }
 * ```
 */
inline fun <reified T : kotlin.Enum<T>> enumFilterType(): TypedFilterKey.FilterType.Enum {
  return Enum(
    enumType = T::class.className,
    allowedValues = T::class.enumEntries.map { it.name },
  )
}
