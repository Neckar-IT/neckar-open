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
package it.neckar.auth.rest.messages

import it.neckar.contact.EmailAddress
import it.neckar.open.tenancy.Tenant
import it.neckar.rest.RestResponse
import it.neckar.rest.RestResponseFilteredSorted
import it.neckar.rest.RestResponseFilters
import it.neckar.rest.RestResponseSorts
import it.neckar.rest.filtering.TypedFilterKey
import it.neckar.rest.pagination.Page
import it.neckar.rest.pagination.PaginationInfo
import it.neckar.user.UserLoginName
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Response containing users that can be impersonated.
 * Only available for NeckarIT admin users.
 * Supports server-side filtering, sorting, and pagination.
 */
@Serializable
sealed interface ImpersonationTargetsResponse : RestResponseFilteredSorted<List<ImpersonationTarget>, ImpersonationTargetsResponse.FilterCriteria, ImpersonationTargetsResponse.SortCriteria> {

  @Serializable
  @SerialName("good")
  data class Good(
    override val data: List<ImpersonationTarget>,
    override val paginationInfo: PaginationInfo,
  ) : ImpersonationTargetsResponse, RestResponse.PaginatedGoodResponse<List<ImpersonationTarget>>

  companion object {
    fun good(page: Page<ImpersonationTarget>): ImpersonationTargetsResponse = Good(page.pageElements, page.paginationInfo)
  }

  enum class SortCriteria : RestResponseSorts {
    LoginName,

    FirstName,

    LastName,

    Email,
    ;
  }

  enum class FilterCriteria(
    override val filterType: TypedFilterKey.FilterType = TypedFilterKey.FilterType.String,
  ) : RestResponseFilters {
    LoginName,
    FirstName,
    LastName,
    Email,
    TenantName,
    ;
  }
}

/**
 * Represents a user that can be impersonated.
 */
@Serializable
data class ImpersonationTarget(
  val loginName: UserLoginName,
  val firstName: String,
  val lastName: String,
  val email: EmailAddress?,
  val tenantId: Tenant.Id,
  val tenantName: String,
)
