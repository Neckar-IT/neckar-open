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
package it.neckar.open.observable

import assertk.*
import assertk.assertions.*
import org.junit.jupiter.api.Test

/**
 */
class ObservableBindingTest {

  data class MyButtonState(val selected: Boolean, val magicNumber: Int) {
  }

  @Test
  internal fun testBindBooleanBidirectionalManually() {
    val stateProperty: ObservableObject<MyButtonState> = ObservableObject(MyButtonState(false, 77))

    val selectedProperty: ObservableBoolean = ObservableBoolean(false).also { selectedProperty ->
      //Update this property whenever the state property is updated
      stateProperty.consumeImmediately {
        selectedProperty.value = it.selected
      }

      //On update to the selected property write the changes to the state property
      selectedProperty.consume {
        val old = stateProperty.value
        stateProperty.value = old.copy(selected = it)
      }
    }

    verifyBidirectionalBindingWithConversion(stateProperty, selectedProperty)
  }

  @Test
  internal fun testBindBooleanBidirectional() {
    val stateProperty: ObservableObject<MyButtonState> = ObservableObject(MyButtonState(false, 77))
    val selectedProperty: ObservableBoolean = ObservableBoolean(false)

    val converterForward: (newValueToConvert: Boolean, oldConvertedValue: MyButtonState) -> MyButtonState = { selected, oldConvertedValue -> oldConvertedValue.copy(selected = selected) }
    val converterBack: (newValueToConvert: MyButtonState, oldConvertedValue: Boolean) -> Boolean = { newValueToConvert, _ -> newValueToConvert.selected }

    selectedProperty.bindBidirectional(stateProperty, converterForward, converterBack)

    verifyBidirectionalBindingWithConversion(stateProperty, selectedProperty)
  }

  private fun verifyBidirectionalBindingWithConversion(
    stateProperty: ObservableObject<MyButtonState>,
    selectedProperty: ObservableBoolean,
  ) {

    assertThat(stateProperty.value.selected).isFalse()
    assertThat(stateProperty.value.magicNumber).isEqualTo(77)
    assertThat(selectedProperty.value).isFalse()

    stateProperty.value = MyButtonState(true, 78)

    assertThat(stateProperty.value.selected).isTrue()
    assertThat(stateProperty.value.magicNumber).isEqualTo(78)
    assertThat(selectedProperty.value).isTrue()

    selectedProperty.value = false

    assertThat(stateProperty.value.selected).isFalse()
    assertThat(stateProperty.value.magicNumber).isEqualTo(78)
    assertThat(selectedProperty.value).isFalse()
  }

  @Test
  fun testBindMapped() {
    val observable0 = ObservableObject("a")

    val observableLength = ObservableInt(0)
    observableLength.bind(observable0.map { it.length })

    assertThat(observableLength.get()).isEqualTo(1)
    observable0.value = "abc"

    assertThat(observableLength.get()).isEqualTo(3)
  }

  /**
   * Bind to a nested value
   */
  @Test
  internal fun testProps() {
    val observable0 = ObservableObject(MyClass("Martha"))
    assertThat(observable0.value.name).isEqualTo("Martha")
    observable0.value.name = "Hans"
    assertThat(observable0.value.name).isEqualTo("Hans")
  }

  @Test
  internal fun testSelect() {
    val outer0 = MyClass("Martha")
    val outer1 = MyClass("Bla")

    val observable0 = ObservableObject(outer0)
    assertThat(observable0.value.name).isEqualTo("Martha")

    val selected: ReadOnlyObservableObject<String> = observable0.select {
      it.nameProperty
    }

    assertThat(selected.value).isEqualTo("Martha")

    //Change the inner value
    outer0.name = "Hans"
    assertThat(selected.value).isEqualTo("Hans")

    //Change the outer value
    observable0.value = outer1
    assertThat(selected.value).isEqualTo("Bla")

    outer1.name = "Fasel"
    assertThat(selected.value).isEqualTo("Fasel")

    //Changing the old outer value --> no changes
    outer0.name = "######"
    assertThat(selected.value).isEqualTo("Fasel") //did not change!
  }

  @Test
  internal fun testNullableValueInner() {
    val outer0 = MyClass("Martha")
    val outer1 = MyClass("Bla")

    val observable0 = ObservableObject(outer0)
    assertThat(observable0.value.nameNullable).isEqualTo("Martha")

    val selected: ReadOnlyObservableObject<String?> = observable0.select {
      it.nameNullableProperty
    }

    assertThat(selected.value).isEqualTo("Martha")

    //Change the inner value
    outer0.nameNullable = "Hans"
    assertThat(selected.value).isEqualTo("Hans")
    outer0.nameNullable = null
    assertThat(selected.value).isNull()

    //Change the outer value
    observable0.value = outer1
    assertThat(selected.value).isEqualTo("Bla")

    outer1.nameNullable = "Fasel"
    assertThat(selected.value).isEqualTo("Fasel")

    //Changing the old outer value --> no changes
    outer0.nameNullable = "######"
    assertThat(selected.value).isEqualTo("Fasel") //did not change!
  }

  @Test
  internal fun testBindingConversion() {
    val observable0 = ObservableObject("1")

    val observableInt = ObservableInt(0)
    assertThat(observableInt.value).isEqualTo(0)

    observableInt.bind(observable0.map {
      Integer.parseInt(it)
    })

    assertThat(observableInt.value).isEqualTo(1)
    observable0.value = "2"
    assertThat(observableInt.value).isEqualTo(2)
  }

  private class MyClass(name: String) {
    val nameProperty = ObservableObject(name)
    var name by nameProperty

    val nameNullableProperty = ObservableObject<String?>(name)
    var nameNullable by nameNullableProperty
  }
}
