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

import it.neckar.open.javafx.properties.*
import it.neckar.open.kotlin.lang.use
import javafx.beans.property.BooleanProperty
import javafx.beans.property.DoubleProperty
import javafx.beans.property.IntegerProperty
import javafx.beans.property.Property
import javafx.beans.value.ChangeListener
import javafx.beans.value.ObservableValue
import kotlin.math.roundToInt

/**
 * Helper methods for bidirectional bindings
 */
object BidirectionalBinding {
  /**
   * Registers the given change listeners to the corresponding property.
   * Avoids reoccurring events by wrapping the change listener.
   *
   * <u>Beware that the order matters:</u><br></br>
   * this method copies the value from propertyA to propertyB initially by calling updateB!
   *
   * @param a2b converts values from A to B on changes to [propertyA]
   * @param b2a converts values from B to A on changes to [propertyB]
   */
  @JvmStatic
  fun <A, B> bindBidirectional(
    propertyA: Property<A>,
    propertyB: Property<B>,
    a2b: (newValue: A) -> B,
    b2a: (newValue: B) -> A,
    vararg additionalDependencies: ObservableValue<out Any>
  ) {
    bindBidirectional(
      propertyA = propertyA,
      propertyB = propertyB,
      updateFromAToB = { _, _, newValue ->
        propertyB.value = a2b(newValue)
      },
      updateFromBToA = { _, _, newValue ->
        propertyA.value = b2a(newValue)
      },
      *additionalDependencies
    )
  }

  @JvmStatic
  fun <A, B> bindBidirectional(
    propertyA: Property<A>,
    propertyB: Property<B>,
    a2b: (newValue: A) -> B,
    b2a: (newValue: B) -> A,
    updatesActiveA2B: () -> Boolean,
    updatesActiveB2A: () -> Boolean,
    vararg additionalDependencies: ObservableValue<out Any>
  ) {
    bindBidirectional(
      propertyA = propertyA,
      propertyB = propertyB,
      updateFromAToB = { _, _, newValue ->
        if (updatesActiveA2B()) {
          propertyB.value = a2b(newValue)
        }
      },
      updateFromBToA = { _, _, newValue ->
        if (updatesActiveB2A()) {
          propertyA.value = b2a(newValue)
        }
      },
      *additionalDependencies
    )
  }

  /**
   * Registers the given change listeners to the corresponding property.
   * Avoids reoccurring events by wrapping the change listener.
   *
   *
   * <u>Beware that the order matters:</u><br></br>
   * this method copies the value from propertyA to propertyB initially by calling updateB!
   *
   * @param updateFromAToB is called when A has changed and should update property B
   * @param updateFromBToA is called when B has changed and should update property A
   */
  @JvmStatic
  fun <A, B> bindBidirectional(
    propertyA: ObservableValue<A>,
    propertyB: ObservableValue<B>,
    updateFromAToB: ChangeListener<in A>,
    updateFromBToA: ChangeListener<in B>,
    vararg additionalDependencies: ObservableValue<out Any>
  ) {
    val flaggedChangeListenerFromA2B = FlaggedChangeListener(updateFromAToB)

    propertyA.addListener(flaggedChangeListenerFromA2B)
    propertyB.addListener(FlaggedChangeListener(updateFromBToA))

    for (additionalDependency in additionalDependencies) {
      additionalDependency.addListener(FlaggedChangeListener { _: ObservableValue<*>?, _: Any?, _: Any? ->
        updateFromAToB.changed(propertyA, null, propertyA.value)
        updateFromBToA.changed(propertyB, null, propertyB.value)
      })
    }

    //Initially update property B
    flaggedChangeListenerFromA2B.changed(propertyA, null, propertyA.value)
  }

  /**
   * Binds a boolean property and an enum property
   */
  @JvmStatic
  fun <E : Enum<E>> bindBidirectional(
    propertyA: BooleanProperty,
    propertyB: Property<E>,
    /**
     * The enum value that represents true
     */
    trueValue: E,
    /**
     * The enum value that represents false
     */
    falseValue: E,
    vararg additionalDependencies: ObservableValue<out Any>
  ) {

    bindBidirectional(propertyA, propertyB,
      ChangeListener { _, _, newValue ->
        propertyB.value = if (newValue) trueValue else falseValue
      }, { _, _, newValue ->
        propertyA.value = newValue == trueValue
      }, *additionalDependencies
    )
  }

  /**
   * Binds properties including a writable state
   */
  @JvmStatic
  fun <A, B> bindBidirectional(
    propertyA: PropertyWithWritableState<A>,
    propertyB: PropertyWithWritableState<B>,
    updateFromAToB: ChangeListener<A>,
    updateFromBToA: ChangeListener<B>,
    vararg additionalDependencies: ObservableValue<out Any>
  ) {
    bindBidirectional(propertyA.property, propertyB.property, updateFromAToB, updateFromBToA, *additionalDependencies)

    //Bind the writable state bidirectional
    propertyB.mutableWritableProperty.bind(propertyA.writableProperty)
  }

  /**
   * Binds the properties to each other. Conversion is done using rounding!
   *
   * Copies the value from [propertyA] to [propertyB] initially
   */
  fun bindBidirectionalWithRounding(propertyA: IntegerProperty, propertyB: DoubleProperty) {
    bindBidirectional(propertyA, propertyB, { it.toDouble() }, { it.toDouble().roundToInt() })
  }

  /**
   * Flagged change listener that avoids cycles
   */
  internal class FlaggedChangeListener<T> internal constructor(
    private val updateProperty: ChangeListener<T>
  ) : ChangeListener<T> {
    /**
     * Is set to true if a call is currently made
     */
    private var alreadyCalled = false
    override fun changed(observable: ObservableValue<out T>, oldValue: T?, newValue: T) {
      if (alreadyCalled) {
        return
      }

      ::alreadyCalled.use {
        updateProperty.changed(observable, oldValue, newValue)
      }
    }
  }


  /**
   * Bidirectional builder
   */
  class Builder<A, B>(
    val propertyA: Property<A>,
    val propertyB: Property<B>,
  ) {
    var a2b: ((newValue: A) -> B)? = null
    var b2a: ((newValue: B) -> A)? = null

    fun build() {
      bindBidirectional(
        propertyA,
        propertyB,
        a2b ?: throw IllegalStateException("a2b required"),
        b2a ?: throw IllegalStateException("b2a required")
      )
    }
  }
}

/**
 * Creates a new bidirectional binding.
 *
 * It is required to set
 * * [BidirectionalBinding.Builder.a2b]
 * * [BidirectionalBinding.Builder.b2a]
 * in [config]
 */
fun <A, B> bindBidirectional(
  propertyA: Property<A>,
  propertyB: Property<B>,
  config: BidirectionalBinding.Builder<A, B>.() -> Unit
) {
  BidirectionalBinding.Builder<A, B>(propertyA, propertyB)
    .also(config)
    .build()
}


/**
 * Binds bidirectional using a factor.
 *
 * This double property is bound *to* [other]. The factor is used to convert from other to this.
 *
 * ATTENTION: The value from [other] is applied to this initially (using the factor)
 */
fun Property<Number>.bindBidirectionalWithFactor(other: Property<Number>, otherDividedBy: Double) {
  BidirectionalBinding.bindBidirectional(
    propertyA = other,
    propertyB = this,
    updateFromAToB = { _, _, newValue ->
      this.value = newValue.toDouble() / otherDividedBy
    },
    updateFromBToA = { _, _, newValue ->
      other.value = newValue.toDouble() * otherDividedBy
    }
  )
}

fun DoubleProperty.bindBidirectionalWithRounding(other: IntegerProperty) {
  BidirectionalBinding.bindBidirectionalWithRounding(other, this)
}
