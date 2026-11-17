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
package it.neckar.react.common.upload

import it.neckar.commons.kotlin.js.safeGet
import it.neckar.react.common.*
import kotlinx.css.*
import kotlinx.html.InputType
import kotlinx.html.id
import kotlinx.html.js.onChangeFunction
import kotlinx.html.js.onDragEnterFunction
import kotlinx.html.js.onDragLeaveFunction
import kotlinx.html.js.onDragOverFunction
import kotlinx.html.js.onDropFunction
import org.w3c.dom.events.Event
import react.*
import react.dom.*
import react.dom.events.*
import styled.*
import web.file.File

/**
 * Upload field for files, opens popup when clicked or file is dropped into the field.
 * calls uploadFunction when file is selected
 */
val ImageUploadField: FC<ImageUploadFieldProps> = fc("ImageUploadField") { props ->
  val (dragActive, setDragActive) = useState(false)

  val fileOnChange: (Event) -> Unit = useCallback { event ->
    val file = event.target.asDynamic().files[0] as File
    props::uploadFunction.safeGet()(file)
  }

  val handleDrop: (Event) -> Unit = useCallback { event ->
    event.preventDefault()
    event.stopPropagation()
    setDragActive(false)
    val dataTransfer = (event.unsafeCast<DragEvent<*>>()).dataTransfer
    if (dataTransfer.files.length > 0) {
      val file = dataTransfer.files[0]
      props::uploadFunction.safeGet()(file)
    }
  }

  val handleDrag: (Event) -> Unit = useCallback { event ->
    event.preventDefault()
    event.stopPropagation()
    if (event.type === "dragenter" || event.type === "dragover") {
      setDragActive(true)
    } else if (event.type === "dragleave") {
      setDragActive(false)
    }
  }

  div {
    styledForm {
      css {
        height = props::height.safeGet()
      }

      attrs.onDragEnterFunction = handleDrag
      attrs.onDragLeaveFunction = handleDrag
      attrs.onDragOverFunction = handleDrag
      attrs.onDropFunction = handleDrop

      input(InputType.file, classes = "d-none") {
        attrs.id = "input-file-upload"
        attrs.multiple = false
        attrs {
          onChangeFunction = fileOnChange
        }
        attrs.accept = props::acceptableFileTypes.safeGet().joinToString(",")
      }

      styledLabel {
        attrs.htmlForFixed = "input-file-upload"
        css {
          classes.addAll(listOf("bg-white", "border", "border-2", "h-100", "rounded-3", "d-flex", "justify-content-center", "align-items-center"))
          cursor = Cursor.pointer
        }
        if (dragActive) {
          css {
            classes.removeAt(0)
            classes.add("border-danger")
            classes.add("bg-secondary")
          }
        }

        p("text-dark") {
          +props::fieldText.safeGet()
        }
      }
    }
  }
}

external interface ImageUploadFieldProps : Props {
  var fieldText: String
  var uploadFunction: (File) -> Any
  var height: LinearDimension
  var acceptableFileTypes: List<String>
}
