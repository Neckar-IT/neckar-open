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

import it.neckar.commons.kotlin.js.safeGet
import it.neckar.react.common.*
import kotlinx.coroutines.*
import kotlinx.html.BUTTON
import kotlinx.html.I
import kotlinx.html.js.onClickFunction
import react.*
import react.dom.*


/**
 * Creates a button - supporting a suspend function for the action.
 */
fun RBuilder.button2(
  classes: String = "",

  /**
   * The icon for the button
   */
  icon: ButtonIcon? = null,

  /**
   * The optional label for the action button
   */
  label: String? = null,

  /**
   * The action that is called. While the action is running, the button will be marked as "busy"
   */
  action: suspend () -> Unit,

  block: RDOMBuilder<BUTTON>.() -> Unit = {},
): Unit = child(Button2) {
  attrs {
    this.classes = classes
    this.icon = icon
    this.label = label
    this.action = action
    this.block = block
  }
}

/**
 * Represents a button component - supporting a suspend function for the action.
 * Can be further extended by other components.
 */
val Button2: FC<Button2Props> = fc("Button") { props ->
  val coroutineScope: CoroutineScope = props::coroutineScope.safeGet() ?: AppScope

  val classes: String = props::classes.safeGet()
  val icon: ButtonIcon? = props::icon.safeGet()
  val label: String? = props::label.safeGet()

  val iconBlock: ((RDOMBuilder<I>) -> Unit)? = props::iconBlock.safeGet()

  require(icon != null || label != null) {
    "At least one of icon or label must be provided"
  }

  val block: (RDOMBuilder<BUTTON>) -> Unit = props::block.safeGet()
  val busyBlock: (RDOMBuilder<BUTTON>) -> Unit = props::busyBlock.safeGet() ?: DefaultBusySpinner
  val action: suspend () -> Unit = props.action

  val (busy, setBusy) = useState(false)

  button(classes = classes) {
    attrs {
      disabled = busy //disable the button if busy

      onClickFunction = {
        //Launch the action in a coroutine. Automatically update the busy state
        coroutineScope.launch {
          setBusy(true)
          try {
            action()
          } finally {
            setBusy(false)
          }
        }
      }
    }

    if (busy.not()) {
      if (icon.isCenter()) {
        i(icon.icon) {
          iconBlock?.invoke(this)
        }
      }

      if (icon.isLeft()) {
        i(icon.icon + " me-2") {
          iconBlock?.invoke(this)
        }
      }

      if (label != null) {
        +label
      }

      if (icon.isRight()) {
        i(icon.icon + " ms-2") {
          iconBlock?.invoke(this)
        }
      }
    } else {
      busyBlock(this)
    }

    block(this)
  }
}

external interface Button2Props : Props {
  /**
   * The scope where the action is executed in.
   * If not configured, the [AppScope] is used.
   */
  var coroutineScope: CoroutineScope?

  /**
   * The CSS classes for the button
   */
  var classes: String

  /**
   * The (optional) icon.
   * At least one of [icon] or [label] must be provided.
   */
  var icon: ButtonIcon?

  /**
   * The (optional) label.
   * At least one of [icon] or [label] must be provided.
   */
  var label: String?

  /**
   * The action that is executed.
   * While the action is running, the button will be marked as "busy" (see [busyBlock])
   */
  var action: suspend () -> Unit

  /**
   * The block that is executed, if the button is busy.
   * This block is only executed while the [action] is running.
   *
   * If this block is not provided, a default busy spinner is shown.
   */
  var busyBlock: ((RDOMBuilder<BUTTON>) -> Unit)?

  /**
   * This block is called for the icon - if there is one
   */
  var iconBlock: ((RDOMBuilder<I>) -> Unit)?

  /**
   * The block that configures the button itself
   */
  var block: (RDOMBuilder<BUTTON>) -> Unit
}

/**
 * Default implementation for a busy block
 */
val DefaultBusySpinner: RDOMBuilder<BUTTON>.() -> Unit = {
  i("spinner-border spinner-border-sm") { }
}
