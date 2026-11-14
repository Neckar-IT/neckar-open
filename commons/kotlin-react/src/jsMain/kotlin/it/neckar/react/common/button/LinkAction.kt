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
import kotlinx.html.js.onClickFunction
import kotlinx.html.title
import react.*
import react.dom.*

/**
 * Creates a link with callback
 */
fun RBuilder.linkAction(
  icon: (() -> String)? = null,
  label: (() -> String)? = null,
  title: (() -> String)? = null,
  action: suspend () -> Unit,
): Unit = child(LinkAction) {
  require(icon != null || label != null) {
    "At least one of icon or label must be provided"
  }

  attrs {
    this.icon = icon
    this.label = label
    this.action = action
    this.title = title
  }
}

val LinkAction: FC<LinkActionProps> = fc("linkAction") { props ->
  val icon = props::icon.safeGet()?.invoke()
  val action: suspend () -> Unit = props::action.safeGet()
  val label = props::label.safeGet()?.invoke()
  val title = props::title.safeGet()?.invoke()

  a("#") {
    if (icon != null) {
      i("$icon px-1") { }
    }

    if (label != null) {
      +label
    }

    attrs {
      onClickFunction = useCallback(action) {
        //do *not* follow the link
        it.preventDefault()

        AppScope.launch {
          action()
        }
      }

      if (title != null) {
        this.title = title
      }
    }
  }

}

external interface LinkActionProps : Props {
  var icon: (() -> String)?
  var label: (() -> String)?
  var title: (() -> String)?

  var action: suspend () -> Unit
}
