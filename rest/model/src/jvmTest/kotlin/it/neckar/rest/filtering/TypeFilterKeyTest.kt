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

import assertk.*
import assertk.assertions.*
import it.neckar.open.kotlin.lang.className
import org.junit.jupiter.api.Test
import kotlin.reflect.full.createType

/**
 *
 */
class TypeFilterKeyTest {
  @Test
  fun testKotlinEnumConvenienceFunction() {
    // Test with Kotlin enum class
    val key = FilterKey("status").enum(TestEnum::class)

    assertThat(key.type).isInstanceOf<TypedFilterKey.FilterType.Enum>()
    val enum = key.type as TypedFilterKey.FilterType.Enum

    assertThat(enum.allowedValues).containsExactly("ACTIVE", "INACTIVE", "PENDING")
    assertThat(enum.enumType).isEqualTo(TestEnum::class.className)
  }

  @Test
  fun testComplexType() {
    val key = TypedFilterKey(
      filterKey = FilterKey("testKey"),
      type = TypedFilterKey.FilterType.Complex(FoobarEnum::class.createType()),
    )

    val complex = key.type as TypedFilterKey.FilterType.Complex
    assertThat(complex.complexType.toString()).isEqualTo("it.neckar.rest.filtering.TypeFilterKeyTest.FoobarEnum")
  }

  @Test
  fun testOtherConstructor() {
    assertThat(FilterKey("testKey").string()).isEqualTo(
      TypedFilterKey(
        filterKey = FilterKey("testKey"),
        type = TypedFilterKey.FilterType.String,
      )
    )

    assertThat(FilterKey("testKey").boolean()).isEqualTo(
      TypedFilterKey(
        filterKey = FilterKey("testKey"),
        type = TypedFilterKey.FilterType.Boolean,
      )
    )

    assertThat(FilterKey("testKey").instant()).isEqualTo(
      TypedFilterKey(
        filterKey = FilterKey("testKey"),
        type = TypedFilterKey.FilterType.Instant,
      )
    )

    assertThat(FilterKey("testKey").date()).isEqualTo(
      TypedFilterKey(
        filterKey = FilterKey("testKey"),
        type = TypedFilterKey.FilterType.Date,
      )
    )
  }

  @Test
  fun testEnumWithKotlinEnum() {
    // Test extracting values from a Kotlin enum class
    val key = FilterKey("status").enum(TestEnum::class)

    assertThat(key.filterKey.value).isEqualTo("status")
    val enumType = key.type as TypedFilterKey.FilterType.Enum

    assertThat(enumType.allowedValues).containsExactlyInAnyOrder("ACTIVE", "INACTIVE", "PENDING")
  }

  @Test
  fun testEnumWithReifiedTypeParameter() {
    // Test inline reified variant
    val key = FilterKey("priority").enum<TestEnum>()

    assertThat(key.filterKey.value).isEqualTo("priority")
    val enumType = key.type as TypedFilterKey.FilterType.Enum
    assertThat(enumType.allowedValues).containsExactlyInAnyOrder("ACTIVE", "INACTIVE", "PENDING")
  }

  private enum class FoobarEnum {
    Foo,
    Bar,
  }

  private enum class TestEnum {
    ACTIVE,
    INACTIVE,
    PENDING
  }
}
