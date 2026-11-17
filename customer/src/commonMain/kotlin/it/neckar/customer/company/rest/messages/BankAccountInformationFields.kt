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

import it.neckar.customer.company.BankAccountInformation
import it.neckar.customer.company.BicInput
import it.neckar.customer.company.BicParseResult
import it.neckar.customer.company.IbanInput
import it.neckar.customer.company.IbanParseResult
import it.neckar.open.serialization.TrimmedString
import kotlinx.serialization.Serializable

/**
 * Flat representation of [BankAccountInformation] for embedding in REST Request DTOs.
 *
 * Free-text fields are typed as [TrimmedString] so trimming happens transparently on REST decode.
 * [iban]/[bic] carry no invariant on the wire — a violation would otherwise fail while the request
 * body is still being deserialized, reaching the routing as a technical error instead of a
 * validation error for the field. Converts via the total [parse].
 */
@Serializable
data class BankAccountInformationFields(
  val bankName: TrimmedString,
  val accountOwner: TrimmedString,
  val iban: IbanInput,
  val bic: BicInput,
) {

  /**
   * Checks [iban] and [bic] against their format rules and returns the first violation instead of
   * throwing.
   */
  fun parse(): BankAccountInformationParseResult {
    val parsedIban = when (val result = iban.parse()) {
      is IbanParseResult.Invalid -> return BankAccountInformationParseResult.Invalid(CompanyValidationError.InvalidIban(result.violation))
      is IbanParseResult.Valid -> result.iban
    }
    val parsedBic = when (val result = bic.parse()) {
      is BicParseResult.Invalid -> return BankAccountInformationParseResult.Invalid(CompanyValidationError.InvalidBic(result.violation))
      is BicParseResult.Valid -> result.bic
    }

    return BankAccountInformationParseResult.Valid(
      BankAccountInformation(
        bankName = bankName,
        accountOwner = accountOwner,
        iban = parsedIban,
        bic = parsedBic,
      )
    )
  }

  companion object {
    fun fromBankAccountInformation(info: BankAccountInformation): BankAccountInformationFields = BankAccountInformationFields(
      bankName = info.bankName,
      accountOwner = info.accountOwner,
      iban = IbanInput(info.iban.value),
      bic = BicInput(info.bic.value),
    )
  }
}
