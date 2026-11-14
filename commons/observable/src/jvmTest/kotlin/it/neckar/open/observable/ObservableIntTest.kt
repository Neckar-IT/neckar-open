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

class ObservableIntTest {
  @Test
  internal fun testTypes() {
    val observable: ObservableInt = ObservableInt(12)
    val observableReadOnly: ReadOnlyObservableInt = ObservableInt(12)

    assertThat(observable.get()).isEqualTo(12)
    assertThat(observableReadOnly.value).isEqualTo(12)
  }

  @Test
  fun testBindingIsEqual() {
    val observable = ObservableInt(12)

    val isEqualTo = observable.isEqualTo(56)
    assertThat(isEqualTo.value).isFalse()

    var updated: Boolean? = null
    isEqualTo.consume {
      updated = it
    }
    assertThat(updated).isNull()

    observable.value = 21
    assertThat(isEqualTo.value).isFalse()

    observable.value = 56
    assertThat(updated).isEqualTo(true)
    assertThat(isEqualTo.value).isTrue()
  }

}
