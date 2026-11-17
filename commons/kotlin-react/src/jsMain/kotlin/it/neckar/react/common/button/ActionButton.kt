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
package it.neckar.react.common.button

import it.neckar.react.common.*
import kotlinx.html.BUTTON
import react.*
import react.dom.*

/**
 * Creates an action button - that has only an icon
 */
fun RBuilder.actionButton(
  /**
   * The icon for the button
   */
  icon: ButtonIconCentered,

  /**
   * The css classes for the button
   */
  classes: String = "btn btn-primary",

  /**
   * The action that is called.
   * While the action is running, the button will be marked as "busy"
   */
  action: suspend () -> Unit,

  /**
   * Additional configuration for the button
   */
  block: RDOMBuilder<BUTTON>.() -> Unit = {},

  ): Unit = child(Button2) {

  attrs {
    this.icon = icon
    this.label = null
    this.classes = classes
    this.action = action
    this.block = block
  }
}

/**
 * Creates a link button.
 */
fun RBuilder.linkButton(
  /**
   * The icon for the link
   */
  icon: ButtonIcon? = null,

  /**
   * The link label
   */
  label: String,

  /**
   * The css classes for the button
   */
  classes: String = "btn btn-link",

  /**
   * The action that is called. While the action is running, the button will be marked as "busy"
   */
  action: suspend () -> Unit,

  block: RDOMBuilder<BUTTON>.() -> Unit = {},

  ): Unit = child(Button2) {
  attrs {
    this.icon = icon
    this.label = label
    this.classes = classes
    this.action = action
    this.block = block
    this.iconBlock = {
      it.attrs {
        addClass("ms-1")
        addClass("me-1")
      }
    }
  }
}


/**
 * Creates an edit button
 */
fun RBuilder.editButton(action: suspend () -> Unit) {
  actionButton(icon = ButtonIcon.centered(FontAwesomeIcons.Edit), classes = "btn btn-link btn-sm", action = action) {}
}

/**
 * Creates an "add" button
 */
fun RBuilder.addButton(action: suspend () -> Unit) {
  actionButton(icon = ButtonIcon.centered(FontAwesomeIcons.Add), classes = "btn btn-link btn-sm", action = action) {}
}

/**
 * Creates a "copy" button
 */
fun RBuilder.copyButton(action: suspend () -> Unit) {
  actionButton(icon = ButtonIcon.centered(FontAwesomeIcons.Copy), classes = "btn btn-link btn-sm", action = action) {}
}

/**
 * Creates a "delete" button
 */
fun RBuilder.deleteButton(action: suspend () -> Unit) {
  actionButton(icon = ButtonIcon.centered(FontAwesomeIcons.Trash), classes = "btn btn-link btn-sm", action = action) {}
}


