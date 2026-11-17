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
package it.neckar.open.test.utils

import assertk.*
import assertk.assertions.*
import org.junit.jupiter.api.Test
import org.opentest4j.MultipleFailuresError
import kotlin.test.assertFailsWith

class AssertkExtensionsKtTest {
  @Test
  fun testdoesNotContainNull() {
    assertFailure {
      assertThat(listOf("a", null, "c")).doesNotContainNull()
    }.hasMessage("Element at index 1 is null")

    assertThat(listOf("a", "b", "c")).doesNotContainNull()
  }

  @Test
  fun testFailAll() {
    val numbers = listOf(1, 2, 3, 4, 5)

    assertFailure {
      assertAllSuspend(
        numbers.map {
          suspend {
            assertThat(it).isLessThan(3)
          }
        }
      )
    }.all {
      messageContains("Multiple Failures (3 failures)")
      messageContains("org.opentest4j.AssertionFailedError: expected to be less than:<3> but was:<3>")
      messageContains("org.opentest4j.AssertionFailedError: expected to be less than:<3> but was:<4>")
      messageContains("org.opentest4j.AssertionFailedError: expected to be less than:<3> but was:<5>")
      isInstanceOf(MultipleFailuresError::class)
    }
  }

  @Test
  fun testisEqualIgnoringWhitespacesEmptyLines() {
    assertFailure {
      assertThat(
        """
    a
    b
      """
      ).isEqualIgnoringWhitespacesEmptyLines(
        """
          a
          c
        """.trimIndent()
      )
    }.messageContains("Comparing Lines failed @ line 1")
  }

  @Test
  fun testLines() {
    assertThat(
      """
    a
    b


    c
    d
  """.trimIndent()
    ).isEqualComparingLinesTrim(
      """
a
b

c
d
    """.trimIndent()
    )

    assertFailsWith<AssertionError> {
      assertThat(
        """
      a
      b
      c
      d
    """.trimIndent()
      ).isEqualComparingLinesTrim(
        """
  a
  b
  cee###
  d
      """.trimIndent()
      )

    }
  }

  @Test
  fun testNaN() {
    assertFailure {
      assertThat(70.0).isNaN()
    }.hasMessage("expected to be NaN but was <70.0>")
  }

  @Test
  fun testAssertFirst() {
    listOf("a", "b").let {
      assertThat(it).all {
        first {
          it.isEqualTo("a")
        }
        last {
          it.isEqualTo("b")
        }
      }

      assertFailure {
        assertThat(it).first {
          it.isEqualTo("INVALID")
        }
      }.hasMessage("""expected [["a"]]:<"[INVALID]"> but was:<"[a]"> (["a", "b"])""")

      assertFailure {
        assertThat(it).last {
          it.isEqualTo("INVALID")
        }
      }.hasMessage("""expected [["b"]]:<"[INVALID]"> but was:<"[b]"> (["a", "b"])""")
    }
  }

  @Test
  fun testIsNotNullAndSuccess() {
    val value: String? = "hello"
    assertThat(value).isNotNullAnd {
      isEqualTo("hello")
      hasLength(5)
    }
  }

  @Test
  fun testIsNotNullAndFailsOnNull() {
    val value: String? = null
    assertFailure {
      assertThat(value).isNotNullAnd {
        isEqualTo("hello")
      }
    }.messageContains("to not be null")
  }

  @Test
  fun testIsNotNullAndCollectsMultipleFailures() {
    val value: String? = "hello"
    assertFailure {
      assertThat(value).isNotNullAnd {
        isEqualTo("world")
        hasLength(10)
      }
    }.all {
      messageContains("expected:<\"[world]\"> but was:<\"[hello]\">")
      messageContains("expected [length]:<[10]> but was:<[5]>")
      isInstanceOf(MultipleFailuresError::class)
    }
  }

  @Test
  fun testHasSingleElementSuccess() {
    assertThat(listOf("only")).hasSingleElement {
      isEqualTo("only")
      hasLength(4)
    }
  }

  @Test
  fun testHasSingleElementFailsOnEmpty() {
    assertFailure {
      assertThat(emptyList<String>()).hasSingleElement {
        isEqualTo("x")
      }
    }.messageContains("expected to have single element")
  }

  @Test
  fun testHasSingleElementFailsOnMultiple() {
    assertFailure {
      assertThat(listOf("a", "b")).hasSingleElement {
        isEqualTo("a")
      }
    }.messageContains("expected to have single element")
  }

  @Test
  fun testMessageContainsAllOfSuccess() {
    val ex = IllegalStateException("Serializer for class 'Foo' is not found. Please provide a serializer.")
    assertThat(ex).messageContainsAllOf(
      "Serializer for class 'Foo'",
      "Please provide",
    )
  }

  @Test
  fun testMessageContainsAllOfFailsOnMissing() {
    val ex = IllegalStateException("Some error message")
    assertFailure {
      assertThat(ex as Throwable).messageContainsAllOf("Some", "missing fragment")
    }.messageContains("missing")
  }

  @Test
  fun testMessageContainsAllOfFailsOnNullMessage() {
    val ex = RuntimeException()
    assertFailure {
      assertThat(ex as Throwable).messageContainsAllOf("anything")
    }.messageContains("message was null")
  }
}
