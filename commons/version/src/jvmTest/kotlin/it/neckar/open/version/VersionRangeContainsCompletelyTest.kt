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

import assertk.assertThat
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import org.junit.jupiter.api.Test

class VersionRangeContainsCompletelyTest {
  private val v1 = Version(1, 0, 0)
  private val v2 = Version(2, 0, 0)

  @Test
  fun `an open range contains itself completely`() {
    val open = VersionRange(v1, v2, includeLower = false, includeUpper = false)
    assertThat(open.containsCompletely(open)).isTrue()
  }

  @Test
  fun `a closed range contains itself completely`() {
    val closed = VersionRange(v1, v2, includeLower = true, includeUpper = true)
    assertThat(closed.containsCompletely(closed)).isTrue()
  }

  @Test
  fun `a closed range contains the open range with the same borders`() {
    val closed = VersionRange(v1, v2, includeLower = true, includeUpper = true)
    val open = VersionRange(v1, v2, includeLower = false, includeUpper = false)
    assertThat(closed.containsCompletely(open)).isTrue()
  }

  @Test
  fun `an open range does not contain the closed range with the same borders`() {
    val open = VersionRange(v1, v2, includeLower = false, includeUpper = false)
    val closed = VersionRange(v1, v2, includeLower = true, includeUpper = true)
    // other includes its endpoints, this excludes them → not fully contained
    assertThat(open.containsCompletely(closed)).isFalse()
  }

  @Test
  fun `a range does not contain a wider one`() {
    val inner = VersionRange(Version(1, 5, 0), Version(1, 8, 0))
    val wider = VersionRange(v1, v2)
    assertThat(inner.containsCompletely(wider)).isFalse()
  }
}
