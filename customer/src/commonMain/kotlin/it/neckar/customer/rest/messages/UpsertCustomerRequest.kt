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
package it.neckar.customer.rest.messages

import it.neckar.contact.EmailAddress
import it.neckar.contact.PhoneNumberOrNull
import it.neckar.customer.Customer
import it.neckar.open.serialization.TrimmedString
import it.neckar.open.serialization.TrimmedStringOrNull
import kotlinx.serialization.Serializable

/**
 * REST Request DTO for `PUT /customers/{customerId}` — create or update a customer.
 *
 * Free-text fields are typed as [TrimmedString] / [TrimmedStringOrNull] so trimming happens
 * transparently on REST decode. The address is carried as a flat [AddressConfirmedFields]
 * to avoid leaking the [it.neckar.customer.Address.Confirmed] domain class into the REST layer.
 */
@Serializable
data class UpsertCustomerRequest(
  val id: Customer.Id,
  val firstName: TrimmedString,
  val lastName: TrimmedString,
  val address: AddressConfirmedFields? = null,
  val phone: PhoneNumberOrNull,
  val cellphone: PhoneNumberOrNull,
  val email: EmailAddress?,
  val company: TrimmedStringOrNull = null,
) {
  /**
   * Returns the [Customer] domain object for the upsert.
   */
  fun toDomain(): Customer = Customer(
    id = id,
    firstName = firstName,
    lastName = lastName,
    address = address?.toConfirmed(),
    phone = phone,
    cellphone = cellphone,
    email = email,
    company = company,
  )

  companion object {
    fun fromCustomer(customer: Customer): UpsertCustomerRequest = UpsertCustomerRequest(
      id = customer.id,
      firstName = customer.firstName,
      lastName = customer.lastName,
      address = customer.address?.let { AddressConfirmedFields.fromConfirmed(it) },
      phone = customer.phone,
      cellphone = customer.cellphone,
      email = customer.email,
      company = customer.company,
    )
  }
}
