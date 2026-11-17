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
package it.neckar.ktor.pagination

import assertk.assertThat
import assertk.assertions.hasMessage
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import io.ktor.http.Parameters
import io.ktor.http.parametersOf
import it.neckar.rest.InvalidQueryParameterException
import it.neckar.rest.pagination.PageIndex
import it.neckar.rest.pagination.PageSize
import it.neckar.rest.sorting.Sort
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/**
 * Tests for [pageIndex] and [pageSize]
 */
class KtorPaginationExtTest {
  @Test
  fun `absent parameters are null`() {
    assertThat(Parameters.Empty.pageIndex()).isNull()
    assertThat(Parameters.Empty.pageSize()).isNull()
  }

  @Test
  fun `valid parameters are parsed`() {
    assertThat(parametersOf("pageIndex", "7").pageIndex()).isEqualTo(PageIndex(7))
    assertThat(parametersOf("pageSize", "50").pageSize()).isEqualTo(PageSize(50))
    assertThat(parametersOf("pageSize", "-1").pageSize()).isEqualTo(PageSize.Unlimited)
  }

  @Test
  fun `a non-numeric page index names the parameter`() {
    val exception = assertThrows<InvalidQueryParameterException> {
      parametersOf("pageIndex", "abc").pageIndex()
    }

    assertThat(exception).hasMessage("Invalid value [abc] for query parameter [pageIndex]. Value must be an integer.")
  }

  @Test
  fun `a non-numeric page size names the parameter`() {
    val exception = assertThrows<InvalidQueryParameterException> {
      parametersOf("pageSize", "").pageSize()
    }

    assertThat(exception).hasMessage("Invalid value [] for query parameter [pageSize]. Value must be an integer.")
  }

  @Test
  fun `an unknown sort order names the parameter`() {
    val exception = assertThrows<InvalidQueryParameterException> {
      parametersOf("sortOrder", "ascending").pageSortOrder()
    }

    assertThat(exception).hasMessage("Invalid sort value [ascending] for key [sortOrder]. Value must be one of: [Asc, Desc]")
  }

  @Test
  fun `a known sort order is parsed in either spelling`() {
    assertThat(parametersOf("sortOrder", "Desc").pageSortOrder()).isEqualTo(Sort.Order.Desc)
    assertThat(parametersOf("sortOrder", "desc").pageSortOrder()).isEqualTo(Sort.Order.Desc)
    assertThat(parametersOf("sortOrder", "ASC").pageSortOrder()).isEqualTo(Sort.Order.Asc)
    assertThat(Parameters.Empty.pageSortOrder()).isNull()
  }

  @Test
  fun `a rejected page size keeps the reason`() {
    val exception = assertThrows<InvalidQueryParameterException> {
      parametersOf("pageSize", "0").pageSize()
    }

    assertThat(exception).hasMessage("Invalid value [0] for query parameter [pageSize]. The page size must be -1 (unlimited) or between 1 and 10000 but was 0")
  }

  @Test
  fun `page sizes at the bounds`() {
    assertThat(parametersOf("pageSize", "1").pageSize()).isEqualTo(PageSize(1))
    assertThat(parametersOf("pageSize", "10000").pageSize()).isEqualTo(PageSize(10_000))

    val exception = assertThrows<InvalidQueryParameterException> {
      parametersOf("pageSize", "10001").pageSize()
    }
    assertThat(exception).hasMessage("Invalid value [10001] for query parameter [pageSize]. The page size must be -1 (unlimited) or between 1 and 10000 but was 10001")
  }

  @Test
  fun `page indices at the bounds`() {
    assertThat(parametersOf("pageIndex", "0").pageIndex()).isEqualTo(PageIndex.Zero)
    assertThat(parametersOf("pageIndex", "1").pageIndex()).isEqualTo(PageIndex(1))
    assertThat(parametersOf("pageIndex", "10000").pageIndex()).isEqualTo(PageIndex(10_000))

    val exception = assertThrows<InvalidQueryParameterException> {
      parametersOf("pageIndex", "-1").pageIndex()
    }
    assertThat(exception).hasMessage("Invalid value [-1] for query parameter [pageIndex]. The page index must be >= 0 but was -1")
  }
}
