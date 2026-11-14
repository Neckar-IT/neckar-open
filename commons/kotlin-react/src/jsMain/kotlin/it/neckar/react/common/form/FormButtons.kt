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
package it.neckar.react.common.form

import it.neckar.commons.kotlin.js.safeGet
import it.neckar.react.common.*
import it.neckar.react.common.button.*
import kotlinx.html.BUTTON
import kotlinx.html.role
import react.*
import react.dom.*

/**
 * Adds cancel/ok buttons for a form
 */
fun RBuilder.formButtons(
  cancelIcon: ButtonIcon? = null,
  cancelText: String = "Abbrechen",
  okIcon: ButtonIcon? = ButtonIcon.left(
    icon = FontAwesomeIcons.Save,
  ),
  okText: String = "Speichern",

  cancelConfig: ((RDOMBuilder<BUTTON>) -> Unit)? = null,
  okConfig: ((RDOMBuilder<BUTTON>) -> Unit)? = null,
  cancelAction: () -> Unit,
  okAction: () -> Unit,
): Unit = child(FormButtons) {
  attrs {
    this.cancelIcon = cancelIcon
    this.cancelText = cancelText
    this.okIcon = okIcon
    this.okText = okText

    this.cancelConfig = cancelConfig
    this.okConfig = okConfig
    this.cancelAction = cancelAction
    this.okAction = okAction
  }
}

/**
 * Creates wizard buttons ("backward"/"forward")
 */
fun RBuilder.wizardFormButtons(
  backwardIcon: ButtonIcon? = ButtonIcon.left(
    icon = FontAwesomeIcons.ArrowLeft,
  ),
  backwardText: String = "Zurück",

  forwardText: String = "Weiter",
  forwardIcon: ButtonIcon? = ButtonIcon.right(
    icon = FontAwesomeIcons.ArrowRight,
  ),

  backwardConfig: ((RDOMBuilder<BUTTON>) -> Unit)? = null,
  backwardAction: () -> Unit,

  forwardConfig: ((RDOMBuilder<BUTTON>) -> Unit)? = null,
  forwardAction: () -> Unit,

  ): Unit = child(FormButtons) {
  attrs {
    this.cancelIcon = backwardIcon
    this.cancelText = backwardText
    this.okIcon = forwardIcon
    this.okText = forwardText

    this.cancelConfig = backwardConfig
    this.okConfig = forwardConfig
    this.cancelAction = backwardAction
    this.okAction = forwardAction
  }
}

/**
 * Represents two buttons side by side.
 * Left one to "cancel", right one to "ok"
 */
val FormButtons: FC<FormButtonsProps> = fc("FormButtons") { props ->
  val cancelIcon = props::cancelIcon.safeGet()
  val cancelText = props::cancelText.safeGet()
  val okIcon: ButtonIcon? = props::okIcon.safeGet()
  val okText = props::okText.safeGet()

  val cancelConfig = props::cancelConfig.safeGet()
  val okConfig = props::okConfig.safeGet()
  val cancelAction = props::cancelAction.safeGet()
  val okAction: () -> Unit = props::okAction.safeGet()

  div(classes = "btn-group mt-3 ") {
    attrs {
      role = "group"
      ariaLabel = "Form Buttons"
    }

    button2(
      classes = "btn btn-secondary",
      label = cancelText,
      icon = cancelIcon,
      action = cancelAction,
    ) {
      cancelConfig?.invoke(this)
    }

    button2(
      classes = "btn btn-primary",
      label = okText,
      icon = okIcon,
      action = okAction,
    ) {
      okConfig?.invoke(this)
    }
  }
}


external interface FormButtonsProps : Props {
  var cancelIcon: ButtonIcon?
  var cancelText: String
  var okIcon: ButtonIcon?
  var okText: String

  var cancelConfig: ((RDOMBuilder<BUTTON>) -> Unit)?
  var okConfig: ((RDOMBuilder<BUTTON>) -> Unit)?

  // workaround: dependency=libs:kotlin:2.4.20 observed-on=2026-10-07 upstream=https://youtrack.jetbrains.com/issue/KT-63000
  // A suspend function in React props fails the Kotlin/JS compiler with a TypeError, so both actions are plain functions.
  var cancelAction: () -> Unit
  var okAction: () -> Unit
}
