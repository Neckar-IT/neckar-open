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

import it.neckar.open.time.monotonicTimeSource
import it.neckar.open.unit.si.ms
import javafx.beans.property.BooleanProperty
import javafx.beans.property.Property
import javafx.beans.property.StringProperty
import javafx.beans.value.ObservableValue
import javafx.event.ActionEvent
import javafx.event.EventHandler
import javafx.scene.control.TextField
import javafx.scene.input.KeyEvent
import javafx.scene.layout.StackPane
import javafx.util.StringConverter
import javafx.util.converter.DefaultStringConverter
import kotlin.time.ComparableTimeMark
import kotlin.time.Duration.Companion.milliseconds

/**
 * A text field that is bound to a value but is not updated while it has the focus
 */
class AdvancedTextField : StackPane() {
  val textField: TextField

  /**
   * A handler that is executed on enter or focus lost
   */
  fun addCommitHandler(commitHandler: CommitHandler) {
    textField.focusedProperty().addListener { observable: ObservableValue<out Boolean?>?, oldValue: Boolean?, newValue: Boolean? -> commitHandler.commit(this@AdvancedTextField, textField.text) }
    textField.onAction = EventHandler { _: ActionEvent -> commitHandler.commit(this@AdvancedTextField, textField.text) }
  }

  fun bindTextBidirectional(property: StringProperty) {
    bindTextBidirectional(property, DefaultStringConverter())
  }

  /**
   * The [monotonicTimeSource] mark of the last relevant interaction (input / focus change), or `null` when none counts
   */
  private var lastChange: ComparableTimeMark? = null

  init {
    textField = TextField()
    children.add(textField)
  }

  @Suppress("UNUSED_PARAMETER")
  private fun updateLastChange(reason: String) {
    lastChange = monotonicTimeSource.markNow()
  }

  fun <T> bindTextBidirectional(property: Property<T>, converter: StringConverter<T>) {
    //Commit the text on enter
    textField.onAction = EventHandler { event: ActionEvent? ->
      property.value = converter.fromString(textField.text)
      lastChange = null //reset the last change to ensure updates are reflected
    }

    //Fill the text field initially
    textField.text = converter.toString(property.value)

    //Remember the last time a key has been pressed
    textField.caretPositionProperty().addListener { observable: ObservableValue<out Number?>?, oldValue: Number?, newValue: Number? -> updateLastChange("caret position changed") }
    textField.onKeyPressedProperty().addListener { observable: ObservableValue<out EventHandler<in KeyEvent?>?>?, oldValue: EventHandler<in KeyEvent?>?, newValue: EventHandler<in KeyEvent?>? -> updateLastChange("key pressed") }
    textField.focusedProperty().addListener { observable: ObservableValue<out Boolean?>?, oldValue: Boolean?, newValue: Boolean? -> updateLastChange("focus changed") }
    textField.selectedTextProperty().addListener { observable: ObservableValue<out String?>?, oldValue: String?, newValue: String? -> updateLastChange("selected text changed") }
    property.addListener { observable: ObservableValue<out T>?, oldValue: T, newValue: T ->
      if (textField.isFocused) {
        //Field is focused
        val currentLastChange = lastChange
        if (currentLastChange != null && currentLastChange.elapsedNow() < FocusedDebounceMillis.milliseconds) {
          //Only overwrite if the last edit action is at least FocusedDebounceMillis old
          return@addListener
        }
      }
      textField.text = converter.toString(newValue)
    }

    //Update the property when the text field changes
    textField.textProperty().addListener { observable: ObservableValue<out String?>?, oldValue: String?, newValue: String? -> property.setValue(converter.fromString(newValue)) }

    //Update the text field on focus lost
    textField.focusedProperty().addListener { observable: ObservableValue<out Boolean>, oldValue: Boolean, newValue: Boolean ->
      if (newValue.not()) {
        textField.text = converter.toString(property.value)
      }
    }
  }

  fun editableProperty(): BooleanProperty {
    return textField.editableProperty()
  }

  fun setPrefColumnCount(value: Int) {
    textField.prefColumnCount = value
  }

  /**
   * Handler that is called when the text field is committed
   */
  fun interface CommitHandler {
    fun commit(advancedTextField: AdvancedTextField, text: String)
  }

  companion object {
    /**
     * While the field is focused, programmatic property changes within this many milliseconds
     * after the user's last input are suppressed so the user's typing is not overwritten.
     */
    private const val FocusedDebounceMillis: @ms Long = 3000
  }
}
