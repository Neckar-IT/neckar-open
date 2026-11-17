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

package it.neckar.financial.quote

import it.neckar.customer.Customer
import it.neckar.customer.company.Company
import it.neckar.financial.currency.Money
import it.neckar.financial.now
import it.neckar.financial.priced.PricedElements
import it.neckar.open.annotations.GermanTerm
import it.neckar.open.unit.currency.EUR
import it.neckar.open.unit.si.ms
import it.neckar.open.unit.time.Months
import it.neckar.open.unit.time.d
import it.neckar.uuid.NativeUuid
import it.neckar.uuid.UuidSerializer
import it.neckar.uuid.ValueClassUuid
import it.neckar.uuid.randomForEnvironment
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.jvm.JvmInline
import kotlin.uuid.Uuid

/**
 * The default duration a quote is valid
 */
@GermanTerm("Standard Gültigkeitsdauer für ein Angebot")
const val DefaultQuoteValidityDuration: @d Int = 30

/**
 * The default delivery time
 */
@GermanTerm("Standard Lieferzeit in Monaten")
const val DefaultDeliveryTimeInMonths: @Months Int = 5

/**
 * Until when a quote created on [creationDate] is valid, unless it carries a date of its own.
 */
fun defaultValidUntil(creationDate: LocalDate): LocalDate = creationDate.plus(DefaultQuoteValidityDuration, DateTimeUnit.DAY)

/**
 * Represents an actual quote.
 * A quote does *not* contain economics calculations
 */
@Serializable
data class Quote(
  val id: Id,

  /**
   * The company the quote has been created by
   */
  val sellingCompany: Company,

  /**
   * The UUID this quote belongs to (usually the project configuration id).
   * This should only be used for debugging / tracing purposes.
   */
  val belongsTo: Uuid,

  /**
   * The title of the quote
   */
  val title: String,

  /**
   * The name of the maintainer of the project
   */
  val maintainerName: String,

  /**
   * The customer
   */
  val customer: Customer,

  val pricedElements: PricedElements,

  /**
   * The sources of this quote
   */
  val sources: QuoteSources,

  /**
   * The creation date
   */
  val creationDate: LocalDate,

  /**
   * Until when the quote is valid
   */
  @GermanTerm("Gültig bis")
  val validUntil: LocalDate,

  /**
   * More human-readable ID for this [Quote]
   */
  @GermanTerm("Angebotsnummer")
  val quoteNumber: String,

  /**
   * Additional information about delivery time
   */
  //TODO use Period?
  @GermanTerm("Lieferzeit in Monaten")
  val deliveryTimeInMonths: @Months Int,

  /**
   * The legal notice
   * May contain multiple lines to be added to the end of the [Quote]
   */
  val legalNotice: QuoteLegalNotice,
) {

  init {
    require(title.isNotBlank()) { "title must not be blank" }
    require(quoteNumber.length <= QuoteNumberMaxLength) {
      "Quote number must be <= than $QuoteNumberMaxLength characters long, but was ${quoteNumber.length} characters long"
    }
  }

  /**
   * The total price of the quote. Read from the sum block rather than stored, so a quote states its net sum once.
   */
  val netPrice: @EUR Money.Net
    get() = pricedElements.sums.net

  /**
   * Is the current time past the valid until date
   */
  fun hasExpired(today: @ms LocalDate = LocalDate.now()): Boolean {
    return today > validUntil
  }

  companion object {
    /** The longest [quoteNumber] a quote carries. */
    const val QuoteNumberMaxLength: Int = 11
  }

  /**
   * ID for a quote
   */
  @Serializable
  @JvmInline
  value class Id(override val uuid: NativeUuid) : ValueClassUuid {

    override fun toString(): String {
      return uuid.toString()
    }

    fun format(): String {
      return uuid.toString()
    }

    companion object {
      fun random(): Id = Id(Uuid.randomForEnvironment())
    }
  }

  /**
   * Executes the given block if there is a discount
   */
  fun ifDiscounted(action: () -> Unit) {
    contract {
      callsInPlace(action, InvocationKind.AT_MOST_ONCE)
    }

    return pricedElements.ifDiscounted(action)
  }
}
