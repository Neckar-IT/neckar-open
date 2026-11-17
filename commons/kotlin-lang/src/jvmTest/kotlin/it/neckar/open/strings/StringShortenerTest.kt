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
package it.neckar.open.strings

import it.neckar.open.string.StringShortener
import assertk.*
import assertk.assertions.*

import org.junit.jupiter.api.Test

class StringShortenerTest {
  val text: String = "abcdefghijklmnopqrstuvwxyz"

  @Test
  fun testNoOp() {
    assertThat(StringShortener.NoOp.shorten(text, 999)).isEqualTo(text)
  }

  @Test
  fun testTruncateWithoutSymbol() {
    assertThat(StringShortener.TruncateToLength.shorten(text, 7, "")).isEqualTo("abcdefg")
    assertThat(StringShortener.TruncateToLength.shorten("a", 7, "")).isEqualTo("a")
    assertThat(StringShortener.TruncateToLength.shorten("abcde", 7, "")).isEqualTo("abcde")
    assertThat(StringShortener.TruncateToLength.shorten("abcdef", 7, "")).isEqualTo("abcdef")
    assertThat(StringShortener.TruncateToLength.shorten("abcdefg", 7, "")).isEqualTo("abcdefg")
    assertThat(StringShortener.TruncateToLength.shorten("abcdefgh", 7, "")).isEqualTo("abcdefg")
  }

  @Test
  fun testVeryShort() {
    assertThat(StringShortener.TruncateToLength.shorten(text, 0, "...")).isNull()
    assertThat(StringShortener.TruncateToLength.shorten(text, 1, "...")).isEqualTo("!")
    assertThat(StringShortener.TruncateToLength.shorten(text, 2, "...")).isEqualTo("!")
    assertThat(StringShortener.TruncateToLength.shorten(text, 3, "...")).isEqualTo("...")
  }

  @Test
  fun testTruncateWithSymbol() {
    assertThat(StringShortener.TruncateToLength.shorten(text, 7, "...")).isEqualTo("abcd...")
    assertThat(StringShortener.TruncateToLength.shorten("a", 7, "...")).isEqualTo("a")
    assertThat(StringShortener.TruncateToLength.shorten("ab", 7, "...")).isEqualTo("ab")
    assertThat(StringShortener.TruncateToLength.shorten("abc", 7, "...")).isEqualTo("abc")
    assertThat(StringShortener.TruncateToLength.shorten("abcd", 7, "...")).isEqualTo("abcd")
    assertThat(StringShortener.TruncateToLength.shorten("abcde", 7, "...")).isEqualTo("abcde")
    assertThat(StringShortener.TruncateToLength.shorten("abcdef", 7, "...")).isEqualTo("abcdef")
    assertThat(StringShortener.TruncateToLength.shorten("abcdefg", 7, "...")).isEqualTo("abcdefg")
    assertThat(StringShortener.TruncateToLength.shorten("abcdefgh", 7, "...")).isEqualTo("abcd...")
  }

  @Test
  fun testTruncateWithEllipsis() {
    val truncationSymbol = "…"
    assertThat(StringShortener.TruncateToLength.shorten("abcdefghijklmnopqrstuvwxyz", 7, truncationSymbol)).isEqualTo("abcdef…")
    assertThat(StringShortener.TruncateToLength.shorten("a", 7, truncationSymbol)).isEqualTo("a")
    assertThat(StringShortener.TruncateToLength.shorten("ab", 7, truncationSymbol)).isEqualTo("ab")
    assertThat(StringShortener.TruncateToLength.shorten("abc", 7, truncationSymbol)).isEqualTo("abc")
    assertThat(StringShortener.TruncateToLength.shorten("abcd", 7, truncationSymbol)).isEqualTo("abcd")
    assertThat(StringShortener.TruncateToLength.shorten("abcde", 7, truncationSymbol)).isEqualTo("abcde")
    assertThat(StringShortener.TruncateToLength.shorten("abcdef", 7, truncationSymbol)).isEqualTo("abcdef")
    assertThat(StringShortener.TruncateToLength.shorten("abcdefg", 7, truncationSymbol)).isEqualTo("abcdefg")
    assertThat(StringShortener.TruncateToLength.shorten("abcdefgh", 7, truncationSymbol)).isEqualTo("abcdef…")
  }

  @Test
  fun testTruncateCenterWithSymbol() {
    assertThat(StringShortener.TruncateCenterToLength.shorten(text, 7, "...")).isEqualTo("ab...yz")
    assertThat(StringShortener.TruncateCenterToLength.shorten(text, 8, "...")).isEqualTo("abc...yz")
    assertThat(StringShortener.TruncateCenterToLength.shorten("a", 7, "...")).isEqualTo("a")
    assertThat(StringShortener.TruncateCenterToLength.shorten("ab", 7, "...")).isEqualTo("ab")
    assertThat(StringShortener.TruncateCenterToLength.shorten("abc", 7, "...")).isEqualTo("abc")
    assertThat(StringShortener.TruncateCenterToLength.shorten("abcd", 7, "...")).isEqualTo("abcd")
    assertThat(StringShortener.TruncateCenterToLength.shorten("abcde", 7, "...")).isEqualTo("abcde")
    assertThat(StringShortener.TruncateCenterToLength.shorten("abcdef", 7, "...")).isEqualTo("abcdef")
    assertThat(StringShortener.TruncateCenterToLength.shorten("abcdefg", 7, "...")).isEqualTo("abcdefg")
    assertThat(StringShortener.TruncateCenterToLength.shorten("abcdefgh", 7, "...")).isEqualTo("ab...gh")
  }
}
