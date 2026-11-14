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
package it.neckar.financial.quote

import it.neckar.open.unit.other.pct
import it.neckar.open.unit.time.d
import kotlinx.serialization.Serializable

/**
 * When and how a customer pays a quote.
 */
@Serializable
data class PaymentTerms(
  /** The share paid before delivery, as a fraction (`0.6` is 60 %). */
  val downPaymentPercent: @pct Double,
  /** Days from the invoice date until the rest is due. */
  val dueDays: @d Int,
  /** The sentence the quote prints. */
  val text: String,
) {
  init {
    require(downPaymentPercent in 0.0..1.0) { "Down payment must be within 0.0..1.0, but was $downPaymentPercent" }
    require(dueDays >= 0) { "Due days must not be negative, but was $dueDays" }
  }

  companion object {
    val Default: PaymentTerms = PaymentTerms(downPaymentPercent = 0.0, dueDays = 14, text = "Zahlbar innerhalb von 14 Tagen ohne Abzug")
  }
}
