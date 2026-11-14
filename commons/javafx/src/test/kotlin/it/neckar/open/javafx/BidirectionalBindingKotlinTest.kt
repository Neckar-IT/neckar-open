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
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleDoubleProperty
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleObjectProperty
import org.junit.jupiter.api.Test

class BidirectionalBindingKotlinTest {
  @Test
  fun testBooleanEnum() {
    val booleanProperty = SimpleBooleanProperty()
    val enumProperty = SimpleObjectProperty(MyEnum.True)

    BidirectionalBinding.bindBidirectional(booleanProperty, enumProperty, MyEnum.True, MyEnum.False)

    //Copied from A to B
    assertThat(booleanProperty.get()).isEqualTo(false)
    assertThat(enumProperty.get()).isEqualTo(MyEnum.False)

    booleanProperty.set(true)
    assertThat(booleanProperty.get()).isEqualTo(true)
    assertThat(enumProperty.get()).isEqualTo(MyEnum.True)

    booleanProperty.set(false)
    assertThat(booleanProperty.get()).isEqualTo(false)
    assertThat(enumProperty.get()).isEqualTo(MyEnum.False)


    //Changing B
    enumProperty.set(MyEnum.True)
    assertThat(booleanProperty.get()).isEqualTo(true)
    assertThat(enumProperty.get()).isEqualTo(MyEnum.True)

    enumProperty.set(MyEnum.False)
    assertThat(booleanProperty.get()).isEqualTo(false)
    assertThat(enumProperty.get()).isEqualTo(MyEnum.False)
  }

  internal enum class MyEnum {
    True,
    False
  }

  @Test
  fun testBinding() {
    val doubleProp = SimpleDoubleProperty(5.0)
    val intProp = SimpleIntegerProperty(105)

    doubleProp.bindBidirectionalWithFactor(intProp, 10.0)

    //initial values
    assertThat(doubleProp.value).isEqualTo(10.5)
    assertThat(intProp.value).isEqualTo(105)

    //set int
    intProp.value = 700
    assertThat(intProp.value).isEqualTo(700)
    assertThat(doubleProp.value).isEqualTo(70.0)

    //set double
    doubleProp.value = 80.0
    assertThat(intProp.value).isEqualTo(800)
    assertThat(doubleProp.value).isEqualTo(80.0)
  }
}
