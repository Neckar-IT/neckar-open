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
import it.neckar.customer.Address
import it.neckar.open.http.Url
import kotlin.uuid.Uuid

object CompanyExample {
  val Musterbau: Company = Company(
    id = Company.Id(Uuid.parse("c0c81386-6459-4c3b-b070-ccb3b2fd1ca9")),
    namespace = "musterbau",
    name = "Musterbau",
    address = Address.Confirmed(
      street = "Musterweg",
      houseNumber = "7",
      zipCode = "72074",
      city = "Tübingen",
      country = "Germany",
    ),
    contactInformation = ContactInformation(
      mail = EmailAddress("info@musterbau.example"),
      phone = PhoneNumber("+49 7071 555 0400"),
      fax = PhoneNumber("+49 7071 555 0401"),
      url = Url.https("www.musterbau.example"),
    ),
    bankAccountInformation = BankAccountInformation(
      bankName = "Kreissparkasse Tübingen",
      accountOwner = "Musterbau GmbH",
      iban = IBAN("DE 40 6415 0020 7777 8888 99"),
      bic = BIC("SOLADES1TUB"),
    ),
    legalInformation = CompanyLegalInformation(
      director = "Bernd Musterbauer",
      registerCourt = "Amtsgericht Tübingen",
      taxId = TaxId("DE 111 222 333"),
      registrationNumber = RegistrationNumber("HRB 1234"),
    )
  )

  //zweiter bauträger
  val Fertigo: Company = Company(
    id = Company.Id(Uuid.parse("b1a2c3d4-5e6f-7a8b-9c0d-e1f2a3b4c5d6")),
    namespace = "fertigo",
    name = "Fertigo Bau GmbH",
    address = Address.Confirmed(
      street = "Bauernstraße",
      houseNumber = "20",
      zipCode = "70565",
      city = "Stuttgart",
      country = "Germany",
    ),
    contactInformation = ContactInformation(
      mail = EmailAddress("info@fertigo-bau.example"),
      phone = PhoneNumber("+49 711 555 0600"),
      fax = PhoneNumber("+49 711 555 0601"),
      url = Url.https("www.fertigo-bau.example"),
    ),
    bankAccountInformation = BankAccountInformation(
      bankName = "Volksbank Stuttgart",
      accountOwner = "Fertigo Bau GmbH",
      iban = IBAN("DE 89 6009 0100 1234 5678 90"),
      bic = BIC("VOBADESS"),
    ),
    legalInformation = CompanyLegalInformation(
      director = "Anna Fertig",
      registerCourt = "Amtsgericht Stuttgart",
      taxId = TaxId("DE 444 164 932"),
      registrationNumber = RegistrationNumber("HRB 18902"),
    )
  )

  val ElektroTechnikMueller: Company = Company(
    id = Company.Id(Uuid.parse("d5e72a1b-3f89-4c6d-a72a-1b3f894c6d9e")),
    namespace = "mueller",
    name = "Elektrotechnik Müller GmbH",
    address = Address.Confirmed(
      street = "Industriestraße",
      houseNumber = "27",
      zipCode = "70565",
      city = "Stuttgart",
      country = "Germany",
    ),
    contactInformation = ContactInformation(
      mail = EmailAddress("info@elektrotechnik-mueller.example"),
      phone = PhoneNumber("+49 711 555 0700"),
      fax = PhoneNumber("+49 711 555 0701"),
      url = Url.https("www.elektrotechnik-mueller.example"),
    ),
    bankAccountInformation = BankAccountInformation(
      bankName = "Stuttgarter Volksbank",
      accountOwner = "Elektrotechnik Müller GmbH",
      iban = IBAN("DE 55 6009 0100 0123 4567 89"),
      bic = BIC("VOBADESS"),
    ),
    legalInformation = CompanyLegalInformation(
      director = "Peter Müller",
      registerCourt = "Amtsgericht Stuttgart",
      taxId = TaxId("DE 287 345 612"),
      registrationNumber = RegistrationNumber("HRB 25478"),
    )
  )

  val ElektroWagner: Company = Company(
    id = Company.Id(Uuid.parse("a1b2c3d4-5e6f-7a8b-9c0d-e1f2a3b4c5d6")),
    namespace = "wagner",
    name = "Elektro Wagner",
    address = Address.Confirmed(
      street = "Hauptstraße",
      houseNumber = "42",
      zipCode = "80331",
      city = "München",
      country = "Germany",
    ),
    contactInformation = ContactInformation(
      mail = EmailAddress("info@elektro-wagner.example"),
      phone = PhoneNumber("+49 89 555 0800"),
      fax = PhoneNumber("+49 89 555 0801"),
      url = Url.https("www.elektro-wagner.example"),
    ),
    bankAccountInformation = BankAccountInformation(
      bankName = "Stadtsparkasse München",
      accountOwner = "Elektro Wagner GmbH",
      iban = IBAN("DE 67 7015 0000 1234 5678 90"),
      bic = BIC("SSKMDEMMXXX")
    ),
    legalInformation = CompanyLegalInformation(
      director = "Thomas Wagner",
      registerCourt = "Amtsgericht München",
      taxId = TaxId("DE 123 456 789"),
      registrationNumber = RegistrationNumber("HRA 95175"),
    )
  )

  val ElektroSchmidt: Company = Company(
    id = Company.Id(Uuid.parse("f9e8d7c6-b5a4-3210-9f8e-7d6c5b4a3210")),
    namespace = "schmidt",
    name = "Schmidt Elektrotechnik",
    address = Address.Confirmed(
      street = "Berliner Allee",
      houseNumber = "78",
      zipCode = "40212",
      city = "Düsseldorf",
      country = "Germany",
    ),
    contactInformation = ContactInformation(
      mail = EmailAddress("info@schmidt-elektrotechnik.example"),
      phone = PhoneNumber("+49 211 555 0900"),
      fax = PhoneNumber("+49 211 555 0901"),
      url = Url.https("www.schmidt-elektrotechnik.example"),
    ),
    bankAccountInformation = BankAccountInformation(
      bankName = "Deutsche Bank Düsseldorf",
      accountOwner = "Schmidt Elektrotechnik GmbH",
      iban = IBAN("DE 89 3007 0010 0123 4567 89"),
      bic = BIC("DEUTDEDDXXX"),
    ),
    legalInformation = CompanyLegalInformation(
      director = "Markus Schmidt",
      registerCourt = "Amtsgericht Düsseldorf",
      taxId = TaxId("DE 987 654 321"),
      registrationNumber = RegistrationNumber("HRB 65432"),
    )
  )

  val ElektroHoffmann: Company = Company(
    id = Company.Id(Uuid.parse("1a2b3c4d-5e6f-7a8b-9c0d-1e2f3a4b5c6d")),
    namespace = "hoffmann",
    name = "Hoffmann Elektroinstallationen",
    address = Address.Confirmed(
      street = "Königsstraße",
      houseNumber = "15",
      zipCode = "01067",
      city = "Dresden",
      country = "Germany",
    ),
    contactInformation = null,
    bankAccountInformation = BankAccountInformation(
      bankName = "Ostsächsische Sparkasse Dresden",
      accountOwner = "Hoffmann Elektroinstallationen GmbH",
      iban = IBAN("DE 45 8505 0300 0123 4567 89"),
      bic = BIC("OSDDDE81XXX"),
    ),
    legalInformation = CompanyLegalInformation(
      director = "Werner Hoffmann",
      registerCourt = "Amtsgericht Dresden",
      taxId = TaxId("DE 456 789 123"),
      registrationNumber = RegistrationNumber("HRB 34567"),
    )
  )

  val ElektroSchneiderCompany: Company = Company(
    id = Company.Id(Uuid.parse("650fb118-d728-45a8-a37d-06c63cf5f68d")),
    name = "Elektro Schneider",
    namespace = "elektro-schneider",
    address = Address.Confirmed(
      street = "Wilhelmstraße",
      houseNumber = "8",
      zipCode = "72074",
      city = "Tübingen",
      country = "Germany",
    ),
    contactInformation = ContactInformation(
      mail = EmailAddress("info@elektro-schneider.example"),
      phone = PhoneNumber("+49 7071 555 0500"),
      fax = PhoneNumber("+49 7071 555 0501"),
      url = Url.https("www.elektro-schneider.example"),
    ),
    bankAccountInformation = BankAccountInformation(
      bankName = "Kreissparkasse Tübingen",
      accountOwner = "Elektro Schneider GmbH",
      iban = IBAN("DE 05 6415 0020 0003 0380 00"),
      bic = BIC("SOLADES1TUB"),
    ),
    legalInformation = CompanyLegalInformation(
      director = "Ingo Schneider",
      registerCourt = "Amtsgericht Tübingen",
      taxId = TaxId("DE 333 222 111"),
      registrationNumber = RegistrationNumber("HRB 667788"),
    ),
  )

  val companies: List<Company> = listOf(
    Musterbau,
    ElektroTechnikMueller,
    ElektroWagner,
    ElektroSchmidt,
    ElektroHoffmann,
    ElektroSchneiderCompany,
  )

  val bautraeger: List<Company> = listOf(
    Musterbau,
    Fertigo
  )
}
