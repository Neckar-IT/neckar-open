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
 * Standalone bank information. The entries of [CompanyExample] carry a whole company each; these
 * carry a single case. The spelling an IBAN arrives in is not one of them — [IBAN] groups it into
 * fours on construction, so an example could only differ in its literal, never in its value.
 */
object BankAccountInformationExample {
  val demo: BankAccountInformation = BankAccountInformation("Sparkasse KölnBonn", "Demo Company", IBAN("DE 02 3705 0198 0000 1234 56"), BIC("COLSDE33XXX"))

  /** An eight-character BIC, where every other example carries the eleven-character form. */
  val shortBic: BankAccountInformation = BankAccountInformation("Ostsächsische Sparkasse Dresden", "Beispiel Bau GmbH", IBAN("DE 55 8505 0300 0123 4567 00"), BIC("OSDDDE81"))
}
