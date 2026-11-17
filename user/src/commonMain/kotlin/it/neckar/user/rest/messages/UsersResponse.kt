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
package it.neckar.user.rest.messages

import it.neckar.rest.RestResponse
import it.neckar.rest.RestResponseFilteredSorted
import it.neckar.rest.RestResponseFilters
import it.neckar.rest.RestResponseSorts
import it.neckar.rest.filtering.TypedFilterKey
import it.neckar.rest.pagination.Page
import it.neckar.rest.pagination.PaginationInfo
import it.neckar.user.rest.views.UserDetails
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Response for a request returning multiple users
 */
@Serializable
sealed interface UsersResponse : RestResponseFilteredSorted<List<UserDetails>, UsersResponse.FilterCriteria, UsersResponse.SortCriteria> {

  @Serializable
  @SerialName("good")
  data class Good(
    override val data: List<UserDetails>,
    override val paginationInfo: PaginationInfo,
  ) : UsersResponse, RestResponse.PaginatedGoodResponse<List<UserDetails>>

  companion object {
    /**
     * Helper method to create a success object - with the common type
     */
    fun good(page: Page<UserDetails>): UsersResponse = Good(page.pageElements, page.paginationInfo)
  }

  enum class SortCriteria : RestResponseSorts {
    FirstName,

    LastName,

    Login,

    Email,
    ;
  }

  /**
   * The filter keys for the [UsersResponse]
   */
  enum class FilterCriteria(
    override val filterType: TypedFilterKey.FilterType = TypedFilterKey.FilterType.String,
  ) : RestResponseFilters {
    FirstName,
    LastName,
    Login,
    Email,
  }
}
