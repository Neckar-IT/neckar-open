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
package it.neckar.open.version

import assertk.*
import assertk.assertions.*
import it.neckar.open.version.Version.Companion.parse
import it.neckar.open.version.Version.Companion.valueOf
import it.neckar.open.version.Version.Companion.verifyMatch
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith

/**
 *
 */
class VersionTest {
  @Test
  fun testIsSnapshot() {
    assertThat(Version.of(1, 2, 3).isSnapshot).isFalse()
    assertThat(Version.of(1, 2, 3, "SNAPSHOT").isSnapshot).isTrue()
  }

  @Test
  fun testSmapper() {
    assertThat(valueOf(1, 0, 0).smallerThan(valueOf(2, 0, 0))).isTrue()
    assertThat(valueOf(3, 1, 0, "SNAPSHOT").smallerThan(valueOf(2, 0, 0))).isFalse()
    assertThat(valueOf(2, 0, 0).smallerThan(valueOf(3, 1, 0, "SNAPSHOT"))).isTrue()
    assertThat(valueOf(3, 0, 0).smallerThan(valueOf(3, 1, 0, "SNAPSHOT"))).isTrue()
  }

  @Test
  fun parseLongNumber() {
    val version = parse("0.0.110")
    assertThat(version.major).isEqualTo(0)
    assertThat(version.minor).isEqualTo(0)
    assertThat(version.build).isEqualTo(110)
  }

  @Test
  fun testMatch() {
    verifyMatch(valueOf(1, 2, 3), valueOf(1, 2, 3))
    assertFailsWith<VersionMismatchException> {
      verifyMatch(valueOf(1, 2, 3), valueOf(1, 2, 4))
    }
  }

  @Test
  fun testToInt() {
    assertEquals(10203, Version(1, 2, 3).toInt())
    assertEquals(123456, Version(12, 34, 56).toInt())
  }

  @Test
  fun testEquals() {
    assertEquals(Version(1, 2, 3), Version(1, 2, 3))
    assertEquals(Version(1, 2, 3, "asdf"), Version(1, 2, 3, "asdf"))
  }

  @Test
  fun testToString() {
    assertEquals("1.2.3", Version(1, 2, 3).toString())
    assertEquals("1.2.3-asdf", Version(1, 2, 3, "asdf").toString())
  }

  @Test
  fun testParse() {
    assertEquals(Version(1, 2, 3), parse("1.2.3"))
    assertEquals(Version(1, 2, 3, "build76"), parse("1.2.3-build76"))
    assertEquals(Version(1, 2, 3, "build76"), parse(Version(1, 2, 3, "build76").toString()))
  }

  @Test
  fun testCompareGreater() {
    assertTrue(Version(1, 2, 3).sameOrGreaterThan(Version(1, 2, 3)))
    assertTrue(Version(1, 2, 3).sameOrGreaterThan(Version(1, 2, 2)))
    assertTrue(Version(1, 2, 3).sameOrGreaterThan(Version(0, 2, 2)))
    assertFalse(Version(1, 2, 3).sameOrGreaterThan(Version(1, 2, 4)))
  }

  @Test
  fun testCompareSmaller() {
    assertTrue(Version(1, 2, 3).sameOrSmallerThan(Version(1, 2, 3)))
    assertTrue(Version(1, 2, 3).sameOrSmallerThan(Version(1, 2, 4)))
    assertTrue(Version(1, 2, 3).sameOrSmallerThan(Version(2, 2, 2)))
    assertFalse(Version(1, 2, 3).sameOrSmallerThan(Version(0, 2, 4)))
  }

  @Test
  fun withMethods() {
    val version = valueOf(1, 2, 3)
    assertThat(version.withSuffix("daSuffix").suffix).isEqualTo("daSuffix")
  }

}
