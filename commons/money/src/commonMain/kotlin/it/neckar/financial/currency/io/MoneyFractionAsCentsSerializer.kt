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
package it.neckar.financial.currency.io

import it.neckar.financial.currency.MoneyFraction
import it.neckar.open.unit.currency.ct
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * Serializes a [MoneyFraction] as cents — `179.0` for 1,79 €.
 *
 * Cents is the base unit money travels in across this repository: every [it.neckar.financial.currency.MoneyAmount]
 * serializes as its cents, and the frontend states it in
 * `quantities/conversionFactors.ts` ("Base unit: EuroCent [Ct]", `toCent: 1`, `toEuro: 0.01`) —
 * every `EuroOutput` multiplies the value it receives by 0.01. A [MoneyFraction] written as euros
 * therefore renders a hundred times too large wherever it reaches one of those components.
 *
 * A [Double] rather than the [Long] a [it.neckar.financial.currency.Money] amount writes: [MoneyFraction] exists
 * precisely because some prices need more precision than whole cents, and encoding it as a Long would throw
 * that away at the module boundary. The value is stored in centi-cents, so two decimals of a cent
 * survive the round trip exactly.
 */
object MoneyFractionAsCentsSerializer : KSerializer<MoneyFraction> {
  override val descriptor: SerialDescriptor = Double.serializer().descriptor

  override fun serialize(encoder: Encoder, value: MoneyFraction) {
    encoder.encodeDouble(value.cents)
  }

  override fun deserialize(decoder: Decoder): MoneyFraction {
    val cents: @ct Double = decoder.decodeDouble()
    return MoneyFraction.cents(cents)
  }
}
