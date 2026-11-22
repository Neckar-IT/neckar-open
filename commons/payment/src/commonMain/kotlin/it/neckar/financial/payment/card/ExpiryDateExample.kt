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

import it.neckar.time.today

/**
 * Example expiry dates for OpenAPI documentation and tests.
 *
 * Derived from the current year (via [today], which honours the test clock) so the
 * not-in-past invariant on [ExpiryDate] always holds regardless of when the fixtures are
 * constructed.
 */
object ExpiryDateExample {
  /** Three years out — comfortably in the future. */
  val nearFuture: ExpiryDate = ExpiryDate(year = today().year + 3, month = 6)

  /** Five years out. */
  val farFuture: ExpiryDate = ExpiryDate(year = today().year + 5, month = 12)
}
