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
package it.neckar.react.common.bootstrapToast


import it.neckar.react.common.*
import react.*
import react.dom.*
import styled.*
import kotlinx.css.*
import kotlinx.html.classes


fun RBuilder.bootstrapToast(
  title: String,
  toastType: ToastType,
): Unit = child(BootstrapToast) {
  attrs {
    this.title = title
    this.toastType = toastType
  }
}


val BootstrapToast: FC<ToastProps> = fc("bootstrapToast") { props ->
  styledDiv {
    attrs {
      classes = setOf("alert", "d-flex", "align-items-center", "p-2", "my-1")
      when (props.toastType) {
        ToastType.Info -> addClass("alert-info")
        ToastType.Warning -> addClass("alert-warning")
        ToastType.Danger -> addClass("alert-danger")
        ToastType.Success -> addClass("alert-success")
      }
    }
    css {
      width = 250.px
    }

    i(classes = "mx-2") {
      attrs {
        when(props.toastType) {
          ToastType.Info -> addClass("fas fa-info-circle")
          ToastType.Warning -> addClass("fas fa-exclamation-triangle")
          ToastType.Danger -> addClass("fas fa-exclamation-circle")
          ToastType.Success -> addClass("fas fa-check-circle")
        }
      }
    }

    small {
      +props.title
    }
  }
}

enum class ToastType {
  Info,
  Warning,
  Danger,
  Success,
}


external interface ToastProps : Props {
  var title: String
  var toastType: ToastType
}
