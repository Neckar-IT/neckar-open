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
package it.neckar.financial.quote

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

/**
 * How a tenant numbers its quotes: `{year}` becomes the four-digit year, `{seq:N}` the sequence
 * number zero-padded to `N` digits, every other character stays (`B-{year}-{seq:4}` → `B-2026-0001`).
 * A drawn quote number takes `N` as its least width; [Quote] refuses one that grows past its 11 characters.
 */
@JvmInline
@Serializable
value class QuoteNumberPattern(val value: String) {
  init {
    // The sequence restarts every year, so a pattern without the year would repeat its numbers.
    require(value.contains(YearPlaceholder)) { "Quote number pattern <$value> must contain {year}" }
    val sequencePlaceholders = SequencePlaceholder.findAll(value).toList()
    require(sequencePlaceholders.size == 1) { "Quote number pattern <$value> must contain exactly one {seq:N}, but contains ${sequencePlaceholders.size}" }
    // The number scheme of the quote numbers reads every other brace as a placeholder of its own.
    val unknown = AnyPlaceholder.findAll(value).map { it.value }.filterNot { it == YearPlaceholder || SequencePlaceholder.matches(it) }.toList()
    require(unknown.isEmpty()) { "Quote number pattern <$value> contains unknown placeholders $unknown" }
    val longestNumber = format(year = 9999, sequence = maxSequence(sequencePlaceholders.single().groupValues[1].toInt()))
    require(longestNumber.length <= Quote.QuoteNumberMaxLength) {
      "Quote number pattern <$value> formats to up to ${longestNumber.length} characters, the quote number holds ${Quote.QuoteNumberMaxLength}"
    }
  }

  /** Throws when [sequence] is negative or needs more digits than `{seq:N}` grants. */
  fun format(year: Int, sequence: Long): String {
    require(sequence >= 0) { "Sequence must not be negative, but was $sequence" }
    return value
      .replace(YearPlaceholder, year.toString().padStart(4, '0'))
      .replace(SequencePlaceholder) { match ->
        val digits = match.groupValues[1].toInt()
        require(sequence <= maxSequence(digits)) { "Sequence $sequence needs more than the $digits digits of <$value>" }
        sequence.toString().padStart(digits, '0')
      }
  }

  override fun toString(): String = value

  companion object {
    private const val YearPlaceholder: String = "{year}"

    private val SequencePlaceholder: Regex = Regex("""\{seq:([1-9])\}""")

    private val AnyPlaceholder: Regex = Regex("""\{[^}]*\}""")

    val Default: QuoteNumberPattern = QuoteNumberPattern("B-{year}-{seq:4}")

    /** The quote number of a quote that is only shown and never frozen, so it consumes no sequence number. */
    const val Preview: String = "Entwurf"

    private fun maxSequence(digits: Int): Long = "9".repeat(digits).toLong()
  }
}
