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

import it.neckar.logging.Logger
import it.neckar.logging.LoggerFactory
import it.neckar.open.kotlin.lang.withNullAtFirst
import it.neckar.react.common.*
import it.neckar.react.common.form.EditableStatus.*
import it.neckar.uuid.HasUuid
import kotlinx.html.SELECT
import kotlinx.html.id
import react.*
import react.dom.*

private val logger: Logger = LoggerFactory.getLogger("it.neckar.react.common.form.FloatingSelect")


/**
 * Creates a floating select field for
 */
fun <T> RBuilder.floatingSelect(
  valueAndSetter: StateInstance<T>,
  idProvider: (T) -> String,
  formatter: (T) -> String,
  availableOptions: List<T>,

  fieldName: String,
  title: String,

  additionalOnChange: ((T) -> Unit)? = null,

  editableStatus: EditableStatus = Editable,

  config: (RDOMBuilder<SELECT>.() -> Unit)? = null,
) {
  floatingSelect(
    selectedValue = valueAndSetter.value,
    onChange = {
      valueAndSetter.setter.invoke(it)
      additionalOnChange?.invoke(it)
    },
    availableOptions = availableOptions,
    formatter = formatter,
    idProvider = idProvider,
    fieldName = fieldName,
    title = title,
    editableStatus = editableStatus,
    config = config
  )
}

/**
 * Creates a floating select field for [HasUuid] elements
 */
fun <T : HasUuid> RBuilder.floatingSelect(
  valueAndSetter: StateInstance<T>,
  formatter: (T) -> String,
  availableOptions: List<T>,

  fieldName: String,
  title: String,

  additionalOnChange: ((T) -> Unit)? = null,

  editableStatus: EditableStatus = Editable,

  config: (RDOMBuilder<SELECT>.() -> Unit)? = null,
) {
  floatingSelect(
    selectedValue = valueAndSetter.value,
    onChange = {
      valueAndSetter.setter.invoke(it)
      additionalOnChange?.invoke(it)
    },
    formatter = formatter,
    availableOptions = availableOptions,
    idProvider = {
      //Do *NOT* use callback since this method may be called from another component within a condition
      it.uuid.toString()
    },
    fieldName = fieldName,
    title = title,
    editableStatus = editableStatus,
    config = config,
  )
}

fun <T> RBuilder.floatingSelectNullable(
  valueAndSetter: StateInstance<T?>,
  idProvider: (T?) -> String,
  formatter: (T?) -> String,
  availableOptionsWithoutNull: List<T>,

  fieldName: String,
  title: String,

  additionalOnChange: ((T?) -> Unit)? = null,

  editableStatus: EditableStatus = Editable,

  config: (RDOMBuilder<SELECT>.() -> Unit)? = null,
) {
  floatingSelectNullable(
    selectedValue = valueAndSetter.value,
    onChange = {
      //Do *NOT* use callback since this method may be called from another component within a condition
      valueAndSetter.setter.invoke(it)
      additionalOnChange?.invoke(it)
    },
    availableOptionsWithoutNull = availableOptionsWithoutNull,
    formatter = formatter,
    idProvider = idProvider,
    fieldName = fieldName,
    title = title,
    editableStatus = editableStatus,
    config = config,
  )
}

fun <T : HasUuid> RBuilder.floatingSelectUuidNullable(
  valueAndSetter: StateInstance<T?>,
  formatter: (T?) -> String,
  availableOptionsWithoutNull: List<T>,

  fieldName: String,
  title: String,

  additionalOnChange: ((T?) -> Unit)? = null,

  editableStatus: EditableStatus = Editable,

  config: (RDOMBuilder<SELECT>.() -> Unit)? = null,
) {
  floatingSelectNullable(
    selectedValue = valueAndSetter.value,
    onChange = {
      //Do *NOT* use callback since this method may be called from another component within a condition
      valueAndSetter.setter(it)
      additionalOnChange?.invoke(it)
    },
    availableOptionsWithoutNull = availableOptionsWithoutNull,
    formatter = formatter,
    idProvider = { it.uuid.toString() },
    fieldName = fieldName,
    title = title,
    editableStatus = editableStatus,
    config = config,
  )
}

fun <E : Enum<E>> RBuilder.floatingSelectEnumNullable(
  valueAndSetter: StateInstance<E?>,
  formatter: (E?) -> String,
  availableOptions: List<E?>,

  fieldName: String,
  title: String,

  additionalOnChange: ((E?) -> Unit)? = null,

  editableStatus: EditableStatus = Editable,

  config: (RDOMBuilder<SELECT>.() -> Unit)? = null,
) {
  floatingSelect(
    selectedValue = valueAndSetter.value,
    onChange = {
      //Do *NOT* use callback since this method may be called from another component within a condition
      valueAndSetter.setter.invoke(it)
      additionalOnChange?.invoke(it)
    },
    availableOptions = availableOptions.toList().withNullAtFirst(),
    formatter = formatter,
    idProvider = {
      //Do *NOT* use callback since this method may be called from another component within a condition
      it?.name ?: "NULL"
    },
    fieldName = fieldName,
    title = title,
    editableStatus = editableStatus,
    config = config,
  )
}

fun <E : Enum<E>> RBuilder.floatingSelectEnum(
  valueAndSetter: StateInstance<E>,
  formatter: (E) -> String,
  availableOptions: List<E>,

  fieldName: String,
  title: String,

  additionalOnChange: ((E) -> Unit)? = null,

  editableStatus: EditableStatus = Editable,

  config: (RDOMBuilder<SELECT>.() -> Unit)? = null,
) {
  floatingSelect(
    selectedValue = valueAndSetter.value,
    onChange = {
      //Do *NOT* use callback since this method may be called from another component within a condition
      valueAndSetter.setter.invoke(it)
      additionalOnChange?.invoke(it)
    },
    availableOptions = availableOptions.toList(),
    formatter = formatter,
    idProvider = {
      //Do *NOT* use callback since this method may be called from another component within a condition
      it.name
    },
    fieldName = fieldName,
    title = title,
    editableStatus = editableStatus,
    config = config,
  )
}

fun <T> RBuilder.floatingSelect(
  selectedValue: T,
  onChange: OnChange<T>,
  availableOptions: List<T>,
  formatter: (T) -> String,

  idProvider: (T) -> String,

  fieldName: String,
  title: String,

  editableStatus: EditableStatus = Editable,

  config: ((RDOMBuilder<SELECT>) -> Unit)? = null,
): Unit = child(FloatingSelect) {
  require(availableOptions.isNotEmpty()) {
    "availableOptions must not be empty for $fieldName ($title)"
  }

  attrs {
    this.selectedValue = selectedValue
    this.onChange = onChange.unsafeCast<OnChange<Any?>>()
    this.formatter = formatter.unsafeCast<(Any?) -> String>()
    this.idProvider = idProvider.unsafeCast<(Any?) -> String>()
    this.availableOptions = availableOptions.unsafeCast<List<Any>>()
    this.fieldName = fieldName
    this.title = title
    this.editableStatus = editableStatus
    this.config = config
  }
}

/**
 * Floating select that supports null values.
 *
 * This method automatically adds null to the available options
 */
fun <T> RBuilder.floatingSelectNullable(
  selectedValue: T?,
  onChange: OnChange<T?>,
  availableOptionsWithoutNull: List<T>,
  formatter: (T?) -> String,

  idProvider: (T) -> String,

  fieldName: String,
  title: String,

  editableStatus: EditableStatus,

  config: ((RDOMBuilder<SELECT>) -> Unit)? = null,
): Unit = child(FloatingSelect) {
  attrs {
    this.selectedValue = selectedValue
    this.onChange = onChange.unsafeCast<OnChange<Any?>>()
    this.formatter = formatter.unsafeCast<(Any?) -> String>()
    this.idProvider = idProvider.unsafeCast<(Any?) -> String>()

    val optionsIncludingNull = availableOptionsWithoutNull.withNullAtFirst()

    this.availableOptions = optionsIncludingNull.unsafeCast<List<Any>>()
    this.fieldName = fieldName
    this.title = title

    this.editableStatus = editableStatus

    this.config = config
  }
}

val FloatingSelect: FC<FloatingSelectProps> = fc("FloatingSelect") { props ->
  val uniqueId = uniqueIdMemo(props.fieldName)

  //Enforce calling onChange if the current value is *not* in available options
  val availableOptions = props.availableOptions
  val selectedValue = if (availableOptions.contains(props.selectedValue)) {
    props.selectedValue
  } else {
    logger.error(buildString {
      appendLine("selected value (${props.selectedValue.toString()}) must be in list of available options.")
      appendLine("Available options:")
      append(availableOptions.joinToString("\n"))
      append("Set value to null!")
    })
    null
  }

  div("form-floating") {
    select(
      selectedValue = selectedValue,
      onChange = props.onChange,
      availableOptions = availableOptions,
      formatter = props.formatter,
      idProvider = props.idProvider,
      fieldName = props.fieldName,
      title = props.title,
      editableStatus = props.editableStatus,
    ) {
      attrs {
        id = uniqueId
      }

      props.config?.invoke(it)
    }

    label("form-label") {
      +props.title

      attrs {
        htmlForFixed = uniqueId
      }
    }
  }
}

external interface FloatingSelectProps : Props {
  /**
   * The selected value
   */
  var selectedValue: Any?
  var formatter: (Any?) -> String

  var onChange: OnChange<Any?>
  var availableOptions: List<Any?>
  var idProvider: (Any?) -> String

  var fieldName: String
  var title: String

  var editableStatus: EditableStatus

  var config: ((RDOMBuilder<SELECT>) -> Unit)?
}
