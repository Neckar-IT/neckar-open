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
@file:Suppress("RETURN_VALUE_NOT_USED") // Test-scope subscriptions don't need lifecycle management

package it.neckar.open.observable

import assertk.*
import assertk.assertions.*
import org.junit.jupiter.api.Test

class BindingsKtTest {
  @Test
  fun testSelect() {
    val fooProperty = ObservableObject(Foo("first"))

    assertThat(fooProperty.value.name.value).isEqualTo("first")
    val selectedName = fooProperty.select {
      it.name
    }

    assertThat(selectedName.value).isEqualTo("first")

    fooProperty.value = Foo("second")
    assertThat(selectedName.value).isEqualTo("second")
    fooProperty.value.name.value = "second"
  }

  @Test
  fun testReduce() {
    val prop1 = ObservableString("1")
    val prop2 = ObservableString("2")

    val reduced = prop1.reduce(prop2) {
      it
    }

    var updated: List<String>? = null
    reduced.consume {
      assertThat(updated).isNull()
      updated = it
    }

    assertThat(updated).isNull()
    prop2.value = "2updated"
    assertThat(updated).isNotNull()
    assertThat(updated!!).containsExactly("1", "2updated")
  }

  private class Foo(initialName: String) {
    val name = ObservableString(initialName)
  }
}
