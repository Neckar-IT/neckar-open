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

import it.neckar.customer.company.CompanyLegalInformation
import it.neckar.customer.company.RegistrationNumber
import it.neckar.customer.company.TaxId
import it.neckar.open.serialization.TrimmedString
import kotlinx.serialization.Serializable

/**
 * Flat representation of [CompanyLegalInformation] for embedding in REST Request DTOs.
 *
 * Free-text fields are typed as [TrimmedString] so trimming happens transparently on REST decode.
 * Typed identifier fields ([TaxId], [RegistrationNumber]) carry their own normalization through
 * their custom serializers. Converts via [toCompanyLegalInformation].
 */
@Serializable
data class CompanyLegalInformationFields(
  val director: TrimmedString,
  val registerCourt: TrimmedString,
  val taxId: TaxId,
  val registrationNumber: RegistrationNumber,
) {
  fun toCompanyLegalInformation(): CompanyLegalInformation = CompanyLegalInformation(
    director = director,
    registerCourt = registerCourt,
    taxId = taxId,
    registrationNumber = registrationNumber,
  )

  companion object {
    fun fromCompanyLegalInformation(info: CompanyLegalInformation): CompanyLegalInformationFields = CompanyLegalInformationFields(
      director = info.director,
      registerCourt = info.registerCourt,
      taxId = info.taxId,
      registrationNumber = info.registrationNumber,
    )
  }
}
