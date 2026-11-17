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
import it.neckar.datetime.minimal.LocalDate
import it.neckar.open.formatting.dateTimeFormatIso8601
import it.neckar.open.i18n.I18nConfiguration
import org.junit.jupiter.api.Test
import kotlin.time.Instant

/**
 *
 */
class FilterDescriptorsTest {
  @Test
  fun testAll() {
    val begin: FilterDescriptor = MyFilterCriteria.Begin.withValue(dateTimeFormatIso8601.format(17_000.0, I18nConfiguration.US_UTC))
    val end: FilterDescriptor = MyFilterCriteria.End.withValue(dateTimeFormatIso8601.format(75_000.0, I18nConfiguration.US_UTC))

    assertThat(begin.isoDateTimeValue()).isEqualTo(17_000.0)
    assertThat(end.isoDateTimeValue()).isEqualTo(75_000.0)

    val restFilters: FilterDescriptors<MyFilterCriteria> = FilterDescriptors.from(begin, end)
    assertThat(restFilters.size).isEqualTo(2)
  }

  @Test
  fun testSingle() {
    val begin: FilterDescriptor = MyFilterCriteria.Begin.withValue(dateTimeFormatIso8601.format(17_000.0, I18nConfiguration.US_UTC))

    assertThat(begin.isoDateTimeValue()).isEqualTo(17_000.0)

    val restFilters: FilterDescriptors<MyFilterCriteria> = FilterDescriptors.from(begin)
    assertThat(restFilters.size).isEqualTo(1)
  }

  @Test
  fun testNullSkipped() {
    val begin: FilterDescriptor = MyFilterCriteria.Begin.withValue(dateTimeFormatIso8601.format(17_000.0, I18nConfiguration.US_UTC))

    assertThat(FilterDescriptors.from<MyFilterCriteria>(begin, null).size).isEqualTo(1)
    assertThat(FilterDescriptors.from<MyFilterCriteria>(null).size).isEqualTo(0)
  }

  @Test
  fun testNotConsumed() {
    val begin: FilterDescriptor = MyFilterCriteria.Begin.withValue(dateTimeFormatIso8601.format(17_000.0, I18nConfiguration.US_UTC))

    assertFailure {
      val daConstraintFilterDescriptor = FilterDescriptor(FilterKey("the-other-key").string(), "daConstraintValue")
      FilterDescriptors.from<MyFilterCriteria>(begin, daConstraintFilterDescriptor)
    }.all {
      messageContains("The following filter descriptors have not been consumed")
      messageContains("[the-other-key]")
    }
  }

  private enum class StatusTestEnum {
    active,
    inactive,
    pending
  }

  @Test
  fun testEnumValue() {
    // Valid enum value
    val statusFilterKey = FilterKey("status").enum(StatusTestEnum::class)
    val activeFilter = FilterDescriptor(statusFilterKey, "active")
    assertThat(activeFilter.enumValue()).isEqualTo("active")

    val inactiveFilter = FilterDescriptor(statusFilterKey, "inactive")
    assertThat(inactiveFilter.enumValue()).isEqualTo("inactive")
  }

  @Test
  fun testEnumValueInvalid() {
    val statusFilterKey = FilterKey("status").enum(StatusTestEnum::class)
    val invalidFilter = FilterDescriptor(statusFilterKey, "invalid-value")

    assertFailure {
      invalidFilter.enumValue()
    }.all {
      isInstanceOf(InvalidFilterConstraintException::class)
      messageContains("Invalid filter value [invalid-value] for key [status]. Value must be one of: [active, inactive, pending]")
    }
  }

  @Test
  fun testEmailValue() {
    val descriptor = FilterDescriptor(FilterKey("reviewerEmail").email(), "  Anna@Example.COM ")
    assertThat(descriptor.emailValue().value).isEqualTo("anna@example.com")
  }

  @Test
  fun testEmailValueInvalid() {
    val descriptor = FilterDescriptor(FilterKey("reviewerEmail").email(), "anna")

    assertFailure {
      descriptor.emailValue()
    }.all {
      isInstanceOf(InvalidFilterConstraintException::class)
      messageContains("Email address required")
    }
  }

  @Test
  fun testInstantValue() {
    val descriptor = FilterDescriptor(MyFilterCriteria.Begin, "2026-05-13T12:00:00Z")
    assertThat(descriptor.instantValue()).isEqualTo(Instant.parse("2026-05-13T12:00:00Z"))
  }

  @Test
  fun testInstantValueInvalid() {
    val descriptor = FilterDescriptor(MyFilterCriteria.Begin, "not-a-date")

    assertFailure {
      descriptor.instantValue()
    }.all {
      isInstanceOf(InvalidFilterConstraintException::class)
      messageContains("ISO instant required")
    }
  }

  @Test
  fun testLocalDateValue() {
    val descriptor = FilterDescriptor(MyFilterCriteria.Day, "2026-05-13")
    assertThat(descriptor.localDateValue()).isEqualTo(LocalDate(2026, 5, 13))
  }

  @Test
  fun testLocalDateValueInvalidText() {
    val descriptor = FilterDescriptor(MyFilterCriteria.Day, "not-a-date")

    assertFailure {
      descriptor.localDateValue()
    }.all {
      isInstanceOf(InvalidFilterConstraintException::class)
      messageContains("ISO date")
    }
  }

  @Test
  fun testLocalDateValueInvalidCalendarDay() {
    val descriptor = FilterDescriptor(MyFilterCriteria.Day, "2026-13-99")

    assertFailure {
      descriptor.localDateValue()
    }.all {
      isInstanceOf(InvalidFilterConstraintException::class)
      messageContains("ISO date")
    }
  }

  @Test
  fun testLocalDateValueRejectsInstant() {
    val descriptor = FilterDescriptor(MyFilterCriteria.Day, "2026-05-13T12:00:00Z")

    assertFailure {
      descriptor.localDateValue()
    }.all {
      isInstanceOf(InvalidFilterConstraintException::class)
      messageContains("ISO date")
    }
  }

  @Test
  fun testEnumValueWithWrongFilterType() {
    // Try to call enumValue() on a non-enum filter
    val stringFilterKey = FilterKey("name").string()
    val filter = FilterDescriptor(stringFilterKey, "test")

    assertFailure {
      filter.enumValue()
    }.all {
      isInstanceOf(InvalidFilterConstraintException::class)
      messageContains("Enum filter type required")
    }
  }

  private enum class MyFilterCriteria(
    override val typedFilterKey: TypedFilterKey,
  ) : HasTypedFilterKey {
    Begin(FilterKey("begin").instant()),
    End(FilterKey("end").instant()),
    Day(FilterKey("day").date()),
  }
}
