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
import kotlin.jvm.JvmInline
import kotlinx.serialization.Serializable

/**
 * Value class for a payment card number, stored **masked**.
 *
 * The card number is never persisted in clear text: [value] always holds the masked form
 * `**** **** **** 1234` (all but the last four digits replaced by `*`). The private primary
 * constructor enforces that the stored value already matches [MaskedPattern]; callers
 * build instances from a raw PAN via [invoke], which strips separators, validates the digit
 * count (13..19, the ISO/IEC 7812 range) and masks before construction. Round-tripping an
 * already-masked value (e.g. on deserialization) is idempotent.
 */
@GermanTerm("Kartennummer")
@Serializable(with = CardNumberSerializer::class)
@JvmInline
value class CardNumber private constructor(val value: String) {

  init {
    require(MaskedPattern.matches(value)) { "CardNumber must already be masked and match $MaskedPattern, was <$value>" }
  }

  override fun toString(): String = value

  companion object {
    private const val MinDigits: Int = 13
    private const val MaxDigits: Int = 19
    private const val VisibleDigits: Int = 4

    /** The shape of a *masked* card number — a raw PAN does not match it: four-character groups
     * grouped from the right, so only the final group is always four digits and the leading group
     * may hold 1..4 characters (PANs are 13..19 digits). */
    val MaskedPattern: Regex = Regex("^[*0-9]{1,4}( [*0-9]{4})* [0-9]{4}$")

    /**
     * Builds a [CardNumber] from any input. Accepts a raw PAN (with or without spaces/hyphens)
     * and masks it, or an already-masked value (idempotent re-construction on deserialization).
     *
     * An input counts as already-masked only when it contains a `*`; an all-digit value — even
     * if space-grouped — is always treated as a raw PAN and masked, so a clear-text PAN never
     * slips through unmasked.
     */
    operator fun invoke(raw: String): CardNumber {
      val trimmed = raw.trim()
      if (trimmed.contains('*') && MaskedPattern.matches(trimmed)) {
        return CardNumber(trimmed)
      }
      val digits = trimmed.filter { it.isDigit() }
      require(digits.length in MinDigits..MaxDigits) {
        "CardNumber must contain $MinDigits..$MaxDigits digits, got ${digits.length}: <$raw>"
      }
      return CardNumber(mask(digits))
    }

    /**
     * Masks a digit string into space-separated four-character groups, keeping only the last
     * [VisibleDigits] digits visible. Grouping runs from the right so the last group is always the
     * four visible digits; the leading group holds the remaining 1..3 characters when the length is
     * not a multiple of four (e.g. 15-digit Amex, 14-digit Diners).
     */
    private fun mask(digits: String): String {
      val visible = digits.takeLast(VisibleDigits)
      val hiddenCount = digits.length - VisibleDigits
      val ungrouped = "*".repeat(hiddenCount) + visible
      return ungrouped.chunkedFromRight(4).joinToString(" ")
    }

    /**
     * Splits into chunks of [size] characters counting from the right, so the last chunk is always
     * full and only the first chunk may be shorter. `"abcdefg".chunkedFromRight(4)` → `["abc", "defg"]`.
     */
    private fun String.chunkedFromRight(size: Int): List<String> {
      if (isEmpty()) return emptyList()
      val firstChunkLength = ((length - 1) % size) + 1
      return listOf(substring(0, firstChunkLength)) + substring(firstChunkLength).chunked(size)
    }
  }
}
