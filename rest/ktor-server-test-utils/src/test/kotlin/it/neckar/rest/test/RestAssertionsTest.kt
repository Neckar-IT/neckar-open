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
@file:Suppress("RETURN_VALUE_NOT_USED")

package it.neckar.rest.test

import assertk.*
import assertk.assertions.*
import it.neckar.rest.RestResponse
import it.neckar.rest.pagination.Page
import it.neckar.rest.pagination.PaginationInfo
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.Test

/**
 *
 */
class RestAssertionsTest {
  @Test
  fun testAssertionsSuccess() {
    //Declared to the sealed interface type
    val data = listOf("a", "b")
    val response: MyTestResponse = MyTestResponse.good(Page.single(data))

    assertThat(response).isGood()
    assertThat(response).isGood().isDataEqualTo(data)
    assertThat(response).isGood().assertData().hasSize(2)
    assertThat(response).isGood().assertData().isNotNull().isInstanceOf(List::class).hasSize(2)
  }

  @Test
  fun testAssertionsFailure() {
    //Declared to the sealed interface type
    val response: MyTestResponse = MyTestResponse.bad()

    assertThat(response).isBad(MyTestResponse.Failed)
  }

  @Test
  fun testMoreData() {
    val data = listOf("a", "b")
    val response: MyTestResponse = MyTestResponse.good(Page.single(data))

    assertThat(response).isGood()
      .assertDataTyped<List<String>>()
      .hasSize(2)

    assertThat(response).isGood()
      .assertData()
      .isNotNull()
      .isInstanceOf<List<String>>()
      .hasSize(2)
  }

  /**
   * Response for
   */
  @Serializable
  sealed interface MyTestResponse : RestResponse<List<String>> {

    @Serializable
    @SerialName("good")
    data class Good(
      override val data: List<String>,
      override val paginationInfo: PaginationInfo,
    ) : MyTestResponse, RestResponse.PaginatedGoodResponse<List<String>>

    @Serializable
    @SerialName("bad/Failed")
    data object Failed : MyTestResponse, RestResponse.BadResponse<List<String>>

    companion object {
      /**
       * Helper method to create a success object - with the correct type
       */
      fun good(data: List<String>, paginationInfo: PaginationInfo): MyTestResponse = Good(data, paginationInfo)
      fun good(page: Page<String>): MyTestResponse = Good(page.pageElements, page.paginationInfo)

      /**
       * Helper method to create a failure object - with the correct type
       */
      fun bad(): MyTestResponse = Failed
    }
  }

}

