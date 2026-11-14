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

import it.neckar.open.kotlin.lang.ValueClassString
import kotlin.jvm.JvmInline
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * Represents an EU VAT identification number (Umsatzsteuer-Identifikationsnummer) with
 * backend-side formatting (ADL 0145).
 *
 * Accepts any input format (with/without spaces, lowercase) and normalizes to the canonical
 * form: country prefix in uppercase, no spaces.
 *
 * Example: `TaxId("DE 192 736 450")` and `TaxId("de192736450")` both produce
 * `value = "DE192736450"`.
 *
 * Validates structural shape only: a two-letter country prefix followed by alphanumeric
 * characters. Length range covers all EU member states (per Council Directive 2006/112/EC).
 */
@Serializable(with = TaxId.TaxIdSerializer::class)
@JvmInline
value class TaxId private constructor(override val value: String) : ValueClassString {

  override fun toString(): String = value

  companion object {
    /**
     * Minimum and maximum length of an EU VAT identification number, including the
     * two-letter country prefix.
     */
    private const val MinLength: Int = 4
    private const val MaxLength: Int = 15

    /**
     * Creates a [TaxId] from any input format.
     * Trims whitespace, strips all whitespace, uppercases, and validates the structural shape.
     */
    operator fun invoke(raw: String): TaxId {
      val cleaned = raw.trim().filter { it.isWhitespace().not() }.uppercase()
      require(cleaned.isNotEmpty()) {
        "TaxId must not be blank"
      }
      require(cleaned.length in MinLength..MaxLength) {
        "TaxId must be $MinLength-$MaxLength characters (was ${cleaned.length}): $cleaned"
      }
      require(cleaned[0].isLetter() && cleaned[1].isLetter()) {
        "TaxId must start with a two-letter country prefix: $cleaned"
      }
      require(cleaned.drop(2).all { it.isLetterOrDigit() }) {
        "TaxId must contain only letters and digits after the country prefix: $cleaned"
      }
      return TaxId(cleaned)
    }
  }

  internal object TaxIdSerializer : KSerializer<TaxId> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("TaxId", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: TaxId) {
      encoder.encodeString(value.value)
    }

    override fun deserialize(decoder: Decoder): TaxId {
      return invoke(decoder.decodeString())
    }
  }
}
