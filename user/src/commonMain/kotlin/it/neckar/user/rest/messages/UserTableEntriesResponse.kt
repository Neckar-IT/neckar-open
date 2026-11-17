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

import it.neckar.open.tenancy.TenantName
import it.neckar.rest.RestResponse
import it.neckar.rest.RestResponseFilteredSorted
import it.neckar.rest.RestResponseFilters
import it.neckar.rest.RestResponseSorts
import it.neckar.rest.filtering.FilterKey
import it.neckar.rest.filtering.TypedFilterKey
import it.neckar.rest.pagination.Page
import it.neckar.rest.pagination.PaginationInfo
import it.neckar.user.rest.views.UserTableEntry
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * One page of the active users of every tenant.
 */
@Serializable
sealed interface UserTableEntriesResponse :
  RestResponseFilteredSorted<List<UserTableEntry>, UserTableEntriesResponse.FilterCriteria, UserTableEntriesResponse.SortCriteria> {

  val filterState: FilterCriteria.State

  @Serializable
  @SerialName("good")
  data class Good(
    override val data: List<UserTableEntry>,
    override val paginationInfo: PaginationInfo,
    override val filterState: FilterCriteria.State,
  ) : UserTableEntriesResponse, RestResponse.PaginatedGoodResponse<List<UserTableEntry>>

  companion object {
    fun good(page: Page<UserTableEntry>, filterState: FilterCriteria.State): UserTableEntriesResponse =
      Good(page.pageElements, page.paginationInfo, filterState)
  }

  enum class SortCriteria : RestResponseSorts {
    /** Last name, then first name, then id. */
    Name,
    Email,
    CreatedAt,
    ;
  }

  enum class FilterCriteria(
    override val filterType: TypedFilterKey.FilterType = TypedFilterKey.FilterType.String,
  ) : RestResponseFilters {
    /**
     * Words that each occur, ignoring case, in first name, last name, mail address or active tenant name.
     */
    FreeText { override val typedFilterKey: TypedFilterKey = FilterKey.freeText },

    TenantId(TypedFilterKey.FilterType.Uuid),
    ;

    @Serializable
    data class State(
      /** The free text the page applied; null without one. */
      val freeText: String? = null,
      /** The name of the tenant the filter [TenantId] names; null without that filter and for a tenant that no longer exists or is archived. */
      val tenantName: TenantName? = null,
    )
  }
}
