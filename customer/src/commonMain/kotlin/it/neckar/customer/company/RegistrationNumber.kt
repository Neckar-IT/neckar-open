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
 * Represents a German commercial register number (Handelsregisternummer) with backend-side
 * formatting (ADL 0145).
 *
 * Accepts any input format (with/without colon, varying whitespace, varying case) and normalizes
 * to the canonical form: official register-type prefix, single space, digits.
 *
 * Example: `RegistrationNumber("hrb: 25478")` and `RegistrationNumber("  HRB  25478 ")` both
 * produce `value = "HRB 25478"`.
 *
 * Recognized German commercial register types (per § 8 HGB):
 * - `HRA` — Handelsregister Abteilung A (Einzelkaufleute, Personengesellschaften)
 * - `HRB` — Handelsregister Abteilung B (Kapitalgesellschaften)
 * - `GnR` — Genossenschaftsregister (official mixed-case form per § 8 HGB)
 * - `PR` — Partnerschaftsregister
 * - `VR` — Vereinsregister
 *
 * Inputs without a recognized prefix are accepted as long as they contain at least one digit.
 */
@Serializable(with = RegistrationNumber.RegistrationNumberSerializer::class)
@JvmInline
value class RegistrationNumber private constructor(override val value: String) : ValueClassString {

  override fun toString(): String = value

  companion object {
    /**
     * Canonical forms of recognized German commercial register prefixes per § 8 HGB,
     * keyed by their uppercase lookup form. The value is the official spelling used in
     * the normalized output — `Genossenschaftsregister` is officially abbreviated as
     * `GnR` (mixed case), all others are fully uppercase.
     */
    private val KnownPrefixes: Map<String, String> = mapOf(
      "HRA" to "HRA",
      "HRB" to "HRB",
      "GNR" to "GnR",
      "PR" to "PR",
      "VR" to "VR",
    )

    /**
     * Creates a [RegistrationNumber] from any input format.
     * Trims whitespace, removes colons, collapses internal whitespace, normalizes known prefixes
     * to their official spelling, and validates that the value is non-blank and contains at
     * least one digit.
     */
    operator fun invoke(raw: String): RegistrationNumber {
      val cleaned = raw.trim().replace(":", " ").replace(Regex("\\s+"), " ")
      require(cleaned.isNotEmpty()) {
        "RegistrationNumber must not be blank"
      }
      require(cleaned.any { it.isDigit() }) {
        "RegistrationNumber must contain at least one digit: $cleaned"
      }

      val firstSpace = cleaned.indexOf(' ')
      val canonical = if (firstSpace > 0) {
        val canonicalPrefix = KnownPrefixes[cleaned.substring(0, firstSpace).uppercase()]
        if (canonicalPrefix != null) {
          "$canonicalPrefix ${cleaned.substring(firstSpace + 1)}"
        } else {
          cleaned
        }
      } else {
        cleaned
      }
      return RegistrationNumber(canonical)
    }
  }

  internal object RegistrationNumberSerializer : KSerializer<RegistrationNumber> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("RegistrationNumber", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: RegistrationNumber) {
      encoder.encodeString(value.value)
    }

    override fun deserialize(decoder: Decoder): RegistrationNumber {
      return invoke(decoder.decodeString())
    }
  }
}
