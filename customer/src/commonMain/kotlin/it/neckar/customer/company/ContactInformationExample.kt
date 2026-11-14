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
package it.neckar.customer.company

import it.neckar.contact.EmailAddress
import it.neckar.contact.PhoneNumber
import it.neckar.open.http.Url

/**
 * Standalone contact information, one entry per shape a renderer has to cope with. The entries of
 * [CompanyExample] carry a whole company each; these carry a single case.
 */
object ContactInformationExample {
  val demo: ContactInformation = ContactInformation(EmailAddress("info@demo-company.example"), PhoneNumber("+49 30 555 0100"), PhoneNumber("+49 30 555 0199"), Url.https("www.demo-company.example"))

  /** No fax — the case a renderer has to leave out rather than print empty. */
  val withoutFax: ContactInformation = ContactInformation(EmailAddress("info@beispiel-handel.example"), PhoneNumber("+49 89 555 0200"), null, Url.https("www.beispiel-handel.example"))

  /** A phone number without an extension block. */
  val withoutExtensionBlock: ContactInformation = ContactInformation(EmailAddress("kontakt@beispiel-technik.example"), PhoneNumber("+49 211 5550300"), PhoneNumber("+49 211 5550301"), Url.https("www.beispiel-technik.example"))
}

val ContactInformation.Companion.Example: ContactInformationExample
  get() = ContactInformationExample
