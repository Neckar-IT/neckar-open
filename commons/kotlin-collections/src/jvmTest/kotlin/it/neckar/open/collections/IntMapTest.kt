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
package it.neckar.open.collections

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotEqualTo
import org.junit.jupiter.api.Test

class IntMapTest {
  @Test
  fun `equals is reflexive and value-based`() {
    val a = IntMap<String>()
    a[1] = "x"
    a[2] = "y"

    assertThat(a).isEqualTo(a)

    val b = IntMap<String>()
    b[1] = "x"
    b[2] = "y"
    assertThat(a).isEqualTo(b)

    val c = IntMap<String>()
    c[1] = "x"
    c[2] = "z"
    assertThat(a).isNotEqualTo(c)
  }

  @Test
  fun `equals is symmetric - a subset must not equal its superset`() {
    val subset = IntMap<String>()
    subset[1] = "x"

    val superset = IntMap<String>()
    superset[1] = "x"
    superset[2] = "y"

    //Both directions must agree, and a subset differs from its superset.
    assertThat(subset).isNotEqualTo(superset)
    assertThat(superset).isNotEqualTo(subset)
  }
}
