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

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * Serializes [CardNumber] as the masked string primitive.
 *
 * Needed because the upsert wire carries a **raw PAN** (typed in the form), not the canonical
 * masked form: decoding routes through [CardNumber.invoke], which masks the raw value before it
 * enters the domain (an already-masked value round-trips idempotently). Pattern *enforcement* is
 * not the reason — the generated value-class serializer already runs the `init` `require`; this
 * serializer exists purely for the raw→masked transform. Same shape as the normalizing
 * [it.neckar.customer.company.IBAN] serializer, which routes raw input through its `invoke`.
 */
object CardNumberSerializer : KSerializer<CardNumber> {
  override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("CardNumber", PrimitiveKind.STRING)

  override fun serialize(encoder: Encoder, value: CardNumber) {
    encoder.encodeString(value.value)
  }

  override fun deserialize(decoder: Decoder): CardNumber {
    return CardNumber(decoder.decodeString())
  }
}
