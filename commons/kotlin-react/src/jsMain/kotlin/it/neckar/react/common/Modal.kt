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
package it.neckar.react.common

import it.neckar.commons.kotlin.js.safeGet
import it.neckar.react.common.button.*
import kotlinx.html.BUTTON
import kotlinx.html.ButtonType
import kotlinx.html.DIV
import kotlinx.html.id
import kotlinx.html.js.onClickFunction
import org.w3c.dom.events.Event
import react.*
import react.dom.*


/**
 * Creates a button that shows a modal window
 */
fun RBuilder.modalButton(
  idOfModal: String,

  /**
   * The icon for the button
   */
  icon: String? = null,

  /**
   * The css classes
   */
  classes: String,

  buttonContent: RDOMBuilder<BUTTON>.() -> Unit,

  ): Unit = child(ModalButton) {
  attrs {
    this.id = idOfModal
    this.icon = icon
    this.classes = classes
    this.buttonContent = buttonContent
  }
}

val ModalButton: FC<ModalButtonProps> = fc("ModalButton") { props ->
  val id = props::id.safeGet()
  val icon = props::icon.safeGet()
  val classes = props::classes.safeGet()
  val buttonContent = props::buttonContent.safeGet()

  button(classes = classes) {
    attrs["data-bs-toggle"] = "modal"
    attrs["data-bs-target"] = "#${id}"

    icon?.let { i(it) {} }

    buttonContent(this)
  }
}

external interface ModalButtonProps : Props {
  var id: String
  var icon: String?
  var classes: String
  var buttonContent: (RDOMBuilder<BUTTON>) -> Unit
}


/**
 * Shows a modal dialog
 */
fun RBuilder.modal(
  id: String,
  title: String,
  size: ModalSize = ModalSize.Default,

  /**
   * When backdrop is set to static, the modal will not close when clicking outside it.
   */
  staticBackdrop: Boolean = false,
  modalCentered: Boolean = false,
  textCentered: Boolean = false,
  cancelButtonText: String = "Schließen",
  cancelButtonClasses: String = "btn btn-secondary",
  confirmationButtonClasses: String = "btn btn-primary",
  modalButtonPosition: ModalButtonPosition = ModalButtonPosition.CancelOK,
  cancelFunction: (Event) -> Unit = {},
  modalContent: RElementBuilder<ModalProps>.() -> Unit,
) {
  child(Modal) {
    this.attrs {
      this.id = id
      this.title = title
      this.size = size
      this.staticBackdrop = staticBackdrop
      this.modalCentered = modalCentered
      this.textCentered = textCentered
      this.cancelButtonText = cancelButtonText
      this.confirmationButtonText = ""
      this.cancelButtonClasses = cancelButtonClasses
      this.confirmationButtonClasses = confirmationButtonClasses
      this.modalButtonPosition = modalButtonPosition
      this.onConfirm = null
      this.cancelFunction = cancelFunction
      this.modalContent = modalContent
    }
  }
}


fun RBuilder.actionButtonWithConfirmationModal(
  icon: String? = null,
  buttonClasses: String = "btn btn-link btn-sm",
  buttonContent: RDOMBuilder<BUTTON>.() -> Unit = {},

  modalTitle: String,
  modalSize: ModalSize = ModalSize.Default,
  /**
   * When backdrop is set to static, the modal will not close when clicking outside it.
   */
  staticBackdrop: Boolean = true,
  modalCentered: Boolean = true,
  textCentered: Boolean = false,
  cancelButtonText: String = "Schließen",
  confirmationButtonText: String = "OK",
  cancelButtonClasses: String = "btn btn-secondary",
  confirmationButtonClasses: String = "btn btn-primary",
  modalButtonPosition: ModalButtonPosition = ModalButtonPosition.CancelOK,
  cancelFunction: (Event) -> Unit = {},
  modalContent: (RElementBuilder<ModalProps>.() -> Unit)? = null,
  onConfirm: suspend () -> Unit,
): Unit = child(ButtonWithConfirmationModal) {
  attrs {
    this.icon = icon
    this.buttonClasses = buttonClasses
    this.buttonContent = buttonContent

    this.modalTitle = modalTitle
    this.size = modalSize
    this.staticBackdrop = staticBackdrop
    this.modalCentered = modalCentered
    this.textCentered = textCentered
    this.cancelButtonText = cancelButtonText
    this.confirmationButtonText = confirmationButtonText
    this.cancelButtonClasses = cancelButtonClasses
    this.confirmationButtonClasses = confirmationButtonClasses
    this.modalButtonPosition = modalButtonPosition

    this.cancelFunction = cancelFunction
    this.modalContent = modalContent
    this.onConfirm = onConfirm
  }
}

val ButtonWithConfirmationModal: FC<ButtonWithConfirmationModalProps> = fc("ButtonWithConfirmationModal") { props ->
  val icon = props::icon.safeGet()
  val buttonClasses = props::buttonClasses.safeGet()
  val buttonContent = props::buttonContent.safeGet()

  val modalTitle = props::modalTitle.safeGet()
  val size = props::size.safeGet()
  val staticBackdrop = props::staticBackdrop.safeGet()
  val modalCentered = props::modalCentered.safeGet()
  val textCentered = props::textCentered.safeGet()
  val cancelButtonText = props::cancelButtonText.safeGet()
  val confirmationButtonText = props::confirmationButtonText.safeGet()
  val cancelButtonClasses = props::cancelButtonClasses.safeGet()
  val confirmationButtonClasses = props::confirmationButtonClasses.safeGet()
  val modalButtonPosition = props::modalButtonPosition.safeGet()

  val cancelFunction = props::cancelFunction.safeGet()
  val modalContent = props::modalContent.safeGet()
  val onConfirm = props.onConfirm

  val confirmationModalId = uniqueIdMemo("confirmationModalId")


  confirmationModal(
    id = confirmationModalId,
    title = modalTitle,
    size = size,
    staticBackdrop = staticBackdrop,
    modalCentered = modalCentered,
    textCentered = textCentered,
    cancelButtonText = cancelButtonText,
    confirmationButtonText = confirmationButtonText,
    cancelFunction = cancelFunction,
    cancelButtonClasses = cancelButtonClasses,
    confirmationButtonClasses = confirmationButtonClasses,
    modalButtonPosition = modalButtonPosition,
    onConfirm = onConfirm,
    modalContent = modalContent,
  )

  modalButton(
    idOfModal = confirmationModalId,
    icon = icon,
    classes = buttonClasses,
    buttonContent = buttonContent,
  )
}

external interface ButtonWithConfirmationModalProps : Props {
  var icon: String?
  var buttonClasses: String
  var buttonContent: (RDOMBuilder<BUTTON>) -> Unit

  var modalTitle: String
  var size: ModalSize
  var staticBackdrop: Boolean
  var modalCentered: Boolean
  var textCentered: Boolean
  var cancelButtonText: String
  var confirmationButtonText: String
  var cancelButtonClasses: String
  var confirmationButtonClasses: String
  var modalButtonPosition: ModalButtonPosition

  var cancelFunction: (Event) -> Unit
  var modalContent: ((RElementBuilder<ModalProps>) -> Unit)?
  var onConfirm: suspend () -> Unit
}


fun RBuilder.confirmationModal(
  id: String,
  title: String,
  size: ModalSize = ModalSize.Default,
  /**
   * When backdrop is set to static, the modal will not close when clicking outside it.
   */
  staticBackdrop: Boolean = true,
  modalCentered: Boolean = true,
  textCentered: Boolean = false,
  cancelButtonText: String = "Schließen",
  confirmationButtonText: String = "OK",
  cancelButtonClasses: String = "btn btn-secondary",
  confirmationButtonClasses: String = "btn btn-primary",
  modalButtonPosition: ModalButtonPosition = ModalButtonPosition.CancelOK,
  cancelFunction: (Event) -> Unit = {},
  onConfirm: suspend () -> Unit = {},
  modalContent: (RElementBuilder<ModalProps>.() -> Unit)?,
) {
  child(Modal) {
    this.attrs {
      this.id = id
      this.title = title
      this.size = size
      this.staticBackdrop = staticBackdrop
      this.modalCentered = modalCentered
      this.textCentered = textCentered
      this.cancelButtonText = cancelButtonText
      this.confirmationButtonText = confirmationButtonText
      this.cancelButtonClasses = cancelButtonClasses
      this.confirmationButtonClasses = confirmationButtonClasses
      this.modalButtonPosition = modalButtonPosition
      this.cancelFunction = cancelFunction
      this.onConfirm = onConfirm
      this.modalContent = modalContent
    }
  }
}

enum class ModalButtonPosition {
  CancelOK,
  OkCancel,
}

val Modal: FC<ModalProps> = fc("Modal") { props ->
  val id = props::id.safeGet()
  val title = props::title.safeGet()
  val size = props::size.safeGet()
  val staticBackdrop = props::staticBackdrop.safeGet()
  val modalCentered = props::modalCentered.safeGet()
  val textCentered = props::textCentered.safeGet()

  val cancelButtonText = props::cancelButtonText.safeGet()
  val confirmationButtonText = props::confirmationButtonText.safeGet()
  val cancelButtonClasses = props::cancelButtonClasses.safeGet()
  val confirmationButtonClasses = props::confirmationButtonClasses.safeGet()
  val modalButtonPosition = props::modalButtonPosition.safeGet()

  val cancelFunction = props::cancelFunction.safeGet()
  val onConfirm: (suspend () -> Unit)? = props.onConfirm
  val modalContent = props::modalContent.safeGet()

  div(classes = "modal fade") {
    attrs {
      this.id = id
    }

    if (staticBackdrop) {
      attrs["data-bs-backdrop"] = "static"
    }

    div(classes = "modal-dialog modal-dialog-scrollable") {
      attrs {
        size.cssClass?.let { addClass(it) }
      }

      addClassIf("modal-dialog-centered") { modalCentered }

      div(classes = "modal-content") {

        div(classes = "modal-header") {
          h5(classes = "modal-title") {
            +title
          }

          button(classes = "btn-close") {
            attrs {
              type = ButtonType.button
              onClickFunction = cancelFunction
            }

            attrs["data-bs-dismiss"] = "modal"
          }
        }

        modalContent?.let {
          div(classes = "modal-body") {
            addClassIf("text-center") { textCentered }

            it(this.unsafeCast<RElementBuilder<ModalProps>>())
          }
        }

        div(classes = "modal-footer") {
          when (modalButtonPosition) {
            ModalButtonPosition.CancelOK -> {
              cancelButton(cancelButtonClasses, cancelFunction, cancelButtonText)
              confirmationButton(onConfirm, confirmationButtonText, confirmationButtonClasses)
            }

            ModalButtonPosition.OkCancel -> {
              confirmationButton(onConfirm, confirmationButtonText, confirmationButtonClasses)
              cancelButton(cancelButtonClasses, cancelFunction, cancelButtonText)
            }
          }
        }
      }
    }
  }
}

private fun RDOMBuilder<DIV>.cancelButton(cancelButtonClasses: String, cancelFunction: (Event) -> Unit, cancelButtonText: String) {
  button(classes = cancelButtonClasses) {
    attrs["data-bs-dismiss"] = "modal"
    attrs {
      type = ButtonType.button
      onClickFunction = cancelFunction
    }

    +cancelButtonText
  }
}

private fun RDOMBuilder<DIV>.confirmationButton(onConfirm: (suspend () -> Unit)?, confirmationButtonText: String, confirmationButtonClasses: String) {
  if (onConfirm != null) {
    button2(icon = null, label = confirmationButtonText, classes = confirmationButtonClasses, action = onConfirm) {
      attrs["data-bs-dismiss"] = "modal"
    }
  }
}

external interface ModalProps : Props {
  var id: String
  var title: String

  var size: ModalSize

  /**
   * When backdrop is set to static, the modal will not close when clicking outside it.
   */
  var staticBackdrop: Boolean

  var modalCentered: Boolean
  var textCentered: Boolean

  var cancelButtonText: String
  var confirmationButtonText: String
  var cancelButtonClasses: String
  var confirmationButtonClasses: String
  var modalButtonPosition: ModalButtonPosition

  var onConfirm: (suspend () -> Unit)?
  var cancelFunction: (Event) -> Unit

  var modalContent: ((RElementBuilder<ModalProps>) -> Unit)?
}

enum class ModalSize(val cssClass: String?) {
  Small("modal-sm"),
  Default(null),
  Large("modal-lg"),
  ExtraLarge("modal-xl")
}
