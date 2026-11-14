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

import it.neckar.open.observable.ObservableBoolean
import it.neckar.open.observable.ObservableDouble
import it.neckar.open.observable.ObservableObject
import it.neckar.open.observable.ReadOnlyObservableObject
import javafx.beans.property.DoubleProperty
import javafx.beans.property.ObjectProperty
import javafx.beans.property.Property
import javafx.beans.property.ReadOnlyDoubleProperty
import javafx.beans.property.ReadOnlyObjectProperty
import javafx.beans.property.SimpleDoubleProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.value.ObservableBooleanValue
import javafx.beans.value.ObservableDoubleValue
import javafx.beans.value.ObservableValue

/**
 * Contains extension methods that allow binding of [ObservableObject] to JavaFX properties
 */

/**
 * The key that is used to store the observable value this object is bound to
 */
const val KeyFxBoundTo: String = "fxBoundTo"

/**
 * The key that is used to store the JavaFX *object* property that has been created for this observable object
 */
const val KeyFxObjectProperty: String = "fxObjectProperty"

/**
 * The key that is used to store the JavaFX *double* property that has been created for this observable object
 */
const val KeyFxDoubleProperty: String = "fxDoubleProperty"

/**
 * The key used to store the subscription that forwards value changes to the JavaFX object property.
 */
const val KeyFxObjectSubscription: String = "fxObjectSubscription"

/**
 * The key used to store the subscription that forwards value changes to the JavaFX double property.
 */
const val KeyFxDoubleSubscription: String = "fxDoubleSubscription"

/**
 * Converts an observable object to a property
 */
fun ObservableObject<Double>.toJavaFx(): DoubleProperty {
  getDependentObject(KeyFxDoubleProperty)?.let {
    @Suppress("UNCHECKED_CAST")
    return it as DoubleProperty
  }

  val fxProperty = SimpleDoubleProperty(value)
  addUpstreamSubscription(consume(false) { newValue -> fxProperty.value = newValue })
  fxProperty.addListener { _, _, newValue ->
    value = newValue.toDouble()
  }

  addDependentObject(KeyFxDoubleProperty, fxProperty)
  return fxProperty
}

/**
 * Returns a *READ ONLY* property
 */
fun ReadOnlyObservableObject<Double>.toJavaFx(): ReadOnlyDoubleProperty {
  getDependentObject(KeyFxDoubleProperty)?.let {
    @Suppress("UNCHECKED_CAST")
    return it as DoubleProperty
  }

  val fxProperty = SimpleDoubleProperty(value)
  //Keep the subscription alive as a dependent object so it shares the lifetime of this observable.
  addDependentObject(KeyFxDoubleSubscription, consume(false) { newValue -> fxProperty.value = newValue })

  addDependentObject(KeyFxDoubleProperty, fxProperty)
  return fxProperty
}

fun <T> ObservableObject<T>.toJavaFx(): ObjectProperty<T> {
  getDependentObject(KeyFxObjectProperty)?.let {
    @Suppress("UNCHECKED_CAST")
    return it as ObjectProperty<T>
  }

  val fxProperty = SimpleObjectProperty(value)
  addUpstreamSubscription(consume { newValue -> fxProperty.value = newValue })
  fxProperty.addListener { _, _, newValue ->
    value = newValue
  }

  addDependentObject(KeyFxObjectProperty, fxProperty)
  return fxProperty
}

fun <T> ReadOnlyObservableObject<T>.toJavaFx(): ReadOnlyObjectProperty<T> {
  getDependentObject(KeyFxObjectProperty)?.let {
    @Suppress("UNCHECKED_CAST")
    return it as ObjectProperty<T>
  }

  val fxProperty = SimpleObjectProperty(value)
  //Keep the subscription alive as a dependent object so it shares the lifetime of this observable.
  addDependentObject(KeyFxObjectSubscription, consume { newValue -> fxProperty.value = newValue })

  addDependentObject(KeyFxObjectProperty, fxProperty)
  return fxProperty
}

/**
 * Binds this object to the given binding
 */
fun <T> ObservableObject<T>.bind(observableValue: ObservableValue<T>) {
  markAsBound()

  observableValue.addListener { _, _, newValue ->
    updateFromBinding(newValue)
  }
  updateFromBinding(observableValue.value)

  //Avoid premature GC
  addDependentObject(KeyFxBoundTo, observableValue)
}

/**
 * Binds this observable object to a JavaFX property
 */
fun <T> ObservableObject<in T>.bindToJavaFx(fxProperty: ObservableValue<out T>) {
  markAsBound()

  fxProperty.addListener { _, _, newValue ->
    updateFromBinding(newValue)
  }

  updateFromBinding(fxProperty.value)

  //Avoid premature GC
  addDependentObject(KeyFxBoundTo, fxProperty)
}

fun ObservableDouble.bindToJavaFx(fxProperty: ObservableDoubleValue) {
  markAsBound()

  fxProperty.addListener { _, _, newValue ->
    updateFromBinding(newValue.toDouble())
  }

  updateFromBinding(fxProperty.value.toDouble())

  //Avoid premature GC
  addDependentObject(KeyFxBoundTo, fxProperty)
}

fun ObservableBoolean.bindToJavaFx(fxProperty: ObservableBooleanValue) {
  markAsBound()

  fxProperty.addListener { _, _, newValue ->
    updateFromBinding(newValue)
  }

  updateFromBinding(fxProperty.value)

  //Avoid premature GC
  addDependentObject(KeyFxBoundTo, fxProperty)
}


fun <T> ObservableObject<T>.bindBidirectionalToJavaFx(fxProperty: Property<T>) {
  //the initial value is taken from the JavaFX property
  this.value = fxProperty.value

  fxProperty.addListener { _, _, newValue ->
    this.value = newValue
  }

  addUpstreamSubscription(consume {
    fxProperty.value = this.value
  })
}

/**
 * Initial value is taken from the JavaFX [fxProperty]
 */
fun ObservableBoolean.bindBidirectionalToJavaFx(fxProperty: Property<Boolean>) {
  //the initial value is taken from the JavaFX property
  this.value = fxProperty.value


  fxProperty.addListener { _, _, newValue ->
    this.value = newValue
  }

  addUpstreamSubscription(consume {
    fxProperty.value = this.value
  })
}
