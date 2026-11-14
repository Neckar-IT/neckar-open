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

import it.neckar.open.javafx.BidirectionalBinding.bindBidirectional
import javafx.beans.property.DoubleProperty
import javafx.beans.property.SimpleDoubleProperty
import javafx.beans.property.SimpleStringProperty
import javafx.beans.property.StringProperty
import javafx.beans.value.ObservableValue
import assertk.*
import assertk.assertions.*

import org.junit.jupiter.api.Test

internal class BidirectionalBindingTest {
  @Test
  fun testBuilder() {
    val doubleProperty: DoubleProperty = SimpleDoubleProperty(0.0)
    val stringProperty: StringProperty = SimpleStringProperty("7")

    bindBidirectional(doubleProperty, stringProperty) {
      a2b = { newValue -> newValue.toDouble().toString() }
      b2a = { newValue: String -> newValue.toDouble() }
    }

    assertThat(doubleProperty.doubleValue()).isEqualTo(0.0)
    assertThat(stringProperty.value).isEqualTo("0.0")

    //assign new double
    doubleProperty.value = 17.0
    assertThat(doubleProperty.doubleValue()).isEqualTo(17.0)
    assertThat(stringProperty.value).isEqualTo("17.0")

    //assign string
    stringProperty.value = "22.2"
    assertThat(doubleProperty.doubleValue()).isEqualTo(22.2)
    assertThat(stringProperty.value).isEqualTo("22.2")
  }

  @Test
  fun testWithConverter() {
    val doubleProperty: DoubleProperty = SimpleDoubleProperty(0.0)
    val stringProperty: StringProperty = SimpleStringProperty("7")

    bindBidirectional(doubleProperty,
      stringProperty,
      { number: Number -> number.toDouble().toString() }, { s: String -> s.toDouble() })


    assertThat(doubleProperty.doubleValue()).isEqualTo(0.0)
    assertThat(stringProperty.value).isEqualTo("0.0")

    //assign new double
    doubleProperty.value = 17.0
    assertThat(doubleProperty.doubleValue()).isEqualTo(17.0)
    assertThat(stringProperty.value).isEqualTo("17.0")

    //assign string
    stringProperty.value = "22.2"
    assertThat(doubleProperty.doubleValue()).isEqualTo(22.2)
    assertThat(stringProperty.value).isEqualTo("22.2")
  }

  @Test
  fun bindBidirectional() {
    val lengthCommunication: DoubleProperty = SimpleDoubleProperty(0.0)
    val lengthUI: DoubleProperty = SimpleDoubleProperty(0.0)
    val offsetProperty: DoubleProperty = SimpleDoubleProperty(0.0)

    val updateCounter = intArrayOf(0)

    bindBidirectional(
      lengthCommunication,
      lengthUI,
      { _: ObservableValue<out Number?>?, _: Number?, newValue: Number? ->
        updateCounter[0]++
        org.junit.jupiter.api.Assertions.assertEquals(newValue, lengthCommunication.get())
        lengthUI.value = (lengthCommunication.get() + offsetProperty.get()) / 10
      },
      { _: ObservableValue<out Number?>?, _: Number?, newValue: Number? ->
        updateCounter[0]++
        org.junit.jupiter.api.Assertions.assertEquals(newValue, lengthUI.get())
        lengthCommunication.value = lengthUI.get() * 10 - offsetProperty.get()
      },
      offsetProperty
    )

    assertThat(lengthCommunication.get()).isEqualTo(0.0)
    assertThat(lengthUI.get()).isEqualTo(0.0)

    lengthCommunication.set(10.0)
    assertThat(lengthCommunication.get()).isEqualTo(10.0)
    assertThat(lengthUI.get()).isEqualTo(1.0)

    offsetProperty.value = 20.0
    assertThat(lengthCommunication.get()).isEqualTo(10.0)
    assertThat(lengthUI.get()).isEqualTo(3.0)

    lengthUI.value = 5.0
    assertThat(lengthCommunication.get()).isEqualTo(30.0)
    assertThat(lengthUI.get()).isEqualTo(5.0)
  }
}
