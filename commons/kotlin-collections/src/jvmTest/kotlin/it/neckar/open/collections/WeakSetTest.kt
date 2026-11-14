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

import assertk.*
import assertk.assertions.*
import it.neckar.open.test.utils.*
import org.junit.jupiter.api.Test

class WeakSetTest {
  @Test
  fun testWeakSet() {
    val weakSet = WeakSet<SetElement>()

    SetElement(1).let {
      weakSet.add(it)
      assertThat(weakSet.contains(it))
    }
    assertThat(weakSet.size).isEqualTo(1)

    SetElement(2).let {
      weakSet.add(it)
      assertThat(weakSet.contains(it))
    }
    assertThat(weakSet.size).isEqualTo(2)

    SetElement(3).let {
      weakSet.add(it)
      assertThat(weakSet.contains(it))
    }
    assertThat(weakSet.size).isEqualTo(3)

    assertThat(weakSet.remove(SetElement(1))).isTrue()
    assertThat(weakSet.remove(SetElement(2))).isTrue()
    assertThat(weakSet.remove(SetElement(3))).isTrue()

    assertThat(weakSet.isEmpty())
    assertThat(weakSet.size).isEqualTo(0)
  }

  @Test
  fun testWeakSetGc() {
    val weakSet = WeakSet<SetElement>()

    weakSet.add(SetElement(1))
    weakSet.add(SetElement(2))
    weakSet.add(SetElement(3))

    forceGc {
      !weakSet.contains(SetElement(1)) &&
        !weakSet.contains(SetElement(2)) &&
        !weakSet.contains(SetElement(3))
    }

    //Ensure no elements are left
    assertThat(weakSet.isEmpty())
  }

  data class SetElement(val id: Int)
}

