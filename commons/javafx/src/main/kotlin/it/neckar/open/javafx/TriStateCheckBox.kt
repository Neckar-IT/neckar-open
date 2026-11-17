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

import it.neckar.open.kotlin.lang.use
import javafx.beans.binding.Bindings
import javafx.beans.binding.ObjectBinding
import javafx.beans.property.ReadOnlyBooleanProperty
import javafx.beans.value.ChangeListener
import javafx.beans.value.ObservableObjectValue
import javafx.beans.value.ObservableValue
import javafx.scene.control.CheckBox
import javax.annotation.Nonnull

/**
 * Helper class that supports a tri state checkbox
 *
 */
class TriStateCheckBox(
  label: String,
  callback: Callback,
  state: ObservableObjectValue<TriStateCheckboxState>,
) {
  val checkBox: CheckBox

  private val callback: Callback

  private val state: ObservableObjectValue<TriStateCheckboxState>

  init {
    checkBox = CheckBox(label)
    //Set selected to true initially to allow an initial indeterminate state
    checkBox.isSelected = true
    this.callback = callback
    this.state = state

    //Update the model when the checkbox has changed
    checkBox.selectedProperty().addListener(object : ChangeListener<Boolean> {
      /**
       * Is set to true during modification to avoid duplicate events
       */
      private var isChanging = false
      override fun changed(observable: ObservableValue<out Boolean>, oldValue: Boolean, newValue: Boolean) {
        if (isChanging) {
          return
        }
        if (newValue) {

          ::isChanging.use {
            this@TriStateCheckBox.callback.selectAll()
          }
        } else {
          ::isChanging.use {
            this@TriStateCheckBox.callback.selectNone()
          }
        }
      }
    })

    //Update the checkbox if the state has changed
    stateProperty().addListener { _: ObservableValue<out TriStateCheckboxState>?, _: TriStateCheckboxState?, newValue: TriStateCheckboxState? -> updateSelectAllCheckbox(checkBox) }
    updateSelectAllCheckbox(checkBox)
  }

  private fun updateSelectAllCheckbox(@Nonnull selectAllCheckBox: CheckBox) {
    when (stateProperty().get()) {
      TriStateCheckboxState.UNSELECTED -> {
        selectAllCheckBox.isIndeterminate = false
        selectAllCheckBox.isSelected = false
        return
      }

      TriStateCheckboxState.INDETERMINATE -> {
        selectAllCheckBox.isIndeterminate = true
        return
      }

      TriStateCheckboxState.SELECTED -> {
        selectAllCheckBox.isIndeterminate = false
        selectAllCheckBox.isSelected = true
        return
      }

      else -> {
        throw IllegalArgumentException("Invalid state <" + stateProperty().get() + ">")
      }
    }
  }

  fun stateProperty(): ObservableObjectValue<TriStateCheckboxState> {
    return state
  }

  /**
   * A model for the tri state checkbox
   */
  interface Callback {
    /**
     * Select all elements
     */
    fun selectAll()

    /**
     * Select no elements
     */
    fun selectNone()
  }

  companion object {
    /**
     * Creates a binding to a tri state checkbox state
     */
    @JvmStatic
    fun createStateProperty(allSelectedProperty: ReadOnlyBooleanProperty, noneSelectedProperty: ReadOnlyBooleanProperty): ObjectBinding<TriStateCheckboxState> {
      return Bindings.createObjectBinding({
        if (allSelectedProperty.get()) {
          return@createObjectBinding TriStateCheckboxState.SELECTED
        }
        if (noneSelectedProperty.get()) {
          return@createObjectBinding TriStateCheckboxState.UNSELECTED
        }
        TriStateCheckboxState.INDETERMINATE
      }, allSelectedProperty, noneSelectedProperty)
    }
  }
}
