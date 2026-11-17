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
package it.neckar.open.kotlin.lang

import assertk.*
import assertk.assertions.*
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import kotlin.time.measureTime

class StringExtKtTest {
  @Test
  fun testBetween() {
    assertThat("asdf".substringBetween("a", "f")).isEqualTo("sd")
    assertThat("aaaaaaaaa".substringBetween("a", "a")).isEqualTo("")

    assertFailure {
      "aaaaaaaaa".substringBetween("a", "NOT_EXISTS")
    }.hasMessage("End [NOT_EXISTS] not found in [aaaaaaaaa]")
    assertFailure {
      "aaaaaaaaa".substringBetween("NOT_EXISTS", "a")
    }.hasMessage("Start [NOT_EXISTS] not found in [aaaaaaaaa]")
    assertFailure {
      "aaaaaaaaa".substringBetween("NOT_EXISTS", "NOT_EXISTS")
    }.hasMessage("Start [NOT_EXISTS] not found in [aaaaaaaaa]")
  }

  @Disabled
  @Test
  fun testPerformance() {
    val count = 50_000

    measureTime {
      buildString {
        count.fastFor {
          append("This is a very long line!")
          deleteSuffix("!")
        }
      }
    }.also {
      println("Old took ${it.inWholeMilliseconds} ms")
    }
  }

  @Test
  fun testDeleteSuffix() {
    val builder = buildString {
      append("123456789df")
      deleteSuffix("df")
    }

    assertThat(builder).isEqualTo("123456789")
  }

  @Test
  fun testCssIdentifier() {
    assertThat("asdf".encodeForCssIdentifier()).isEqualTo("asdf")
    assertThat("as df".encodeForCssIdentifier()).isEqualTo("as_df")
    assertThat("""as :\/df""".encodeForCssIdentifier()).isEqualTo("as____df")
  }

  @Test
  fun testDeleteTrailing() {
    assertThat(buildString {
      deleteSuffix("x")
    }).isEqualTo("")

    assertThat(buildString {
      append("Hello World!x")
      deleteSuffix("x")
    }).isEqualTo("Hello World!")

    assertThat(buildString {
      append("Hello World!x!")
      deleteSuffix("x")
    }).isEqualTo("Hello World!x!")

    assertThat(buildString {
      append("Hello World!x!")
      deleteSuffix("!x!")
    }).isEqualTo("Hello World")
  }

  @Test
  fun testSplit() {
    assertThat("asdf".centerIndexOf(' ')).isEqualTo(-1)
    assertThat("as df".centerIndexOf(' ')).isEqualTo(2)
    assertThat("as dffsdfsdfsdf".centerIndexOf(' ')).isEqualTo(2)
    assertThat("0123456789 dddddd".centerIndexOf(' ')).isEqualTo(10)
    assertThat("0 23456789 dddddd".centerIndexOf(' ')).isEqualTo(10)
    assertThat("0 23456789 dddd d".centerIndexOf(' ')).isEqualTo(10)
  }

  @Test
  fun testTakeIfNotBlank() {
    assertThat("asdf".nullIfBlank()).isEqualTo("asdf")
    assertThat((null as String?).nullIfBlank()).isNull()
    assertThat(("  ").nullIfBlank()).isNull()
  }

  @Test
  fun testFileEncode() {
    assertThat("asdf".encodeForFileName()).isEqualTo("asdf")
    assertThat("äöüß?#+*".encodeForFileName()).isEqualTo("äöüß_#+_")
    assertThat("/".encodeForFileName()).isEqualTo("_")
    assertThat("\\".encodeForFileName()).isEqualTo("_")
  }

  @Test
  fun testWrap() {
    val text = "This is a text that is wrapped"
    assertThat(text.wrap(10)).containsExactly("This is a", "text that", "is wrapped")
    assertThat(text.wrap(5)).containsExactly("This", "is a", "text", "that", "is", "wrapp", "ed")
    assertThat(text.wrap(2)).containsExactly("Th", "is", "is", "a", "te", "xt", "th", "at", "is", "wr", "ap", "pe", "d")
    assertThat(text.wrap(9)).containsExactly("This is", "a text", "that is", "wrapped")

    for (i in 2..20) {
      val wrapped = text.wrap(i)
      wrapped.forEach {
        assertThat(it.length, "length: $i, wrapped: $wrapped ").isLessThanOrEqualTo(i)
      }
    }
  }

  @Test
  fun testWrapEdgeCases() {
    assertThat("".wrap(10)).isEmpty()
    assertThat("a".wrap(10)).containsExactly("a")
    assertThat(" ".wrap(10)).containsExactly(" ")
    assertThat("abc".wrap(10)).containsExactly("abc")
    assertThat("a bc".wrap(10)).containsExactly("a bc")

    assertThat(" a bc".wrap(2)).containsExactly(" a", "bc")
    assertThat(" a bc ".wrap(2)).containsExactly(" a", "bc")
    assertThat(" a bc".wrap(2)).containsExactly(" a", "bc")
    assertThat(" a  bc".wrap(2)).containsExactly(" a", " b", "c")
    assertThat(" a  bc ".wrap(2)).containsExactly(" a", " b", "c ")
  }

  @Test
  fun testTruncateVeryShort() {
    assertThat("abcde".truncateToLength(1)).isEqualTo("…")
    assertThat("abcde".truncateToLength(2)).isEqualTo("a…")
    assertThat("abcde".truncateToLength(3)).isEqualTo("ab…")
    assertThat("abcde".truncateToLength(4)).isEqualTo("abc…")
  }

  @Test
  fun testRestrictLengthWithEllipsis() {
    assertThat("ABCDEFHG".restrictLengthWithEllipsis(5)).isEqualTo("ABCD…")
    assertThat("ABCD".restrictLengthWithEllipsis(5)).isEqualTo("ABCD")
    assertThat("ABCDE".restrictLengthWithEllipsis(5)).isEqualTo("ABCDE")
    assertThat("ABCDEFHG".restrictLengthWithEllipsis(5, "...")).isEqualTo("AB...")
  }

  @Test
  fun `restrictLengthWithEllipsis truncates without the ellipsis when the ellipsis does not fit`() {
    assertThat("abc".restrictLengthWithEllipsis(0)).isEqualTo("")
    assertThat("abc".restrictLengthWithEllipsis(1)).isEqualTo("…")
    assertThat("abcdef".restrictLengthWithEllipsis(0, "...")).isEqualTo("")
    assertThat("abcdef".restrictLengthWithEllipsis(1, "...")).isEqualTo("a")
    assertThat("abcdef".restrictLengthWithEllipsis(2, "...")).isEqualTo("ab")
    assertThat("abcdef".restrictLengthWithEllipsis(3, "...")).isEqualTo("...")
  }

  @Test
  fun `truncateToLength returns the exclamation mark when the truncation symbol does not fit`() {
    assertThat("ABCDE".truncateToLength(1, "...")).isEqualTo("!")
    assertThat("ABCDE".truncateToLength(2, "...")).isEqualTo("!")
    assertThat("ABCDE".truncateToLength(3, "...")).isEqualTo("...")
    assertThat("ABCDE".truncateToLength(4, "...")).isEqualTo("A...")
  }

  @Test
  fun testTruncate() {
    assertThat("a".truncateToLength(12)).isEqualTo("a")
    assertThat("abcd".truncateToLength(12)).isEqualTo("abcd")

    "abcde".let { text ->
      assertThat(text.truncateToLength(12)).isEqualTo("abcde")
      assertThat(text.truncateToLength(5)).isEqualTo("abcde")
      assertThat(text.truncateToLength(4)).isEqualTo("abc…")
      assertThat(text.truncateToLength(3)).isEqualTo("ab…")
      assertThat(text.truncateToLength(2)).isEqualTo("a…")
      assertThat(text.truncateToLength(1)).isEqualTo("…")
    }

    "ab cd".let { text ->
      assertThat(text.truncateToLength(12)).isEqualTo("ab cd")
      assertThat(text.truncateToLength(5)).isEqualTo("ab cd")
      assertThat(text.truncateToLength(4)).isEqualTo("ab …")
      assertThat(text.truncateToLength(3)).isEqualTo("ab…")
      assertThat(text.truncateToLength(2)).isEqualTo("a…")
      assertThat(text.truncateToLength(1)).isEqualTo("…")
    }
  }


  @Test
  fun `when maxCharacters is 0, return null`() {
    assertThat("Hello, world!".truncateCenterToLength(0)).isNull()
  }

  @Test
  fun `when text length is less than or equal to maxCharacters, return original text`() {
    assertThat("Hello, world!".truncateCenterToLength(20)).isEqualTo("Hello, world!")
  }

  @Test
  fun `when truncationSymbol length is greater than maxCharacters, return !`() {
    assertThat("Hello, world!".truncateCenterToLength(1, "...")).isEqualTo("!")
  }

  @Test
  fun `when text length is greater than maxCharacters, return truncated text`() {
    assertThat("Hello, world!".truncateCenterToLength(10)).isEqualTo("Hello…rld!")
  }

  @Test
  fun `when text length is greater than maxCharacters and maxCharacters is odd, return truncated text with first part one character longer`() {
    val result = "Hello, world!".truncateCenterToLength(9)
    requireNotNull(result)
    assertThat(result).hasLength(9)
    assertThat(result).isEqualTo("Hell…rld!")
  }

  @Test
  fun encodeForDockerTagTest() {
    val string = "a/b:c.d e"
    assertThat(string.encodeForDockerTag()).isEqualTo("a_b_c_d_e")
  }

  @Test
  fun testKebapCase() {
    assertThat("HelloWorld".toKebabCase()).isEqualTo("hello-world")
    assertThat("Hello World".toKebabCase()).isEqualTo("hello-world")
    assertThat("Hello-World".toKebabCase()).isEqualTo("hello-world")
    assertThat("Hello_World".toKebabCase()).isEqualTo("hello-world")
    assertThat("HelloWorld123".toKebabCase()).isEqualTo("hello-world-123")
    assertThat("123HelloWorld".toKebabCase()).isEqualTo("123-hello-world")
    assertThat("_123Hello World".toKebabCase()).isEqualTo("123-hello-world")
  }

  @Test
  fun testSnakeCase() {
    assertThat("HelloWorld".toSnakeCase()).isEqualTo("hello_world")
    assertThat("Hello World".toSnakeCase()).isEqualTo("hello_world")
    assertThat("Hello-World".toSnakeCase()).isEqualTo("hello_world")
    assertThat("Hello_World".toSnakeCase()).isEqualTo("hello_world")
    assertThat("HelloWorld123".toSnakeCase()).isEqualTo("hello_world123")
    assertThat("123HelloWorld".toSnakeCase()).isEqualTo("123_hello_world")
    assertThat("-123Hello World".toSnakeCase()).isEqualTo("123_hello_world")
  }

  @Test
  fun testCamelCase() {
    assertThat("Hello World".toCamelCase()).isEqualTo("helloWorld")
    assertThat("hello-world".toCamelCase()).isEqualTo("helloWorld")
    assertThat("hello_world".toCamelCase()).isEqualTo("helloWorld")
    assertThat("Hello-World".toCamelCase()).isEqualTo("helloWorld")
    assertThat("Hello_World".toCamelCase()).isEqualTo("helloWorld")
    assertThat("HelloWorld123".toCamelCase()).isEqualTo("helloWorld123")
    assertThat("_123Hello-World".toCamelCase()).isEqualTo("123HelloWorld")
  }
}
