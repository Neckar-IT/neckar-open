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

import it.neckar.react.common.*
import it.neckar.react.common.form.EditableStatus.*
import kotlinx.html.TEXTAREA
import kotlinx.html.id
import react.*
import react.dom.*

/**
 * Creates a floating input area
 */
fun RBuilder.floatingInputArea(
  /**
   * The value and setter for the value.
   * Must be created with a useState hook
   */
  valueAndSetter: StateInstance<String>,

  fieldName: String,
  title: String,
  placeHolder: String? = null,

  editableStatus: EditableStatus = Editable,

  config: (RDOMBuilder<TEXTAREA>.() -> Unit)? = null,

  ): Unit = child(FloatingInputArea) {
  attrs {
    this.valueAndSetter = valueAndSetter
    this.fieldName = fieldName
    this.title = title
    this.placeHolder = placeHolder
    this.editableStatus = editableStatus
    this.config = config
  }
}

val FloatingInputArea: FC<FloatingInputAreaProps> = fc("FloatingInputArea") { props ->
  val uniqueId = uniqueIdMemo(props.fieldName)

  div("form-floating") {
    inputArea(
      props.valueAndSetter,
      fieldName = props.fieldName,
      title = props.title,
      editableStatus = props.editableStatus,
    ) {
      attrs {
        id = uniqueId

        addClasses("form-control height-100px")

        //Execute the provided config if there is one
        props.config?.let {
          it(this@inputArea)
        }
      }
    }
    label("form-label") {
      +props.title

      attrs {
        htmlForFixed = uniqueId
      }
    }
  }
}


fun RBuilder.nullableFloatingInputArea(
  /**
   * The value and setter for the value.
   * Must be created with a useState hook
   */
  valueAndSetter: StateInstance<String?>,

  fieldName: String,
  title: String,
  placeHolder: String? = null,

  editableStatus: EditableStatus = Editable,

  config: (RDOMBuilder<TEXTAREA>.() -> Unit)? = null,

  ): Unit = child(NullableFloatingInputArea) {
  attrs {
    this.valueAndSetter = valueAndSetter
    this.fieldName = fieldName
    this.title = title
    this.placeHolder = placeHolder
    this.editableStatus = editableStatus
    this.config = config
  }
}

val NullableFloatingInputArea: FC<NullableFloatingInputAreaProps> = fc("NullableFloatingInputArea") { props ->
  val uniqueId = uniqueIdMemo(props.fieldName)

  div("form-floating") {
    nullableInputArea(
      valueAndSetter = props.valueAndSetter,
      fieldName = props.fieldName,
      title = props.title,
      classes = "form-control height-100px",
      editableStatus = props.editableStatus,
    ) {
      attrs {
        id = uniqueId

        //Execute the provided config if there is one
        props.config?.let {
          it(this@nullableInputArea)
        }
      }
    }
    label("form-label") {
      +props.title

      attrs {
        htmlForFixed = uniqueId
      }
    }
  }
}


external interface FloatingInputAreaProps : Props {
  /**
   * The unique ID that is used to connect input area and label
   */
  var id: String

  /**
   * The value and setter for the value.
   * Must be created with a useState hook
   */
  var valueAndSetter: StateInstance<String>

  /**
   * The name of the text area
   */
  var fieldName: String

  /**
   * The title that is shown to the user
   */
  var title: String

  /**
   * The (optional) placeholder - if no placeholder is provided, the title is used
   */
  var placeHolder: String?

  var editableStatus: EditableStatus

  var config: ((RDOMBuilder<TEXTAREA>) -> Unit)?
}

external interface NullableFloatingInputAreaProps : Props {
  /**
   * The unique ID that is used to connect input area and label
   */
  var id: String

  /**
   * The value and setter for the value.
   * Must be created with a useState hook
   */
  var valueAndSetter: StateInstance<String?>

  /**
   * The name of the text area
   */
  var fieldName: String

  /**
   * The title that is shown to the user
   */
  var title: String

  /**
   * The (optional) placeholder - if no placeholder is provided, the title is used
   */
  var placeHolder: String?

  var editableStatus: EditableStatus

  var config: ((RDOMBuilder<TEXTAREA>) -> Unit)?
}
