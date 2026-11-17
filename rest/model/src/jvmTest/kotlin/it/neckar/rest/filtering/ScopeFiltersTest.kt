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

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import it.neckar.rest.RestResponseFilters
import org.junit.jupiter.api.Test

class ScopeFiltersTest {
  @Test
  fun `an empty key set yields no scope filter`() {
    assertThat(scopeFiltersOf(TestFilterCriteria.entries, emptySet())).isEmpty()
  }

  @Test
  fun `matching entries keep the enum order`() {
    val scopeFilters = scopeFiltersOf(TestFilterCriteria.entries, setOf(FilterKey("minYear"), FilterKey("country")))

    assertThat(scopeFilters.toList()).containsExactly(TestFilterCriteria.Country, TestFilterCriteria.MinYear)
  }

  @Test
  fun `a key outside the enum is ignored`() {
    val scopeFilters = scopeFiltersOf(TestFilterCriteria.entries, setOf(FilterKey("country"), FilterKey("publisherId")))

    assertThat(scopeFilters.toList()).containsExactly(TestFilterCriteria.Country)
  }

  @Test
  fun `an overridden typed filter key decides the match`() {
    assertThat(scopeFiltersOf(TestFilterCriteria.entries, setOf(FilterKey("search")))).isEmpty()
    assertThat(scopeFiltersOf(TestFilterCriteria.entries, setOf(FilterKey.freeText.filterKey)).toList()).containsExactly(TestFilterCriteria.Search)
  }

  private enum class TestFilterCriteria(
    override val filterType: TypedFilterKey.FilterType = TypedFilterKey.FilterType.String,
  ) : RestResponseFilters {
    Country,
    MinYear(TypedFilterKey.FilterType.Int),
    Search { override val typedFilterKey: TypedFilterKey = FilterKey.freeText },
    ;
  }
}
