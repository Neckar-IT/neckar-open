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

import assertk.*
import assertk.assertions.*
import it.neckar.open.observable.ObservableInt
import it.neckar.open.observable.ObservableObject
import it.neckar.open.observable.toNumber
import javafx.beans.property.SimpleStringProperty
import org.junit.jupiter.api.Test

/**
 *
 */
class ObservableObjectFxExtTest {
  @Test
  internal fun testToNumber() {
    val o = ObservableInt(17)

    val number = o.toNumber()
    assertThat(number.value.toDouble()).isEqualTo(17.0)

    o.value = 27
    assertThat(number.value.toDouble()).isEqualTo(27.0)

    number.value = 12.2
    assertThat(number.value.toDouble()).isEqualTo(12.0)
    assertThat(o.value).isEqualTo(12)
  }

  @Test
  internal fun testPropRepeat() {
    val observableObject = ObservableObject("a")
    assertThat(observableObject.toJavaFx()).isSameAs(observableObject.toJavaFx())
  }

  @Test
  internal fun testBindings() {
    val observableObject = ObservableObject("a")
    val fxProperty = SimpleStringProperty("asdf")

    assertThat(observableObject.value).isEqualTo("a")
    observableObject.bind(fxProperty)
    assertThat(observableObject.value).isEqualTo("asdf")

    fxProperty.set("foo")
    assertThat(observableObject.value).isEqualTo("foo")
  }

  @Test
  internal fun testToFx() {
    val observableObject = ObservableObject("a")
    val fxProperty = observableObject.toJavaFx()


    assertThat(observableObject.value).isEqualTo("a")
    assertThat(fxProperty.value).isEqualTo("a")

    fxProperty.set("foo")
    assertThat(observableObject.value).isEqualTo("foo")
    assertThat(fxProperty.value).isEqualTo("foo")

    observableObject.value = "bla"
    assertThat(observableObject.value).isEqualTo("bla")
    assertThat(fxProperty.value).isEqualTo("bla")
  }

  @Test
  internal fun testBidirectionalDirection() {
    //Check with JavaFX
    val fxProp0 = SimpleStringProperty("foo")
    val fxProp1 = SimpleStringProperty("bar")

    fxProp0.bindBidirectional(fxProp1)

    assertThat(fxProp0.value).isEqualTo("bar")
    assertThat(fxProp1.value).isEqualTo("bar")

    //Same with ObservableObject
    val observableObject0 = ObservableObject("foo")
    val observableObject1 = ObservableObject("bar")

    observableObject0.bindBidirectional(observableObject1)
    assertThat(observableObject0.value).isEqualTo("bar")
    assertThat(observableObject1.value).isEqualTo("bar")
  }
}
