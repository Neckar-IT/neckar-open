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
package it.neckar.ktor.client.pagination

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import io.ktor.client.request.HttpRequestBuilder
import it.neckar.rest.RestResponseSorts
import it.neckar.rest.pagination.PageIndex
import it.neckar.rest.pagination.PageQuery
import it.neckar.rest.pagination.PageSize
import it.neckar.rest.sorting.Sort
import org.junit.jupiter.api.Test

class KtorClientPaginationExtTest {
  enum class BookSorts : RestResponseSorts {
    Title,
    PublishedAt,
  }

  @Test
  fun `the request carries the sort criterion and its order`() {
    val parameters = parametersOf(PageQuery(PageIndex(2), PageSize(20), BookSorts.Title, Sort.Order.Desc))

    assertThat(parameters["pageIndex"]).isEqualTo("2")
    assertThat(parameters["pageSize"]).isEqualTo("20")
    assertThat(parameters["sortBy"]).isEqualTo("Title")
    assertThat(parameters["sortOrder"]).isEqualTo("Desc")
  }

  @Test
  fun `a query without a sort criterion sends no order`() {
    val parameters = parametersOf(PageQuery<BookSorts>(PageIndex.Zero, PageSize(20), sortBy = null))

    assertThat(parameters["pageIndex"]).isEqualTo("0")
    assertThat(parameters["sortBy"]).isNull()
    assertThat(parameters["sortOrder"]).isNull()
  }

  private fun parametersOf(pageQuery: PageQuery<BookSorts>): Map<String, String> {
    val builder = HttpRequestBuilder()
    builder.requestPage(pageQuery)
    return builder.url.parameters.entries().associate { it.key to it.value.single() }
  }
}
