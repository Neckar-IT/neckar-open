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

import it.neckar.open.kotlin.lang.nextMultipleOf
import it.neckar.open.kotlin.lang.nullIfBlank
import it.neckar.open.unit.si.ms
import it.neckar.react.common.*
import kotlinx.html.INPUT
import kotlinx.html.InputType
import kotlinx.html.js.onChangeFunction
import org.w3c.dom.HTMLInputElement
import react.*
import react.dom.*
import kotlin.js.Date

fun RBuilder.datePicker(
  /**
   * Is state instance to be updated
   */
  dateState: StateInstance<@ms Double>,

  /**
   * Configuration for the input field
   */
  config: (RDOMBuilder<INPUT>.() -> Unit)? = null,

  ): Unit = child(DatePicker) {

  attrs {
    this.dateState = dateState
    this.config = config
  }
}

/**
 * A date picker
 */
val DatePicker: FC<DatePickerProps> = fc("DatePicker") { props ->
  val dateString = Date(props.dateState.value).toDatePickerString()

  input(type = InputType.date, classes = "form-control datepicker") {
    attrs {
      value = dateString
      onChangeFunction = {
        val updatedDate = (it.target as HTMLInputElement).value
        // Automatically update the value in element
        val parsed = Date.parse(updatedDate)
        // An incomplete input parses to NaN, which must never reach the state
        if (parsed.isFinite()) {
          props.dateState.setter(parsed)
        }
      }
    }

    props.config?.invoke(this)
  }

}

fun RBuilder.datePickerNullable(
  /**
   * Is state instance to be updated
   */
  dateState: StateInstance<@ms Double?>,

  /**
   * Configuration for the input field
   */
  config: (RDOMBuilder<INPUT>.() -> Unit)? = null,

  ): Unit = child(DatePickerNullable) {

  attrs {
    this.dateState = dateState
    this.config = config
  }
}

/**
 * A date picker
 */
val DatePickerNullable: FC<DatePickerNullableProps> = fc("DatePickerNullable") { props ->
  val dateString = props.dateState.value.let {
    it?.let { Date(it) }.toDatePickerString()
  }

  input(type = InputType.date, classes = "form-control datepicker") {
    attrs {
      value = dateString
      onChangeFunction = {
        // Automatically update the value in element
        val updatedDate = (it.target as HTMLInputElement).value.nullIfBlank()
        val parsed = updatedDate?.let { Date.parse(it) }
        if (parsed != null && parsed.isFinite()) {
          props.dateState.setter(parsed)
        } else {
          props.dateState.setter(null)
        }
      }
    }

    props.config?.invoke(this)
  }

}


fun RBuilder.timePicker(
  /**
   * Is state instance to be updated
   */
  timeState: StateInstance<@ms Double?>,

  timeStep: Int = 15,

  /**
   * Configuration for the input field
   */
  config: (RDOMBuilder<INPUT>.() -> Unit)? = null,

  ): Unit = child(TimePicker) {

  attrs {
    this.timeState = timeState
    this.timeStep = timeStep
    this.config = config
  }
}

val TimePicker: FC<TimePickerProps> = fc("TimePicker") { props ->
  val timeString = props.timeState.value.let {
    it?.let { Date(it) }.toTimePickerString(props.timeStep)
  }

  input(type = InputType.time, classes = "form-control datepicker") {
    attrs {
      value = timeString
      step = (props.timeStep * 60).toString()
      onChangeFunction = {
        // Automatically update the value in element
        props.timeState.setter((it.target as HTMLInputElement).value.parseTimePickerValue())
      }
    }

    props.config?.invoke(this)
  }

}


fun RBuilder.dateTimePicker(
  /**
   * Is state instance to be updated
   */
  dateTimeState: StateInstance<@ms Double>,

  /**
   * Configuration for the input field
   */
  config: (RDOMBuilder<INPUT>.() -> Unit)? = null,

  ): Unit = child(DateTimePicker) {

  attrs {
    this.dateTimeState = dateTimeState
    this.config = config
  }
}

val DateTimePicker: FC<DateTimePickerProps> = fc("DateTimePicker") { props ->
  val dateTimeString = props.dateTimeState.value.let {
    Date(it).toDateTimePickerString()
  }

  input(type = InputType.dateTimeLocal, classes = "form-control datepicker") {
    attrs {
      value = dateTimeString
      onChangeFunction = {
        val updatedValue = (it.target as HTMLInputElement).value
        // Automatically update the value in element
        props.dateTimeState.setter(Date.parse(updatedValue))
      }
    }

    props.config?.invoke(this)
  }

}


fun RBuilder.dateTimePickerNullable(
  /**
   * Is state instance to be updated
   */
  dateTimeState: StateInstance<@ms Double?>,

  editableStatus: EditableStatus = EditableStatus.Editable,

  /**
   * Configuration for the input field
   */
  config: (RDOMBuilder<INPUT>.() -> Unit)? = null,

  ): Unit = child(DateTimePickerNullable) {

  attrs {
    this.dateTimeState = dateTimeState
    this.editableStatus = editableStatus
    this.config = config
  }
}

val DateTimePickerNullable: FC<DateTimePickerNullableProps> = fc("DateTimePickerNullable") { props ->
  val dateTimeString = props.dateTimeState.value.let {
    it?.let { Date(it) }.toDateTimePickerString()
  }

  input(type = InputType.dateTimeLocal, classes = "form-control datepicker") {
    attrs {
      value = dateTimeString
      onChangeFunction = {
        val updatedValue = (it.target as HTMLInputElement).value
        // Automatically update the value in element
        props.dateTimeState.setter(Date.parse(updatedValue))
      }
      disabled = props.editableStatus == EditableStatus.ReadOnly
    }

    props.config?.invoke(this)
  }

}


fun Date?.toDatePickerString(): String {
  if (this == null) return "yyyy-mm-dd"
  val paddedYear = "${getFullYear()}".padStart(4, '0')
  val paddedMonth = "${getMonth().plus(1)}".padStart(2, '0')
  val paddedDay = "${getDate()}".padStart(2, '0')
  return "$paddedYear-$paddedMonth-$paddedDay"
}

fun Date?.toTimePickerString(timeStep: Int? = null): String {
  if (this == null) return "hh:mm"
  val minutes = getMinutes().let {
    if (timeStep != null) {
      it.nextMultipleOf(timeStep)
    } else {
      it
    }
  }
  val hours = getHours().let {
    if (timeStep != null) {
      it + if (minutes == 60) 1 else 0
    } else {
      it
    }
  }
  val paddedHours = "${hours % 24}".padStart(2, '0')
  val paddedMinutes = "${minutes % 60}".padStart(2, '0')
  return "$paddedHours:$paddedMinutes"
}

/**
 * Parses the value of a time input field. Returns null for an empty or incomplete field - a cleared field clears the state.
 */
fun String.parseTimePickerValue(): @ms Double? {
  val parts = nullIfBlank()?.split(":") ?: return null
  if (parts.size < 2) return null
  val hours = parts[0].toIntOrNull() ?: return null
  val minutes = parts[1].toIntOrNull() ?: return null
  return Date(0, 0, 0, hours, minutes).getTime()
}

fun Date?.toDateTimePickerString(timeStep: Int? = null): String {
  if (this == null) return "yyyy-MM-ddhh:mm"
  return "${toDatePickerString()} ${toTimePickerString(timeStep)}"
}


/**
 * Properties for date picker
 */
external interface DatePickerProps : Props {
  /**
   * The state instance to be changed
   */
  var dateState: StateInstance<@ms Double>

  /**
   * The configuration for the input field
   */
  var config: ((RDOMBuilder<INPUT>) -> Unit)?
}

external interface DatePickerNullableProps : Props {
  /**
   * The state instance to be changed
   */
  var dateState: StateInstance<@ms Double?>

  /**
   * The configuration for the input field
   */
  var config: ((RDOMBuilder<INPUT>) -> Unit)?
}

external interface TimePickerProps : Props {
  /**
   * The state instance to be changed
   */
  var timeState: StateInstance<@ms Double?>

  var timeStep: Int

  /**
   * The configuration for the input field
   */
  var config: ((RDOMBuilder<INPUT>) -> Unit)?
}

external interface DateTimePickerProps : Props {
  /**
   * The state instance to be changed
   */
  var dateTimeState: StateInstance<@ms Double>

  /**
   * The configuration for the input field
   */
  var config: ((RDOMBuilder<INPUT>) -> Unit)?
}

external interface DateTimePickerNullableProps : Props {
  /**
   * The state instance to be changed
   */
  var dateTimeState: StateInstance<@ms Double?>

  var editableStatus: EditableStatus

  /**
   * The configuration for the input field
   */
  var config: ((RDOMBuilder<INPUT>) -> Unit)?
}
