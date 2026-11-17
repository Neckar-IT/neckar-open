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
@file:UseSerializers(UuidSerializer::class)

package it.neckar.financial.quote.rest.views

import it.neckar.customer.company.rest.views.CompanyDetails
import it.neckar.customer.company.rest.views.toDetails
import it.neckar.customer.company.rest.views.toDomain
import it.neckar.customer.rest.views.CustomerDetails
import it.neckar.customer.rest.views.toDetails
import it.neckar.customer.rest.views.toDomain
import it.neckar.financial.currency.Money
import it.neckar.financial.priced.PricedElements
import it.neckar.financial.quote.Quote
import it.neckar.financial.quote.QuoteLegalNotice
import it.neckar.financial.quote.QuoteSources
import it.neckar.open.annotations.DataView
import it.neckar.open.annotations.GermanTerm
import it.neckar.open.unit.currency.EUR
import it.neckar.open.unit.time.Months
import it.neckar.uuid.UuidSerializer
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.UseSerializers
import kotlin.uuid.Uuid

/**
 * REST projection of a [Quote].
 *
 * Mirrors the domain class field-for-field, except `sellingCompany` is
 * projected to [CompanyDetails] and `customer` to [CustomerDetails] so
 * the [it.neckar.customer.company.Company] and [it.neckar.customer.Customer]
 * domain classes never cross the REST boundary.
 *
 * Nested types ([PricedElements] tree, [QuoteSources], [QuoteLegalNotice])
 * carry no domain-class references and are reused as-is.
 */
@DataView("Full quote for detail responses")
@Serializable
data class QuoteDetails(
  val id: Quote.Id,
  val sellingCompany: CompanyDetails,
  val belongsTo: Uuid,
  val title: String,
  val maintainerName: String,
  val customer: CustomerDetails,
  val pricedElements: PricedElements,
  val sources: QuoteSources,
  val creationDate: LocalDate,
  @GermanTerm("Gültig bis")
  val validUntil: LocalDate,
  @GermanTerm("Angebotsnummer")
  val quoteNumber: String,
  @GermanTerm("Lieferzeit in Monaten")
  val deliveryTimeInMonths: @Months Int,
  val legalNotice: QuoteLegalNotice,
) {
  @Transient
  val netPrice: @EUR Money.Net = pricedElements.sums.net
}

/**
 * Converts a [Quote] domain object to a [QuoteDetails] for REST output.
 */
fun Quote.toDetails(): QuoteDetails = QuoteDetails(
  id = id,
  sellingCompany = sellingCompany.toDetails(),
  belongsTo = belongsTo,
  title = title,
  maintainerName = maintainerName,
  customer = customer.toDetails(),
  pricedElements = pricedElements,
  sources = sources,
  creationDate = creationDate,
  validUntil = validUntil,
  quoteNumber = quoteNumber,
  deliveryTimeInMonths = deliveryTimeInMonths,
  legalNotice = legalNotice,
)

/**
 * Reconstructs a [Quote] domain object from a [QuoteDetails]. Used at the
 * persistence boundary where the project-configuration domain classes carry
 * the projection but the persistence/entity layer expects the [Quote] domain
 * class.
 */
fun QuoteDetails.toDomain(): Quote = Quote(
  id = id,
  sellingCompany = sellingCompany.toDomain(),
  belongsTo = belongsTo,
  title = title,
  maintainerName = maintainerName,
  customer = customer.toDomain(),
  pricedElements = pricedElements,
  sources = sources,
  creationDate = creationDate,
  validUntil = validUntil,
  quoteNumber = quoteNumber,
  deliveryTimeInMonths = deliveryTimeInMonths,
  legalNotice = legalNotice,
)
