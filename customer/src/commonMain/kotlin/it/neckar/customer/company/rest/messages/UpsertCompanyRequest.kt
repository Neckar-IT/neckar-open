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

import it.neckar.customer.company.BankAccountInformation
import it.neckar.customer.company.Company
import it.neckar.customer.company.ContactInformation
import it.neckar.customer.rest.messages.AddressConfirmedFields
import it.neckar.open.http.Url
import it.neckar.open.serialization.TrimmedString
import kotlinx.serialization.Serializable

/**
 * REST Request DTO for `PUT /companies/{companyId}` and the tenant-information PUT — create
 * or update a company.
 *
 * Free-text fields are typed as [TrimmedString] so trimming happens transparently on REST decode.
 * Address, bank account information and legal information are carried as flat field types to
 * avoid leaking the corresponding domain classes into the REST layer.
 *
 * [ContactInformation] is embedded as-is since it carries only typed value classes
 * ([it.neckar.contact.EmailAddress], [it.neckar.contact.PhoneNumber], [Url]) and no free-text
 * fields that would require trimming.
 */
@Suppress("DEPRECATION")
@Serializable
data class UpsertCompanyRequest(
  val id: Company.Id,
  val namespace: TrimmedString,
  val name: TrimmedString,
  val address: AddressConfirmedFields?,
  val contactInformation: ContactInformation?,
  val bankAccountInformation: BankAccountInformationFields?,
  val legalInformation: CompanyLegalInformationFields?,
) {

  /**
   * Checks [bankAccountInformation] at the REST→Domain boundary. Returns `null` when the request
   * carries no bank account information.
   */
  fun parseBankAccountInformation(): BankAccountInformationParseResult? = bankAccountInformation?.parse()

  /**
   * [parsedBankAccountInformation] is resolved by the caller from [parseBankAccountInformation] so
   * a format violation is answered with a [CompanyValidationError] instead of reaching the domain.
   */
  fun toDomain(parsedBankAccountInformation: BankAccountInformation?): Company = Company(
    id = id,
    namespace = namespace,
    name = name,
    address = address?.toConfirmed(),
    contactInformation = contactInformation,
    bankAccountInformation = parsedBankAccountInformation,
    legalInformation = legalInformation?.toCompanyLegalInformation(),
  )

  companion object {
    fun fromCompany(company: Company): UpsertCompanyRequest = UpsertCompanyRequest(
      id = company.id,
      namespace = company.namespace,
      name = company.name,
      address = company.address?.let { AddressConfirmedFields.fromConfirmed(it) },
      contactInformation = company.contactInformation,
      bankAccountInformation = company.bankAccountInformation?.let { BankAccountInformationFields.fromBankAccountInformation(it) },
      legalInformation = company.legalInformation?.let { CompanyLegalInformationFields.fromCompanyLegalInformation(it) },
    )
  }
}
