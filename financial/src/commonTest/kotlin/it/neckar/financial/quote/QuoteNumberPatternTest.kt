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

import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.hasMessage
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import kotlin.test.Test

/** Common: Kotlin/JS compiles the placeholder regex under the unicode flag, which rejects what the JVM accepts. */
class QuoteNumberPatternTest {
  @Test
  fun `the default pattern pads the sequence to four digits`() {
    assertThat(QuoteNumberPattern.Default.format(year = 2026, sequence = 1)).isEqualTo("B-2026-0001")
  }

  @Test
  fun `a pattern without a sequence is refused`() {
    assertFailure { QuoteNumberPattern("B-{year}") }.isInstanceOf<IllegalArgumentException>()
  }

  @Test
  fun `a pattern without the year is refused`() {
    assertFailure { QuoteNumberPattern("B-{seq:5}") }.isInstanceOf<IllegalArgumentException>()
  }

  @Test
  fun `a pattern with two sequences is refused`() {
    assertFailure { QuoteNumberPattern("{year}{seq:2}-{seq:2}") }.isInstanceOf<IllegalArgumentException>()
  }

  @Test
  fun `a pattern with another placeholder is refused`() {
    assertFailure { QuoteNumberPattern("B{mm}{year}{seq:2}") }.isInstanceOf<IllegalArgumentException>().hasMessage("Quote number pattern <B{mm}{year}{seq:2}> contains unknown placeholders [{mm}]")
  }

  @Test
  fun `a pattern longer than the quote number is refused`() {
    assertFailure { QuoteNumberPattern("BO-{year}-{seq:5}") }.isInstanceOf<IllegalArgumentException>()
  }

  @Test
  fun `a sequence beyond its digits is refused`() {
    assertFailure { QuoteNumberPattern.Default.format(year = 2026, sequence = 10_000) }.isInstanceOf<IllegalArgumentException>()
  }
}
