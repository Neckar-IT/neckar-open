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
package it.neckar.financial.currency

import it.neckar.open.annotations.GermanTerm
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

/**
 * The unit a quantity counts in, and the unit a price is stated per — "Stück", "Meter", "m²", "kg",
 * "Pauschale", …
 *
 * Free text on purpose: the set of units differs per trade and per company, and a closed enum would
 * force every new trade to extend a shared type. The invariant is only that a unit is stated.
 */
@GermanTerm("Einheit")
@Serializable
@JvmInline
value class QuantityUnit(val value: String) {
  init {
    require(value.isNotBlank()) { "unit must not be blank, was: <$value>" }
  }

  override fun toString(): String = value

  companion object {
    /**
     * The "per piece" unit — the canonical meaning of a missing unit. A record that prices per piece
     * carries `null` for its unit, so the default case needs no input and the same value is not
     * stored in two forms. [Piece] names that default for the few call sites that render or compare
     * against it; it must never be written as an explicit unit where `null` is expected.
     */
    val Piece: QuantityUnit = QuantityUnit("Stück")
  }
}
