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

import it.neckar.contact.EmailAddress
import it.neckar.contact.PhoneNumberOrNull
import it.neckar.customer.company.Company
import it.neckar.customer.company.CompanyContactPerson
import it.neckar.open.serialization.TrimmedString
import kotlinx.serialization.Serializable

/**
 * REST Request DTO for `PUT /companies/{companyId}/contact-persons/{contactPersonId}` —
 * create or update a company contact person.
 *
 * Free-text fields are typed as [TrimmedString] so trimming happens transparently on REST decode.
 * Replaces the previous wrapper that embedded the [CompanyContactPerson] domain class.
 */
@Serializable
data class UpsertCompanyContactPersonRequest(
  val id: CompanyContactPerson.Id,
  val companyId: Company.Id,
  val firstName: TrimmedString,
  val lastName: TrimmedString,
  val phone: PhoneNumberOrNull = null,
  val cellphone: PhoneNumberOrNull = null,
  val email: EmailAddress? = null,
) {
  fun toDomain(): CompanyContactPerson = CompanyContactPerson(
    id = id,
    companyId = companyId,
    firstName = firstName,
    lastName = lastName,
    phone = phone,
    cellphone = cellphone,
    email = email,
  )

  companion object {
    fun fromContactPerson(contactPerson: CompanyContactPerson): UpsertCompanyContactPersonRequest = UpsertCompanyContactPersonRequest(
      id = contactPerson.id,
      companyId = contactPerson.companyId,
      firstName = contactPerson.firstName,
      lastName = contactPerson.lastName,
      phone = contactPerson.phone,
      cellphone = contactPerson.cellphone,
      email = contactPerson.email,
    )
  }
}
