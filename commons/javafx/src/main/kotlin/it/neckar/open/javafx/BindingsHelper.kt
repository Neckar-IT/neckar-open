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
import javafx.beans.InvalidationListener
import javafx.beans.binding.Binding
import javafx.beans.binding.Bindings
import javafx.beans.binding.LongBinding
import javafx.beans.binding.StringBinding
import javafx.beans.binding.StringExpression
import javafx.beans.property.DoubleProperty
import javafx.beans.property.ObjectProperty
import javafx.beans.property.Property
import javafx.beans.property.ReadOnlyIntegerProperty
import javafx.beans.property.ReadOnlyProperty
import javafx.beans.property.SimpleStringProperty
import javafx.beans.value.ChangeListener
import javafx.beans.value.ObservableBooleanValue
import javafx.beans.value.ObservableValue
import javafx.beans.value.WritableDoubleValue
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.css.PseudoClass
import javafx.scene.Node
import javafx.scene.control.ComboBox
import javafx.scene.control.ListView
import java.text.NumberFormat
import java.text.ParseException
import java.util.Locale

@Suppress("UNUSED_PARAMETER")
object BindingsHelper {
  /**
   * Binds a combo box
   */
  @JvmStatic
  fun <T : Enum<T>> bindEnumCombo(property: ObjectProperty<T>, type: Class<T>): ComboBox<T> {
    return bindEnumCombo(ComboBox(), property, type)
  }

  @JvmStatic
  fun <T : Enum<T>> bindEnumCombo(property: PropertyWithWritableState<T>, type: Class<T>): ComboBox<T> {
    return bindEnumCombo(ComboBox(), property, type)
  }

  @JvmStatic
  fun <T : Enum<T>> bindEnumComboSpecificEnums(property: Property<T>, type: Class<T>, enumConstants: Array<T>): ComboBox<T> {
    return bindEnumComboSpecificEnums(ComboBox(), property, enumConstants)
  }

  @JvmStatic
  fun <T : Enum<T>> bindEnumComboSpecificEnums(property: PropertyWithWritableState<T>, type: Class<T>, enumConstants: Array<T>): ComboBox<T> {
    return bindEnumComboSpecificEnums(ComboBox(), property, enumConstants)
  }

  @JvmStatic
  fun <T : Enum<T>> bindEnumCombo(combo: ComboBox<T>, property: ObjectProperty<T>, type: Class<T>): ComboBox<T> {
    return bindEnumComboSpecificEnums(combo, property, type.enumConstants)
  }

  @JvmStatic
  fun <T : Enum<T>> bindEnumCombo(combo: ComboBox<T>, property: PropertyWithWritableState<T>, type: Class<T>): ComboBox<T> {
    return bindEnumComboSpecificEnums(combo, property, type.enumConstants)
  }

  @JvmStatic
  @IgnorableReturnValue
  fun <T : Enum<T>> bindEnumComboSpecificEnums(combo: ComboBox<T>, property: PropertyWithWritableState<T>, enumConstants: Array<T>): ComboBox<T> {
    bindEnumComboSpecificEnums(combo, property.property, enumConstants)
    combo.disableProperty().bind(property.writableProperty.not())
    return combo
  }

  @JvmStatic
  @IgnorableReturnValue
  fun <T : Enum<T>> bindEnumComboSpecificEnums(combo: ComboBox<T>, property: Property<T>, enumConstants: Array<T>): ComboBox<T> {
    combo.itemsProperty().set(FXCollections.observableArrayList(*enumConstants))
    combo.valueProperty().bindBidirectional(property)
    combo.buttonCell = EnumListCell()
    combo.setCellFactory { _: ListView<T> -> EnumListCell() }
    return combo
  }

  @JvmStatic
  fun toBinding(stringProperty: StringExpression): StringBinding {
    return Bindings.createStringBinding({ stringProperty.get() }, stringProperty)
  }

  @JvmStatic
  fun <T : Enum<T>> bindEnumCombo(combo: ComboBox<T>, property: ObjectProperty<T>, items: ObservableList<T>): ComboBox<T> {
    combo.itemsProperty().set(items)
    combo.valueProperty().bindBidirectional(property)
    return combo
  }

  /**
   * Sets the pseudo class when the given property is true
   */
  @JvmStatic
  fun bindPseudoClass(node: Node, pseudoClass: PseudoClass, pseudoClassActive: ObservableBooleanValue) {
    //set initially
    node.pseudoClassStateChanged(pseudoClass, pseudoClassActive.get())

    //Update the pseudo class whenever the property changes
    pseudoClassActive.addListener { _: ObservableValue<out Boolean>, _: Boolean, newValue: Boolean -> node.pseudoClassStateChanged(pseudoClass, newValue) }
  }

  @JvmStatic
  fun asFormattedHex(longProperty: ReadOnlyProperty<Number>): StringBinding {
    return Bindings.createStringBinding({
      val longValue = longProperty.value.toLong()
      if (longValue == 0L) {
        return@createStringBinding ""
      }
      toHex(longValue)
    }, longProperty)
  }

  /**
   * Converts to upper case hex with prefix "0x"
   */
  @JvmStatic
  fun toHex(longValue: Long): String {
    return "0x" + java.lang.Long.toHexString(longValue).uppercase(Locale.getDefault())
  }

  /**
   * Wraps a string binding and ensures that no empty string is returned
   */
  @JvmStatic
  fun avoidEmptyString(binding: Binding<String>, emptyStringReplacement: String): Binding<String> {
    return object : Binding<String> {
      override fun getValue(): String {
        val value = binding.value
        return if (value == null || value.trim { it <= ' ' }.isEmpty()) {
          emptyStringReplacement
        } else value
      }

      override fun isValid(): Boolean {
        return binding.isValid
      }

      override fun invalidate() {
        binding.invalidate()
      }

      override fun getDependencies(): ObservableList<*> {
        return binding.dependencies
      }

      override fun dispose() {
        binding.dispose()
      }

      override fun addListener(listener: ChangeListener<in String>) {
        binding.addListener(listener)
      }

      override fun removeListener(listener: ChangeListener<in String>) {
        binding.removeListener(listener)
      }

      override fun addListener(listener: InvalidationListener) {
        binding.addListener(listener)
      }

      override fun removeListener(listener: InvalidationListener) {
        binding.removeListener(listener)
      }
    }
  }

  @JvmStatic
  fun bindFormattedNumber(property: ObservableValue<Number>, numberFormat: NumberFormat): StringBinding {
    return Bindings.createStringBinding({
      val value = property.value
      if (java.lang.Double.isNaN(value.toDouble())) {
        return@createStringBinding "-"
      }
      numberFormat.format(value)
    }, property)
  }

  /**
   * Creates a listener that ensures a given property always is within the given min and max value (both inclusive)
   */
  @JvmStatic
  fun createBoundsChecker(minValue: Double, maxValue: Double): ChangeListener<Number> {
    return ChangeListener { observable: ObservableValue<out Number>, oldValue: Number?, newValue: Number ->
      if (newValue.toDouble() < minValue) {
        (observable as WritableDoubleValue).set(minValue)
      }
      if (newValue.toDouble() > maxValue) {
        (observable as WritableDoubleValue).set(maxValue)
      }
    }
  }

  /**
   * Can be used to bind a double property to a string property
   */
  @JvmStatic
  fun double2stringBiDirectional(doubleProperty: DoubleProperty): Property<String> {
    val stringProperty = SimpleStringProperty()
    stringProperty.addListener { observable: ObservableValue<out String?>, oldValue: String?, newValue: String? ->
      try {
        val doubleValue = string2double(newValue)
        doubleProperty.value = doubleValue
      } catch (e: ParseException) {
        //Enforce the old values if can not be parsed
        stringProperty.value = oldValue
      }
    }
    doubleProperty.addListener(ChangeListener { observable, oldValue, newValue ->
      try {
        val oldValueFromString = string2double(stringProperty.get())
        if (oldValueFromString == newValue.toDouble()) {
          return@ChangeListener
        }
      } catch (_: ParseException) {
      }
      stringProperty.value = NumberFormat.getInstance().format(newValue.toDouble())
    })
    return stringProperty
  }

  /**
   * Converts a string to double. Handles null values and empty strings
   */
  @Throws(ParseException::class)
  private fun string2double(newValue: String?): Double {
    return if (newValue.isNullOrEmpty()) {
      0.0
    } else NumberFormat.getInstance().parse(newValue.trim { it <= ' ' }).toDouble()
  }

  /**
   * Packs three bytes into one long - [intProperty0] becomes the most significant byte, [intProperty2] the least significant one.
   * Each input is truncated to its low byte.
   */
  @JvmStatic
  fun createLongBinding(
    intProperty0: ReadOnlyIntegerProperty,
    intProperty1: ReadOnlyIntegerProperty,
    intProperty2: ReadOnlyIntegerProperty,
  ): LongBinding {
    return Bindings.createLongBinding({
      val byte0 = intProperty0.value and 0xff
      val byte1 = intProperty1.value and 0xff
      val byte2 = intProperty2.value and 0xff
      (byte2 + byte1 * 256 + byte0 * 256 * 256).toLong()
    }, intProperty0, intProperty1, intProperty2)
  }

  /**
   * Binds two method calls to a boolean property.
   * The method corresponding to the current state is called once initially
   */
  @JvmStatic
  fun bindMethodsToBoolean(property: ObservableValue<Boolean>, onTrue: Runnable, onFalse: Runnable) {
    property.addListener { _: ObservableValue<out Boolean>?, _: Boolean?, newValue: Boolean ->
      if (newValue) {
        onTrue.run()
      } else {
        onFalse.run()
      }
    }

    //Initial
    if (property.value) {
      onTrue.run()
    } else {
      onFalse.run()
    }
  }
}
