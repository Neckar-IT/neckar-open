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
@file:Suppress("RETURN_VALUE_NOT_USED")

package it.neckar.open.version

import assertk.*
import assertk.assertions.*
import com.google.common.collect.ImmutableList
import it.neckar.open.version.Version.Companion.valueOf
import it.neckar.open.version.VersionRange.Companion.from
import it.neckar.open.version.VersionRange.Companion.fromVersions
import it.neckar.open.version.VersionRange.Companion.single
import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 *
 */
class VersionRangeTest {
  @Test
  fun testSame() {
    (val min, val max) = from(1, 1, 1).to(1, 1, 1)
    assertThat(min).isEqualTo(valueOf(1, 1, 1))
    assertThat(max).isEqualTo(valueOf(1, 1, 1))
  }

  @Test
  @Throws(Exception::class)
  fun fromList() {
    assertThat(fromVersions(ImmutableList.of(valueOf(1, 0, 0), valueOf(2, 0, 0)))).isEqualTo(from(1, 0, 0).to(2, 0, 0))
    assertThat(
      assertFailsWith<IllegalArgumentException> {
        fromVersions(ImmutableList.of())
      }
    ).messageContains("Need at least one version")
    assertThat(fromVersions(ImmutableList.of(valueOf(1, 0, 0)))).isEqualTo(from(1, 0, 0).to(1, 0, 0))
    assertThat(fromVersions(ImmutableList.of(valueOf(2, 0, 0), valueOf(1, 0, 0)))).isEqualTo(from(1, 0, 0).to(2, 0, 0))
    assertThat(fromVersions(ImmutableList.of(valueOf(1, 5, 0), valueOf(2, 0, 0), valueOf(1, 0, 0)))).isEqualTo(from(1, 0, 0).to(2, 0, 0))
  }

  @Test
  fun testConstructor() {
    // Valid ranges construct successfully
    assertThat(from(1, 0, 0).to(2, 0, 0)).isNotNull()
    assertThat(from(1, 0, 0).to(1, 0, 0)).isNotNull()
    // Invalid range (from > to) must throw
    assertFailure {
      from(1, 0, 1).to(1, 0, 0)
    }
  }

  @Test
  fun testFormat() {
    MatcherAssert.assertThat(single(1, 0, 0).format(), CoreMatchers.`is`("[1.0.0]"))
    MatcherAssert.assertThat(from(1, 0, 0).to(1, 0, 0).format(), CoreMatchers.`is`("[1.0.0]"))
    MatcherAssert.assertThat(from(1, 0, 0).to(2, 0, 0).format(), CoreMatchers.`is`("[1.0.0-2.0.0]"))
  }

  @Test
  fun testSingle() {
    MatcherAssert.assertThat(single(1, 0, 0).max, CoreMatchers.`is`(valueOf(1, 0, 0)))
    MatcherAssert.assertThat(single(1, 1, 0).max, CoreMatchers.`is`(valueOf(1, 1, 0)))
    MatcherAssert.assertThat(single(valueOf(1, 0, 0)).max, CoreMatchers.`is`(valueOf(1, 0, 0)))
    MatcherAssert.assertThat(single(valueOf(1, 1, 0)).max, CoreMatchers.`is`(valueOf(1, 1, 0)))
  }

  @Test
  fun testFluent() {
    assertEquals(from(1, 0, 0).to(2, 0, 0), VersionRange(Version(1, 0, 0), Version(2, 0, 0)))
  }

  @Test
  fun testMinMax() {
    run {
      (val min, val max) = VersionRange(Version(1, 0, 0), Version(2, 0, 0))
      assertEquals(min, Version(1, 0, 0))
      assertEquals(max, Version(2, 0, 0))
    }
    run {
      (val min, val max) = VersionRange(Version(1, 0, 0), Version(2, 0, 0), false, false)
      assertEquals(min, Version(1, 0, 0))
      assertEquals(max, Version(2, 0, 0))
    }
  }

  @Test
  fun testOverlap() {
    val range = VersionRange(Version(1, 0, 0), Version(2, 0, 0))
    assertTrue(range.overlaps(VersionRange(Version(1, 0, 0), Version(2, 0, 0))))
    assertTrue(range.overlaps(VersionRange(Version(1, 0, 0), Version(1, 0, 0))))
    assertTrue(range.overlaps(VersionRange(Version(2, 0, 0), Version(2, 0, 0))))
    assertFalse(range.overlaps(VersionRange(Version(0, 0, 0), Version(0, 99, 99))))
    assertFalse(range.overlaps(VersionRange(Version(0, 0, 0), Version(1, 0, 0), true, false)))
    assertTrue(range.overlaps(VersionRange(Version(0, 0, 0), Version(1, 0, 0), true, true)))
    assertFalse(range.overlaps(VersionRange(Version(2, 0, 1), Version(3, 0, 0))))
    assertFalse(range.overlaps(VersionRange(Version(2, 0, 0), Version(3, 0, 0), false, true)))
    assertTrue(range.overlaps(VersionRange(Version(2, 0, 0), Version(3, 0, 0), true, true)))
  }

  @Test
  fun testContains() {
    assertTrue(VersionRange(Version(1, 0, 0), Version(2, 0, 0)).containsCompletely(VersionRange(Version(1, 0, 0), Version(2, 0, 0))))
    assertTrue(VersionRange(Version(1, 0, 0), Version(2, 0, 0)).containsCompletely(VersionRange(Version(1, 0, 0), Version(1, 0, 0))))
    assertTrue(VersionRange(Version(1, 0, 0), Version(2, 0, 0)).containsCompletely(VersionRange(Version(2, 0, 0), Version(2, 0, 0))))
    assertFalse(VersionRange(Version(1, 0, 0), Version(2, 0, 0), false, true).containsCompletely(VersionRange(Version(1, 0, 0), Version(2, 0, 0))))
    assertFalse(VersionRange(Version(1, 0, 0), Version(2, 0, 0), true, false).containsCompletely(VersionRange(Version(1, 0, 0), Version(2, 0, 0))))
    assertTrue(VersionRange(Version(1, 0, 0), Version(2, 0, 0), true, true).containsCompletely(VersionRange(Version(1, 0, 0), Version(2, 0, 0), false, false)))
    assertTrue(VersionRange(Version(1, 0, 0), Version(2, 0, 0), true, true).containsCompletely(VersionRange(Version(1, 0, 0), Version(2, 0, 0), true, false)))
    assertTrue(VersionRange(Version(1, 0, 0), Version(2, 0, 0), true, true).containsCompletely(VersionRange(Version(1, 0, 0), Version(2, 0, 0), false, true)))
    assertTrue(VersionRange(Version(1, 0, 0), Version(2, 0, 0), true, true).containsCompletely(VersionRange(Version(1, 0, 0), Version(2, 0, 0), true, true)))
    assertFalse(VersionRange(Version(1, 0, 0), Version(2, 0, 0), false, true).containsCompletely(VersionRange(Version(1, 0, 0), Version(2, 0, 0), true, true)))
    assertFalse(VersionRange(Version(1, 0, 0), Version(2, 0, 0), false, true).containsCompletely(VersionRange(Version(1, 0, 0), Version(2, 0, 0), true, false)))
    assertFalse(VersionRange(Version(1, 0, 0), Version(2, 0, 0), true, false).containsCompletely(VersionRange(Version(1, 0, 0), Version(2, 0, 0), true, true)))
    assertFalse(VersionRange(Version(1, 0, 0), Version(2, 0, 0), true, false).containsCompletely(VersionRange(Version(1, 0, 0), Version(2, 0, 0), false, true)))
    assertFalse(VersionRange(Version(1, 0, 0), Version(2, 0, 0), false, false).containsCompletely(VersionRange(Version(1, 0, 0), Version(2, 0, 0), true, true)))
  }

  @Test
  fun testExclude() {
    run {
      val range = VersionRange(Version(1, 0, 0), Version(2, 0, 0), true, true)
      assertTrue(range.contains(Version(1, 0, 0)))
      assertTrue(range.contains(Version(1, 0, 1)))
      assertTrue(range.contains(Version(1, 99, 99)))
      assertTrue(range.contains(Version(2, 0, 0)))
      assertEquals("[1.0.0-2.0.0]", range.toString())
    }
    run {
      val range = VersionRange(Version(1, 0, 0), Version(2, 0, 0), true, false)
      assertTrue(range.contains(Version(1, 0, 0)))
      assertTrue(range.contains(Version(1, 0, 1)))
      assertTrue(range.contains(Version(1, 99, 99)))
      assertFalse(range.contains(Version(2, 0, 0)))
      assertEquals("[1.0.0-2.0.0[", range.toString())
    }
    run {
      val range = VersionRange(Version(1, 0, 0), Version(2, 0, 0), false, true)
      assertFalse(range.contains(Version(1, 0, 0)))
      assertTrue(range.contains(Version(1, 0, 1)))
      assertTrue(range.contains(Version(1, 99, 99)))
      assertTrue(range.contains(Version(2, 0, 0)))
      assertEquals("]1.0.0-2.0.0]", range.toString())
    }
    run {
      val range = VersionRange(Version(1, 0, 0), Version(2, 0, 0), false, false)
      assertFalse(range.contains(Version(1, 0, 0)))
      assertTrue(range.contains(Version(1, 0, 1)))
      assertTrue(range.contains(Version(1, 99, 99)))
      assertFalse(range.contains(Version(2, 0, 0)))
      assertEquals("]1.0.0-2.0.0[", range.toString())
    }
  }

  @Test
  fun testIt() {
    val range = VersionRange(Version(1, 0, 0), Version(1, 1, 90))
    assertTrue(range.contains(Version(1, 0, 0)))
    assertTrue(range.contains(Version(1, 1, 0)))
    assertTrue(range.contains(Version(1, 1, 89)))
    assertTrue(range.contains(Version(1, 1, 90)))
    assertFalse(range.contains(Version(1, 1, 91)))
    assertFalse(range.contains(Version(1, 2, 0)))
    assertFalse(range.contains(Version(0, 99, 99)))
  }

  @Test
  fun testEquals() {
    assertEquals(VersionRange(Version(1, 0, 0), Version(1, 1, 90)), VersionRange(Version(1, 0, 0), Version(1, 1, 90)))
    assertEquals(VersionRange(Version(1, 0, 0), Version(2, 1, 90)), VersionRange(Version(1, 0, 0), Version(2, 1, 90)))
    assertNotEquals(VersionRange(Version(1, 0, 0), Version(2, 1, 90)), VersionRange(Version(1, 0, 0), Version(2, 1, 91)))
    assertNotEquals(VersionRange(Version(1, 0, 0), Version(2, 1, 90)), VersionRange(Version(1, 0, 0), Version(1, 1, 90)))
    assertNotEquals(VersionRange(Version(1, 0, 1), Version(2, 1, 90)), VersionRange(Version(1, 0, 0), Version(2, 1, 90)))
  }

  @Test
  fun testToString() {
    assertEquals("[1.0.0-1.1.90]", VersionRange(Version(1, 0, 0), Version(1, 1, 90)).toString())
  }

}
