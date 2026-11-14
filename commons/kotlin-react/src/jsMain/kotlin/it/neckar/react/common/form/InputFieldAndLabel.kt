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
import kotlinx.html.INPUT
import kotlinx.html.InputType
import kotlinx.html.id
import react.*
import react.dom.*

fun RBuilder.inputFieldAndLabel(
  /**
   * The current value
   */
  value: String,

  /**
   * Is called on change
   */
  onChange: OnChange<String>,

  fieldName: String,
  title: String,
  placeholder: String? = null,

  editableStatus: EditableStatus,

  config: (RDOMBuilder<INPUT>.() -> Unit)? = null,

  ): Unit = child(InputFieldAndLabel) {
  attrs {
    this.value = value
    this.onChange = onChange
    this.fieldName = fieldName
    this.title = title
    this.placeholder = placeholder
    this.editableStatus = editableStatus
    this.config = config
  }
}

val InputFieldAndLabel: FC<InputFieldAndLabelProps> = fc("InputFieldAndLabel") { props ->

  val uniqueId = uniqueIdMemo(props.fieldName)

  inputField(
    value = props.value,
    onChange = props.onChange,
    fieldName = props.fieldName,
    title = props.title,
    placeholder = props.placeholder,
    classes = "form-control",
    editableStatus = props.editableStatus,
  ) {
    attrs {
      id = uniqueId
      type = InputType.text

      //Execute the provided config if there is one
      props.config?.let {
        it(this@inputField)
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

external interface InputFieldAndLabelProps : Props {
  /**
   * The unique ID that is used to connect input field and label
   */
  var id: String

  /**
   * The current value
   */
  var value: String

  /**
   * Is called on change
   */
  var onChange: OnChange<String>

  /**
   * The name of the field
   */
  var fieldName: String

  /**
   * The title that is shown to the user
   */
  var title: String

  /**
   * The (optional) placeholder - if no placeholder is provided, the title is used
   */
  var placeholder: String?

  var editableStatus: EditableStatus

  var config: ((RDOMBuilder<INPUT>) -> Unit)?
}
