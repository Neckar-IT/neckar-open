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
package it.neckar.customer.company.rest.messages

import it.neckar.rest.RestResponse
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Response for the company-upsert endpoints (`PUT /property-developers/{id}`,
 * `PUT /tenant-information`). Answers with a [ValidationFailure] naming the field when the
 * submitted [BankAccountInformationFields] violates the IBAN/BIC format, instead of failing while
 * the request body is still being deserialized.
 */
@Serializable
sealed interface UpsertCompanyResponse : RestResponse<Unit> {

  @Serializable
  @SerialName("good")
  data object Good : UpsertCompanyResponse, RestResponse.GoodResponse<Unit> {
    override val data: Unit = Unit
  }

  @Serializable
  @SerialName("bad/ValidationFailure")
  data class ValidationFailure(val errors: List<CompanyValidationError>) : UpsertCompanyResponse, RestResponse.BadResponse<Unit>

  companion object {
    fun good(): UpsertCompanyResponse = Good

    fun validationFailure(errors: List<CompanyValidationError>): UpsertCompanyResponse = ValidationFailure(errors)
  }
}
