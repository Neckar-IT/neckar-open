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
package it.neckar.open.javafx

import it.neckar.open.javafx.test.JavaFxTest
import it.neckar.open.test.utils.*
import javafx.beans.binding.DoubleBinding
import javafx.beans.binding.StringBinding
import javafx.beans.property.SimpleDoubleProperty
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.lang.ref.WeakReference

@Disabled //for performance reasons
@JavaFxTest
class BindingsGcTest {
  @Test
  fun testOnlyBinding() {
    val property = SimpleDoubleProperty(17.0)
    val ref = createRef(property)

    //We create a binding - that is *not* held and should be garbage collected
    forceGc {
      ref.get() == null
    }
  }

  @Test
  fun testBindingWithTextField() {
    val property = SimpleDoubleProperty(17.0)

    val ref = createRef(property)

    val textField = Components.textFieldDoubleReadonly(ref.get()!!)
    //Should *not* be garbage collected — forceGc times out with IllegalStateException
    assertThrows(IllegalStateException::class.java) {
      forceGc {
        ref.get() == null
      }
    }
  }

  @Test
  fun testBindingWithTextFieldStringProperty() {
    val property = SimpleDoubleProperty(17.0)

    val ref = createRefAsString(property)

    val textField = Components.textFieldReadonly(ref.get()!!)
    //Should *not* be garbage collected — forceGc times out with IllegalStateException
    assertThrows(IllegalStateException::class.java) {
      forceGc {
        ref.get() == null
      }
    }
  }
}

private fun createRef(property: SimpleDoubleProperty): WeakReference<DoubleBinding> {
  return WeakReference(property.divide(100.0))
}

private fun createRefAsString(property: SimpleDoubleProperty): WeakReference<StringBinding> {
  return WeakReference(property.asString())
}
