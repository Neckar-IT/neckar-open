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
 * Represents an IBAN (International Bank Account Number) with backend-side formatting (ADL 0145).
 *
 * Accepts any input format (with/without spaces) and normalizes to the canonical
 * format with groups of 4 characters separated by spaces.
 *
 * Example: `IBAN("DE89370400440532013000")` and `IBAN("DE89 3704 0044 0532 0130 00")`
 * both produce `value = "DE89 3704 0044 0532 0130 00"`.
 */
@Serializable(with = IBAN.IbanSerializer::class)
@JvmInline
value class IBAN private constructor(override val value: String) : ValueClassString {

  override fun toString(): String = value

  companion object {
    /**
     * Creates an IBAN from any input format. Throws when [raw] violates the format —
     * use [parse] for anything that originates from a user.
     */
    operator fun invoke(raw: String): IBAN {
      return when (val result = parse(raw)) {
        is IbanParseResult.Valid -> result.iban
        is IbanParseResult.Invalid -> throw IllegalArgumentException("Not a valid IBAN (${result.violation}): \"$raw\"")
      }
    }

    /**
     * Checks [raw] against the format rule and returns the violation instead of throwing.
     */
    fun parse(raw: String): IbanParseResult {
      val cleaned = normalizeToAlphanumericUppercase(raw)
      if (cleaned.length !in 15..34) {
        return IbanParseResult.Invalid(IbanFormatViolation.InvalidLength)
      }
      return IbanParseResult.Valid(IBAN(cleaned.chunked(4).joinToString(" ")))
    }
  }

  internal object IbanSerializer : KSerializer<IBAN> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("IBAN", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: IBAN) {
      encoder.encodeString(value.value)
    }

    override fun deserialize(decoder: Decoder): IBAN {
      return invoke(decoder.decodeString())
    }
  }
}
