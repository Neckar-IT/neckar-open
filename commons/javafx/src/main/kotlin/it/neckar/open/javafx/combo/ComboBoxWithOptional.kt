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
package it.neckar.open.javafx.combo

import it.neckar.open.javafx.properties.*
import javafx.beans.property.BooleanProperty
import javafx.beans.property.SimpleBooleanProperty
import javafx.scene.control.ListCell
import javafx.scene.control.ListView
import javafx.util.Callback

/**
 * Supports a combo box with optional items (that can be disabled)
 */


/**
 * Wraps an option for a combo box that can be disabled
 */
class ItemThatMayBeDisabled<T>(
  /**
   * The value
   */
  val value: T
) {

  /**
   * The disabled property
   */
  val disabledProperty: BooleanProperty = SimpleBooleanProperty()
  var disabled: Boolean by disabledProperty

  override fun toString(): String {
    return super.toString()
  }
}


/**
 * Callback that provides a ListCell that visualizes the disabled state
 */
class ListViewListCellCallbackForItemThatMayBeDisabled<T>(val converter: (T) -> String) : Callback<ListView<ItemThatMayBeDisabled<T>?>, ListCell<ItemThatMayBeDisabled<T>>> {
  override fun call(param: ListView<ItemThatMayBeDisabled<T>?>): ListCell<ItemThatMayBeDisabled<T>> {
    return object : ListCell<ItemThatMayBeDisabled<T>>() {
      override fun updateItem(item: ItemThatMayBeDisabled<T>?, empty: Boolean) {
        super.updateItem(item, empty)

        if (item != null) {
          setText(converter(item.value))
        } else {
          setText("")
        }

        if (item != null && item.disabled) {
          isDisable = true
          setStyle("-fx-opacity: 0.7;")
        } else {
          isDisable = false
          setStyle("")
        }
      }
    }
  }
}
