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

import it.neckar.open.javafx.converter.String2StringConverter
import javafx.beans.property.Property
import javafx.beans.value.ChangeListener
import javafx.beans.value.ObservableValue
import javafx.event.EventHandler
import javafx.scene.control.TextField
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.util.StringConverter

/**
 * Binds a property to a text field that is only updated on focus lost or enter
 */
class DelayedTextFieldBinding<T>(
  val textField: TextField,
  val property: Property<T>,
  converter: StringConverter<T>,
) {
  val converter: StringConverter<T>

  private val text2fieldListener: ChangeListener<T>

  private val focusLostListener: ChangeListener<Boolean>

  /**
   * Constructor
   *
   * @param textField the target text field
   * @param property  the property the text field's text property is bidirectionally bound to
   * @param converter its `toString`-method is called to convert the value of the property to the text of the text field;
   * its `fromString`-method is called to convert the text of the text field to the value of the property.
   */
  init {
    check(!property.isBound) { "property must not be bound" }
    this.converter = converter

    //Set initial value
    updateTextField()

    //Update text field on property change
    text2fieldListener = ChangeListener { _: ObservableValue<out T>?, _: T, newValue: T ->
      if (textField.text == newValue) {
        return@ChangeListener
      }
      updateTextField()
    }
    property.addListener(text2fieldListener)

    //Update the property on focus lost
    focusLostListener = ChangeListener { _: ObservableValue<out Boolean>, _: Boolean, newValue: Boolean ->
      if (!newValue) {
        updateProperty()
      }
    }
    textField.focusedProperty().addListener(focusLostListener)

    //Update the property on enter
    val enterPressedListener = EventHandler { event: KeyEvent ->
      if (event.code == KeyCode.ENTER) {
        updateProperty()
      }
    }
    textField.onKeyPressedProperty().value = enterPressedListener
  }

  private fun updateTextField() {
    textField.text = converter.toString(property.value)
  }

  /**
   * Writes the value from the text field to the property
   */
  private fun updateProperty() {
    property.value = converter.fromString(textField.text)
  }

  /**
   * Unbinds everything
   */
  fun unbind() {
    property.removeListener(text2fieldListener)
    textField.focusedProperty().removeListener(focusLostListener)
    textField.onKeyPressedProperty().value = null
  }

  companion object {
    @JvmStatic
    @IgnorableReturnValue
    fun connect(textField: TextField, text: Property<String>): DelayedTextFieldBinding<String> {
      return DelayedTextFieldBinding(textField, text, String2StringConverter())
    }

    /**
     * Creates a delayed text field binding.
     *
     * @param textField the target text field
     * @param text      the property the text field's text property is bidirectionally bound to
     * @param converter its `toString`-method is called to convert the value of the property to the text of the text field;
     * its `fromString`-method is called to convert the text of the text field to the value of the property.
     */
    @JvmStatic
    @IgnorableReturnValue
    fun <T> connect(textField: TextField, text: Property<T>, converter: StringConverter<T>): DelayedTextFieldBinding<T> {
      return DelayedTextFieldBinding(textField, text, converter)
    }
  }
}
