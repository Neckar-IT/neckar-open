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
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * Serializes a [MoneyFraction] as euros — `1.79` for 1,79 €.
 *
 * NOT the default: [MoneyFractionAsCentsSerializer] is, because cents is the base unit money
 * travels in here.
 *
 * This is the format documents written before that change carry, and the reason the serializer
 * stays: a migration reads them back with `Json.decodeFromString(MoneyFractionAsEuroSerializer, …)`
 * and gets the value the euro-format document meant. No property declares it today — the only
 * caller is `MoneyFractionTest`, which pins that the old shape is still readable.
 */
object MoneyFractionAsEuroSerializer : KSerializer<MoneyFraction> {
  override val descriptor: SerialDescriptor = Double.serializer().descriptor

  override fun serialize(encoder: Encoder, value: MoneyFraction) {
    encoder.encodeDouble(value.euros)
  }

  override fun deserialize(decoder: Decoder): MoneyFraction {
    val euros = decoder.decodeDouble()
    return MoneyFraction.euros(euros)
  }
}
