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
package it.neckar.rest.test

import assertk.*
import assertk.assertions.*
import it.neckar.rest.RestResponse
import it.neckar.rest.pagination.PageIndex
import it.neckar.rest.pagination.PageSize
import it.neckar.rest.pagination.PaginationInfo
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.Test

class RestAssertionsPaginationTest {

  private val goodResponse: MyPagedResponse.Good = MyPagedResponse.Good(
    data = listOf("a", "b", "c"),
    paginationInfo = PaginationInfo(
      pageIndex = PageIndex(2),
      pageSize = PageSize(10),
      pageCount = 17,
      filteredElementsCount = 165,
      totalElementsCount = 200,
    ),
  )

  @Test
  fun testHasPageCount() {
    assertThat(goodResponse).hasPageCount(17)

    assertFailure {
      assertThat(goodResponse).hasPageCount(99)
    }.messageContains("page count")
  }

  @Test
  fun testHasFilteredElementsCount() {
    assertThat(goodResponse).hasFilteredElementsCount(165)

    assertFailure {
      assertThat(goodResponse).hasFilteredElementsCount(999)
    }.messageContains("filtered elements count")
  }

  @Test
  fun testHasTotalElementsCount() {
    assertThat(goodResponse).hasTotalElementsCount(200)

    assertFailure {
      assertThat(goodResponse).hasTotalElementsCount(999)
    }.messageContains("total elements count")
  }

  @Test
  fun testHasPageIndex() {
    assertThat(goodResponse).hasPageIndex(PageIndex(2))

    assertFailure {
      assertThat(goodResponse).hasPageIndex(PageIndex(9))
    }.messageContains("page index")
  }

  @Test
  fun testHasPageSize() {
    assertThat(goodResponse).hasPageSize(PageSize(10))

    assertFailure {
      assertThat(goodResponse).hasPageSize(PageSize(50))
    }.messageContains("page size")
  }

  @Test
  fun testChainable() {
    assertThat(goodResponse)
      .hasPageIndex(PageIndex(2))
      .hasPageSize(PageSize(10))
      .hasPageCount(17)
      .hasFilteredElementsCount(165)
      .hasTotalElementsCount(200)
  }

  @Serializable
  sealed interface MyPagedResponse : RestResponse<List<String>> {
    @Serializable
    @SerialName("good")
    data class Good(
      override val data: List<String>,
      override val paginationInfo: PaginationInfo,
    ) : MyPagedResponse, RestResponse.PaginatedGoodResponse<List<String>>

    @Serializable
    @SerialName("bad/Failed")
    data object Failed : MyPagedResponse, RestResponse.BadResponse<List<String>>
  }
}
