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

package it.neckar.customer

import it.neckar.contact.EmailAddress
import it.neckar.contact.PhoneNumber
import it.neckar.open.annotations.GermanTerm
import it.neckar.open.i18n.CurrentI18nConfiguration
import it.neckar.open.i18n.I18nConfiguration
import it.neckar.open.kotlin.lang.ifBlank
import it.neckar.uuid.HasValueClassUuid
import it.neckar.uuid.NativeUuid
import it.neckar.uuid.UuidSerializer
import it.neckar.uuid.ValueClassUuid
import it.neckar.uuid.randomForEnvironment
import it.neckar.uuid.randomUuid4
import kotlin.js.JsExport
import kotlin.jvm.JvmInline
import kotlin.uuid.Uuid
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers


/**
 * Information about the customer
 */
@JsExport
@Serializable
@GermanTerm("Kunde")
data class Customer(
  override val id: Id,

  val firstName: String,
  val lastName: String,

  /**
   * The customer's own postal address — where invoices and letters go.
   *
   * *Not* the site address of a project: consumers of this module keep that on the project itself
   * — several projects may point at the same customer, each with its own site.
   *
   * Only ever [Address.Confirmed] — the upsert request carries `AddressConfirmedFields`, so a
   * partially filled address never reaches a stored customer. Null means no address is known yet.
   */
  val address: Address.Confirmed? = null,

  val phone: PhoneNumber? = null,
  val cellphone: PhoneNumber? = null,
  val email: EmailAddress? = null,

  /**
   * The company a customer states for themselves, as free text — *not* a reference to
   * [it.neckar.customer.company.Company]. That class models the operating and partner companies
   * (it carries namespace, logo, bank and legal information); a customer's own company is never
   * one of them, so there is nothing to point at.
   *
   * Null or blank both mean "no company"; [companyOrCompleteName] and [companyOrLastName] fall
   * back for either.
   */
  val company: String? = null,
  ) : HasValueClassUuid {

  /**
   * First name then last name, space-separated. No salutation and no title, and the order is
   * fixed — it does not follow the locale.
   */
  val completeName: String
    get() = "$firstName $lastName"

  /**
   * The customer as one line: [completeName], and the address appended after a comma when set.
   *
   * [i18nConfiguration] is accepted but ignored: the output is identical for every locale.
   * Passing a different configuration changes nothing.
   */
  @Suppress("UNUSED_PARAMETER")
  fun format(i18nConfiguration: I18nConfiguration = CurrentI18nConfiguration): String {
    if (address == null) {
      return completeName
    }

    return "$completeName, ${address.singleLine()}"
  }

  /**
   * The label to address this customer by: [company] when set, else [completeName].
   */
  fun companyOrCompleteName(): String {
    return company.ifBlank {
      completeName
    }
  }

  /**
   * Like [companyOrCompleteName], but the fallback is [lastName] alone rather than the full name.
   */
  fun companyOrLastName(): String {
    return company.ifBlank {
      lastName
    }
  }

  companion object {
    fun empty(uuid: Uuid = randomUuid4()): Customer = Customer(
      id = Id(uuid),
      firstName = "",
      lastName = "",
      address = null,
    )
  }

  @Serializable
  @JvmInline
  @JsExport.Ignore
  value class Id(
    override val uuid: NativeUuid,
  ) : ValueClassUuid {
    override fun toString(): String {
      return uuid.toHexDashString()
    }

    companion object {
      fun random(): Id = Id(Uuid.randomForEnvironment())
    }
  }
}
