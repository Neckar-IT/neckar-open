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

import it.neckar.commons.kotlin.js.getNotNull
import it.neckar.commons.kotlin.js.safeGet
import it.neckar.open.formatting.format
import it.neckar.open.i18n.I18nConfiguration
import it.neckar.open.kotlin.lang.parseInt
import it.neckar.react.common.*
import it.neckar.react.common.form.EditableStatus.*
import kotlinx.html.DIV
import kotlinx.html.INPUT
import kotlinx.html.InputType
import kotlinx.html.LABEL
import kotlinx.html.TEXTAREA
import kotlinx.html.id
import kotlinx.html.js.onChangeFunction
import kotlinx.html.role
import kotlinx.html.title
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.HTMLTextAreaElement
import react.*
import react.dom.*

fun RBuilder.checkbox(
  /**
   * The current value
   */
  value: Boolean,
  /**
   * Is called on change
   */
  onChange: OnChange<Boolean>,
  /**
   * The field name
   */
  fieldName: String,
  /**
   * The title
   */
  title: String,

  editableStatus: EditableStatus = Editable,

  labelConfig: (RDOMBuilder<LABEL>.() -> Unit)? = null,
  checkboxConfig: (RDOMBuilder<INPUT>.() -> Unit)? = null,

  ): Unit = child(Checkbox) {
  attrs {
    this.value = value
    this.onChange = onChange
    this.fieldName = fieldName
    this.title = title
    this.editableStatus = editableStatus
    this.labelConfig = labelConfig
    this.checkboxConfig = checkboxConfig
  }
}

fun RBuilder.checkbox(
  /**
   * The current value
   */
  valueAndSetter: StateInstance<Boolean>,
  /**
   * The field name
   */
  fieldName: String,
  /**
   * The title
   */
  title: String,

  editableStatus: EditableStatus = Editable,

  labelConfig: (RDOMBuilder<LABEL>.() -> Unit)? = null,
  checkboxConfig: (RDOMBuilder<INPUT>.() -> Unit)? = null,
) {
  checkbox(
    value = valueAndSetter.value,
    onChange = { valueAndSetter.setter.invoke(it) },
    fieldName = fieldName,
    title = title,
    editableStatus = editableStatus,
    labelConfig = labelConfig,
    checkboxConfig = checkboxConfig,
  )
}

val Checkbox: FC<CheckboxProps> = fc("Checkbox") { props ->
  val value = props::value.safeGet()
  val onChange = props::onChange.safeGet()
  val fieldName = props::fieldName.safeGet()
  val title = props::title.safeGet()
  val editableStatus = props::editableStatus.safeGet()
  val labelConfig = props::labelConfig.safeGet()
  val checkboxConfig = props::checkboxConfig.safeGet()

  val uniqueId = uniqueIdMemo("checkbox")


  span {

    label(classes = "form-check-label") {
      attrs {
        htmlForFixed = uniqueId
      }

      input(name = fieldName, type = InputType.checkBox, classes = "form-check-input me-2") {
        attrs {
          this.id = uniqueId

          this.placeholder = title
          this.title = title
          this.checked = value
          // workaround: dependency=libs:kotlinx-html:0.12.0 observed-on=2026-10-07 upstream=https://github.com/Kotlin/kotlinx.html/issues/110
          // The typed `checked` attribute logs a React warning; the raw attribute does not.
          set("checked", value)

          this.disabled = editableStatus == ReadOnly

          this.onChangeFunction = {
            val inputElement = it.target as HTMLInputElement
            onChange(inputElement.checked)
          }
        }

        checkboxConfig?.invoke(this)
      }

      +title

      labelConfig?.invoke(this)
    }

  }
}


external interface CheckboxProps : Props {
  /**
   * The current value
   */
  var value: Boolean

  /**
   * Is called on change
   */
  var onChange: OnChange<Boolean>

  /**
   * The field name
   */
  var fieldName: String

  /**
   * The title
   */
  var title: String

  var editableStatus: EditableStatus

  var labelConfig: ((RDOMBuilder<LABEL>) -> Unit)?
  var checkboxConfig: ((RDOMBuilder<INPUT>) -> Unit)?
}


fun RBuilder.buttonGroup(classes: String? = null, config: (RDOMBuilder<DIV>.() -> Unit)) {
  div(classes = "btn-group") {
    attrs {
      role = "group"
      classes?.let { addClass(it) }
    }

    config(this)
  }
}


fun RBuilder.radioButton(
  buttonId: String,
  /**
   * The current value
   */
  value: Boolean,
  /**
   * Is called on change
   */
  onChange: OnChange<Boolean>,

  editableStatus: EditableStatus = Editable,

  inputClasses: String = "btn-check",
  labelClasses: String = "btn btn-outline-primary",

  inputConfig: (RDOMBuilder<INPUT>.() -> Unit)? = null,
  labelConfig: (RDOMBuilder<LABEL>.() -> Unit)?,

  ): Unit = child(RadioButton) {
  attrs {
    this.buttonId = buttonId
    this.value = value
    this.onChange = onChange
    this.editableStatus = editableStatus
    this.inputClasses = inputClasses
    this.labelClasses = labelClasses
    this.inputConfig = inputConfig
    this.labelConfig = labelConfig
  }
}

fun RBuilder.radioButton(
  buttonId: String,
  /**
   * The current value
   */
  valueAndSetter: StateInstance<Boolean>,

  editableStatus: EditableStatus = Editable,

  inputClasses: String = "btn-check",
  labelClasses: String = "btn btn-outline-primary",

  inputConfig: (RDOMBuilder<INPUT>.() -> Unit)? = null,
  labelConfig: (RDOMBuilder<LABEL>.() -> Unit)?,

  ) {
  radioButton(
    buttonId = buttonId,
    value = valueAndSetter.value,
    onChange = { valueAndSetter.setter.invoke(it) },
    editableStatus = editableStatus,
    inputClasses = inputClasses,
    labelClasses = labelClasses,
    inputConfig = inputConfig,
    labelConfig = labelConfig,
  )
}

val RadioButton: FC<RadioButtonProps> = fc("RadioButton") { props ->
  val buttonId = props::buttonId.safeGet()
  val value = props::value.safeGet()
  val onChange = props::onChange.safeGet()
  val editableStatus = props::editableStatus.safeGet()
  val inputClasses = props::inputClasses.safeGet()
  val labelClasses = props::labelClasses.safeGet()
  val inputConfig = props::inputConfig.safeGet()
  val labelConfig = props::labelConfig.safeGet()


  input(type = InputType.radio, classes = inputClasses) {
    attrs {
      id = buttonId
      autoComplete = "off"

      // workaround: dependency=libs:kotlinx-html:0.12.0 observed-on=2026-10-07 upstream=https://github.com/Kotlin/kotlinx.html/issues/110
      // The typed `checked` attribute logs a React warning; the raw attribute does not.
      set("checked", value)

      disabled = editableStatus == ReadOnly

      onChangeFunction = {
        val inputElement = it.target as HTMLInputElement
        onChange(inputElement.checked)
      }
    }

    inputConfig?.invoke(this)
  }
  label(classes = labelClasses) {
    attrs {
      htmlForFixed = buttonId
    }

    labelConfig?.invoke(this)
  }

}

external interface RadioButtonProps : Props {
  var buttonId: String

  /**
   * The current value
   */
  var value: Boolean

  /**
   * Is called on change
   */
  var onChange: OnChange<Boolean>

  var editableStatus: EditableStatus

  var inputClasses: String
  var labelClasses: String
  var inputConfig: ((RDOMBuilder<INPUT>) -> Unit)?
  var labelConfig: ((RDOMBuilder<LABEL>) -> Unit)?
}


/**
 * Creates an input field. The value and updates are handled using a [StateInstance]
 */
fun RBuilder.inputField(
  /**
   * The value and setter for the value.
   * Must be created with a useState hook
   */
  valueAndSetter: StateInstance<String>,

  fieldName: String,
  title: String,

  /**
   * The (optional) placeholder - if no placeholder is provided, the title is used
   */
  placeholder: String? = null,

  editableStatus: EditableStatus = Editable,

  config: (RDOMBuilder<INPUT>.() -> Unit)? = null,

  ) {
  inputField(
    value = valueAndSetter.value,
    onChange = { valueAndSetter.setter.invoke(it) },
    fieldName = fieldName,
    title = title,
    placeholder = placeholder,
    editableStatus = editableStatus,
    config = config,
  )
}

fun RBuilder.intInput(
  /**
   * The value and setter for the value.
   * Must be created with a useState hook
   */
  valueAndSetter: StateInstance<Int>,
  fieldName: String,
  title: String,
  editableStatus: EditableStatus = Editable,
  config: (RDOMBuilder<INPUT>.() -> Unit)? = null,
) {
  intInput(
    value = valueAndSetter.value,
    onChange = { valueAndSetter.setter.invoke(it) },
    fieldName = fieldName,
    title = title,
    numberConstraint = Unconstrained,
    editableStatus = editableStatus,
    config = config,
  )
}

fun RBuilder.intInput(
  /**
   * The value and setter for the value.
   * Must be created with a useState hook
   */
  value: Int,
  onChange: (Int) -> Unit,
  fieldName: String,
  title: String,
  numberConstraint: NumberConstraint,
  editableStatus: EditableStatus = Editable,
  config: (RDOMBuilder<INPUT>.() -> Unit)? = null,
) {
  inputField(
    value = value.toString(),
    onChange = {
      if (it.isBlank()) {
        onChange.invoke(0) // If the input is blank, we set it to 0
      } else {
        onChange.invoke(numberConstraint.constraint(it.parseInt()))
      }
    },
    fieldName = fieldName,
    title = title,
    editableStatus = editableStatus,
  ) {
    attrs {
      type = InputType.number
    }

    configure(numberConstraint)
    config?.invoke(this)
  }
}

/**
 * Input for a double value
 */
fun RBuilder.doubleInput(
  /**
   * The value and setter for the value.
   * Must be created with a useState hook
   */
  valueAndSetter: StateInstance<Double>,
  fieldName: String,
  title: String,
  numberOfDecimals: Int = 2,
  placeholder: String? = null,
  numberConstraint: NumberConstraint,
  editableStatus: EditableStatus = Editable,

  config: (RDOMBuilder<INPUT>.() -> Unit)? = null,
) {
  inputField(
    value = valueAndSetter.value.format(numberOfDecimals, false, I18nConfiguration.US),
    onChange = valueAndSetter.asOnChangeForDouble(numberConstraint),
    fieldName = fieldName,
    title = title,
    placeholder = placeholder,
    editableStatus = editableStatus,
  ) {
    attrs {
      type = InputType.number
    }

    configure(numberConstraint)
    config?.invoke(this)
  }
}

/**
 * Creates a password field backed by a [StateInstance]
 */
fun RBuilder.passwordField(
  valueAndSetter: StateInstance<String>,
  fieldName: String,
  title: String,
  placeholder: String? = null,
  editableStatus: EditableStatus = Editable,

  config: (RDOMBuilder<INPUT>.() -> Unit)? = null,
) {
  inputField(
    value = valueAndSetter.value,
    onChange = { valueAndSetter.setter.invoke(it) },
    fieldName = fieldName,
    title = title,
    placeholder = placeholder,
    editableStatus = editableStatus,
    config = {
      attrs {
        type = InputType.password
      }
      config?.invoke(this)
    })
}

/**
 * Creates the input field
 */
fun RBuilder.inputField(
  /**
   * The current value
   */
  value: String,
  /**
   * Is called on change
   */
  onChange: OnChange<String>,
  /**
   * The field name
   */
  fieldName: String,
  /**
   * The title
   */
  title: String,

  /**
   * The (optional) placeholder.
   * If no placeholder is present, the title is used instead
   */
  placeholder: String? = null,

  /**
   * The classes for the input field
   */
  classes: String = "form-control",

  editableStatus: EditableStatus = Editable,

  /**
   * Configuration for the input field
   */
  config: (RDOMBuilder<INPUT>.() -> Unit)?,

  ): Unit = child(InputField) {

  //ensureSameCallback(onChange, fieldName)

  attrs {
    require(value != undefined) {
      "Value must not be undefined but was <$value>"
    }

    this.value = value
    this.onChange = onChange
    this.title = title
    this.fieldName = fieldName
    this.placeholder = placeholder
    this.classes = classes
    this.editableStatus = editableStatus
    this.config = config as ((RDOMBuilder<*>) -> Unit)?
  }
}

/**
 * an input field
 */
val InputField: FC<InputFieldProps> = fc("InputField") { props ->
  val value = props::value.safeGet()
  val onChange = props::onChange.safeGet()
  val title = props::title.safeGet()
  val fieldName = props::fieldName.safeGet()
  val placeholder = props::placeholder.safeGet()
  val editableStatus = props::editableStatus.safeGet()

  /**
   * We wrap the value into a separate [StateInstance] as the input and the parsed result of [onChange] may divert
   *   e.g. in a double input, "-" is not a valid double value, but it is a valid input value
   *   once the input is completed to e.g. -1.0, the value is parsed to -1.0
   *   without the separate state instance, the input of "-" would be lost during parsing and the input would be reset
   */
  val valueState = useState(value)

  useEffect(value) {
    valueState.setter(value)
  }

  input(name = fieldName, classes = props.classes) {

    attrs {
      this.placeholder = placeholder ?: title
      this.title = title

      //Always assign the value in element
      this.value = valueState.value

      onChangeFunction = {
        val updatedValue = (it.target as HTMLInputElement).value

        //Notify the change listener - this *may* result in model changes, but sometimes does *not*
        valueState.setter(updatedValue)
        onChange(updatedValue)
      }

      disabled = editableStatus == ReadOnly
    }

    props.config?.invoke(this)
  }

}

/**
 * Creates an input area
 */
fun RBuilder.inputArea(
  /**
   * The value and setter for the value.
   * Must be created with a useState hook
   */
  valueAndSetter: StateInstance<String>,

  fieldName: String,
  title: String,

  /**
   * The (optional) placeholder - if no placeholder is provided, the title is used
   */
  placeHolder: String? = null,

  /**
   * The classes for the input field
   */
  classes: String = "form-control",

  editableStatus: EditableStatus = Editable,

  config: (RDOMBuilder<TEXTAREA>.() -> Unit)? = null,

  ) {
  child(InputArea) {
    attrs {
      this.value = valueAndSetter.value
      this.onChange = { valueAndSetter.setter.invoke(it) }
      this.title = title
      this.fieldName = fieldName
      this.placeholder = placeHolder
      this.classes = classes
      this.editableStatus = editableStatus
      this.config = config as ((RDOMBuilder<*>) -> Unit)?
    }
  }
}

/**
 * Creates a text area
 */
val InputArea: FC<InputAreaProps> = fc("InputArea") { props ->
  textarea(classes = "form-control") {
    attrs {
      name = props::fieldName.safeGet()

      placeholder = props.placeholder ?: props::title.safeGet()
      title = props::title.safeGet()
      value = props::value.safeGet()

      val onChange = props::onChange.safeGet()
      onChangeFunction = { event ->
        val element = event.target as HTMLTextAreaElement
        onChange(element.value)
      }

      onKeyPress = { keyboardEvent ->
        keyboardEvent.stopPropagation()
      }

      if (props::editableStatus.safeGet() == ReadOnly) {
        disabled = true
      }
    }

    props.config?.invoke(this)
  }
}

/**
 * Properties for text input
 */
external interface InputFieldProps : Props {
  /**
   * The (optional) classes
   */
  var classes: String?

  /**
   * The initial value for the field
   */
  var value: String

  /**
   * The name of the field
   */
  var fieldName: String

  /**
   * The setter that is called when the value has changed
   */
  var onChange: OnChange<String>

  /**
   * The title
   */
  var title: String

  /**
   * The (optional) placeholder.
   * If no placeholder is set, the title is used instead
   */
  var placeholder: String?

  var editableStatus: EditableStatus

  /**
   * The configuration for the input field
   */
  var config: ((RDOMBuilder<INPUT>) -> Unit)?
}

external interface InputAreaProps : Props {
  /**
   * The (optional) classes
   */
  var classes: String?

  /**
   * The initial value for the field
   */
  var value: String

  /**
   * The name of the field
   */
  var fieldName: String

  /**
   * The setter that is called when the value has changed
   */
  var onChange: OnChange<String>

  /**
   * The title
   */
  var title: String

  /**
   * The (optional) placeholder.
   * If no placeholder is set, the title is used instead
   */
  var placeholder: String?

  var editableStatus: EditableStatus

  /**
   * The configuration for the input field
   */
  var config: ((RDOMBuilder<TEXTAREA>) -> Unit)?
}


/**
 * Creates the input field
 */
fun RBuilder.nullableInputField(
  /**
   * The current value
   */
  value: String?,
  /**
   * Is called on change
   */
  onChange: OnChange<String?>,
  /**
   * The field name
   */
  fieldName: String,
  /**
   * The title
   */
  title: String,

  /**
   * The (optional) placeholder.
   * If no placeholder is present, the title is used instead
   */
  placeholder: String? = null,

  /**
   * The classes for the input field
   */
  classes: String = "form-control",

  editableStatus: EditableStatus = Editable,

  /**
   * Configuration for the input field
   */
  config: (RDOMBuilder<INPUT>.() -> Unit)? = null,

  ): Unit = child(NullableInputField) {

  ensureSameCallback(onChange, fieldName)

  attrs {
    require(value == null || value != undefined) {
      "Value must not be undefined but was <$value>"
    }

    this.value = value
    this.onChange = onChange
    this.title = title
    this.fieldName = fieldName
    this.placeholder = placeholder
    this.classes = classes
    this.editableStatus = editableStatus
    this.config = config
  }
}

/**
 * an input field
 */
val NullableInputField: FC<NullableInputFieldProps> = fc("NullableInputField") { props ->
  val classes = props::classes.safeGet()
  val fieldName = props::fieldName.getNotNull()
  val placeholder = props::placeholder.safeGet()
  val title = props::title.getNotNull()
  val value = props::value.safeGet()
  val onChange = props::onChange.getNotNull()
  val editableStatus = props::editableStatus.getNotNull()
  val config = props::config.safeGet()

  /**
   * We wrap the value into a separate [StateInstance] as the input and the parsed result of [onChange] may divert
   *   e.g. in a nullable double input, "-" is not a valid double value, but it is a valid input value
   *   once the input is completed to e.g. -1.0, the value is parsed to -1.0
   *   without the separate state instance, the input of "-" would be lost during parsing and the input would be reset
   */
  val valueState = useState(value)

  useEffect(value) {
    valueState.setter(value)
  }

  input(name = fieldName, classes = classes) {

    attrs {
      this.placeholder = placeholder ?: title
      this.title = title

      //Always assign the value in element
      valueState.value?.let { this.value = it }

      onChangeFunction = {
        val updatedValue = (it.target as HTMLInputElement).value

        //Notify the change listener - this *may* result in model changes, but sometimes does *not*
        valueState.setter(updatedValue)
        onChange(updatedValue)
      }

      if (editableStatus == ReadOnly) this.disabled = true
    }

    config?.invoke(this)
  }

}

fun RBuilder.nullableInputArea(
  /**
   * The value and setter for the value.
   * Must be created with a useState hook
   */
  valueAndSetter: StateInstance<String?>,

  fieldName: String,
  title: String,

  /**
   * The (optional) placeholder - if no placeholder is provided, the title is used
   */
  placeHolder: String? = null,

  /**
   * The classes for the input field
   */
  classes: String = "form-control",

  editableStatus: EditableStatus = Editable,

  config: (RDOMBuilder<TEXTAREA>.() -> Unit)? = null,

  ) {
  child(NullableInputArea) {
    attrs {
      this.value = valueAndSetter.value
      this.onChange = { valueAndSetter.setter.invoke(it) }
      this.title = title
      this.fieldName = fieldName
      this.placeholder = placeHolder
      this.classes = classes
      this.editableStatus = editableStatus
      this.config = config
    }
  }
}

/**
 * Creates a text area
 */
val NullableInputArea: FC<NullableInputAreaProps> = fc("NullableInputArea") { props ->
  val classes = props::classes.safeGet()
  val fieldName = props::fieldName.safeGet()
  val placeholder = props::placeholder.safeGet()
  val title = props::title.safeGet()
  val value = props::value.safeGet()
  val onChange = props::onChange.safeGet()
  val editableStatus = props::editableStatus.safeGet()
  val config = props::config.safeGet()


  textarea(classes = classes) {

    attrs {
      this.name = fieldName

      this.placeholder = placeholder ?: title
      this.title = title

      //Always assign the value in element
      value?.let { this.value = it }

      this.onChangeFunction = {
        val updatedValue = (it.target as HTMLTextAreaElement).value

        //Notify the change listener - this *may* result in model changes, but sometimes does *not*
        onChange(updatedValue)
      }

      if (editableStatus == ReadOnly) this.disabled = true
    }

    config?.invoke(this)
  }

}

/**
 * Properties for nullable text input
 */
external interface NullableInputFieldProps : Props {
  /**
   * The (optional) classes
   */
  var classes: String?

  /**
   * The initial value for the field
   */
  var value: String?

  /**
   * The name of the field
   */
  var fieldName: String

  /**
   * The setter that is called when the value has changed
   */
  var onChange: OnChange<String?>

  /**
   * The title
   */
  var title: String

  /**
   * The (optional) placeholder.
   * If no placeholder is set, the title is used instead
   */
  var placeholder: String?

  var editableStatus: EditableStatus

  /**
   * The configuration for the input field
   */
  var config: ((RDOMBuilder<INPUT>) -> Unit)?
}

external interface NullableInputAreaProps : Props {
  /**
   * The (optional) classes
   */
  var classes: String?

  /**
   * The initial value for the field
   */
  var value: String?

  /**
   * The name of the field
   */
  var fieldName: String

  /**
   * The setter that is called when the value has changed
   */
  var onChange: OnChange<String?>

  /**
   * The title
   */
  var title: String

  /**
   * The (optional) placeholder.
   * If no placeholder is set, the title is used instead
   */
  var placeholder: String?

  var editableStatus: EditableStatus

  /**
   * The configuration for the input field
   */
  var config: ((RDOMBuilder<TEXTAREA>) -> Unit)?
}


/**
 * Configure the input for the given number constraint
 */
fun RDOMBuilder<INPUT>.configure(numberConstraint: NumberConstraint) {
  when (numberConstraint) {
    ZeroOrPositive -> {
      zeroOrPositiveValues()
    }

    is CoerceIn -> {
      attrs {
        min = "${numberConstraint.lowerLimit}"
        max = "${numberConstraint.upperLimit}"
      }
    }

    else -> {}
  }
}

/**
 * Sets the min value to "0"
 */
fun INPUT.zeroOrPositiveValues() {
  min = "0"
}

/**
 * Only support positive values
 */
fun RDOMBuilder<INPUT>.zeroOrPositiveValues() {
  attrs {
    zeroOrPositiveValues()
  }
}

