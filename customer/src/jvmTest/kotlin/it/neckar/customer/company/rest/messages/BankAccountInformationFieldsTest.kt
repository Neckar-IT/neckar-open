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
package it.neckar.customer.company.rest.messages

import assertk.assertThat
import assertk.assertions.isEqualTo
import it.neckar.customer.company.BankAccountInformation
import it.neckar.customer.company.BIC
import it.neckar.customer.company.BicFormatViolation
import it.neckar.customer.company.BicInput
import it.neckar.customer.company.IBAN
import it.neckar.customer.company.IbanFormatViolation
import it.neckar.customer.company.IbanInput
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test

class BankAccountInformationFieldsTest {

  @Test
  fun `JSON decode trims bankName and accountOwner`() {
    val json = """
      {
        "bankName": "  Volksbank  ",
        "accountOwner": "  Anna Schmidt  ",
        "iban": "DE89370400440532013000",
        "bic": "COBADEFFXXX"
      }
    """.trimIndent()

    val fields = Json.decodeFromString<BankAccountInformationFields>(json)

    assertThat(fields.bankName).isEqualTo("Volksbank")
    assertThat(fields.accountOwner).isEqualTo("Anna Schmidt")
  }

  @Test
  fun `parse returns Valid for well-formed iban and bic`() {
    val fields = BankAccountInformationFields(
      bankName = "Volksbank",
      accountOwner = "Anna Schmidt",
      iban = IbanInput("DE89370400440532013000"),
      bic = BicInput("COBADEFFXXX"),
    )

    val result = fields.parse()

    assertThat(result).isEqualTo(
      BankAccountInformationParseResult.Valid(
        BankAccountInformation(
          bankName = "Volksbank",
          accountOwner = "Anna Schmidt",
          iban = IBAN("DE89370400440532013000"),
          bic = BIC("COBADEFFXXX"),
        )
      )
    )
  }

  @Test
  fun `parse returns InvalidIban for a too short iban`() {
    val fields = BankAccountInformationFields(
      bankName = "Volksbank",
      accountOwner = "Anna Schmidt",
      iban = IbanInput("DE8937040044"),
      bic = BicInput("COBADEFFXXX"),
    )

    val result = fields.parse()

    assertThat(result).isEqualTo(BankAccountInformationParseResult.Invalid(CompanyValidationError.InvalidIban(IbanFormatViolation.InvalidLength)))
  }

  @Test
  fun `parse returns InvalidBic for a too short bic`() {
    val fields = BankAccountInformationFields(
      bankName = "Volksbank",
      accountOwner = "Anna Schmidt",
      iban = IbanInput("DE89370400440532013000"),
      bic = BicInput("COBADE"),
    )

    val result = fields.parse()

    assertThat(result).isEqualTo(BankAccountInformationParseResult.Invalid(CompanyValidationError.InvalidBic(BicFormatViolation.InvalidLength)))
  }
}
