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
import kotlin.uuid.Uuid

object CompanyContactPersonExample {
  val companyContactPersonMusterbau: CompanyContactPerson = CompanyContactPerson(
    id = CompanyContactPerson.Id(Uuid.parse("0ab22b86-49c2-4461-b22b-8649c27461e5")),
    companyId = CompanyExample.Musterbau.id,
    firstName = "Bernd",
    lastName = "Musterbauer",
    phone = PhoneNumber("+49 7071 555 0101"),
    email = EmailAddress("bernd.musterbauer@musterbau.example")
  )

  val companyContactPersonFertigo: CompanyContactPerson = CompanyContactPerson(
    id = CompanyContactPerson.Id(Uuid.parse("be2886e3-d0dc-40be-a886-e3d0dc10be9b")),
    companyId = CompanyExample.Fertigo.id,
    firstName = "Anna",
    lastName = "Fertig",
    phone = PhoneNumber("+49 711 555 0102"),
    email = EmailAddress("anna.fertig@fertigo-bau.example")
  )

  val companyContactPersonMueller: CompanyContactPerson = CompanyContactPerson(
    id = CompanyContactPerson.Id(Uuid.parse("2c3d4e5f-6a7b-8c9d-0e1f-2a3b4c5d6e7f")),
    companyId = CompanyExample.ElektroTechnikMueller.id,
    firstName = "Klaus",
    lastName = "Müller",
    phone = PhoneNumber("+49 711 555 0103"),
    email = EmailAddress("klaus.mueller@elektrotechnik-mueller.example")
  )

  val companyContactPersonWagner: CompanyContactPerson = CompanyContactPerson(
    id = CompanyContactPerson.Id(Uuid.parse("3d4e5f6a-7b8c-9d0e-1f2a-3b4c5d6e7f8a")),
    companyId = CompanyExample.ElektroWagner.id,
    firstName = "Thomas",
    lastName = "Wagner",
    phone = PhoneNumber("+49 89 555 0104"),
    email = EmailAddress("thomas.wagner@elektro-wagner.example")
  )

  val companyContactPersonSchmidt: CompanyContactPerson = CompanyContactPerson(
    id = CompanyContactPerson.Id(Uuid.parse("4e5f6a7b-8c9d-0e1f-2a3b-4c5d6e7f8a9b")),
    companyId = CompanyExample.ElektroSchmidt.id,
    firstName = "Markus",
    lastName = "Schmidt",
    phone = PhoneNumber("+49 211 555 0105"),
    email = EmailAddress("markus.schmidt@schmidt-elektrotechnik.example")
  )

  val companyContactPersonHoffmann: CompanyContactPerson = CompanyContactPerson(
    id = CompanyContactPerson.Id(Uuid.parse("5f6a7b8c-9d0e-1f2a-3b4c-5d6e7f8a9b0c")),
    companyId = CompanyExample.ElektroHoffmann.id,
    firstName = "Werner",
    lastName = "Hoffmann",
    phone = PhoneNumber("+49 351 555 0106"),
    email = EmailAddress("werner.hoffmann@hoffmann-elektroinstallationen.example")
  )

  val companyContactPersonMuellerAssistant: CompanyContactPerson = CompanyContactPerson(
    id = CompanyContactPerson.Id(Uuid.parse("6a7b8c9d-0e1f-2a3b-4c5d-6e7f8a9b0c1d")),
    companyId = CompanyExample.ElektroTechnikMueller.id,
    firstName = "Sabine",
    lastName = "Weber",
    phone = PhoneNumber("+49 711 555 0107"),
    email = EmailAddress("sabine.weber@elektrotechnik-mueller.example")
  )

  val companyContactPersonWagnerAssistant: CompanyContactPerson = CompanyContactPerson(
    id = CompanyContactPerson.Id(Uuid.parse("7b8c9d0e-1f2a-3b4c-5d6e-7f8a9b0c1d2e")),
    companyId = CompanyExample.ElektroWagner.id,
    firstName = "Michael",
    lastName = "Becker",
    phone = PhoneNumber("+49 89 555 0108"),
    email = EmailAddress("michael.becker@elektro-wagner.example")
  )

  val companyContactPersons: List<CompanyContactPerson> = listOf(
    companyContactPersonMusterbau,
    companyContactPersonMueller,
    companyContactPersonWagner,
    companyContactPersonSchmidt,
    companyContactPersonHoffmann,
    companyContactPersonMuellerAssistant,
    companyContactPersonWagnerAssistant
  )

  val contactPersonBautraeger: List<CompanyContactPerson> = listOf(
    companyContactPersonMusterbau,
    companyContactPersonFertigo,
  )
}
