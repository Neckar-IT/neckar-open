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
package it.neckar.financial.payment.card

import it.neckar.open.annotations.GermanTerm
import it.neckar.time.today
import kotlin.jvm.JvmInline
import kotlinx.datetime.YearMonth
import kotlinx.datetime.yearMonth
import kotlinx.serialization.Serializable

/**
 * Year-month expiry of a [PaymentMethod.CardPayment].
 *
 * Wraps [kotlinx.datetime.YearMonth] rather than re-deriving year/month handling: a card carries
 * an expiry month, and `YearMonth` already models exactly that (and serializes as the ISO
 * `YYYY-MM` string). The domain-specific invariant lives here — a card is valid through the
 * **end** of its expiry month, so [yearMonth] must be `>=` the current month at construction
 * time. The current month is read via [today], which honours the test clock
 * (`it.neckar.open.time.now`) so fixtures and unit tests stay deterministic.
 */
@GermanTerm("Ablaufdatum")
@Serializable
@JvmInline
value class ExpiryDate(val yearMonth: YearMonth) {

  init {
    require(yearMonth >= currentYearMonth()) {
      "ExpiryDate must not be in the past, was $yearMonth"
    }
  }

  override fun toString(): String = yearMonth.toString()

  companion object {
    /** Convenience factory from 1-based year/month components. */
    operator fun invoke(year: Int, month: Int): ExpiryDate = ExpiryDate(YearMonth(year, month))

    private fun currentYearMonth(): YearMonth = today().yearMonth
  }
}
