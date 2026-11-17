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
 * Represents a BIC (Business Identifier Code) with backend-side normalization (ADL 0145).
 *
 * Accepts any input format and normalizes to uppercase without spaces.
 *
 * Example: `BIC("  coba deff xxx ")` produces `value = "COBADEFFXXX"`.
 */
@Serializable(with = BIC.BicSerializer::class)
@JvmInline
value class BIC private constructor(override val value: String) : ValueClassString {

  override fun toString(): String = value

  companion object {
    /**
     * Creates a BIC from any input format. Throws when [raw] violates the format —
     * use [parse] for anything that originates from a user.
     */
    operator fun invoke(raw: String): BIC {
      return when (val result = parse(raw)) {
        is BicParseResult.Valid -> result.bic
        is BicParseResult.Invalid -> throw IllegalArgumentException("Not a valid BIC (${result.violation}): \"$raw\"")
      }
    }

    /**
     * Checks [raw] against the format rule and returns the violation instead of throwing.
     */
    fun parse(raw: String): BicParseResult {
      val cleaned = normalizeToAlphanumericUppercase(raw)
      if (cleaned.length != 8 && cleaned.length != 11) {
        return BicParseResult.Invalid(BicFormatViolation.InvalidLength)
      }
      return BicParseResult.Valid(BIC(cleaned))
    }
  }

  internal object BicSerializer : KSerializer<BIC> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("BIC", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: BIC) {
      encoder.encodeString(value.value)
    }

    override fun deserialize(decoder: Decoder): BIC {
      return invoke(decoder.decodeString())
    }
  }
}
