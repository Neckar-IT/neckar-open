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
package it.neckar.open.javafx.properties

import assertk.*
import assertk.assertions.*
import javafx.beans.property.DoubleProperty
import javafx.beans.property.IntegerProperty
import javafx.beans.property.SimpleDoubleProperty
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleStringProperty
import javafx.beans.property.StringProperty
import javafx.beans.value.ChangeListener
import javafx.beans.value.ObservableValue
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicBoolean

/**
 */
internal class KotlinFxPropertiesKtTest {
  @Test
  internal fun name() {
    val foo = Foo()

    assertThat(foo.name).isEqualTo("")
    foo.name = "asdf"
    assertThat(foo.name).isEqualTo("asdf")
    assertThat(foo.nameProperty.get()).isEqualTo("asdf")
  }

  @Test
  internal fun testSetMultipleTimes() {
    val foo = Foo()
    foo.name = "daName"

    val called = AtomicBoolean()

    foo.nameProperty.addListener(ChangeListener<String?> { _, _, _ ->
      called.set(true)
    })


    assertThat(called.get()).isFalse()
    //Same name, should not trigger the listener
    foo.name = "daName"
    assertThat(called.get()).isFalse()
    foo.name = "daName2"
    assertThat(called.get()).isTrue()
  }
}

class Foo {
  val nameProperty: StringProperty = SimpleStringProperty("")
  var name: String by nameProperty

  val ageProperty: DoubleProperty = SimpleDoubleProperty(7.0)
  var age: Double by ageProperty

  val age2Property: IntegerProperty = SimpleIntegerProperty(7)
  var age2: Int by age2Property
}
