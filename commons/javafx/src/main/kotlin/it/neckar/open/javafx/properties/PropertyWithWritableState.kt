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

import javafx.beans.property.BooleanProperty
import javafx.beans.property.DoubleProperty
import javafx.beans.property.IntegerProperty
import javafx.beans.property.LongProperty
import javafx.beans.property.Property
import javafx.beans.property.ReadOnlyBooleanProperty
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleDoubleProperty
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleLongProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.value.ChangeListener
import javafx.beans.value.ObservableValue

/**
 * Represents a property with a writable state
 */
open class PropertyWithWritableState<T>(
  /**
   * The property itself
   */
  open val property: Property<T>,
) {

  constructor(initialValue: T) : this(SimpleObjectProperty(initialValue))

  /**
   * The value of the property
   */
  var value: T
    get() {
      return property.value
    }
    set(value) {
      property.value = value
    }

  protected val internalWritableProperty = SimpleBooleanProperty()

  /**
   * The writable property for this property
   */
  val writableProperty: ReadOnlyBooleanProperty
    get() {
      return internalWritableProperty
    }

  /**
   * The same property as [writableProperty] - but mutable.
   * ATTENTION: Use with care!
   */
  val mutableWritableProperty: BooleanProperty
    get() {
      return internalWritableProperty
    }

  /**
   * Whether the property is currently writable
   */
  val writable: Boolean by writableProperty

  fun addListener(listener: ChangeListener<in T>) {
    property.addListener(listener)
  }

  fun bindBidirectional(other: Property<T>, otherWritableState: ObservableValue<out Boolean>) {
    property.bindBidirectional(other)
    internalWritableProperty.bind(otherWritableState)
  }

  fun bindBidirectional(other: PropertyWithWritableState<T>) {
    bindBidirectional(other.property, other.writableProperty)
  }

  companion object {
    @JvmStatic
    fun <T> create(
      property: Property<T>,
      writableProperty: ReadOnlyBooleanProperty
    ): PropertyWithWritableState<T> {
      return PropertyWithWritableState(property).also {
        it.internalWritableProperty.bind(writableProperty)
      }
    }

    @JvmStatic
    fun create(
      property: BooleanProperty,
      writableProperty: ReadOnlyBooleanProperty
    ): BooleanPropertyWithWritableState {
      return BooleanPropertyWithWritableState(property).also {
        it.internalWritableProperty.bind(writableProperty)
      }
    }

    @JvmStatic
    fun create(
      property: IntegerProperty,
      writableProperty: ReadOnlyBooleanProperty
    ): IntegerPropertyWithWritableState {
      return IntegerPropertyWithWritableState(property).also {
        it.internalWritableProperty.bind(writableProperty)
      }
    }

    @JvmStatic
    fun create(
      property: LongProperty,
      writableProperty: ReadOnlyBooleanProperty
    ): LongPropertyWithWritableState {
      return LongPropertyWithWritableState(property).also {
        it.internalWritableProperty.bind(writableProperty)
      }
    }

    @JvmStatic
    fun create(
      property: DoubleProperty,
      writableProperty: ReadOnlyBooleanProperty
    ): DoublePropertyWithWritableState {
      return DoublePropertyWithWritableState(property).also {
        it.internalWritableProperty.bind(writableProperty)
      }
    }
  }
}

class DoublePropertyWithWritableState(
  override val property: DoubleProperty,
) : PropertyWithWritableState<Number>(property) {

  constructor() : this(SimpleDoubleProperty(0.0))
  constructor(initialValue: Double) : this(SimpleDoubleProperty(initialValue))

  fun get(): Double {
    return value.toDouble()
  }

  fun bindBidirectional(other: DoublePropertyWithWritableState) {
    bindBidirectional(other.property, other.writableProperty)
  }
}

class LongPropertyWithWritableState(
  override val property: LongProperty,
) : PropertyWithWritableState<Number>(property) {

  constructor() : this(0L)
  constructor(initialValue: Long) : this(SimpleLongProperty(initialValue))

  fun get(): Long {
    return value.toLong()
  }

  fun bindBidirectional(other: LongPropertyWithWritableState) {
    bindBidirectional(other.property, other.writableProperty)
  }

}

class IntegerPropertyWithWritableState(
  override val property: IntegerProperty,
) : PropertyWithWritableState<Number>(property) {

  constructor(initialValue: Int) : this(SimpleIntegerProperty(initialValue))
  constructor() : this(0)

  fun get(): Int {
    return value.toInt()
  }

  fun bindBidirectional(other: IntegerPropertyWithWritableState) {
    bindBidirectional(other.property, other.writableProperty)
  }

}

class BooleanPropertyWithWritableState(
  override val property: BooleanProperty,
) : PropertyWithWritableState<Boolean>(property) {

  constructor() : this(false)
  constructor(initialValue: Boolean) : this(SimpleBooleanProperty(initialValue))

  fun get(): Boolean {
    return value
  }

  fun bindBidirectional(other: BooleanPropertyWithWritableState) {
    bindBidirectional(other.property, other.writableProperty)
  }
}
