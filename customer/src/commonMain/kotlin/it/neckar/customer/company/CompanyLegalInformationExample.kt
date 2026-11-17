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

/**
 * Standalone legal information, one entry per spelling a register entry can take. The entries of
 * [CompanyExample] carry a whole company each; these carry a single case worth rendering against.
 */
object CompanyLegalInformationExample {
  val demo: CompanyLegalInformation = CompanyLegalInformation(
    director = "Martha Testfrau",
    registerCourt = "Amtsgericht Stuttgart",
    taxId = TaxId("DE 192736450"),
    registrationNumber = RegistrationNumber("HRB 112233"),
  )

  /** Two directors — the case a rendered footer has to join rather than truncate. */
  val twoDirectors: CompanyLegalInformation = CompanyLegalInformation(
    director = "Klara Beispiel, Bernd Beispiel",
    registerCourt = "Amtsgericht München",
    taxId = TaxId("DE 811234567"),
    registrationNumber = RegistrationNumber("HRB 445566"),
  )

  /** A registration number written with the `HRA` prefix a partnership carries. */
  val partnershipRegister: CompanyLegalInformation = CompanyLegalInformation(
    director = "Erwin Beispiel",
    registerCourt = "Amtsgericht Düsseldorf",
    taxId = TaxId("DE 812345678"),
    registrationNumber = RegistrationNumber("HRA 778899"),
  )
}

val CompanyLegalInformation.Companion.Example: CompanyLegalInformationExample
  get() = CompanyLegalInformationExample
