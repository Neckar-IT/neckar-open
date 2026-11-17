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
package it.neckar.customer.rest.views

import it.neckar.contact.EmailAddress
import it.neckar.contact.PhoneNumber
import it.neckar.customer.Address
import it.neckar.customer.Customer
import it.neckar.open.annotations.DataView
import kotlinx.serialization.Serializable

/**
 * REST projection of a [Customer].
 *
 * Carries all editable fields plus identity for detail and edit views. The
 * [Customer] domain class never crosses the REST boundary — this DataView is
 * the wire-format representation. The customer address is carried as the
 * [Address.Confirmed] value type which is itself REST-safe (only typed
 * value-class fields, no domain logic).
 */
@DataView("Full customer details for detail/edit views")
@Serializable
data class CustomerDetails(
  val id: Customer.Id,
  val firstName: String,
  val lastName: String,
  val address: Address.Confirmed?,
  val phone: PhoneNumber?,
  val cellphone: PhoneNumber?,
  val email: EmailAddress?,
  val company: String?,
)

/**
 * Converts a [Customer] domain object to a [CustomerDetails] for REST output.
 */
fun Customer.toDetails(): CustomerDetails = CustomerDetails(
  id = id,
  firstName = firstName,
  lastName = lastName,
  address = address,
  phone = phone,
  cellphone = cellphone,
  email = email,
  company = company,
)

/**
 * Reconstructs the [Customer] domain object from a [CustomerDetails]. Used at
 * persistence boundaries where the configuration class carries the projection
 * but the entity / database representation requires the domain class.
 */
fun CustomerDetails.toDomain(): Customer = Customer(
  id = id,
  firstName = firstName,
  lastName = lastName,
  address = address,
  phone = phone,
  cellphone = cellphone,
  email = email,
  company = company,
)
