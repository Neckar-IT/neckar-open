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
package it.neckar.rest.pagination

import assertk.*
import assertk.assertions.*
import it.neckar.open.serialization.roundTrip
import it.neckar.rest.RestResponse
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.Test

class PaginatedResponseTest {
  @Test
  fun testResponse() {
    val response = MyFooResponse.good(
      data = listOf("a", "b", "c"),
      paginationInfo = PaginationInfo(
        pageIndex = PageIndex(1),
        pageSize = PageSize(10),
        pageCount = 17,
        filteredElementsCount = 3,
        totalElementsCount = 100,
      )
    )
    assertThat(response.isGood).isTrue()
    assertThat(response.asGood().data).isEqualTo(listOf("a", "b", "c"))

    roundTrip(response, MyFooResponse.serializer()) {
      //language=JSON
      """
        {
          "type" : "good",
          "data" : [ "a", "b", "c" ],
          "paginationInfo" : {
            "pageIndex" : 1,
            "pageSize" : 10,
            "pageCount" : 17,
            "filteredElementsCount" : 3,
            "totalElementsCount" : 100
          }
        }
      """.trimIndent()
    }
  }

  @Test
  fun testResponseDefaults() {
    val response = MyFooResponse.createFor(
      data = listOf("a", "b", "c"),
    )
    assertThat(response.isGood).isTrue()
    assertThat(response.asGood().data).isEqualTo(listOf("a", "b", "c"))

    roundTrip(response, MyFooResponse.serializer()) {
      //language=JSON
      """
        {
          "type" : "good",
          "data" : [ "a", "b", "c" ],
          "paginationInfo" : {
            "pageIndex" : 0,
            "pageSize" : -1,
            "pageCount" : 1,
            "filteredElementsCount" : 3,
            "totalElementsCount" : 3
          }
        }
      """.trimIndent()
    }
  }


  /**
   * Response for
   */
  @Serializable
  sealed interface MyFooResponse : RestResponse<List<String>> {
    @Serializable
    @SerialName("good")
    data class Good(
      override val data: List<String>,
      override val paginationInfo: PaginationInfo,
    ) : MyFooResponse, RestResponse.PaginatedGoodResponse<List<String>>

    @Serializable
    @SerialName("bad/Failed")
    data object Failed : MyFooResponse, RestResponse.BadResponse<List<String>>

    companion object {
      /**
       * Helper method to create a success object - with the correct type
       */
      fun good(data: List<String>, paginationInfo: PaginationInfo): MyFooResponse = Good(data, paginationInfo)

      /**
       * Helper method to create a failure object - with the correct type
       */
      fun bad(): MyFooResponse = Failed

      /**
       * Creates a success method for non-null results, failure for null results
       */
      fun createFor(data: List<String>?, totalElementsCount: Int = data?.size ?: 0): MyFooResponse {
        return if (data != null) {
          good(data, PaginationInfo.singlePageFromList(data, totalElementsCount))
        } else {
          bad()
        }
      }
    }
  }
}
