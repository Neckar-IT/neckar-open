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
package it.neckar.customer.company.rest.views

import it.neckar.customer.Address
import it.neckar.customer.company.BankAccountInformation
import it.neckar.customer.company.Company
import it.neckar.customer.company.CompanyLegalInformation
import it.neckar.customer.company.ContactInformation
import it.neckar.lifeCycle.ArchiveState
import it.neckar.open.annotations.DataView
import it.neckar.open.http.Url
import kotlinx.serialization.Serializable

/**
 * REST projection of a [Company].
 *
 * Carries all editable fields plus identity for detail and edit views. The
 * [Company] domain class never crosses the REST boundary — this DataView is
 * the wire-format representation. Sub-types ([Address.Confirmed],
 * [ContactInformation], [BankAccountInformation], [CompanyLegalInformation])
 * are themselves REST-safe value containers (typed value-class fields, no
 * domain logic).
 */
@Suppress("DEPRECATION")
@DataView("Full company details for detail/edit views")
@Serializable
data class CompanyDetails(
  val id: Company.Id,
  val namespace: String,
  val name: String,
  val address: Address.Confirmed?,
  val contactInformation: ContactInformation?,
  val bankAccountInformation: BankAccountInformation?,
  val legalInformation: CompanyLegalInformation?,
  val logoUrl: Url,
  val archiveState: ArchiveState,
)

/**
 * Converts a [Company] domain object to a [CompanyDetails] for REST output.
 */
@Suppress("DEPRECATION")
fun Company.toDetails(): CompanyDetails = CompanyDetails(
  id = id,
  namespace = namespace,
  name = name,
  address = address,
  contactInformation = contactInformation,
  bankAccountInformation = bankAccountInformation,
  legalInformation = legalInformation,
  logoUrl = logoUrl,
  archiveState = archiveState,
)

/**
 * Reconstructs the [Company] domain object from a [CompanyDetails]. Used at
 * persistence boundaries where the configuration class carries the projection
 * but the entity / database representation requires the domain class.
 */
@Suppress("DEPRECATION")
fun CompanyDetails.toDomain(): Company = Company(
  id = id,
  namespace = namespace,
  name = name,
  address = address,
  contactInformation = contactInformation,
  bankAccountInformation = bankAccountInformation,
  legalInformation = legalInformation,
  logoUrl = logoUrl,
  archiveState = archiveState,
)
