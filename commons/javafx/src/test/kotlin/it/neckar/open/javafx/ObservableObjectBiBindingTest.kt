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
import it.neckar.open.javafx.test.JavaFxTest
import it.neckar.open.observable.ObservableBoolean
import javafx.beans.property.SimpleStringProperty
import javafx.beans.property.StringProperty
import org.junit.jupiter.api.Test


/**
 *
 */
@JavaFxTest
class ObservableObjectBiBindingTest {
  @Test
  fun testBindingButtonSelected() {
    val toggleButton = Components.toggleButton("toggle") { _, selected ->
    }

    val selectedProperty = toggleButton.selectedProperty()
    val selectedObservable = ObservableBoolean(false).also { it.bindToJavaFx(selectedProperty) }

    assertThat(selectedProperty.value).isFalse()
    assertThat(selectedObservable.value).isFalse()


    //Update selected property

    selectedProperty.value = true

    assertThat(selectedProperty.value).isTrue()
    assertThat(selectedObservable.value).isTrue()

    selectedProperty.value = true

    assertThat(selectedProperty.value).isTrue()
    assertThat(selectedObservable.value).isTrue()

    selectedProperty.value = false

    assertThat(selectedProperty.value).isFalse()
    assertThat(selectedObservable.value).isFalse()
  }

  @Test
  fun testBidirectionalBinding() {
    val toggleButton = Components.toggleButton("toggle") { _, selected ->
    }

    val selectedProperty = toggleButton.selectedProperty()
    val selectedObservable = ObservableBoolean(false).also { it.bindBidirectionalToJavaFx(selectedProperty) }

    assertThat(selectedProperty.value).isFalse()
    assertThat(selectedObservable.value).isFalse()


    //Update selected property

    selectedProperty.value = true

    assertThat(selectedProperty.value).isTrue()
    assertThat(selectedObservable.value).isTrue()

    selectedProperty.value = true

    assertThat(selectedProperty.value).isTrue()
    assertThat(selectedObservable.value).isTrue()

    selectedProperty.value = false

    assertThat(selectedProperty.value).isFalse()
    assertThat(selectedObservable.value).isFalse()


    //Update observable

    selectedObservable.value = true

    assertThat(selectedProperty.value).isTrue()
    assertThat(selectedObservable.value).isTrue()

    selectedObservable.value = false

    assertThat(selectedProperty.value).isFalse()
    assertThat(selectedObservable.value).isFalse()
  }

  @Test
  fun testJavaFxBiBindingOrder() {
    val a: StringProperty = SimpleStringProperty("Hello")
    val b: StringProperty = SimpleStringProperty("World")

    a.bindBidirectional(b)

    assertThat(a.value).isEqualTo("World")
    assertThat(b.value).isEqualTo("World")
  }

  @Test
  fun testJavaFxBiBindingOrder2() {
    val a: StringProperty = SimpleStringProperty("Hello")
    val b: StringProperty = SimpleStringProperty("World")

    b.bindBidirectional(a)

    assertThat(a.value).isEqualTo("Hello")
    assertThat(b.value).isEqualTo("Hello")
  }
}
